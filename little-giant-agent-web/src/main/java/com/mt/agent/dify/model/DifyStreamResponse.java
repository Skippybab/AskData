package com.mt.agent.dify.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Dify流式响应模型
 *
 * @author wsx
 * @date 2025/6/30
 */
@Data
public class DifyStreamResponse {

    private String id;
    /**
     * 事件类型:
     * - message: 消息内容
     * - agent_message: 智能体消息
     * - message_end: 消息结束
     * - message_replace: 消息替换
     * - error: 错误
     * - ping: 心跳
     */
    private String event;

    private String position;

    private String thought;

    /**
     * 消息唯一 ID
     */
    @JsonProperty("message_id")
    private String messageId;

    /**
     * 对话 ID
     */
    @JsonProperty("conversation_id")
    private String conversationId;

    /**
     * 消息增量内容
     */
    private String answer;

    /**
     * 消息创建时间戳
     */
    @JsonProperty("created_at")
    private Long createdAt;

    /**
     * 任务 ID，用于请求跟踪
     */
    @JsonProperty("task_id")
    private String taskId;

    /**
     * 错误信息（当event为error时）
     */
    private String error;

    /**
     * 错误代码（当event为error时）
     */
    private String code;

    /**
     * 错误状态（当event为error时）
     */
    private Integer status;
}
