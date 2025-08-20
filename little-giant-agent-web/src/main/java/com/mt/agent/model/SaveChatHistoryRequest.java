package com.mt.agent.model;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 保存聊天历史请求模型类
 */
@Data
public class SaveChatHistoryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 聊天历史记录
     */
    private List<ChatMessage> chatHistory;
}