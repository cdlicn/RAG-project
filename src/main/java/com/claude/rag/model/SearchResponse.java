package com.claude.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 检索响应模型
 * 用于封装RAG查询的返回结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {

    /**
     * 原始查询
     */
    private String query;

    /**
     * 检索到的文档列表
     */
    private List<Document> documents;

    /**
     * 生成的答案
     */
    private String answer;

    /**
     * 检索策略
     */
    private RetrievalStrategy strategy;

    /**
     * 检索耗时（毫秒）
     */
    private Long retrievalTime;

    /**
     * 生成耗时（毫秒）
     */
    private Long generationTime;

    /**
     * 总耗时（毫秒）
     */
    private Long totalTime;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 是否成功
     */
    @Builder.Default
    private Boolean success = true;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 检索到的文档数量
     */
    public Integer getDocumentCount() {
        return documents != null ? documents.size() : 0;
    }
}
