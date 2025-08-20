package com.mt.agent.dify.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Dify会话历史消息响应模型
 *
 * @author wsx
 * @date 2025/6/30
 */
@Data
public class DifyMessagesResponse {

    /**
     * 对象类型，固定为"list"
     */
    private String object;

    /**
     * 消息列表
     */
    private List<Message> data;

    /**
     * 第一条消息ID，用于分页
     */
    @JsonProperty("first_id")
    private String firstId;

    /**
     * 最后一条消息ID，用于分页
     */
    @JsonProperty("last_id")
    private String lastId;

    /**
     * 是否有更多数据
     */
    @JsonProperty("has_more")
    private Boolean hasMore;

    /**
     * 消息详情
     */
    @Data
    public static class Message {
        /**
         * 消息ID
         */
        private String id;

        /**
         * 对话ID
         */
        @JsonProperty("conversation_id")
        private String conversationId;

        /**
         * 输入内容
         */
        private String query;

        /**
         * 输入参数
         */
        private Object inputs;

        /**
         * 回答内容
         */
        private String answer;

        /**
         * 反馈信息
         */
        private Feedback feedback;

        /**
         * 检索资源
         */
        @JsonProperty("retriever_resources")
        private List<Object> retrieverResources;

        /**
         * 创建时间戳
         */
        @JsonProperty("created_at")
        private Long createdAt;

        /**
         * 反馈信息
         */
        @Data
        public static class Feedback {
            /**
             * 评分: like, dislike, null
             */
            private String rating;

            /**
             * 反馈内容
             */
            private String content;
        }
    }
} 