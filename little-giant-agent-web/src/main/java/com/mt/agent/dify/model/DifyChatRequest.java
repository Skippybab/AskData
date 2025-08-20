package com.mt.agent.dify.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Dify聊天请求模型
 *
 * @author wsx
 * @date 2025/6/30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DifyChatRequest {

    /**
     * 用户输入/提问内容
     */
    private String query;

    /**
     * 允许传入 App 定义的各变量值
     */
    private Map<String, Object> inputs;

    /**
     * 响应模式：
     * - streaming 流式返回
     * - blocking 阻塞型返回
     */
    @JsonProperty("response_mode")
    private String responseMode = "blocking";

    /**
     * 会话 ID，需要基于之前的聊天记录继续对话，必须传入之前消息的 conversation_id
     */
    @JsonProperty("conversation_id")
    private String conversationId;

    /**
     * 用户标识，用于定义终端用户的身份，必须与之前的对话保持一致
     */
    private String user;

    /**
     * 文件列表，适用于传入文件（图片）进行理解，仅当模型支持 Vision 能力时可用
     */
    private Object[] files;

    /**
     * 自动生成标题，默认 true
     */
    @JsonProperty("auto_generate_name")
    private Boolean autoGenerateName = true;
} 