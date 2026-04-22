package com.claude.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文档模型
 * 用于存储文档内容、元数据和向量表示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    /**
     * 文档唯一标识
     */
    private String id;

    /**
     * 文档内容
     */
    private String content;

    /**
     * 文档元数据（标题、作者、创建时间等）
     */
    @Builder.Default
    private Map<String, Object> metadata = Map.of();

    /**
     * 向量表示
     */
    private float[] embedding;

    /**
     * 检索分数（相似度）
     */
    private Double score;

    /**
     * 文档创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 文档更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 文档状态
     */
    @Builder.Default
    private DocumentStatus status = DocumentStatus.ACTIVE;

    /**
     * 文档状态枚举
     */
    public enum DocumentStatus {
        /**
         * 活跃状态
         */
        ACTIVE,

        /**
         * 已删除
         */
        DELETED,

        /**
         * 处理中
         */
        PROCESSING
    }
}
