package com.claude.rag.service;

import com.claude.rag.model.Document;
import com.claude.rag.model.SearchRequest;

import java.util.List;

/**
 * 查询服务接口
 * 负责整合检索和生成，提供端到端的问答功能
 */
public interface QueryService {

    /**
     * 执行RAG查询
     * 检索相关文档并生成答案
     *
     * @param request 检索请求
     * @return 生成的答案
     */
    String query(SearchRequest request);

    /**
     * 仅检索不生成答案
     *
     * @param request 检索请求
     * @return 检索到的文档列表
     */
    List<Document> retrieveOnly(SearchRequest request);

    /**
     * 基于检索结果生成答案
     *
     * @param query     用户查询
     * @param documents 检索到的文档
     * @return 生成的答案
     */
    String generateAnswer(String query, List<Document> documents);

    /**
     * 构建Prompt模板
     *
     * @param query     用户查询
     * @param documents 检索到的文档
     * @return 完整的Prompt
     */
    String buildPrompt(String query, List<Document> documents);
}
