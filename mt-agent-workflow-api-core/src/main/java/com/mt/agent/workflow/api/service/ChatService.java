package com.mt.agent.workflow.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mt.agent.workflow.api.entity.ChatMessage;
import com.mt.agent.workflow.api.entity.ChatSession;
import com.mt.agent.workflow.api.entity.UserToolConfig;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

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
     * 创建指定ID的聊天会话
     * @param sessionId 指定的会话ID
     * @param userId 用户ID
     * @param sessionName 会话名称
     * @param dbConfigId 数据库配置ID
     * @param tableId 表ID
     * @return 创建的会话信息
     */
    ChatSession createSessionWithId(Long sessionId, Long userId, String sessionName, Long dbConfigId, Long tableId);
    
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
    
    /**
     * 获取最近N轮的会话历史，格式化为Dify需要的格式
     * @param sessionId 会话ID
     * @param rounds 获取的轮数（每轮包含一个user消息和一个assistant消息）
     * @return 格式化的历史对话列表，包含role和content
     */
    List<Map<String, String>> getRecentSessionHistory(Long sessionId, int rounds);
    
    /**
     * 获取上一条助手的回复结果
     * @param sessionId 会话ID
     * @return 上一条助手回复的执行结果，如果没有则返回null
     */
    String getLastAssistantReply(Long sessionId);
    
    /**
     * 根据会话ID和用户ID获取会话信息
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 会话信息，如果不存在或无权访问则返回null
     */
    ChatSession getSessionById(Long sessionId, Long userId);

}
