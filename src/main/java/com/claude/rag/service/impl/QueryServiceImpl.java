package com.claude.rag.service.impl;

import com.claude.rag.model.Document;
import com.claude.rag.model.RetrievalStrategy;
import com.claude.rag.model.SearchRequest;
import com.claude.rag.service.QueryService;
import com.claude.rag.service.RetrievalService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询服务实现
 * 整合检索和生成，提供端到端的问答功能
 */
@Slf4j
@Service
public class QueryServiceImpl implements QueryService {

    private final ChatLanguageModel chatLanguageModel;
    private final Map<String, RetrievalService> retrievalServiceMap;

    @Autowired
    public QueryServiceImpl(ChatLanguageModel chatLanguageModel,
                           List<RetrievalService> retrievalServices) {
        this.chatLanguageModel = chatLanguageModel;
        this.retrievalServiceMap = new HashMap<>();

        // 注册所有检索服务
        for (RetrievalService service : retrievalServices) {
            retrievalServiceMap.put(service.getStrategyName().toUpperCase(), service);
            log.info("注册检索服务: {}", service.getStrategyName());
        }
    }

    @Override
    public String query(SearchRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 根据策略选择检索服务
            RetrievalService retrievalService = getRetrievalService(request.getStrategy());

            // 2. 检索相关文档
            List<Document> documents = retrievalService.retrieve(
                    request.getQuery(),
                    request.getTopK(),
                    request.getParams()
            );

            if (documents.isEmpty()) {
                log.warn("未检索到相关文档");
                return "抱歉，未找到相关信息。";
            }

            log.info("检索到 {} 个文档", documents.size());

            // 3. 生成答案
            String answer = generateAnswer(request.getQuery(), documents);

            long endTime = System.currentTimeMillis();
            log.info("查询完成，耗时: {}ms", endTime - startTime);

            return answer;

        } catch (Exception e) {
            log.error("查询失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Document> retrieveOnly(SearchRequest request) {
        try {
            RetrievalService retrievalService = getRetrievalService(request.getStrategy());
            return retrievalService.retrieve(
                    request.getQuery(),
                    request.getTopK(),
                    request.getParams()
            );
        } catch (Exception e) {
            log.error("检索失败: {}", e.getMessage(), e);
            throw new RuntimeException("检索失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateAnswer(String query, List<Document> documents) {
        try {
            // 构建Prompt
            String prompt = buildPrompt(query, documents);

            // 调用LLM生成答案
            return chatLanguageModel.generate(prompt);

        } catch (Exception e) {
            log.error("生成答案失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成答案失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String buildPrompt(String query, List<Document> documents) {
        StringBuilder prompt = new StringBuilder();

        // 系统提示
        prompt.append("你是一个有帮助的助手。请根据以下参考信息回答用户的问题。\n");
        prompt.append("如果参考信息中没有相关内容，请明确说明。\n");
        prompt.append("请用中文回答，保持简洁明了。\n\n");

        // 参考信息
        prompt.append("参考信息:\n");
        prompt.append("---\n");
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            prompt.append(String.format("[文档%d]\n%s\n", i + 1, doc.getContent()));
            prompt.append("---\n");
        }
        prompt.append("\n");

        // 用户问题
        prompt.append("用户问题: ").append(query).append("\n\n");

        // 回答要求
        prompt.append("请根据上述参考信息回答用户问题。");

        return prompt.toString();
    }

    /**
     * 根据策略获取对应的检索服务
     */
    private RetrievalService getRetrievalService(RetrievalStrategy strategy) {
        String strategyName = strategy.name().toUpperCase();
        RetrievalService service = retrievalServiceMap.get(strategyName);

        if (service == null) {
            log.warn("未找到策略 {} 对应的检索服务，使用默认策略", strategyName);
            // 默认使用语义检索
            service = retrievalServiceMap.get(RetrievalStrategy.SEMANTIC.name());
        }

        return service;
    }
}
