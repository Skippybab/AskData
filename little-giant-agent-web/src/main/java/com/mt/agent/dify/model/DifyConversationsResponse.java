package com.mt.agent.dify.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Dify会话列表响应模型
 *
 * @author wsx
 * @date 2025/6/30
 */
@Data
public class DifyConversationsResponse {

    /**
     * 对象类型，固定为"list"
     */
    private String object;

    /**
     * 会话列表
     */
    private List<Conversation> data;

    /**
     * 第一条会话ID，用于分页
     */
    @JsonProperty("first_id")
    private String firstId;

    /**
     * 最后一条会话ID，用于分页
     */
    @JsonProperty("last_id")
    private String lastId;

    /**
     * 是否有更多数据
     */
    @JsonProperty("has_more")
    private Boolean hasMore;

    /**
     * 会话详情
     */
    @Data
    public static class Conversation {
        /**
         * 会话ID
         */
        private String id;

        /**
         * 会话名称
         */
        private String name;

        /**
         * 输入参数
         */
        private Object inputs;

        /**
         * 状态
         */
        private String status;

        /**
         * 介绍信息
         */
        private String introduction;

        /**
         * 会话创建时间戳
         */
        @JsonProperty("created_at")
        private Long createdAt;
    }
} 