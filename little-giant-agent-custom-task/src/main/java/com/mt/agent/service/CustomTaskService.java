package com.mt.agent.service;

import com.mt.agent.model.workflow.JavaExecutable;
import com.mt.agent.reporter.SubEventReporter;

import java.util.HashMap;

/**
 * 执行定制任务服务接口
 *
 * @author lfz
 * @date 2025/4/23 16:33
 */
public interface CustomTaskService {

    /**
     * 执行java指令对应的执行步骤
     *
     * @author lfz
     * @date 2025/5/29 18:26
     * @param javaExecutable java指令
     * @param paramMap       参数map
     * @param reporter       流式报告器
     */
    void executeJavaOrders(JavaExecutable javaExecutable, HashMap<String, Object> paramMap, SubEventReporter reporter);

    /**
     * 直接执行Python代码
     * 
     * @author lfz
     * @date 2025/1/7 10:00
     * @param pythonCode Python代码字符串
     * @param paramMap   参数map
     * @param reporter   流式报告器
     */
    void executePythonCode(String pythonCode, HashMap<String, Object> paramMap, SubEventReporter reporter,String userId);
}
