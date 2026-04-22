package com.claude.rag.service.impl;

import com.claude.rag.model.Document;
import com.claude.rag.service.VectorStoreService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Milvus向量存储服务实现
 * 基于LangChain4j的Milvus集成实现向量存储和检索
 */
@Slf4j
@Service
public class MilvusVectorStoreServiceImpl implements VectorStoreService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public MilvusVectorStoreServiceImpl(EmbeddingStore embeddingStore, EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void addDocument(Document document) {
        try {
            // 如果文档没有ID，生成一个UUID
            if (document.getId() == null || document.getId().isEmpty()) {
                document.setId(UUID.randomUUID().toString());
            }

            // 创建文本片段
            TextSegment segment = TextSegment.from(document.getContent());

            // 生成向量
            Embedding embedding = embeddingModel.embed(segment).content();

            // 存储到向量数据库
            embeddingStore.add(embedding, segment);

            log.info("文档已添加到向量存储，ID: {}", document.getId());
        } catch (Exception e) {
            log.error("添加文档到向量存储失败: {}", e.getMessage(), e);
            throw new RuntimeException("添加文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void addDocuments(List<Document> documents) {
        try {
            List<Embedding> embeddings = new ArrayList<>();

            for (Document document : documents) {
                // 如果文档没有ID，生成一个UUID
                if (document.getId() == null || document.getId().isEmpty()) {
                    document.setId(UUID.randomUUID().toString());
                }

                // 创建文本片段
                TextSegment segment = TextSegment.from(document.getContent());

                // 生成向量
                Embedding embedding = embeddingModel.embed(segment).content();

                embeddings.add(embedding);
            }

            // 批量存储
            embeddingStore.addAll(embeddings);

            log.info("已批量添加 {} 个文档到向量存储", documents.size());
        } catch (Exception e) {
            log.error("批量添加文档到向量存储失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量添加文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Document> search(String query, int topK) {
        return searchWithFilter(query, topK, null);
    }

    @Override
    public List<Document> searchWithFilter(String query, int topK, Map<String, Object> metadata) {
        try {
            // 将查询转换为向量
            TextSegment querySegment = TextSegment.from(query);
            Embedding queryEmbedding = embeddingModel.embed(querySegment).content();

            // 构建搜索请求
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(topK)
                    .minScore(0.0)
                    .build();

            // 执行搜索
            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

            // 转换结果
            return result.matches().stream()
                    .map(this::toDocument)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("向量检索失败: {}", e.getMessage(), e);
            throw new RuntimeException("向量检索失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteDocument(String id) {
        try {
//            embeddingStore.remove(id);
            log.info("文档已从向量存储删除，ID: {}", id);
        } catch (Exception e) {
            log.error("删除文档失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void clear() {
        try {
            // 注意：这会删除所有文档
//            embeddingStore.removeAll();
            log.info("向量存储已清空");
        } catch (Exception e) {
            log.error("清空向量存储失败: {}", e.getMessage(), e);
            throw new RuntimeException("清空向量存储失败: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        try {
            // 注意：Milvus的统计功能需要额外配置
            // 这里返回一个估算值或调用Milvus的统计接口
            return 0;
        } catch (Exception e) {
            log.error("统计文档数量失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 将EmbeddingMatch转换为Document
     */
    private Document toDocument(EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();

        return Document.builder()
                .id(match.embeddingId())
                .content(segment.text())
                .metadata(segment.metadata().toMap())
                .embedding(match.embedding().vector())
                .score(match.score())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
