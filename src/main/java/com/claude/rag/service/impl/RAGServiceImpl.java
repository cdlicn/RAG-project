package com.claude.rag.service.impl;

import com.claude.rag.model.Document;
import com.claude.rag.model.SearchRequest;
import com.claude.rag.model.SearchResponse;
import com.claude.rag.service.QueryService;
import com.claude.rag.service.RAGService;
import com.claude.rag.service.VectorStoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RAG服务实现
 * 统一的RAG服务入口，整合向量存储、检索和查询功能
 */
@Slf4j
@Service
public class RAGServiceImpl implements RAGService {

    private final VectorStoreService vectorStoreService;
    private final QueryService queryService;

    public RAGServiceImpl(VectorStoreService vectorStoreService, QueryService queryService) {
        this.vectorStoreService = vectorStoreService;
        this.queryService = queryService;
    }

    @Override
    public String ingest(String content, Map<String, Object> metadata) {
        log.info("开始文档入库，内容长度: {}", content.length());

        try {
            // 创建文档对象
            Document document = Document.builder()
                    .id(UUID.randomUUID().toString())
                    .content(content)
                    .metadata(metadata)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // 存储到向量数据库
            vectorStoreService.addDocument(document);

            log.info("文档入库成功，ID: {}", document.getId());
            return document.getId();

        } catch (Exception e) {
            log.error("文档入库失败: {}", e.getMessage(), e);
            throw new RuntimeException("文档入库失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> batchIngest(List<Document> documents) {
        log.info("开始批量文档入库，数量: {}", documents.size());

        try {
            // 为没有ID的文档生成ID
            for (Document doc : documents) {
                if (doc.getId() == null || doc.getId().isEmpty()) {
                    doc.setId(UUID.randomUUID().toString());
                }
                if (doc.getCreatedAt() == null) {
                    doc.setCreatedAt(LocalDateTime.now());
                }
                if (doc.getUpdatedAt() == null) {
                    doc.setUpdatedAt(LocalDateTime.now());
                }
            }

            // 批量存储
            vectorStoreService.addDocuments(documents);

            List<String> ids = documents.stream()
                    .map(Document::getId)
                    .collect(Collectors.toList());

            log.info("批量文档入库成功，数量: {}", ids.size());
            return ids;

        } catch (Exception e) {
            log.error("批量文档入库失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量文档入库失败: " + e.getMessage(), e);
        }
    }

    @Override
    public SearchResponse query(SearchRequest request) {
        log.info("执行RAG查询，查询: {}, 策略: {}", request.getQuery(), request.getStrategy());

        long startTime = System.currentTimeMillis();
        long retrievalStartTime = startTime;

        try {
            // 1. 检索文档
            List<Document> documents = queryService.retrieveOnly(request);
            long retrievalEndTime = System.currentTimeMillis();

            // 2. 如果需要生成答案
            String answer = null;
            Long generationTime = null;

            if (request.getGenerateAnswer() && !documents.isEmpty()) {
                long generationStartTime = System.currentTimeMillis();
                answer = queryService.generateAnswer(request.getQuery(), documents);
                long generationEndTime = System.currentTimeMillis();
                generationTime = generationEndTime - generationStartTime;
            }

            long endTime = System.currentTimeMillis();

            // 3. 构建响应
            SearchResponse response = SearchResponse.builder()
                    .query(request.getQuery())
                    .documents(documents)
                    .answer(answer)
                    .strategy(request.getStrategy())
                    .retrievalTime(retrievalEndTime - retrievalStartTime)
                    .generationTime(generationTime)
                    .totalTime(endTime - startTime)
                    .model("OpenAI GPT")
                    .success(true)
                    .build();

            log.info("RAG查询完成，检索时间: {}ms, 总耗时: {}ms",
                    response.getRetrievalTime(), response.getTotalTime());

            return response;

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();

            log.error("RAG查询失败: {}", e.getMessage(), e);

            return SearchResponse.builder()
                    .query(request.getQuery())
                    .documents(List.of())
                    .answer(null)
                    .strategy(request.getStrategy())
                    .retrievalTime(endTime - retrievalStartTime)
                    .generationTime(null)
                    .totalTime(endTime - startTime)
                    .model("OpenAI GPT")
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public List<Document> listDocuments() {
        log.info("获取文档列表");

        try {
            // 注意：这里只是一个示例实现
            // 实际项目中，需要维护一个单独的文档索引表
            // 因为向量数据库不适合遍历所有文档
            log.warn("listDocuments功能需要额外实现文档索引表");
            return List.of();

        } catch (Exception e) {
            log.error("获取文档列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取文档列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteDocument(String id) {
        log.info("删除文档，ID: {}", id);

        try {
            vectorStoreService.deleteDocument(id);
            log.info("文档删除成功，ID: {}", id);

        } catch (Exception e) {
            log.error("删除文档失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Document getDocument(String id) {
        log.info("获取文档详情，ID: {}", id);

        try {
            // 注意：向量数据库不支持通过ID精确检索文档
            // 需要额外维护一个文档索引表
            log.warn("getDocument功能需要额外实现文档索引表");
            return null;

        } catch (Exception e) {
            log.error("获取文档详情失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取文档详情失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateDocument(String id, String content, Map<String, Object> metadata) {
        log.info("更新文档，ID: {}", id);

        try {
            // 先删除旧文档
            vectorStoreService.deleteDocument(id);

            // 创建新文档
            Document document = Document.builder()
                    .id(id)
                    .content(content)
                    .metadata(metadata)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // 添加新文档
            vectorStoreService.addDocument(document);

            log.info("文档更新成功，ID: {}", id);

        } catch (Exception e) {
            log.error("更新文档失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新文档失败: " + e.getMessage(), e);
        }
    }
}
