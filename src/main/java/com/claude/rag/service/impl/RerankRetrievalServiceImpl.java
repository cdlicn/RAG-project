package com.claude.rag.service.impl;

import com.claude.rag.model.Document;
import com.claude.rag.model.RetrievalStrategy;
import com.claude.rag.service.RetrievalService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 重排序检索服务实现
 * 初步检索后使用模型进行二次重排序
 */
@Slf4j
@Service
public class RerankRetrievalServiceImpl implements RetrievalService {

    private final SemanticRetrievalServiceImpl semanticRetrievalService;
    private final ChatLanguageModel chatLanguageModel;

    /**
     * 初步检索的倍数
     * 初步检索topK * rerankRatio个文档，然后重排序
     */
    @Value("${rag.rerank.ratio:3}")
    private int rerankRatio;

    public RerankRetrievalServiceImpl(SemanticRetrievalServiceImpl semanticRetrievalService,
                                       ChatLanguageModel chatLanguageModel) {
        this.semanticRetrievalService = semanticRetrievalService;
        this.chatLanguageModel = chatLanguageModel;
    }

    @Override
    public List<Document> retrieve(String query, int topK, Map<String, Object> params) {
        log.info("执行重排序检索，查询: {}, topK: {}", query, topK);

        try {
            // 1. 初步检索：检索更多文档
            int initialTopK = topK * rerankRatio;
            List<Document> initialResults = semanticRetrievalService.retrieve(query, initialTopK, params);

            if (initialResults.isEmpty()) {
                log.info("初步检索未找到文档");
                return List.of();
            }

            log.info("初步检索完成，检索到 {} 个文档，开始重排序", initialResults.size());

            // 2. 使用模型进行重排序
            List<Document> rerankedResults = rerankDocuments(query, initialResults);

            // 3. 返回topK结果
            List<Document> results = rerankedResults.stream()
                    .limit(topK)
                    .collect(Collectors.toList());

            log.info("重排序完成，返回 {} 个文档", results.size());

            return results;
        } catch (Exception e) {
            log.error("重排序检索失败: {}", e.getMessage(), e);
            throw new RuntimeException("重排序检索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用模型对文档进行重排序
     * 让模型评估每个文档与查询的相关性
     */
    private List<Document> rerankDocuments(String query, List<Document> documents) {
        String prompt = buildRerankPrompt(query, documents);

        try {
            String response = chatLanguageModel.generate(prompt);

            // 解析模型返回的相关性评分
            List<Double> scores = parseScores(response);

            // 应用新的分数并排序
            if (scores.size() == documents.size()) {
                for (int i = 0; i < documents.size(); i++) {
                    documents.get(i).setScore(scores.get(i));
                }
            }

            return documents.stream()
                    .sorted((d1, d2) -> Double.compare(d2.getScore(), d1.getScore()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("模型重排序失败，使用原始排序: {}", e.getMessage());
            // 重排序失败，返回原始结果
            return documents;
        }
    }

    /**
     * 构建重排序的Prompt
     */
    private String buildRerankPrompt(String query, List<Document> documents) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个文档相关性评估专家。请评估以下文档与查询的相关性。\n\n");
        prompt.append("查询: ").append(query).append("\n\n");
        prompt.append("请为每个文档打分（0-10分），分数越高表示相关性越强。\n\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            prompt.append(String.format("文档%d:\n%s\n\n", i + 1, doc.getContent()));
        }

        prompt.append("\n请只返回每个文档的分数，格式如下：\n");
        prompt.append("文档1: 8\n");
        prompt.append("文档2: 6\n");
        prompt.append("...\n");
        prompt.append("每行一个分数，按文档顺序排列。");

        return prompt.toString();
    }

    /**
     * 解析模型返回的分数
     */
    private List<Double> parseScores(String response) {
        return response.lines()
                .filter(line -> line.matches(".*\\d+.*"))
                .map(line -> {
                    try {
                        // 提取数字
                        String numberStr = line.replaceAll("[^0-9.]", "");
                        if (!numberStr.isEmpty()) {
                            double score = Double.parseDouble(numberStr);
                            return Math.min(10.0, Math.max(0.0, score)); // 限制在0-10之间
                        }
                    } catch (NumberFormatException e) {
                        log.warn("解析分数失败: {}", line);
                    }
                    return 0.0;
                })
                .collect(Collectors.toList());
    }

    @Override
    public String getStrategyName() {
        return RetrievalStrategy.RERANK.name();
    }
}
