package com.claude.rag.service;

import com.claude.rag.model.Document;
import com.claude.rag.model.SearchRequest;
import com.claude.rag.model.SearchResponse;

import java.util.List;
import java.util.Map;

/**
 * RAG核心服务接口
 * 提供文档入库、检索、查询等核心功能
 */
public interface RAGService {

    /**
     * 文档入库
     * 将文档内容转换为向量并存储到向量数据库
     *
     * @param content  文档内容
     * @param metadata 文档元数据
     * @return 文档ID
     */
    String ingest(String content, Map<String, Object> metadata);

    /**
     * 批量文档入库
     *
     * @param documents 文档列表
     * @return 文档ID列表
     */
    List<String> batchIngest(List<Document> documents);

    /**
     * RAG查询
     * 根据查询内容进行检索并生成答案
     *
     * @param request 检索请求
     * @return 检索响应
     */
    SearchResponse query(SearchRequest request);

    /**
     * 获取所有文档列表
     *
     * @return 文档列表
     */
    List<Document> listDocuments();

    /**
     * 删除文档
     *
     * @param id 文档ID
     */
    void deleteDocument(String id);

    /**
     * 获取文档详情
     *
     * @param id 文档ID
     * @return 文档详情
     */
    Document getDocument(String id);

    /**
     * 更新文档
     *
     * @param id       文档ID
     * @param content  新内容
     * @param metadata 新元数据
     */
    void updateDocument(String id, String content, Map<String, Object> metadata);
}
