package com.claude.rag.service;

import com.claude.rag.model.Document;

import java.util.List;
import java.util.Map;

/**
 * 检索服务接口
 * 定义不同的检索策略接口
 */
public interface RetrievalService {

    /**
     * 执行检索
     * 根据查询内容和检索策略检索相关文档
     *
     * @param query  查询内容
     * @param topK   返回结果数量
     * @param params 额外参数
     * @return 检索到的文档列表
     */
    List<Document> retrieve(String query, int topK, Map<String, Object> params);

    /**
     * 获取检索策略名称
     *
     * @return 策略名称
     */
    String getStrategyName();
}
