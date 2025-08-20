//package com.mt.agent.test.service;
//
//import cn.hutool.json.JSONObject;
//import cn.hutool.json.JSONUtil;
//import com.coze.openapi.client.workflows.run.model.WorkflowEvent;
//import com.mt.agent.coze.CozeClient;
//import com.mt.agent.coze.WorkflowEventHandler;
//import com.mt.agent.consensus.util.ConsensusUtil;
//import com.mt.agent.test.model.PlannerTestData;
//import com.mt.agent.test.model.PlannerTestResult;
//import com.mt.agent.test.util.ExcelUtil;
//import com.mt.agent.test.util.PythonExecutor;
//import com.mt.agent.test.util.ThreadPoolUtil;
//import io.reactivex.Flowable;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//import java.util.concurrent.Future;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.stream.Collectors;
//
///**
// * 规划器测试服务
// * 专门处理规划器相关的测试逻辑
// *
// * @author MT Agent Team
// * @version 1.0
// * @since 2025-01-15
// */
//@Slf4j
//@Service
//public class PlannerTestService {
//
//    @Autowired
//    private CozeClient cozeClient;
//
//    @Autowired
//    private ConsensusUtil consensusUtil;
//
//    @Autowired
//    private ExcelUtil excelUtil;
//
//    @Autowired
//    private PythonExecutor pythonExecutor;
//
//    @Autowired
//    private ThreadPoolUtil threadPoolUtil;
//
//    // 常量定义 - 各案例对应的Coze工作流ID
//    private static final String CASE1A_COZE_WORKFLOW_ID = "7518982183785283594";
//    private static final String CASE1B_COZE_WORKFLOW_ID = "7518982183785283595"; // 示例ID，需要根据实际情况配置
//    private static final String CASE1C_COZE_WORKFLOW_ID = "7518982183785283596"; // 示例ID，需要根据实际情况配置
//    private static final String CASE1D_COZE_WORKFLOW_ID = "7518982183785283597"; // 示例ID，需要根据实际情况配置
//    private static final String CASE1E_COZE_WORKFLOW_ID = "7518982183785283598"; // 示例ID，需要根据实际情况配置
//    private static final String CASE2A_COZE_WORKFLOW_ID = "7524281606367674408"; // 示例ID，需要根据实际情况配置
//    private static final String CASE3A_COZE_WORKFLOW_ID = "7524281606367674409";
//    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
//
//    /**
//     * 通用规划器测试方法
//     *
//     * @param roundNumber 数据集重复测试次数（如：2表示数据集测试两遍）
//     * @param testConfig  测试配置
//     * @param fileName    测试数据文件名
//     */
//    public void executeTest(int roundNumber, String testConfig, String fileName) {
//        log.info("=== 开始规划器测试 {}，数据集将重复测试 {} 次 ===", testConfig, roundNumber);
//
//        try {
//            // 1. 读取测试数据（只读取一次）
//            List<PlannerTestData> testDataList = excelUtil.readPlannerTestData(fileName);
//            log.info("读取到 {} 条测试数据", testDataList.size());
//
//            // 2. 准备收集所有轮次的结果
//            List<PlannerTestResult> allResults = new ArrayList<>();
//            int totalTestCount = testDataList.size() * roundNumber;
//
//            // 3. 对数据集进行指定次数的重复测试
//            for (int currentRound = 1; currentRound <= roundNumber; currentRound++) {
//                log.info("--- 开始第 {}/{} 轮数据集测试 ---", currentRound, roundNumber);
//
//                // 4. 按轮次分组，确保同一轮对话使用同一个userId
//                Map<String, List<PlannerTestData>> roundGroups = testDataList.stream()
//                        .collect(Collectors.groupingBy(data -> {
//                            String questionId = data.getQuestionId();
//                            if (questionId.contains("-")) {
//                                return questionId.split("-")[0];
//                            }
//                            return questionId;
//                        }));
//
//                // 5. 准备当前轮次的结果收集
//                List<PlannerTestResult> currentRoundResults = new ArrayList<>();
//                AtomicInteger completedCount = new AtomicInteger(0);
//
//                // 6. 创建当前轮次的测试任务（按轮次分组）
//                List<Future<List<PlannerTestResult>>> futures = new ArrayList<>();
//                AtomicInteger userIdCounter = new AtomicInteger(1);
//
//                for (Map.Entry<String, List<PlannerTestData>> entry : roundGroups.entrySet()) {
//                    String roundKey = entry.getKey();
//                    List<PlannerTestData> roundData = entry.getValue();
//                    String userId = "test-planner-" + testConfig + "-round" + currentRound + "-"
//                            + userIdCounter.getAndIncrement();
//                    final int roundNum = currentRound; // 为lambda表达式创建final变量
//
//                    Future<List<PlannerTestResult>> future = threadPoolUtil.submit(() -> {
//                        return executeRoundTest(roundData, roundNum, userId, testConfig);
//                    });
//                    futures.add(future);
//                }
//
//                // 7. 等待当前轮次所有任务完成并收集结果
//                for (Future<List<PlannerTestResult>> future : futures) {
//                    try {
//                        List<PlannerTestResult> roundResults = future.get();
//                        currentRoundResults.addAll(roundResults);
//                        int completed = completedCount.addAndGet(roundResults.size());
//                        int totalCompleted = allResults.size() + completed;
//                        log.info("第{}轮已完成 {}/{} 个测试，总进度: {}/{}",
//                                currentRound, completed, testDataList.size(), totalCompleted, totalTestCount);
//                    } catch (Exception e) {
//                        log.error("获取第{}轮测试结果时发生异常: {}", currentRound, e.getMessage(), e);
//                    }
//                }
//
//                // 8. 按问题编号排序当前轮次结果
//                currentRoundResults.sort(Comparator.comparing(PlannerTestResult::getQuestionId));
//
//                // 9. 添加到总结果集中
//                allResults.addAll(currentRoundResults);
//
//                log.info("--- 第 {}/{} 轮数据集测试完成，本轮完成 {} 个测试 ---",
//                        currentRound, roundNumber, currentRoundResults.size());
//            }
//
//            // 10. 最终按问题编号和轮次排序
//            allResults.sort((r1, r2) -> {
//                int idCompare = r1.getQuestionId().compareTo(r2.getQuestionId());
//                if (idCompare != 0) {
//                    return idCompare;
//                }
//                return Integer.compare(r1.getRoundNumber(), r2.getRoundNumber());
//            });
//
//            // 11. 保存所有结果
//            String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
//            String resultFileName = String.format("planner-result-%s-rounds%d-%s.xlsx", testConfig, roundNumber,
//                    timestamp);
//            excelUtil.writePlannerTestResults(resultFileName, allResults);
//
//            log.info("=== 规划器测试 {} 完成，共进行 {} 轮测试，总计 {} 个测试，结果已保存到: {} ===",
//                    testConfig, roundNumber, allResults.size(), resultFileName);
//
//        } catch (Exception e) {
//            log.error("规划器测试 {} 发生异常: {}", testConfig, e.getMessage(), e);
//        }
//    }
//
//    /**
//     * 规划器测试方法 - Case 1a (向后兼容)
//     *
//     * @param roundNumber 测试轮次
//     */
//    public void executeCase1aTest(int roundNumber) {
//        executeTest(roundNumber, "case1a", "test-planner.xlsx");
//    }
//
//    /**
//     * 规划器测试方法 - Case 1b
//     *
//     * @param roundNumber 数据集重复测试次数
//     * @param fileName    测试数据文件名
//     */
//    public void executeCase1bTest(int roundNumber, String fileName) {
//        executeTest(roundNumber, "case1b", fileName);
//    }
//
//    /**
//     * 规划器测试方法 - Case 1c
//     *
//     * @param roundNumber 数据集重复测试次数
//     * @param fileName    测试数据文件名
//     */
//    public void executeCase1cTest(int roundNumber, String fileName) {
//        executeTest(roundNumber, "case1c", fileName);
//    }
//
//    /**
//     * 规划器测试方法 - Case 1d
//     *
//     * @param roundNumber 数据集重复测试次数
//     * @param fileName    测试数据文件名
//     */
//    public void executeCase1dTest(int roundNumber, String fileName) {
//        executeTest(roundNumber, "case1d", fileName);
//    }
//
//    /**
//     * 规划器测试方法 - Case 1e
//     *
//     * @param roundNumber 数据集重复测试次数
//     * @param fileName    测试数据文件名
//     */
//    public void executeCase1eTest(int roundNumber, String fileName) {
//        executeTest(roundNumber, "case1e", fileName);
//    }
//
//    /**
//     * 规划器测试方法 - Case 2a
//     *
//     * @param roundNumber 数据集重复测试次数
//     * @param fileName    测试数据文件名
//     */
//    public void executeCase2aTest(int roundNumber, String fileName) {
//        executeTest(roundNumber, "case2a", fileName);
//    }
//
//    /**
//     * 规划器测试方法 - Case 3a
//     *
//     * @param roundNumber 数据集重复测试次数
//     * @param fileName    测试数据文件名
//     */
//    public void executeCase3aTest(int roundNumber, String fileName) {
//        executeTest(roundNumber, "case3a", fileName);
//    }
//
//    /**
//     * 执行单轮规划器测试
//     *
//     * @param roundData   单轮测试数据
//     * @param roundNumber 轮次号
//     * @param userId      用户ID
//     * @param testConfig  测试配置
//     * @return 测试结果列表
//     */
//    private List<PlannerTestResult> executeRoundTest(List<PlannerTestData> roundData, int roundNumber, String userId,
//            String testConfig) {
//        List<PlannerTestResult> results = new ArrayList<>();
//
//        for (PlannerTestData testData : roundData) {
//            PlannerTestResult result = executeSingleTest(testData, roundNumber, userId, testConfig);
//            results.add(result);
//        }
//
//        return results;
//    }
//
//    /**
//     * 执行单个规划器测试
//     *
//     * @param testData    测试数据
//     * @param roundNumber 轮次号
//     * @param userId      用户ID
//     * @param testConfig  测试配置
//     * @return 测试结果
//     */
//    private PlannerTestResult executeSingleTest(PlannerTestData testData, int roundNumber, String userId,
//            String testConfig) {
//        PlannerTestResult result = new PlannerTestResult();
//        result.setQuestionId(testData.getQuestionId());
//        result.setRoundNumber(roundNumber);
//        result.setUserInput(testData.getUserInput());
//        result.setHistoryInput(testData.getHistoryInput());
//        result.setLastPlan(testData.getLastPlan());
//        result.setLastReply(testData.getLastReply());
//
//        long startTime = System.currentTimeMillis();
//
//        try {
//            // 1. 准备Coze工作流参数
//            Map<String, Object> params = new HashMap<>();
//            params.put("input", testData.getUserInput());
//            params.put("userId", userId);
//            params.put("diag_history", testData.getHistoryInput());
//            params.put("last_plan", testData.getLastPlan());
//            params.put("last_reply", testData.getLastReply());
//
//            // 2. 根据测试配置选择对应的Workflow ID
//            String workflowId = getWorkflowIdByConfig(testConfig);
//
//            // 3. 执行Coze工作流
//            Flowable<WorkflowEvent> workflowEventFlowable = cozeClient.executeWorkflow(params, workflowId);
//
//            // 4. 处理工作流事件
//            Map<String, String> completeNodeContent = WorkflowEventHandler
//                    .getCompleteNodeContent(workflowEventFlowable, null, consensusUtil, userId);
//
//            long planTime = System.currentTimeMillis() - startTime;
//            result.setPlanTime(planTime);
//
//            // 5. 解析工作流结果
//            String endNodeContent = completeNodeContent.get("End");
//            if (endNodeContent != null) {
//                try {
//                    JSONObject jsonResult = JSONUtil.parseObj(endNodeContent);
//                    String statusCode = jsonResult.getStr("status_code");
//                    String planResult = jsonResult.getStr("plan_result");
//
//                    // 判断是否生成了Python代码
//                    if ("002".equals(statusCode)) {
//                        result.setGeneratePythonCode(true);
//                        result.setPythonCode(planResult);
//
//                        // 6. 执行Python代码
//                        if (planResult != null && !planResult.trim().isEmpty()) {
//                            PythonExecutor.PythonResult pythonResult = pythonExecutor.executePython(planResult);
//                            result.setPythonExecuteSuccess(pythonResult.isSuccess());
//                            if (pythonResult.isSuccess()) {
//                                result.setPythonExecuteResult(pythonResult.getOutput());
//                            } else {
//                                result.setErrorLog(pythonResult.getError());
//                            }
//                        }
//                    } else {
//                        result.setGeneratePythonCode(false);
//                        result.setPythonExecuteSuccess(false);
//                        result.setPythonExecuteResult("未生成Python代码");
//                    }
//
//                    log.info("规划器测试完成，问题ID: {}, 生成代码: {}", testData.getQuestionId(), result.getGeneratePythonCode());
//
//                } catch (Exception e) {
//                    result.setErrorLog("解析工作流结果时发生异常: " + e.getMessage());
//                    log.error("解析工作流结果时发生异常，问题ID: {}, 错误: {}", testData.getQuestionId(), e.getMessage());
//                }
//            } else {
//                result.setErrorLog("工作流未返回End节点内容");
//                log.error("工作流未返回End节点内容，问题ID: {}", testData.getQuestionId());
//            }
//
//        } catch (Exception e) {
//            result.setErrorLog("执行工作流时发生异常: " + e.getMessage());
//            log.error("执行规划器测试时发生异常，问题ID: {}, 错误: {}", testData.getQuestionId(), e.getMessage());
//        }
//
//        return result;
//    }
//
//    /**
//     * 根据测试配置获取对应的Workflow ID
//     *
//     * @param testConfig 测试配置
//     * @return Workflow ID
//     */
//    private String getWorkflowIdByConfig(String testConfig) {
//        switch (testConfig.toLowerCase()) {
//            case "case1a":
//                return CASE1A_COZE_WORKFLOW_ID;
//            case "case1b":
//                return CASE1B_COZE_WORKFLOW_ID;
//            case "case1c":
//                return CASE1C_COZE_WORKFLOW_ID;
//            case "case1d":
//                return CASE1D_COZE_WORKFLOW_ID;
//            case "case1e":
//                return CASE1E_COZE_WORKFLOW_ID;
//            case "case2a":
//                return CASE2A_COZE_WORKFLOW_ID;
//            case "case3a":
//                return CASE3A_COZE_WORKFLOW_ID;
//            default:
//                log.warn("未知的测试配置: {}，使用默认的case1a配置", testConfig);
//                return CASE1A_COZE_WORKFLOW_ID;
//        }
//    }
//}