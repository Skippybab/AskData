package com.mt.agent.test.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 测试执行服务
 * 提供通用的测试执行入口和管理功能
 *
 * @author MT Agent Team
 * @version 1.0
 * @since 2025-01-15
 */
@Slf4j
@Service
public class TestExecutionService {

    @Autowired
    private ExecutorTestService executorTestService;

//    @Autowired
//    private PlannerTestService plannerTestService;

    /**
     * 总的测试方案入口方法
     * 
     * 该方法会根据配置参数调用相应的测试方法
     * 不通过controller接口调用，通过唯一测试类来调用入口方法
     * 
     * @param datasetName 可选数据集（字符串名称）：规划器数据集（test-planner.xlsx)、执行器数据集（test-executor.xlsx)
     * @param testItem    可选测试项（字符串）：planner、executor
     * @param testConfig  可选方案及配置（字符串）：case1a、case1b...
     * @param repeatCount 可选数据集重复测试次数（整数）
     */
    public void executeTestPlan(String datasetName, String testItem, String testConfig, Integer repeatCount) {
        log.info("=== 启动测试方案 ===");
        log.info("参数: 数据集={}, 测试项={}, 配置={}, 重复次数={}", datasetName, testItem, testConfig, repeatCount);

        try {
            // 参数验证
            if (testItem == null || testItem.trim().isEmpty()) {
                log.error("测试项不能为空");
                return;
            }

            if (testConfig == null || testConfig.trim().isEmpty()) {
                log.error("测试配置不能为空");
                return;
            }

            if (repeatCount == null || repeatCount < 1) {
                repeatCount = 1;
            }

            // 根据测试项调用相应的测试方法
            for (int i = 0; i < repeatCount; i++) {
                log.info("开始第{}轮测试", i + 1);

                switch (testItem.toLowerCase()) {
                    case "executor":
                        executeExecutorTest(datasetName, testConfig, i + 1);
                        break;
                    case "planner":
                        executePlannerTest(datasetName, testConfig, i + 1);
                        break;
                    default:
                        log.error("不支持的测试项: {}", testItem);
                        return;
                }

                log.info("第{}轮测试完成", i + 1);
            }

        } catch (Exception e) {
            log.error("执行测试方案时发生异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 执行器测试方法
     * 
     * @param datasetName 数据集名称
     * @param testConfig  测试配置
     * @param roundNumber 轮次号
     */
    private void executeExecutorTest(String datasetName, String testConfig, int roundNumber) {
        String methodName = "testExecutor" + capitalize(testConfig);
        log.info("执行执行器测试方法: {}", methodName);

        try {
            // 调用执行器测试服务
            executorTestService.executeTest(roundNumber, testConfig, datasetName);
        } catch (Exception e) {
            log.error("执行执行器测试时发生异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 规划器测试方法
     * 
     * @param datasetName 数据集名称
     * @param testConfig  测试配置
     * @param roundNumber 轮次号
     */
    private void executePlannerTest(String datasetName, String testConfig, int roundNumber) {
//        String methodName = "testPlanner" + capitalize(testConfig);
//        log.info("执行规划器测试方法: {}", methodName);
//
//        try {
//            // 调用规划器测试服务
//            if ("case1a".equalsIgnoreCase(testConfig)) {
//                plannerTestService.executeCase1aTest(roundNumber);
//            } else {
//                log.error("不支持的规划器测试配置: {}", testConfig);
//            }
//        } catch (Exception e) {
//            log.error("执行规划器测试时发生异常: {}", e.getMessage(), e);
//        }
    }

    /**
     * 首字母大写
     * 
     * @param str 字符串
     * @return 首字母大写的字符串
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}