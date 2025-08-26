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

import java.util.List;

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
    

}
