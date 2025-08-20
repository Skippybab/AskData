package com.mt.agent.workflow.api.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓冲区管理工具
 * 用于管理用户会话状态和临时数据
 */
@Slf4j
@Component
public class BufferUtil {
    
    // 用户数据缓存，key为userId，value为该用户的数据map
    private final Map<String, Map<String, Object>> userBuffers = new ConcurrentHashMap<>();
    
    /**
     * 获取用户的缓冲区
     */
    private Map<String, Object> getUserBuffer(String userId) {
        return userBuffers.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
    }
    
    /**
     * 设置字段值
     */
    public void setField(String userId, String fieldName, Object value) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("尝试设置字段时userId为空: fieldName={}, value={}", fieldName, value);
            return;
        }
        getUserBuffer(userId).put(fieldName, value);
        log.debug("设置用户{}的字段{}: {}", userId, fieldName, value);
    }
    
    /**
     * 获取字段值
     */
    public String getField(String userId, String fieldName) {
        Object value = getUserBuffer(userId).get(fieldName);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 获取对象字段
     */
    public Object getObjectField(String userId, String fieldName) {
        return getUserBuffer(userId).get(fieldName);
    }
    
    /**
     * 获取Python代码
     */
    public String getPythonCode(String userId) {
        return getField(userId, "pythonCode");
    }
    
    /**
     * 设置Python代码
     */
    public void setPythonCode(String userId, String pythonCode) {
        setField(userId, "pythonCode", pythonCode);
    }
    
    /**
     * 清理用户缓冲区
     */
    public void clearUserBuffer(String userId) {
        userBuffers.remove(userId);
        log.debug("清理用户{}的缓冲区", userId);
    }
    
    /**
     * 检查字段是否存在
     */
    public boolean hasField(String userId, String fieldName) {
        return getUserBuffer(userId).containsKey(fieldName);
    }
    
    /**
     * 获取用户的所有字段
     */
    public Map<String, Object> getAllFields(String userId) {
        return new ConcurrentHashMap<>(getUserBuffer(userId));
    }
}
