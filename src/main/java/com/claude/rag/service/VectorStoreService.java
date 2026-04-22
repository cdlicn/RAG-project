package com.claude.rag.service;

import com.claude.rag.model.Document;

import java.util.List;

/**
 * 向量存储服务接口
 * 负责文档的向量化存储和检索
 */
public interface VectorStoreService {

    /**
     * 添加单个文档到向量存储
     *
     * @param document 文档对象
     */
    void addDocument(Document document);

    /**
     * 批量添加文档到向量存储
     *
     * @param documents 文档列表
     */
    void addDocuments(List<Document> documents);

    /**
     * 向量检索
     * 根据查询内容检索最相似的文档
     *
     * @param query 查询内容
     * @param topK  返回结果数量
     * @return 检索到的文档列表
     */
    List<Document> search(String query, int topK);

    /**
     * 向量检索（带过滤条件）
     *
     * @param query    查询内容
     * @param topK     返回结果数量
     * @param metadata 过滤条件的元数据
     * @return 检索到的文档列表
     */
    List<Document> searchWithFilter(String query, int topK, java.util.Map<String, Object> metadata);

    /**
     * 删除文档
     *
     * @param id 文档ID
     */
    void deleteDocument(String id);

    /**
     * 清空所有文档
     */
    void clear();

    /**
     * 获取文档数量
     *
     * @return 文档总数
     */
    long count();
}
