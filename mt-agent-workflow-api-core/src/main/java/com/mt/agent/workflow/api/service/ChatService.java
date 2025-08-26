package com.mt.agent.workflow.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mt.agent.workflow.api.entity.ChatMessage;
import com.mt.agent.workflow.api.entity.ChatSession;
import com.mt.agent.workflow.api.entity.UserToolConfig;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 聊天服务接口
 */
public interface ChatService {
    
    /**
     * 获取用户的聊天会话列表
     */
    IPage<ChatSession> getUserSessions(Long userId, Page<ChatSession> page);
    
    /**
     * 创建新的聊天会话
     */
    ChatSession createSession(Long userId, String sessionName, Long dbConfigId, Long tableId);
    
    /**
     * 获取会话的消息列表
     */
    List<ChatMessage> getSessionMessages(Long sessionId, Long userId);
    

    
    /**
     * 获取用户可用的工具列表
     */
    List<UserToolConfig> getUserTools(Long userId);
    
    /**
     * 会话消息统计更新
     */
    void updateSessionMessageCount(Long sessionId);
    
    /**
     * 重命名会话
     */
    boolean renameSession(Long userId, Long sessionId, String newName);
    
    /**
     * 删除会话
     */
    boolean deleteSession(Long userId, Long sessionId);
    

}
