package com.mt.agent.model;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 聊天消息模型类
 */
@Data
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息类型：用户消息或AI消息
     */
    private String type;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 响应类型
     */
    private Integer rspType;

    /**
     * 模板信息
     */
    private String template;

    /**
     * 任务结果列表
     */
    private List<ChatTaskResult> taskResults;

    /**
     * 推荐问题列表
     */
    private List<RecommendedQuestion> recommendedQuestions;
}