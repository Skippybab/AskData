package com.mt.agent.workflow.api.service.impl;

import com.mt.agent.workflow.api.service.SessionAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 会话审计服务实现
 */
@Slf4j
@Service
public class SessionAuditServiceImpl implements SessionAuditService {
    
    @Override
    public void logSessionCreated(Long userId, Long sessionId, String sessionName) {
        log.info("会话创建 - 用户ID: {}, 会话ID: {}, 会话名称: {}", userId, sessionId, sessionName);
        // TODO: 可以在这里添加更详细的审计日志记录，比如写入数据库或发送到日志系统
    }
    
    @Override
    public void logSessionRenamed(Long userId, Long sessionId, String oldName, String newName) {
        log.info("会话重命名 - 用户ID: {}, 会话ID: {}, 原名称: {}, 新名称: {}", 
                userId, sessionId, oldName, newName);
        // TODO: 可以在这里添加更详细的审计日志记录
    }
    
    @Override
    public void logSessionDeleted(Long userId, Long sessionId, String sessionName) {
        log.info("会话删除 - 用户ID: {}, 会话ID: {}, 会话名称: {}", userId, sessionId, sessionName);
        // TODO: 可以在这里添加更详细的审计日志记录
    }
    
    @Override
    public void logSessionAccessed(Long userId, Long sessionId) {
        log.debug("会话访问 - 用户ID: {}, 会话ID: {}", userId, sessionId);
        // TODO: 可以在这里添加更详细的审计日志记录
    }
}
