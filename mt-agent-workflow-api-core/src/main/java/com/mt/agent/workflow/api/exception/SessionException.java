package com.mt.agent.workflow.api.exception;

/**
 * 会话管理异常
 */
public class SessionException extends RuntimeException {
    
    public SessionException(String message) {
        super(message);
    }
    
    public SessionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 会话不存在异常
     */
    public static class SessionNotFoundException extends SessionException {
        public SessionNotFoundException(Long sessionId) {
            super("会话不存在: " + sessionId);
        }
    }
    
    /**
     * 会话权限不足异常
     */
    public static class SessionAccessDeniedException extends SessionException {
        public SessionAccessDeniedException(Long userId, Long sessionId) {
            super("用户 " + userId + " 无权访问会话 " + sessionId);
        }
    }
    
    /**
     * 会话名称无效异常
     */
    public static class InvalidSessionNameException extends SessionException {
        public InvalidSessionNameException(String sessionName) {
            super("会话名称无效: " + sessionName);
        }
    }
}
