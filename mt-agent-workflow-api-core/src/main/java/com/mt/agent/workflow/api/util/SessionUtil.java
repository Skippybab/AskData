package com.mt.agent.workflow.api.util;

import java.util.regex.Pattern;

/**
 * 会话管理工具类
 */
public class SessionUtil {
    
    private static final Pattern SESSION_NAME_PATTERN = Pattern.compile("^[\\w\\u4e00-\\u9fa5\\s\\-_]{1,100}$");
    
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
     * 清理会话名称
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
