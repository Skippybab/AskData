package com.mt.agent.workflow.api.service.impl;

import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;

/**
 * 简单的日志报告器，用于不需要SSE功能的场景
 */
@Slf4j
public class SimpleLogReporter extends SubEventReporter {
    
    public SimpleLogReporter() {
        super(null, "log");
    }
    
    /**
     * 报告执行步骤
     */
    @Override
    public void reportStep(String message) {
        log.info("📝 [执行步骤] {}", message);
    }
    
    /**
     * 报告节点结果
     */
    @Override
    public void reportNodeResult(Object result) {
        log.info("📊 [节点结果] {}", result);
    }
    
    /**
     * 报告错误
     */
    @Override
    public void reportError(String error) {
        log.error("❌ [执行错误] {}", error);
    }
}