package com.mt.agent.workflow.api.service;

import com.mt.agent.workflow.api.dto.PythonExecutionResult;
import com.mt.agent.workflow.api.service.impl.SubEventReporter;
import java.util.HashMap;

/**
 * Python代码执行服务接口
 */
public interface PythonExecutorService {
    
    /**
     * 执行Python代码
     * @param pythonCode Python代码字符串
     * @param paramMap 参数映射
     * @param reporter 流式报告器
     * @param userId 用户ID
     */
    void executePythonCode(String pythonCode, HashMap<String, Object> paramMap,
                          SubEventReporter reporter, String userId);
    
    /**
     * 执行Python代码并返回结果
     * @param pythonCode Python代码字符串
     * @param paramMap 参数映射
     * @param userId 用户ID
     * @return 执行结果
     */
    Object executePythonCodeWithResult(String pythonCode, HashMap<String, Object> paramMap, String userId);
    
    /**
     * 执行Python代码并返回结果（新版本）
     * @param messageId 消息ID
     * @param dbConfigId 数据库配置ID
     * @return 执行结果
     */
    PythonExecutionResult executePythonCodeWithResult(Long messageId, Long dbConfigId, Long userId);
}
