package com.claude.rag.service.impl;

import com.claude.rag.model.Document;
import com.claude.rag.model.RetrievalStrategy;
import com.claude.rag.service.RetrievalService;
import com.claude.rag.service.VectorStoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 语义检索服务实现
 * 基于向量相似度的纯语义检索
 */
@Slf4j
@Service
public class SemanticRetrievalServiceImpl implements RetrievalService {

    private final VectorStoreService vectorStoreService;

    public SemanticRetrievalServiceImpl(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    @Override
    public List<Document> retrieve(String query, int topK, Map<String, Object> params) {
        log.info("执行语义检索，查询: {}, topK: {}", query, topK);

        try {
            // 执行向量检索
            List<Document> documents = vectorStoreService.search(query, topK);

            log.info("语义检索完成，检索到 {} 个文档", documents.size());
            return documents;
        } catch (Exception e) {
            log.error("语义检索失败: {}", e.getMessage(), e);
            throw new RuntimeException("语义检索失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getStrategyName() {
        return RetrievalStrategy.SEMANTIC.name();
    }
}
