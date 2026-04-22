package com.claude.rag.model;

import com.claude.rag.model.RetrievalStrategy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 检索请求模型
 * 用于封装RAG查询请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    /**
     * 查询内容
     */
    @NotBlank(message = "查询内容不能为空")
    private String query;

    /**
     * 检索策略
     */
    @Builder.Default
    private RetrievalStrategy strategy = RetrievalStrategy.SEMANTIC;

    /**
     * 返回结果数量
     */
    @NotNull(message = "返回结果数量不能为空")
    @Positive(message = "返回结果数量必须大于0")
    @Builder.Default
    private Integer topK = 5;

    /**
     * 是否需要生成答案
     */
    @Builder.Default
    private Boolean generateAnswer = true;

    /**
     * 额外参数
     */
    @Builder.Default
    private Map<String, Object> params = Map.of();
}
