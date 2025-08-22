package com.mt.agent.workflow.api.service;

/**
 * 会话审计服务
 * 用于记录会话相关的操作日志
 */
public interface SessionAuditService {
    
    /**
     * 记录会话创建
     */
    void logSessionCreated(Long userId, Long sessionId, String sessionName);
    
    /**
     * 记录会话重命名
     */
    void logSessionRenamed(Long userId, Long sessionId, String oldName, String newName);
    
    /**
     * 记录会话删除
     */
    void logSessionDeleted(Long userId, Long sessionId, String sessionName);
    
    /**
     * 记录会话访问
     */
    void logSessionAccessed(Long userId, Long sessionId);
}
