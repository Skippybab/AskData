package com.mt.agent.workflow.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mt.agent.workflow.api.entity.ChatMessage;
import com.mt.agent.workflow.api.entity.ChatSession;
import com.mt.agent.workflow.api.entity.UserToolConfig;
import com.mt.agent.workflow.api.mapper.ChatMessageMapper;
import com.mt.agent.workflow.api.mapper.ChatSessionMapper;
import com.mt.agent.workflow.api.mapper.UserToolConfigMapper;
import com.mt.agent.workflow.api.service.ChatService;
import com.mt.agent.workflow.api.service.SessionAuditService;
import com.mt.agent.workflow.api.exception.SessionException;
import com.mt.agent.workflow.api.util.SessionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatSessionMapper sessionMapper;
    @Autowired
    private ChatMessageMapper messageMapper;
    @Autowired
    private UserToolConfigMapper toolConfigMapper;
    @Autowired
    private SessionAuditService sessionAuditService;

    @Override
    public IPage<ChatSession> getUserSessions(Long userId, Page<ChatSession> page) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getStatus, 1) // 活跃状态
                .orderByDesc(ChatSession::getLastMessageAtMs, ChatSession::getCreatedAtMs);
        
        return sessionMapper.selectPage(page, wrapper);
    }

    @Override
    public ChatSession createSession(Long userId, String sessionName, Long dbConfigId, Long tableId) {
        // 清理和验证会话名称
        String cleanedName = SessionUtil.cleanSessionName(sessionName);
        if (!SessionUtil.isValidSessionName(cleanedName)) {
            throw new SessionException.InvalidSessionNameException(cleanedName);
        }
        
        long now = System.currentTimeMillis();
        
        ChatSession session = new ChatSession();
        session.setTenantId(0L); // 默认租户
        session.setUserId(userId);
        session.setSessionName(cleanedName);
        session.setDbConfigId(dbConfigId);
        session.setTableId(tableId); // 新增表ID字段
        session.setStatus(1);
        session.setMessageCount(0);
        session.setCreatedAtMs(now);
        session.setUpdatedAtMs(now);
        session.setLastMessageAtMs(now);
        
        sessionMapper.insert(session);
        
        // 记录审计日志
        sessionAuditService.logSessionCreated(userId, session.getId(), cleanedName);
        
        return session;
    }
    
    @Override
    public ChatSession createSessionWithId(Long sessionId, Long userId, String sessionName, Long dbConfigId, Long tableId) {
        // 清理和验证会话名称
        String cleanedName = SessionUtil.cleanSessionName(sessionName);
        if (!SessionUtil.isValidSessionName(cleanedName)) {
            throw new SessionException.InvalidSessionNameException(cleanedName);
        }
        
        long now = System.currentTimeMillis();
        
        ChatSession session = new ChatSession();
        session.setId(sessionId); // 设置指定的会话ID
        session.setTenantId(0L); // 默认租户
        session.setUserId(userId);
        session.setSessionName(cleanedName);
        session.setDbConfigId(dbConfigId);
        session.setTableId(tableId); // 新增表ID字段
        session.setStatus(1);
        session.setMessageCount(0);
        session.setCreatedAtMs(now);
        session.setUpdatedAtMs(now);
        session.setLastMessageAtMs(now);
        
        try {
            sessionMapper.insert(session);
            log.info("成功创建指定ID的会话, sessionId: {}, sessionName: {}", sessionId, cleanedName);
        } catch (Exception e) {
            log.error("创建指定ID的会话失败, sessionId: {}, error: {}", sessionId, e.getMessage());
            throw new RuntimeException("创建会话失败，可能是sessionId已存在: " + sessionId, e);
        }
        
        // 记录审计日志
        sessionAuditService.logSessionCreated(userId, session.getId(), cleanedName);
        
        return session;
    }

    @Override
    public List<ChatMessage> getSessionMessages(Long sessionId, Long userId) {
        // 验证会话所有权
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new SessionException.SessionNotFoundException(sessionId);
        }
        if (!session.getUserId().equals(userId)) {
            throw new SessionException.SessionAccessDeniedException(userId, sessionId);
        }
        
        // 记录会话访问审计日志
        sessionAuditService.logSessionAccessed(userId, sessionId);
        
        return messageMapper.selectList(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreatedAtMs)
        );
    }



    @Override
    public List<UserToolConfig> getUserTools(Long userId) {
        // 返回用户可用的工具配置
        List<UserToolConfig> tools = toolConfigMapper.selectList(
            new LambdaQueryWrapper<UserToolConfig>()
                .eq(UserToolConfig::getUserId, userId)
                .eq(UserToolConfig::getEnabled, 1)
                .orderByAsc(UserToolConfig::getToolType, UserToolConfig::getToolName)
        );
        
        // 如果没有配置，则返回默认工具
        if (tools.isEmpty()) {
            UserToolConfig sqlTool = new UserToolConfig();
            sqlTool.setTenantId(0L);
            sqlTool.setUserId(userId);
            sqlTool.setToolType("sql_query");
            sqlTool.setToolName("SQL查询");
            sqlTool.setConfigJson("{\"description\":\"通过自然语言查询数据库\"}");
            sqlTool.setEnabled(1);
            sqlTool.setCreatedAtMs(System.currentTimeMillis());
            sqlTool.setUpdatedAtMs(System.currentTimeMillis());
            tools.add(sqlTool);
        }
        
        return tools;
    }

    @Override
    public void updateSessionMessageCount(Long sessionId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            Long count = messageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSessionId, sessionId)
            );
            
            session.setMessageCount(count != null ? count.intValue() : 0);
            session.setLastMessageAtMs(System.currentTimeMillis());
            session.setUpdatedAtMs(System.currentTimeMillis());
            sessionMapper.updateById(session);
        }
    }
    
    @Override
    public boolean renameSession(Long userId, Long sessionId, String newName) {
        try {
            // 验证会话所有权
            ChatSession session = sessionMapper.selectById(sessionId);
            if (session == null) {
                throw new SessionException.SessionNotFoundException(sessionId);
            }
            if (!session.getUserId().equals(userId)) {
                throw new SessionException.SessionAccessDeniedException(userId, sessionId);
            }
            
            // 验证新名称
            String cleanedName = SessionUtil.cleanSessionName(newName);
            if (!SessionUtil.isValidSessionName(cleanedName)) {
                throw new SessionException.InvalidSessionNameException(cleanedName);
            }
            
            // 保存旧名称用于审计
            String oldName = session.getSessionName();
            
            // 更新会话名称
            session.setSessionName(cleanedName);
            session.setUpdatedAtMs(System.currentTimeMillis());
            
            int result = sessionMapper.updateById(session);
            if (result > 0) {
                log.info("用户 {} 成功重命名会话 {} 为: {}", userId, sessionId, cleanedName);
                
                // 记录审计日志
                sessionAuditService.logSessionRenamed(userId, sessionId, oldName, cleanedName);
                
                return true;
            } else {
                log.warn("重命名会话失败，sessionId: {}", sessionId);
                return false;
            }
        } catch (SessionException e) {
            log.warn("重命名会话验证失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("重命名会话时发生错误: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean deleteSession(Long userId, Long sessionId) {
        try {
            // 验证会话所有权
            ChatSession session = sessionMapper.selectById(sessionId);
            if (session == null) {
                throw new SessionException.SessionNotFoundException(sessionId);
            }
            if (!session.getUserId().equals(userId)) {
                throw new SessionException.SessionAccessDeniedException(userId, sessionId);
            }
            
            // 保存会话名称用于审计
            String sessionName = session.getSessionName();
            
            // 软删除会话（设置状态为已删除）
            session.setStatus(0); // 0表示已删除
            session.setUpdatedAtMs(System.currentTimeMillis());
            
            int result = sessionMapper.updateById(session);
            if (result > 0) {
                log.info("用户 {} 成功删除会话 {}", userId, sessionId);
                
                // 记录审计日志
                sessionAuditService.logSessionDeleted(userId, sessionId, sessionName);
                
                return true;
            } else {
                log.warn("删除会话失败，sessionId: {}", sessionId);
                return false;
            }
        } catch (SessionException e) {
            log.warn("删除会话验证失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("删除会话时发生错误: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public List<Map<String, String>> getRecentSessionHistory(Long sessionId, int rounds) {
        try {
            // 获取最近的N*2条消息（每轮包含user和assistant两条消息）
            int messageLimit = rounds * 2;
            
            // 查询最近的消息，按时间倒序
            List<ChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSessionId, sessionId)
                    .in(ChatMessage::getRole, "user", "assistant")
                    .eq(ChatMessage::getStatus, 1) // 只获取成功的消息
                    .orderByDesc(ChatMessage::getCreatedAtMs)
                    .last("LIMIT " + messageLimit)
            );
            
            // 如果没有历史消息，返回空列表
            if (messages == null || messages.isEmpty()) {
                log.debug("会话 {} 没有历史消息", sessionId);
                return new ArrayList<>();
            }
            
            // 反转列表以获得正确的时间顺序
            List<Map<String, String>> history = new ArrayList<>();
            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessage msg = messages.get(i);
                Map<String, String> historyItem = new HashMap<>();
                historyItem.put("role", msg.getRole());
                
                // 根据角色构建内容
                if ("user".equals(msg.getRole())) {
                    // 用户消息：使用原始内容
                    historyItem.put("content", msg.getContent());
                } else if ("assistant".equals(msg.getRole())) {
                    // 助手消息：优先使用执行结果，其次使用思考内容，最后使用原始内容
                    String content = msg.getExecutionResult();
                    if (content == null || content.trim().isEmpty()) {
                        content = msg.getThinkingContent();
                    }
                    if (content == null || content.trim().isEmpty()) {
                        content = msg.getContent();
                    }
                    historyItem.put("content", content != null ? content : "");
                }
                
                history.add(historyItem);
            }
            
            log.info("获取会话 {} 的历史记录，共 {} 条消息", sessionId, history.size());
            return history;
            
        } catch (Exception e) {
            log.error("获取会话历史失败，sessionId: {}", sessionId, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public String getLastAssistantReply(Long sessionId) {
        try {
            // 查询最近一条助手消息
            List<ChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSessionId, sessionId)
                    .eq(ChatMessage::getRole, "assistant")
                    .eq(ChatMessage::getStatus, 1) // 只获取成功的消息
                    .orderByDesc(ChatMessage::getCreatedAtMs)
                    .last("LIMIT 1")
            );
            
            if (messages != null && !messages.isEmpty()) {
                ChatMessage lastAssistantMsg = messages.get(0);
                // 优先返回执行结果，其次返回思考内容，最后返回原始内容
                String reply = lastAssistantMsg.getExecutionResult();
                if (reply == null || reply.trim().isEmpty()) {
                    reply = lastAssistantMsg.getThinkingContent();
                }
                if (reply == null || reply.trim().isEmpty()) {
                    reply = lastAssistantMsg.getContent();
                }
                
                log.debug("获取会话 {} 的上一条助手回复，长度: {}", 
                    sessionId, reply != null ? reply.length() : 0);
                return reply;
            }
            
            log.debug("会话 {} 没有找到助手回复", sessionId);
            return null;
            
        } catch (Exception e) {
            log.error("获取上一条助手回复失败，sessionId: {}", sessionId, e);
            return null;
        }
    }
    
    @Override
    public ChatSession getSessionById(Long sessionId, Long userId) {
        try {
            // 查询会话信息
            ChatSession session = sessionMapper.selectById(sessionId);
            if (session == null) {
                log.warn("会话不存在, sessionId: {}", sessionId);
                return null;
            }
            
            // 验证会话状态（只允许活跃状态的会话）
            if (session.getStatus() != 1) {
                log.warn("会话状态无效, sessionId: {}, status: {}", sessionId, session.getStatus());
                return null;
            }
            
            // 验证用户权限（对于自动创建会话的场景，可以考虑放宽权限验证）
            if (!session.getUserId().equals(userId)) {
                log.warn("用户无权访问会话, sessionId: {}, userId: {}, sessionUserId: {}", 
                    sessionId, userId, session.getUserId());
                return null;
            }
            
            log.debug("成功获取会话信息, sessionId: {}, sessionName: {}", sessionId, session.getSessionName());
            return session;
            
        } catch (Exception e) {
            log.error("获取会话信息失败, sessionId: {}, userId: {}", sessionId, userId, e);
            return null;
        }
    }

}
