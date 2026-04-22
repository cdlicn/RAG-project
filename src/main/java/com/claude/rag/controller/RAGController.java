package com.claude.rag.controller;

import com.claude.rag.model.Document;
import com.claude.rag.model.SearchRequest;
import com.claude.rag.model.SearchResponse;
import com.claude.rag.service.RAGService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG REST API控制器
 * 提供文档入库、检索、查询等API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "*")
public class RAGController {

    private final RAGService ragService;

    public RAGController(RAGService ragService) {
        this.ragService = ragService;
    }

    /**
     * 文档入库
     * POST /api/rag/ingest
     *
     * @param body 包含content和metadata的请求体
     * @return 文档ID
     */
    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Object>> ingest(@RequestBody Map<String, Object> body) {
        try {
            String content = (String) body.get("content");
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) body.getOrDefault("metadata", new HashMap<>());

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "文档内容不能为空"
                ));
            }

            String documentId = ragService.ingest(content, metadata);

            log.info("文档入库成功，ID: {}", documentId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "文档入库成功",
                    "data", Map.of("documentId", documentId)
            ));

        } catch (Exception e) {
            log.error("文档入库失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "文档入库失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 批量文档入库
     * POST /api/rag/batch-ingest
     *
     * @param documents 文档列表
     * @return 文档ID列表
     */
    @PostMapping("/batch-ingest")
    public ResponseEntity<Map<String, Object>> batchInest(@RequestBody List<Document> documents) {
        try {
            if (documents == null || documents.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "文档列表不能为空"
                ));
            }

            List<String> documentIds = ragService.batchIngest(documents);

            log.info("批量文档入库成功，数量: {}", documentIds.size());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "批量文档入库成功",
                    "data", Map.of(
                            "count", documentIds.size(),
                            "documentIds", documentIds
                    )
            ));

        } catch (Exception e) {
            log.error("批量文档入库失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "批量文档入库失败: " + e.getMessage()
            ));
        }
    }

    /**
     * RAG查询
     * POST /api/rag/query
     *
     * @param request 检索请求
     * @return 检索响应
     */
    @PostMapping("/query")
    public ResponseEntity<SearchResponse> query(@Valid @RequestBody SearchRequest request) {
        try {
            log.info("收到RAG查询请求: {}", request);

            SearchResponse response = ragService.query(request);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("RAG查询失败: {}", e.getMessage(), e);

            return ResponseEntity.internalServerError().body(SearchResponse.builder()
                    .query(request.getQuery())
                    .strategy(request.getStrategy())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build());
        }
    }

    /**
     * 获取文档列表
     * GET /api/rag/documents
     *
     * @return 文档列表
     */
    @GetMapping("/documents")
    public ResponseEntity<Map<String, Object>> listDocuments() {
        try {
            List<Document> documents = ragService.listDocuments();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", documents,
                    "count", documents.size()
            ));

        } catch (Exception e) {
            log.error("获取文档列表失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取文档列表失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 删除文档
     * DELETE /api/rag/documents/{id}
     *
     * @param id 文档ID
     * @return 删除结果
     */
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String id) {
        try {
            ragService.deleteDocument(id);

            log.info("文档删除成功，ID: {}", id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "文档删除成功"
            ));

        } catch (Exception e) {
            log.error("删除文档失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "删除文档失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 更新文档
     * PUT /api/rag/documents/{id}
     *
     * @param id   文档ID
     * @param body 包含content和metadata的请求体
     * @return 更新结果
     */
    @PutMapping("/documents/{id}")
    public ResponseEntity<Map<String, Object>> updateDocument(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        try {
            String content = (String) body.get("content");
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) body.getOrDefault("metadata", new HashMap<>());

            ragService.updateDocument(id, content, metadata);

            log.info("文档更新成功，ID: {}", id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "文档更新成功"
            ));

        } catch (Exception e) {
            log.error("更新文档失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "更新文档失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 健康检查
     * GET /api/rag/health
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "RAG Service",
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 获取支持的检索策略
     * GET /api/rag/strategies
     *
     * @return 检索策略列表
     */
    @GetMapping("/strategies")
    public ResponseEntity<Map<String, Object>> getStrategies() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", List.of(
                        Map.of("name", "SEMANTIC", "description", "语义检索 - 基于向量相似度的检索"),
                        Map.of("name", "HYBRID", "description", "混合检索 - 结合语义和关键词检索"),
                        Map.of("name", "RERANK", "description", "重排序检索 - 检索后使用模型重排序")
                )
        ));
    }
}
