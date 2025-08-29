package com.mt.agent.workflow.api.util;

import java.util.regex.Pattern;

/**
 * 会话管理工具类
 */
public class SessionUtil {
    
    // 支持中文、英文、数字、空格、连字符、下划线以及常见的日期时间字符（斜杠、冒号、点号、括号）
    private static final Pattern SESSION_NAME_PATTERN = Pattern.compile("^[\\w\\u4e00-\\u9fa5\\s\\-_/:\\.\\(\\)]{1,100}$");
    
    /**
     * 验证会话名称是否有效
     */
    public static boolean isValidSessionName(String sessionName) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = sessionName.trim();
        if (trimmed.length() > 100) {
            return false;
        }
        
        return SESSION_NAME_PATTERN.matcher(trimmed).matches();
    }
    
    /**
     * 生成默认会话名称
     */
    public static String generateDefaultSessionName() {
        return "新对话 " + System.currentTimeMillis();
    }
    
    /**
     * 生成带时间的默认会话名称
     */
    public static String generateTimestampSessionName(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = "数据问答会话";
        }
        
        // 使用安全的日期格式，避免特殊字符
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm");
        String timestamp = now.format(formatter);
        
        return prefix + " " + timestamp;
    }
    
    /**
     * 清理会话名称，增强容错机制
     */
    public static String cleanSessionName(String sessionName) {
        if (sessionName == null) {
            return generateDefaultSessionName();
        }
        
        String cleaned = sessionName.trim();
        if (cleaned.isEmpty()) {
            return generateDefaultSessionName();
        }
        
        // 限制长度
        if (cleaned.length() > 100) {
            cleaned = cleaned.substring(0, 100);
        }
        
        return cleaned;
    }
    
    /**
     * 安全的会话名称生成，确保符合验证规则
     * 如果原名称验证失败，自动生成符合规范的名称
     */
    public static String ensureValidSessionName(String sessionName) {
        // 首先尝试清理原名称
        String cleaned = cleanSessionName(sessionName);
        
        // 如果清理后的名称有效，直接返回
        if (isValidSessionName(cleaned)) {
            return cleaned;
        }
        
        // 如果原名称包含"数据问答会话"，使用安全的时间格式重新生成
        if (sessionName != null && sessionName.contains("数据问答会话")) {
            return generateTimestampSessionName("数据问答会话");
        }
        
        // 否则使用默认名称
        return generateDefaultSessionName();
    }
    
    /**
     * 生成会话预览文本
     */
    public static String generateSessionPreview(String content, int maxLength) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        String preview = content.trim();
        if (preview.length() <= maxLength) {
            return preview;
        }
        
        return preview.substring(0, maxLength) + "...";
    }
}
