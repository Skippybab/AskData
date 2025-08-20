package com.mt.agent.dify.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Dify聊天响应模型
 *
 * @author wsx
 * @date 2025/6/30
 */
@Data
public class DifyChatResponse {

    /**
     * 消息唯一 ID
     */
    private String id;

    /**
     * 对话 ID
     */
    @JsonProperty("conversation_id")
    private String conversationId;

    /**
     * 消息模式：chat
     */
    private String mode;

    /**
     * 回答消息内容
     */
    private String answer;

    /**
     * 元数据
     */
    private Metadata metadata;

    /**
     * 消息创建时间戳，如：1705395332
     */
    @JsonProperty("created_at")
    private Long createdAt;

    /**
     * 元数据信息
     */
    @Data
    public static class Metadata {
        /**
         * 使用情况
         */
        private Usage usage;

        /**
         * 检索信息
         */
        @JsonProperty("retriever_resources")
        private List<RetrieverResource> retrieverResources;
    }

    /**
     * 使用情况信息
     */
    @Data
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("prompt_price")
        private String promptPrice;

        @JsonProperty("prompt_unit_price")
        private String promptUnitPrice;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("completion_price")
        private String completionPrice;

        @JsonProperty("completion_unit_price")
        private String completionUnitPrice;

        @JsonProperty("total_tokens")
        private Integer totalTokens;

        @JsonProperty("total_price")
        private String totalPrice;

        private String currency;

        @JsonProperty("latency")
        private Double latency;
    }

    /**
     * 检索资源信息
     */
    @Data
    public static class RetrieverResource {
        private String position;
        @JsonProperty("dataset_id")
        private String datasetId;
        @JsonProperty("dataset_name")
        private String datasetName;
        @JsonProperty("document_id")
        private String documentId;
        @JsonProperty("document_name")
        private String documentName;
        @JsonProperty("segment_id")
        private String segmentId;
        private Double score;
        private String content;
    }
} 