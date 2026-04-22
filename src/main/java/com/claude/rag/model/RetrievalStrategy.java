package com.claude.rag.model;

/**
 * 检索策略枚举
 * 定义不同的RAG检索策略
 */
public enum RetrievalStrategy {

    /**
     * 语义检索
     * 基于向量相似度的纯语义检索
     */
    SEMANTIC,

    /**
     * 混合检索
     * 结合语义检索和关键词检索（BM25）
     */
    HYBRID,

    /**
     * 重排序检索
     * 初步检索后进行二次重排序以提高精度
     */
    RERANK
}
