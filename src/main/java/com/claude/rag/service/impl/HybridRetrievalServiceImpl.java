package com.claude.rag.service.impl;

import com.claude.rag.model.Document;
import com.claude.rag.model.RetrievalStrategy;
import com.claude.rag.service.RetrievalService;
import com.claude.rag.service.VectorStoreService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索服务实现
 * 结合语义检索和关键词检索（BM25）
 */
@Slf4j
@Service
public class HybridRetrievalServiceImpl implements RetrievalService {

    private final VectorStoreService vectorStoreService;
    private final SemanticRetrievalServiceImpl semanticRetrievalService;

    /**
     * 语义检索权重
     */
    private static final double SEMANTIC_WEIGHT = 0.7;

    /**
     * 关键词检索权重
     */
    private static final double KEYWORD_WEIGHT = 0.3;

    public HybridRetrievalServiceImpl(VectorStoreService vectorStoreService,
                                       SemanticRetrievalServiceImpl semanticRetrievalService) {
        this.vectorStoreService = vectorStoreService;
        this.semanticRetrievalService = semanticRetrievalService;
    }

    @Override
    public List<Document> retrieve(String query, int topK, Map<String, Object> params) {
        log.info("执行混合检索，查询: {}, topK: {}", query, topK);

        try {
            // 1. 执行语义检索
            List<Document> semanticResults = semanticRetrievalService.retrieve(query, topK, params);

            // 2. 执行关键词检索（BM25）
            List<Document> keywordResults = keywordSearch(query, semanticResults);

            // 3. 融合两种检索结果
            List<Document> mergedResults = mergeResults(semanticResults, keywordResults);

            // 4. 返回topK结果
            List<Document> results = mergedResults.stream()
                    .limit(topK)
                    .collect(Collectors.toList());

            log.info("混合检索完成，语义检索 {} 个，关键词检索 {} 个，融合后 {} 个",
                    semanticResults.size(), keywordResults.size(), results.size());

            return results;
        } catch (Exception e) {
            log.error("混合检索失败: {}", e.getMessage(), e);
            throw new RuntimeException("混合检索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 关键词搜索（简化版BM25）
     * 在已有文档中根据关键词匹配进行评分
     */
    private List<Document> keywordSearch(String query, List<Document> documents) {
        // 提取查询关键词
        List<String> queryKeywords = extractKeywords(query);

        // 为每个文档计算BM25分数
        Map<String, Double> keywordScores = new HashMap<>();

        for (Document doc : documents) {
            double score = calculateBM25Score(queryKeywords, doc);
            keywordScores.put(doc.getId(), score);
        }

        // 按分数排序
        return documents.stream()
                .map(doc -> {
                    Document scoredDoc = Document.builder()
                            .id(doc.getId())
                            .content(doc.getContent())
                            .metadata(doc.getMetadata())
                            .embedding(doc.getEmbedding())
                            .score(keywordScores.get(doc.getId()))
                            .createdAt(doc.getCreatedAt())
                            .updatedAt(doc.getUpdatedAt())
                            .status(doc.getStatus())
                            .build();
                    return scoredDoc;
                })
                .sorted((d1, d2) -> Double.compare(d2.getScore(), d1.getScore()))
                .collect(Collectors.toList());
    }

    /**
     * 提取关键词（简单分词）
     */
    private List<String> extractKeywords(String text) {
        // 简化版：按空格和标点符号分割
        // 实际项目中可以使用更复杂的分词器（如IK、HanLP等）
        return Arrays.stream(text.toLowerCase()
                        .split("[\\s,.!?;:\"'()\\[\\]{}]+"))
                .filter(StringUtils::isNotBlank)
                .filter(word -> word.length() > 1) // 过滤单字符
                .collect(Collectors.toList());
    }

    /**
     * 计算BM25分数（简化版）
     */
    private double calculateBM25Score(List<String> queryKeywords, Document document) {
        String content = document.getContent().toLowerCase();

        double score = 0.0;

        for (String keyword : queryKeywords) {
            // 计算关键词在文档中的出现次数
            int termFrequency = countOccurrences(content, keyword);

            if (termFrequency > 0) {
                // 简化版BM25：不包含文档长度归一化
                score += termFrequency * Math.log((document.getContent().length() + 1.0) / (termFrequency + 0.5));
            }
        }

        return score;
    }

    /**
     * 计算字符串中子串的出现次数
     */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;

        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }

        return count;
    }

    /**
     * 融合两种检索结果
     * 使用加权平均的分数
     */
    private List<Document> mergeResults(List<Document> semanticResults, List<Document> keywordResults) {
        // 创建文档ID到结果的映射
        Map<String, Document> resultMap = new LinkedHashMap<>();

        // 处理语义检索结果
        for (int i = 0; i < semanticResults.size(); i++) {
            Document doc = semanticResults.get(i);
            // 使用位置归一化分数（排名越靠前分数越高）
            double normalizedScore = 1.0 - (i * 1.0 / semanticResults.size());
            doc.setScore(normalizedScore * SEMANTIC_WEIGHT);
            resultMap.put(doc.getId(), doc);
        }

        // 处理关键词检索结果
        for (int i = 0; i < keywordResults.size(); i++) {
            Document doc = keywordResults.get(i);
            if (resultMap.containsKey(doc.getId())) {
                // 文档已存在，累加分数
                Document existingDoc = resultMap.get(doc.getId());
                double keywordScore = doc.getScore();
                if (keywordScore > 0) {
                    // 归一化关键词分数
                    double maxKeywordScore = keywordResults.get(0).getScore();
                    double normalizedKeywordScore = keywordScore / maxKeywordScore;
                    existingDoc.setScore(existingDoc.getScore() + normalizedKeywordScore * KEYWORD_WEIGHT);
                }
            } else {
                // 新文档，只使用关键词分数
                double keywordScore = doc.getScore();
                if (keywordScore > 0) {
                    double maxKeywordScore = keywordResults.get(0).getScore();
                    double normalizedKeywordScore = keywordScore / maxKeywordScore;
                    doc.setScore(normalizedKeywordScore * KEYWORD_WEIGHT);
                    resultMap.put(doc.getId(), doc);
                }
            }
        }

        // 按融合分数排序
        return resultMap.values().stream()
                .sorted((d1, d2) -> Double.compare(d2.getScore(), d1.getScore()))
                .collect(Collectors.toList());
    }

    @Override
    public String getStrategyName() {
        return RetrievalStrategy.HYBRID.name();
    }
}
