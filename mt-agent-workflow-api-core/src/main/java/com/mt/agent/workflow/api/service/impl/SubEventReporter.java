package com.mt.agent.workflow.api.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * 子事件报告器，用于流式输出执行步骤
 */
@Slf4j
public class SubEventReporter {
    
    private final SseEmitter emitter;
    private final String eventType;
    
    public SubEventReporter(SseEmitter emitter) {
        this(emitter, "progress");
    }
    
    public SubEventReporter(SseEmitter emitter, String eventType) {
        this.emitter = emitter;
        this.eventType = eventType;
    }
    
    /**
     * 报告执行步骤
     */
    public void reportStep(String message) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", "step");
            data.put("content", message);
            
            emitter.send(SseEmitter.event()
                .name(eventType)
                .data(data));
        } catch (Exception e) {
            log.error("发送步骤报告失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 报告节点结果
     */
    public void reportNodeResult(Object result) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "result");
            data.put("data", result);
            
            emitter.send(SseEmitter.event()
                .name(eventType)
                .data(data));
        } catch (Exception e) {
            log.error("发送节点结果失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 报告错误
     */
    public void reportError(String error) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", "error");
            data.put("message", error);
            
            emitter.send(SseEmitter.event()
                .name("error")
                .data(data));
        } catch (Exception e) {
            log.error("发送错误报告失败: {}", e.getMessage(), e);
        }
    }
}
