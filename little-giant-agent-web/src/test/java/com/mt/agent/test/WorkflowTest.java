package com.mt.agent.test;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.coze.openapi.client.workflows.run.model.WorkflowEvent;
import com.mt.agent.bottomReply.service.BottomReplyService;
import com.mt.agent.buffer.util.BufferUtil;
import com.mt.agent.consensus.util.ConsensusUtil;
import com.mt.agent.coze.CozeClient;
import com.mt.agent.coze.WorkflowEventHandler;
import com.mt.agent.reporter.StepResultData;
import com.mt.agent.reporter.SubEventReporter;
import com.mt.agent.router.service.RouterService;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 工作流自动化测试类
 * 用于读取Excel测试数据，执行工作流并保存结果
 *
 * @author MT Team
 * @date 2024/12/25
 */
@SpringBootTest
@Slf4j
public class WorkflowTest {

    @Autowired
    private CozeClient cozeClient;

    @Autowired
    private ConsensusUtil consensusUtil;

    @Autowired
    private RouterService routerService;

    @Autowired
    private BottomReplyService bottomReplyService;

    @Autowired
    private BufferUtil bufferUtil;

    @Autowired
    private TestDataCollector testDataCollector;

    // 测试用户ID
    private static final String TEST_USER_ID = "test-fb3";

    // Excel文件路径
    private static final String TEST_PLAN_FILE = "test_data3.xlsx";

    // 结果文件路径
    private static final String RESULT_FILE = "test_results_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

    // private static final String CHAT_WORKFLOW_ID = "7524699027813548051";
    // private static final String DELETE_WORKFLOW_ID = "7524699027813646355";
    // private static final String SAVE_WORKFLOW_ID = "7524699027813613587";
//    private static final String CHAT_WORKFLOW_ID = "7525630200932384806";
//    private static final String DELETE_WORKFLOW_ID = "7525630200932483110";
//    private static final String SAVE_WORKFLOW_ID = "7525630200932450342";
    private static final String CHAT_WORKFLOW_ID = "7525630200932384806";
    private static final String DELETE_WORKFLOW_ID = "7525630200932483110";
    private static final String SAVE_WORKFLOW_ID = "7525630200932450342";
    /**
     * 测试数据结构
     */
    public static class TestData {
        private String sessionId; // 会话编号
        private String roundNumber; // 对话轮次
        private String userInput; // 用户输入

        public TestData(String sessionId, String roundNumber, String userInput) {
            this.sessionId = sessionId;
            this.roundNumber = roundNumber;
            this.userInput = userInput;
        }

        // Getter methods
        public String getSessionId() {
            return sessionId;
        }

        public String getRoundNumber() {
            return roundNumber;
        }

        public String getUserInput() {
            return userInput;
        }
    }

    /**
     * 测试结果结构
     */
    public static class TestResult {
        private String sessionId; // 会话编号
        private String roundNumber; // 对话轮次
        private String userInput; // 用户输入
        private String historyInput; // 历史输入
        private String lastReply; // 上一次回复
        private String lastPlan; // 上一轮规划
        private String generatedCode; // 生成代码
        private String followUpText; // 追问文本
        private String costTime; // 花费时间
        private boolean executionSuccess; // 是否成功执行
        private String errorMessage; // 错误信息
        private String sqlCollection; // SQL集合
        private String finalOutputResult; // 最终输出结果

        public TestResult(String sessionId, String roundNumber, String userInput) {
            this.sessionId = sessionId;
            this.roundNumber = roundNumber;
            this.userInput = userInput;
            this.executionSuccess = false;
        }

        // Getter and Setter methods
        public String getSessionId() {
            return sessionId;
        }

        public String getRoundNumber() {
            return roundNumber;
        }

        public String getUserInput() {
            return userInput;
        }

        public String getHistoryInput() {
            return historyInput;
        }

        public void setHistoryInput(String historyInput) {
            this.historyInput = historyInput;
        }

        public String getLastReply() {
            return lastReply;
        }

        public void setLastReply(String lastReply) {
            this.lastReply = lastReply;
        }

        public String getLastPlan() {
            return lastPlan;
        }

        public void setLastPlan(String lastPlan) {
            this.lastPlan = lastPlan;
        }

        public String getGeneratedCode() {
            return generatedCode;
        }

        public void setGeneratedCode(String generatedCode) {
            this.generatedCode = generatedCode;
        }

        public String getFollowUpText() {
            return followUpText;
        }

        public void setFollowUpText(String followUpText) {
            this.followUpText = followUpText;
        }

        public String getCostTime() {
            return costTime;
        }

        public void setCostTime(String costTime) {
            this.costTime = costTime;
        }

        public boolean isExecutionSuccess() {
            return executionSuccess;
        }

        public void setExecutionSuccess(boolean executionSuccess) {
            this.executionSuccess = executionSuccess;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getSqlCollection() {
            return sqlCollection;
        }

        public void setSqlCollection(String sqlCollection) {
            this.sqlCollection = sqlCollection;
        }

        public String getFinalOutputResult() {
            return finalOutputResult;
        }

        public void setFinalOutputResult(String finalOutputResult) {
            this.finalOutputResult = finalOutputResult;
        }
    }

    /**
     * 主要的自动化测试方法
     */
    @Test
    public void runAutomatedWorkflowTest() {
        log.info("开始执行自动化工作流测试");

        try {
            // 1. 读取测试数据
            List<TestData> testDataList = readTestDataFromExcel();
            log.info("成功读取 {} 条测试数据", testDataList.size());

            // 2. 创建结果文件
            Workbook resultWorkbook = createResultWorkbook();

            // 3. 按会话编号分组执行测试
            Map<String, List<TestData>> sessionGroups = groupBySession(testDataList);

            int totalTests = 0;
            int successCount = 0;

            for (Map.Entry<String, List<TestData>> sessionEntry : sessionGroups.entrySet()) {
                String sessionId = sessionEntry.getKey();
                List<TestData> sessionTests = sessionEntry.getValue();

                log.info("开始处理会话: {}, 包含 {} 个测试", sessionId, sessionTests.size());

                // 清空历史记录，开始新会话
                clearChatHistory();

                for (TestData testData : sessionTests) {
                    totalTests++;
                    log.info("执行测试 {}/{}: 会话={}, 轮次={}",
                            totalTests, testDataList.size(), sessionId, testData.getRoundNumber());

                    // 执行单个测试
                    TestResult result = executeWorkflowTest(testData);

                    if (result.isExecutionSuccess()) {
                        successCount++;
                    }

                    // 立即保存结果
                    appendResultToWorkbook(resultWorkbook, result);
                    saveWorkbookToFile(resultWorkbook, RESULT_FILE);

                    // 输出详细的测试结果信息
                    String costTimeInfo = (result.getCostTime() != null && !result.getCostTime().isEmpty())
                            ? ", 花费时间: " + result.getCostTime()
                            : "";

                    if (result.isExecutionSuccess()) {
                        if (result.getGeneratedCode() != null && !result.getGeneratedCode().equals("无输出")) {
                            log.info("测试完成: 代码执行成功{}", costTimeInfo);
                        } else if (result.getFollowUpText() != null && !result.getFollowUpText().isEmpty()) {
                            log.info("测试完成: 获得追问文本回复{}", costTimeInfo);
                        } else {
                            log.info("测试完成: 成功但无具体输出{}", costTimeInfo);
                        }
                    } else {
                        log.info("测试完成: 失败 - {}{}", result.getErrorMessage(), costTimeInfo);
                    }
                }
            }

            log.info("自动化测试完成! 总计: {}, 成功: {}, 失败: {}",
                    totalTests, successCount, totalTests - successCount);

        } catch (Exception e) {
            log.error("自动化测试执行失败", e);
            throw new RuntimeException("自动化测试执行失败", e);
        } finally {
            // 📝 [重要] 完全清理测试数据收集器和回调注册
            try {
                testDataCollector.clearAll();
                log.info("🔒 [全局清理] 测试数据收集器已完全清理");
            } catch (Exception e) {
                log.warn("⚠️ [全局清理] 清理测试数据收集器失败", e);
            }
        }
    }

    /**
     * 从Excel文件读取测试数据
     */
    private List<TestData> readTestDataFromExcel() throws IOException {
        List<TestData> testDataList = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(TEST_PLAN_FILE);
                Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            // 跳过标题行，从第二行开始读取
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    String conversationId = getCellValue(row.getCell(0)); // 对话编号，格式如"1-1"
                    String userInput = getCellValue(row.getCell(1)); // 用户输入

                    if (conversationId != null && userInput != null) {
                        // 解析对话编号，从"1-1"中提取会话编号和对话轮次
                        String[] parts = parseConversationId(conversationId);
                        if (parts != null) {
                            String sessionId = parts[0];
                            String roundNumber = parts[1];
                            testDataList.add(new TestData(sessionId, roundNumber, userInput));
                        } else {
                            log.warn("无效的对话编号格式: {}, 跳过该行", conversationId);
                        }
                    }
                }
            }
        }

        return testDataList;
    }

    /**
     * 解析对话编号，从"1-1"格式中提取会话编号和对话轮次
     *
     * @param conversationId 对话编号，格式如"1-1"
     * @return 数组[会话编号, 对话轮次]，解析失败返回null
     */
    private String[] parseConversationId(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return null;
        }

        String[] parts = conversationId.trim().split("-");
        if (parts.length == 2) {
            try {
                // 验证两部分都是数字
                Integer.parseInt(parts[0]);
                Integer.parseInt(parts[1]);
                return parts;
            } catch (NumberFormatException e) {
                log.warn("对话编号包含非数字部分: {}", conversationId);
                return null;
            }
        } else {
            log.warn("对话编号格式不正确，应为'数字-数字'格式: {}", conversationId);
            return null;
        }
    }

    /**
     * 获取Excel单元格值
     */
    private String getCellValue(Cell cell) {
        if (cell == null)
            return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            default:
                return null;
        }
    }

    /**
     * 按会话编号分组
     */
    private Map<String, List<TestData>> groupBySession(List<TestData> testDataList) {
        Map<String, List<TestData>> sessionGroups = new LinkedHashMap<>();

        for (TestData testData : testDataList) {
            sessionGroups.computeIfAbsent(testData.getSessionId(), k -> new ArrayList<>())
                    .add(testData);
        }

        return sessionGroups;
    }

    /**
     * 清空聊天历史记录 (类似newChat方法)
     */
    private void clearChatHistory() {
        try {
            log.info("清空用户 {} 的聊天历史", TEST_USER_ID);

            String workflowID = DELETE_WORKFLOW_ID;
            Map<String, Object> data = new HashMap<>();
            data.put("userId", TEST_USER_ID);

            cozeClient.runWorkflow(data, workflowID);

            // 等待一秒确保清空操作完成
            Thread.sleep(1000);

        } catch (Exception e) {
            log.warn("清空聊天历史失败", e);
        }
    }

    /**
     * 执行单个工作流测试 (类似chatCozeForV13方法)
     */
    private TestResult executeWorkflowTest(TestData testData) {
        TestResult result = new TestResult(testData.getSessionId(),
                testData.getRoundNumber(),
                testData.getUserInput());

        // 创建增强的事件报告器来收集工作流执行过程中的详细信息
        TestSubEventReporter workflowEventReporter = new TestSubEventReporter(testDataCollector);

        try {
            log.info("执行工作流测试: {}", testData.getUserInput());

            // 准备数据
            Map<String, Object> data = new HashMap<>();
            data.put("input", testData.getUserInput());
            bufferUtil.setField(TEST_USER_ID, "question", testData.getUserInput(), -1, TimeUnit.DAYS);
            data.put("userId", TEST_USER_ID);

            String workflowID = CHAT_WORKFLOW_ID;

            // 执行工作流
            Flowable<WorkflowEvent> workflowEventFlowable = cozeClient.executeWorkflow(data, workflowID);

            // 处理工作流事件
            Map<String, String> completeNodeContent = WorkflowEventHandler
                    .getCompleteNodeContentV12(workflowEventFlowable, workflowEventReporter, consensusUtil,
                            TEST_USER_ID);

            // 解析结果
            String workflowEnd = completeNodeContent.get("End");
            if (workflowEnd != null) {
                log.info("工作流结束节点: {}", workflowEnd);

                JSONObject jsonObject = JSONUtil.parseObj(workflowEnd);
                String code = jsonObject.getStr("status_code");
                String targetAndView = jsonObject.getStr("target_and_view");

                // 提取任务名称和规划结果（用于后续处理）
                String taskName = getTaskName(targetAndView);
                String planResult = getPlanResult(targetAndView);

                // 获取历史输入 - 从工作流返回的JSON中获取
                String historyStr = jsonObject.getStr("historyStr");
                result.setHistoryInput(historyStr != null ? historyStr : "");
                bufferUtil.setField(TEST_USER_ID, "historyStr", historyStr, -1, TimeUnit.DAYS);

                // 获取上一次回复 - 从工作流返回的JSON中获取
                String lastReply = jsonObject.getStr("last_reply");
                result.setLastReply(lastReply != null ? lastReply : "");

                // 获取上一轮规划 - 从工作流返回的JSON中获取
                String lastPlan = jsonObject.getStr("last_plan");
                result.setLastPlan(lastPlan != null ? lastPlan : "");

                // 获取花费时间 - 从工作流返回的JSON中获取
                String costTime = jsonObject.getStr("cost_time");
                result.setCostTime(costTime != null ? costTime : "");
                if (costTime != null && !costTime.isEmpty()) {
                    log.info("工作流处理耗时: {}", costTime);
                }

                if ("002".equals(code)) {
                    // 创建增强的事件报告器来收集执行过程中的详细信息
                    // 启用测试数据收集模式
                    testDataCollector.enableTestMode();
                    TestSubEventReporter eventReporter = new TestSubEventReporter(testDataCollector);

                    try {
                        // 执行路由匹配 (代码执行) - 使用真实的RouterService收集数据
                        routerService.routeMatching(TEST_USER_ID, eventReporter);

                        // 获取生成的Python代码
                        String pythonCode = bufferUtil.getPythonCode(TEST_USER_ID);
                        result.setGeneratedCode(pythonCode);
                        result.setFollowUpText(""); // 代码执行情况下无追问文本
                        result.setExecutionSuccess(true);

                        // 从事件报告器中获取SQL集合和最终输出结果 - 合并两个报告器的数据
                        log.info("🔄 [数据收集] 开始从事件报告器收集SQL和输出数据...");

                        // 合并工作流事件报告器和执行事件报告器的数据
                        List<String> allSqlStatements = new ArrayList<>();
                        List<String> allFinalOutputs = new ArrayList<>();

                        // 从工作流事件报告器收集数据
                        allSqlStatements.addAll(workflowEventReporter.getSqlStatements());
                        allFinalOutputs.addAll(workflowEventReporter.getFinalOutputs());
                        log.info("📊 [工作流数据] SQL数量: {}, 输出数量: {}",
                                workflowEventReporter.getSqlStatements().size(),
                                workflowEventReporter.getFinalOutputs().size());

                        // 从执行事件报告器收集数据
                        allSqlStatements.addAll(eventReporter.getSqlStatements());
                        allFinalOutputs.addAll(eventReporter.getFinalOutputs());
                        log.info("📊 [执行数据] SQL数量: {}, 输出数量: {}",
                                eventReporter.getSqlStatements().size(),
                                eventReporter.getFinalOutputs().size());

                        String sqlCollection = String.join("\n", allSqlStatements);
                        String finalOutput = String.join("\n", allFinalOutputs);

                        log.info("📊 [数据收集] 合并后SQL集合内容: {}", sqlCollection);
                        log.info("📈 [数据收集] 合并后输出结果内容: {}", finalOutput);
                        log.info("🔢 [数据收集] 总计SQL数量: {}, 总计输出数量: {}",
                                allSqlStatements.size(), allFinalOutputs.size());

                        result.setSqlCollection(sqlCollection);
                        result.setFinalOutputResult(finalOutput);

                        log.info("✅ [数据收集] 成功设置到TestResult中");

                        // 注意：不要在这里关闭测试数据收集模式，因为可能还有其他地方需要使用数据
                        // testDataCollector.disableTestMode(); // 移到方法最后统一关闭

                        // 生成系统回复
                        String reply = bottomReplyService.replyForExecution(workflowEnd, historyStr, planResult,
                                taskName, TEST_USER_ID);
                        bufferUtil.saveOverReply(TEST_USER_ID, reply);
                        saveChatHistory(TEST_USER_ID, extractSystemReply(reply), pythonCode, SAVE_WORKFLOW_ID);

                    } catch (Exception e) {
                        // 即使执行失败，也要获取生成的Python代码
                        try {
                            String pythonCode = bufferUtil.getPythonCode(TEST_USER_ID);
                            result.setGeneratedCode(pythonCode != null ? pythonCode : "");
                        } catch (Exception codeException) {
                            log.warn("获取Python代码失败", codeException);
                            result.setGeneratedCode("无法获取生成的代码");
                        }

                        result.setFollowUpText(""); // 代码执行失败情况下无追问文本
                        result.setExecutionSuccess(false);

                        // 即使执行失败，也要从事件报告器中获取SQL集合和输出结果 - 合并两个报告器的数据
                        log.info("🔄 [失败场景数据收集] 尝试从事件报告器收集SQL和输出数据...");
                        try {
                            // 合并工作流事件报告器和执行事件报告器的数据
                            List<String> allSqlStatements = new ArrayList<>();
                            List<String> allFinalOutputs = new ArrayList<>();

                            // 从工作流事件报告器收集数据
                            allSqlStatements.addAll(workflowEventReporter.getSqlStatements());
                            allFinalOutputs.addAll(workflowEventReporter.getFinalOutputs());
                            log.info("📊 [失败场景工作流数据] SQL数量: {}, 输出数量: {}",
                                    workflowEventReporter.getSqlStatements().size(),
                                    workflowEventReporter.getFinalOutputs().size());

                            // 从执行事件报告器收集数据
                            String pythonCode = bufferUtil.getPythonCode(TEST_USER_ID);
                            allSqlStatements.addAll(eventReporter.getSqlStatements());
                            allFinalOutputs.addAll(eventReporter.getFinalOutputs());
                            log.info("📊 [失败场景执行数据] SQL数量: {}, 输出数量: {}",
                                    eventReporter.getSqlStatements().size(),
                                    eventReporter.getFinalOutputs().size());

                            String sqlCollection = String.join("\n", allSqlStatements);
                            String finalOutput = String.join("\n", allFinalOutputs);

                            log.info("📊 [失败场景数据收集] 合并后SQL集合内容: {}", sqlCollection);
                            log.info("📈 [失败场景数据收集] 合并后输出结果内容: {}", finalOutput);
                            log.info("🔢 [失败场景数据收集] 总计SQL数量: {}, 总计输出数量: {}",
                                    allSqlStatements.size(), allFinalOutputs.size());

                            result.setSqlCollection(sqlCollection);
                            result.setFinalOutputResult(finalOutput);

                            log.info("✅ [失败场景数据收集] 成功设置到TestResult中");

                            // 注意：不要在这里关闭测试数据收集模式，因为可能还有其他地方需要使用数据
                            // testDataCollector.disableTestMode(); // 移到方法最后统一关闭
                        } catch (Exception reportException) {
                            log.warn("❌ [失败场景数据收集] 获取SQL集合和输出结果失败", reportException);
                            result.setSqlCollection("");
                            result.setFinalOutputResult("");
                        }

                        // 提取详细的错误信息，包括事件报告器收集的信息
                        String detailedErrorMessage = extractDetailedErrorMessage(e);
                        String eventReporterErrors = eventReporter.getErrorSummary();

                        StringBuilder fullErrorMessage = new StringBuilder("代码执行失败: ");
                        fullErrorMessage.append(detailedErrorMessage);

                        if (!eventReporterErrors.isEmpty()) {
                            fullErrorMessage.append(" | ").append(eventReporterErrors);
                        }

                        // 添加执行步骤信息（最后几步）
                        List<String> stepMessages = eventReporter.getStepMessages();
                        if (!stepMessages.isEmpty()) {
                            fullErrorMessage.append(" | 执行步骤: ");
                            // 只显示最后3个步骤，避免信息过长
                            int startIndex = Math.max(0, stepMessages.size() - 3);
                            for (int i = startIndex; i < stepMessages.size(); i++) {
                                if (i > startIndex) {
                                    fullErrorMessage.append(" -> ");
                                }
                                fullErrorMessage.append(stepMessages.get(i));
                            }
                        }

                        result.setErrorMessage(fullErrorMessage.toString());
                        log.error("代码执行失败，详细信息: {}", fullErrorMessage.toString(), e);
                    }
                } else {
                    // 非执行代码的情况，记录追问文本
                    result.setGeneratedCode("无输出");

                    // 获取系统回复作为追问文本
                    String reply = jsonObject.getStr("plan_result");
                    String pythonCode = bufferUtil.getPythonCode(TEST_USER_ID);
                    if (reply != null && !reply.trim().isEmpty()) {
                        result.setFollowUpText(reply);
                        result.setExecutionSuccess(true); // 设置执行成功状态
                        log.info("获得追问文本，代码类型: {}", code);
                    } else {
                        result.setFollowUpText("无回复内容");
                        result.setExecutionSuccess(false);
                        result.setErrorMessage("工作流返回非执行代码但无回复内容: " + code);
                    }

                    // 从工作流事件报告器中获取SQL集合和输出结果 - 非执行场景主要依赖工作流数据
                    log.info("🔄 [非执行场景数据收集] 尝试从工作流事件报告器收集SQL和输出数据...");
                    try {
                        // 非执行场景主要从工作流事件报告器收集数据，因为没有Python代码执行
                        List<String> allSqlStatements = new ArrayList<>();
                        List<String> allFinalOutputs = new ArrayList<>();

                        // 从工作流事件报告器收集数据
                        allSqlStatements.addAll(workflowEventReporter.getSqlStatements());
                        allFinalOutputs.addAll(workflowEventReporter.getFinalOutputs());
                        log.info("📊 [非执行场景工作流数据] SQL数量: {}, 输出数量: {}",
                                workflowEventReporter.getSqlStatements().size(),
                                workflowEventReporter.getFinalOutputs().size());

                        String sqlCollection = String.join("\n", allSqlStatements);
                        String finalOutput = String.join("\n", allFinalOutputs);

                        log.info("📊 [非执行场景数据收集] SQL集合内容: {}", sqlCollection);
                        log.info("📈 [非执行场景数据收集] 输出结果内容: {}", finalOutput);
                        log.info("🔢 [非执行场景数据收集] 总计SQL数量: {}, 总计输出数量: {}",
                                allSqlStatements.size(), allFinalOutputs.size());

                        result.setSqlCollection(sqlCollection);
                        result.setFinalOutputResult(finalOutput);

                        log.info("✅ [非执行场景数据收集] 成功设置到TestResult中");

                        // 注意：不要在这里关闭测试数据收集模式，因为可能还有其他地方需要使用数据
                        // testDataCollector.disableTestMode(); // 移到方法最后统一关闭
                    } catch (Exception reportException) {
                        log.warn("❌ [非执行场景数据收集] 获取SQL集合和输出结果失败", reportException);
                        result.setSqlCollection("");
                        result.setFinalOutputResult("");
                    }
                }
            } else {
                result.setGeneratedCode("无输出");
                result.setFollowUpText("");
                result.setCostTime("");
                String pythonCode = bufferUtil.getPythonCode(TEST_USER_ID);
                result.setExecutionSuccess(true);
                result.setErrorMessage("工作流未返回结束节点");

                // 即使工作流未返回结束节点，也要尝试从事件报告器中获取SQL集合和输出结果
                log.info("🔄 [无结束节点场景数据收集] 尝试从工作流事件报告器收集SQL和输出数据...");
                try {
                    String sqlCollection = workflowEventReporter.getSqlCollectionString();
                    String finalOutput = workflowEventReporter.getFinalOutputString();

                    log.info("📊 [无结束节点场景数据收集] SQL集合内容: {}", sqlCollection);
                    log.info("📈 [无结束节点场景数据收集] 输出结果内容: {}", finalOutput);
                    log.info("🔢 [无结束节点场景数据收集] SQL数量: {}, 输出数量: {}",
                            workflowEventReporter.getSqlStatements().size(),
                            workflowEventReporter.getFinalOutputs().size());

                    result.setSqlCollection(sqlCollection);
                    result.setFinalOutputResult(finalOutput);

                    log.info("✅ [无结束节点场景数据收集] 成功设置到TestResult中");
                } catch (Exception reportException) {
                    log.warn("❌ [无结束节点场景数据收集] 获取SQL集合和输出结果失败", reportException);
                    result.setSqlCollection("");
                    result.setFinalOutputResult("");
                }
            }

        } catch (Exception e) {
            result.setGeneratedCode("无输出");
            result.setFollowUpText("");
            result.setCostTime("");
            String pythonCode = bufferUtil.getPythonCode(TEST_USER_ID);
            result.setExecutionSuccess(true);

            // 提取详细的错误信息，包括工作流事件报告器收集的信息
            String detailedErrorMessage = extractDetailedErrorMessage(e);
            String workflowReporterErrors = workflowEventReporter.getErrorSummary();

            StringBuilder fullErrorMessage = new StringBuilder("工作流执行异常: ");
            fullErrorMessage.append(detailedErrorMessage);

            if (!workflowReporterErrors.isEmpty()) {
                fullErrorMessage.append(" | ").append(workflowReporterErrors);
            }

            // 添加工作流执行步骤信息
            List<String> workflowSteps = workflowEventReporter.getStepMessages();
            if (!workflowSteps.isEmpty()) {
                fullErrorMessage.append(" | 工作流步骤: ");
                // 只显示最后3个步骤
                int startIndex = Math.max(0, workflowSteps.size() - 3);
                for (int i = startIndex; i < workflowSteps.size(); i++) {
                    if (i > startIndex) {
                        fullErrorMessage.append(" -> ");
                    }
                    fullErrorMessage.append(workflowSteps.get(i));
                }
            }

            result.setErrorMessage(fullErrorMessage.toString());
            log.error("工作流执行异常，详细信息: {}", fullErrorMessage.toString(), e);

            // 即使工作流执行异常，也要尝试从事件报告器中获取SQL集合和输出结果
            log.info("🔄 [异常场景数据收集] 尝试从工作流事件报告器收集SQL和输出数据...");
            try {
                String sqlCollection = workflowEventReporter.getSqlCollectionString();
                String finalOutput = workflowEventReporter.getFinalOutputString();

                log.info("📊 [异常场景数据收集] SQL集合内容: {}", sqlCollection);
                log.info("📈 [异常场景数据收集] 输出结果内容: {}", finalOutput);
                log.info("🔢 [异常场景数据收集] SQL数量: {}, 输出数量: {}",
                        workflowEventReporter.getSqlStatements().size(),
                        workflowEventReporter.getFinalOutputs().size());

                result.setSqlCollection(sqlCollection);
                result.setFinalOutputResult(finalOutput);

                log.info("✅ [异常场景数据收集] 成功设置到TestResult中");
            } catch (Exception reportException) {
                log.warn("❌ [异常场景数据收集] 获取SQL集合和输出结果失败", reportException);
                result.setSqlCollection("");
                result.setFinalOutputResult("");
            }
        }

        // 📝 [重要] 在方法结束前统一关闭测试数据收集模式
        // 确保所有数据都已经收集完毕后再关闭
        try {
            testDataCollector.disableTestMode();
            log.info("🔒 [测试结束] 测试数据收集模式已关闭");
        } catch (Exception e) {
            log.warn("⚠️ [测试结束] 关闭测试数据收集模式失败", e);
        }

        return result;
    }

    /**
     * 提取详细的错误信息，包括异常链和堆栈跟踪的关键信息
     *
     * @param throwable 异常对象
     * @return 详细的错误描述
     */
    private String extractDetailedErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "未知错误";
        }

        StringBuilder errorDetails = new StringBuilder();

        // 1. 获取异常类型和消息
        errorDetails.append("[").append(throwable.getClass().getSimpleName()).append("] ");
        if (throwable.getMessage() != null && !throwable.getMessage().trim().isEmpty()) {
            errorDetails.append(throwable.getMessage());
        } else {
            errorDetails.append("无具体错误消息");
        }

        // 2. 查找并提取根本原因
        Throwable rootCause = getRootCause(throwable);
        if (rootCause != throwable && rootCause != null) {
            errorDetails.append(" | 根本原因: [").append(rootCause.getClass().getSimpleName()).append("] ");
            if (rootCause.getMessage() != null && !rootCause.getMessage().trim().isEmpty()) {
                errorDetails.append(rootCause.getMessage());
            }
        }

        // 3. 提取关键的堆栈跟踪信息（前3行相关信息）
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            errorDetails.append(" | 错误位置: ");
            int count = 0;
            for (StackTraceElement element : stackTrace) {
                // 只显示项目相关的堆栈信息，跳过系统库
                if (element.getClassName().contains("com.mt.agent") && count < 2) {
                    if (count > 0) {
                        errorDetails.append(" -> ");
                    }
                    errorDetails.append(element.getClassName())
                            .append(".")
                            .append(element.getMethodName())
                            .append("(")
                            .append(element.getFileName())
                            .append(":")
                            .append(element.getLineNumber())
                            .append(")");
                    count++;
                }
            }
        }

        // 4. 检查是否有特定的已知错误模式
        String errorMessage = throwable.getMessage();
        if (errorMessage != null) {
            if (errorMessage.contains("JavaFileExecutor")) {
                errorDetails.append(" | 分析: Python代码执行器发生错误");
            } else if (errorMessage.contains("SQLException")) {
                errorDetails.append(" | 分析: 数据库执行错误");
            } else if (errorMessage.contains("IOException")) {
                errorDetails.append(" | 分析: 文件I/O操作错误");
            } else if (errorMessage.contains("NullPointerException")) {
                errorDetails.append(" | 分析: 空指针异常，可能是数据未正确初始化");
            } else if (errorMessage.contains("timeout") || errorMessage.contains("超时")) {
                errorDetails.append(" | 分析: 操作超时");
            } else if (errorMessage.contains("Connection")) {
                errorDetails.append(" | 分析: 连接相关错误");
            }
        }

        return errorDetails.toString();
    }

    /**
     * 获取异常链的根本原因
     *
     * @param throwable 异常对象
     * @return 根本原因异常
     */
    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    /**
     * 提取任务名称
     */
    private String getTaskName(String input) {
        if (input == null)
            return "";

        Pattern taskTargetPattern = Pattern.compile("【任务目标】(.*)");
        Matcher taskTargetMatcher = taskTargetPattern.matcher(input);
        if (taskTargetMatcher.find()) {
            return taskTargetMatcher.group(1).trim();
        }
        return "";
    }

    /**
     * 提取规划结果
     */
    private String getPlanResult(String input) {
        if (input == null)
            return "";

        Pattern presentationFormPattern = Pattern.compile("【呈现形式】\\s*([^\\S\\r\\n]*)(.*)", Pattern.DOTALL);
        Matcher presentationFormMatcher = presentationFormPattern.matcher(input);
        if (presentationFormMatcher.find()) {
            return presentationFormMatcher.group(2).trim();
        }
        return "";
    }

    /**
     * 提取系统回复
     */
    private String extractSystemReply(String reply) {
        if (reply == null)
            return "";

        Pattern pattern = Pattern.compile(
                "【系统回复】[\\s]*(.*?)\\s*【追问用户】[\\s]*(.*)",
                Pattern.DOTALL);

        Matcher matcher = pattern.matcher(reply);
        if (matcher.find() && matcher.groupCount() >= 1) {
            return matcher.group(1).trim();
        }
        return reply;
    }

    /**
     * 保存聊天历史
     */
    private void saveChatHistory(String userId, String content, String pyCode, String workflowID) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("content", content);
            data.put("userId", userId);
            data.put("conversationId", userId);
            data.put("role", "system");
            data.put("py_code", pyCode);

            if (workflowID == null) {
                workflowID = SAVE_WORKFLOW_ID;
            }

            Flowable<WorkflowEvent> workflowEventFlowable = cozeClient.executeWorkflow(data, workflowID);
            WorkflowEventHandler.getCompleteNodeContentV12(workflowEventFlowable, new TestSubEventReporter(),
                    consensusUtil, userId);

        } catch (Exception e) {
            log.warn("保存聊天历史失败", e);
        }
    }

    /**
     * 创建结果Excel工作簿
     */
    private Workbook createResultWorkbook() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Test Results");

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = { "会话编号", "对话轮次", "用户输入", "历史输入", "上一次回复", "上一轮规划", "生成代码", "追问文本", "花费时间", "是否成功执行",
                "错误信息", "SQL集合", "最终输出结果" };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        return workbook;
    }

    /**
     * 将结果添加到Excel工作簿
     */
    private void appendResultToWorkbook(Workbook workbook, TestResult result) {
        log.info("📝 [Excel写入] 开始写入测试结果到Excel...");
        log.info("📝 [Excel写入] 会话: {}, 轮次: {}, 输入: {}",
                result.getSessionId(), result.getRoundNumber(), result.getUserInput());

        Sheet sheet = workbook.getSheetAt(0);
        int lastRowNum = sheet.getLastRowNum();
        Row row = sheet.createRow(lastRowNum + 1);

        row.createCell(0).setCellValue(result.getSessionId());
        row.createCell(1).setCellValue(result.getRoundNumber());
        row.createCell(2).setCellValue(result.getUserInput());
        row.createCell(3).setCellValue(result.getHistoryInput() != null ? result.getHistoryInput() : "");
        row.createCell(4).setCellValue(result.getLastReply() != null ? result.getLastReply() : "");
        row.createCell(5).setCellValue(result.getLastPlan() != null ? result.getLastPlan() : "");
        row.createCell(6).setCellValue(result.getGeneratedCode() != null ? result.getGeneratedCode() : "");
        row.createCell(7).setCellValue(result.getFollowUpText() != null ? result.getFollowUpText() : "");
        row.createCell(8).setCellValue(result.getCostTime() != null ? result.getCostTime() : "");
        row.createCell(9).setCellValue(result.isExecutionSuccess() ? "是" : "否");
        row.createCell(10).setCellValue(result.getErrorMessage() != null ? result.getErrorMessage() : "");

        // 重点关注SQL集合和最终输出结果的写入
        String sqlCollection = result.getSqlCollection() != null ? result.getSqlCollection() : "";
        String finalOutputResult = result.getFinalOutputResult() != null ? result.getFinalOutputResult() : "";

        log.info("📊 [Excel写入] SQL集合字段内容(长度{}): {}", sqlCollection.length(), sqlCollection);
        log.info("📈 [Excel写入] 最终输出结果字段内容(长度{}): {}", finalOutputResult.length(), finalOutputResult);

        row.createCell(11).setCellValue(sqlCollection);
        row.createCell(12).setCellValue(finalOutputResult);

        log.info("✅ [Excel写入] 成功写入第{}行数据", lastRowNum + 1);
    }

    /**
     * 将工作簿保存到文件
     */
    private void saveWorkbookToFile(Workbook workbook, String filename) {
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            workbook.write(fos);
            log.debug("结果已保存到文件: {}", filename);
        } catch (IOException e) {
            log.error("保存结果文件失败", e);
        }
    }

    /**
     * 生成示例测试Excel文件
     */
    @Test
    public void generateSampleTestPlan() {
        log.info("开始生成示例测试计划Excel文件");

        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Test Plan");

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("对话编号");
            headerRow.createCell(1).setCellValue("用户输入");

            // 添加示例数据 - 新格式：对话编号（如"1-1"）, 用户输入
            String[][] sampleData = {
                    { "1-1", "分析一下2023年医药制造业的营收情况" },
                    { "1-2", "请生成一个柱状图显示这个数据" },
                    { "2-1", "查询最近3年汽车行业的利润趋势" },
                    { "2-2", "用折线图展示这个趋势" },
                    { "3-1", "统计2024年电子信息制造业的员工数量" },
                    { "3-2", "请按照地区分组显示这些数据" },
                    { "4-1", "计算近5年房地产行业的投资回报率" },
                    { "5-1", "生成一份关于新能源汽车销量的分析报告" }
            };

            for (int i = 0; i < sampleData.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < sampleData[i].length; j++) {
                    row.createCell(j).setCellValue(sampleData[i][j]);
                }
            }

            // 自动调整列宽
            for (int i = 0; i < 2; i++) {
                sheet.autoSizeColumn(i);
            }

            // 保存文件
            try (FileOutputStream fos = new FileOutputStream("test-plan.xlsx")) {
                workbook.write(fos);
                log.info("示例测试计划文件已生成: test-plan.xlsx");
            }

            workbook.close();

        } catch (Exception e) {
            log.error("生成示例测试计划文件失败", e);
            throw new RuntimeException("生成示例测试计划文件失败", e);
        }
    }

    /**
     * 测试用的SubEventReporter实现
     * 增强了错误收集能力，用于捕获更详细的执行过程错误信息
     * 同时收集SQL生成和最终输出结果
     */
    private static class TestSubEventReporter extends SubEventReporter {
        private final List<String> errorMessages = new ArrayList<>();
        private final List<String> stepMessages = new ArrayList<>();
        private final List<String> sqlStatements = new ArrayList<>();
        private final List<String> finalOutputs = new ArrayList<>();
        // 添加去重机制
        private final Set<String> processedMessages = new HashSet<>();
        // 添加TestDataCollector引用
        private final TestDataCollector testDataCollector;

        public TestSubEventReporter() {
            super(null);
            this.testDataCollector = null;
        }

        public TestSubEventReporter(TestDataCollector testDataCollector) {
            super(null);
            this.testDataCollector = testDataCollector;
        }

        /**
         * 检查消息是否已经处理过，避免重复处理
         */
        private boolean isMessageProcessed(String message, String type) {
            String messageKey = type + ":" + message.hashCode();
            return !processedMessages.add(messageKey);
        }

        @Override
        public void reportStep(String message) {
            if (message != null) {
                stepMessages.add(message);
                log.info("=== [STEP] === {}", message);

                // 🚨 添加详细检测日志
                log.info("🔍 [STEP检测] 消息长度: {}, 内容预览: {}",
                        message.length(),
                        message.length() > 100 ? message.substring(0, 100) + "..." : message);

                // 检查是否包含SQL生成信息 - 增强检测条件
                boolean containsSql = message.contains("gen_sql") || message.contains("生成SQL")
                        || message.contains("SQL语句") || message.contains("SQL生成")
                        || message.contains("【定制任务执行】查询单行业数据SQL生成结果")
                        || message.contains("【定制任务执行】")
                        || message.contains("SELECT") || message.contains("select")
                        || message.contains("```sql") || message.contains("```SQL")
                        || message.contains("FROM") || message.contains("from")
                        || message.contains("WHERE") || message.contains("where");

                log.info("🔍 [STEP检测] SQL检测结果: {}", containsSql);

                if (containsSql) {
                    log.info("🔍 [SQL检测] reportStep中发现SQL相关信息，开始提取...");
                    extractSqlFromMessage(message);
                    log.info("📊 [SQL状态] 当前SQL集合大小: {}", sqlStatements.size());
                }

                // 检查是否包含最终输出结果 - 增强检测条件
                boolean containsOutput = message.contains("vis_") || message.contains("reportNodeResult")
                        || message.contains("Node Result") || message.contains("NODE RESULT")
                        || message.contains("可视化") || message.contains("输出结果") || message.contains("报告")
                        || message.contains("图表") || message.contains("表格") || message.contains("分析")
                        || message.contains("steps_summary") || message.contains("总结")
                        || message.contains("执行成功") || message.contains("生成完成") || message.contains("输出完成")
                        || message.contains("IndicatorBlock") || message.contains("type")
                        || message.contains("label") || message.contains("value") || message.contains("unit")
                        || (message.contains("[") && message.contains("]")
                                && (message.contains("营业收入") || message.contains("利润") || message.contains("企业")))
                        || (message.contains("{") && message.contains("}")
                                && (message.contains("title") || message.contains("data")));

                log.info("🎯 [STEP检测] 输出检测结果: {}", containsOutput);

                if (containsOutput) {
                    log.info("🎯 [输出检测] reportStep中发现输出相关信息，开始提取...");
                    extractOutputFromMessage(message);
                    log.info("📈 [输出状态] 当前输出集合大小: {}", finalOutputs.size());
                }
            }
        }

        @Override
        public void reportStepResult(StepResultData resultData) {
            if (resultData != null) {
                log.info("=== [STEP RESULT] === {}", resultData);

                // 检查结果数据中是否包含SQL或输出结果
                String resultStr = resultData.toString();
                boolean containsSql = resultStr.contains("gen_sql") || resultStr.contains("SQL") ||
                        resultStr.contains("【定制任务执行】查询单行业数据SQL生成结果") ||
                        resultStr.contains("SELECT") || resultStr.contains("select") || resultStr.contains("```sql");

                if (containsSql) {
                    log.info("🔍 [SQL检测] reportStepResult中发现SQL相关信息，开始提取...");
                    extractSqlFromMessage(resultStr);
                    log.info("📊 [SQL状态] 当前SQL集合大小: {}", sqlStatements.size());
                }

                // 检查是否包含最终输出结果
                boolean containsOutput = resultStr.contains("vis_") || resultStr.contains("reportNodeResult")
                        || resultStr.contains("Node Result") ||
                        resultStr.contains("可视化") || resultStr.contains("输出结果") || resultStr.contains("报告") ||
                        resultStr.contains("图表") || resultStr.contains("表格") || resultStr.contains("分析") ||
                        resultStr.contains("steps_summary") || resultStr.contains("总结") ||
                        resultStr.contains("执行成功") || resultStr.contains("生成完成") || resultStr.contains("输出完成") ||
                        (resultStr.contains("[") && resultStr.contains("]")
                                && (resultStr.contains("营业收入") || resultStr.contains("利润") || resultStr.contains("企业")))
                        ||
                        (resultStr.contains("{") && resultStr.contains("}")
                                && (resultStr.contains("title") || resultStr.contains("data")));

                if (containsOutput) {
                    log.info("🎯 [输出检测] reportStepResult中发现输出相关信息，开始提取...");
                    extractOutputFromMessage(resultStr);
                    log.info("📈 [输出状态] 当前输出集合大小: {}", finalOutputs.size());
                }
            }
        }

        @Override
        public void reportNodeResult(Object resultPayload) {
            if (resultPayload != null) {
                log.info("=== [NODE RESULT] === {}", resultPayload);

                String resultStr = resultPayload.toString();
                // 检查节点结果中是否包含SQL或输出结果
                boolean containsSql = resultStr.contains("gen_sql") || resultStr.contains("SQL") ||
                        resultStr.contains("【定制任务执行】查询单行业数据SQL生成结果") ||
                        resultStr.contains("SELECT") || resultStr.contains("select") || resultStr.contains("```sql");

                if (containsSql) {
                    log.info("🔍 [SQL检测] reportNodeResult中发现SQL相关信息，开始提取...");
                    extractSqlFromMessage(resultStr);
                    log.info("📊 [SQL状态] 当前SQL集合大小: {}", sqlStatements.size());
                }

                boolean containsOutput = resultStr.contains("vis_") || resultStr.contains("reportNodeResult")
                        || resultStr.contains("Node Result") ||
                        resultStr.contains("可视化") || resultStr.contains("输出结果") || resultStr.contains("报告") ||
                        resultStr.contains("图表") || resultStr.contains("表格") || resultStr.contains("分析") ||
                        resultStr.contains("steps_summary") || resultStr.contains("总结") ||
                        resultStr.contains("执行成功") || resultStr.contains("生成完成") || resultStr.contains("输出完成") ||
                        (resultStr.contains("[") && resultStr.contains("]")
                                && (resultStr.contains("营业收入") || resultStr.contains("利润") || resultStr.contains("企业")))
                        ||
                        (resultStr.contains("{") && resultStr.contains("}")
                                && (resultStr.contains("title") || resultStr.contains("data")));

                if (containsOutput) {
                    log.info("🎯 [输出检测] reportNodeResult中发现输出相关信息，开始提取...");
                    extractOutputFromMessage(resultStr);
                    log.info("📈 [输出状态] 当前输出集合大小: {}", finalOutputs.size());
                }
            }
        }

        @Override
        public void reportError(String errorMessage, Throwable throwable) {
            if (errorMessage != null) {
                String detailedError = "错误消息: " + errorMessage;
                if (throwable != null) {
                    detailedError += " | 异常类型: " + throwable.getClass().getSimpleName();
                    if (throwable.getMessage() != null) {
                        detailedError += " | 异常详情: " + throwable.getMessage();
                    }
                }
                errorMessages.add(detailedError);
                log.error("Error: {}", errorMessage, throwable);
            }
        }

        @Override
        public void reportError(String error) {
            if (error != null) {
                errorMessages.add("错误: " + error);
                log.error("Error: {}", error);
            }
        }

        /**
         * 获取收集到的所有错误信息
         *
         * @return 错误信息列表
         */
        public List<String> getErrorMessages() {
            return new ArrayList<>(errorMessages);
        }

        /**
         * 获取收集到的所有步骤信息
         *
         * @return 步骤信息列表
         */
        public List<String> getStepMessages() {
            return new ArrayList<>(stepMessages);
        }

        /**
         * 获取格式化的错误摘要
         *
         * @return 格式化的错误信息
         */
        public String getErrorSummary() {
            if (errorMessages.isEmpty()) {
                return "";
            }
            return "执行过程错误: " + String.join(" | ", errorMessages);
        }

        /**
         * 从消息中提取SQL语句
         *
         * @param message 消息内容
         */
        private void extractSqlFromMessage(String message) {
            if (message == null || message.trim().isEmpty()) {
                log.info("⚠️  [SQL提取] 消息为空，跳过");
                return;
            }

            // 检查是否已经处理过这条消息
            if (isMessageProcessed(message, "SQL")) {
                log.info("⚠️  [SQL提取] 消息已处理过，跳过重复提取");
                return;
            }

            log.info("🔍 [SQL提取] 开始分析消息: \n{}", message);
            int initialSize = sqlStatements.size();

            // 专注于从实际执行日志中提取SQL，不从代码中提取

            // 1. 从"【定制任务执行】查询单行业数据SQL生成结果"这类实际执行日志中提取
            if (message.contains("【定制任务执行】") && message.contains("SQL生成结果")) {
                log.info("📋 [SQL实际执行] 检测到SQL执行结果日志，开始提取...");

                // 匹配 "【定制任务执行】查询单行业数据SQL生成结果: 实际SQL语句"
                Pattern executionSqlPattern = Pattern.compile("【定制任务执行】[^:]*SQL生成结果[:\\s]+(.*?)(?=\\n|$)",
                        Pattern.MULTILINE);
                Matcher executionSqlMatcher = executionSqlPattern.matcher(message);
                while (executionSqlMatcher.find()) {
                    String sql = executionSqlMatcher.group(1).trim();
                    log.info("📋 [SQL实际执行] 从执行日志中发现SQL: {}", sql);
                    sql = cleanSqlStatement(sql);
                    if (!sql.isEmpty() && sql.toLowerCase().contains("select") && !sqlStatements.contains(sql)) {
                        sqlStatements.add(sql);
                        log.info("✅ [SQL实际执行] 成功添加执行SQL: {}", sql);
                    } else {
                        log.info("⚠️  [SQL实际执行] SQL为空或重复，跳过");
                    }
                }
            }

            // 2. 从其他可能的实际SQL执行日志中提取
            if (message.contains("执行SQL") || message.contains("查询结果") || message.contains("数据库查询")) {
                log.info("📋 [SQL执行日志] 检测到SQL执行相关日志，开始提取...");

                // 查找实际的SELECT语句，但排除代码块和函数调用
                Pattern actualSqlPattern = Pattern.compile(
                        "(?i)(?<!gen_sql\\(|```python|```sql|def |\\s{4,})(SELECT[\\s\\S]*?)(?=\\n|$|;)",
                        Pattern.MULTILINE);
                Matcher actualSqlMatcher = actualSqlPattern.matcher(message);
                while (actualSqlMatcher.find()) {
                    String sql = actualSqlMatcher.group(1).trim();
                    log.info("📋 [SQL执行日志] 发现实际SQL: {}", sql);

                    // 确保这不是Python代码中的字符串
                    if (!sql.contains("gen_sql") && !sql.contains("\"") && !sql.contains("'")) {
                        sql = cleanSqlStatement(sql);
                        if (!sql.isEmpty() && !sqlStatements.contains(sql)) {
                            sqlStatements.add(sql);
                            log.info("✅ [SQL执行日志] 成功添加SQL: {}", sql);
                        } else {
                            log.info("⚠️  [SQL执行日志] SQL为空或重复，跳过");
                        }
                    } else {
                        log.info("⚠️  [SQL执行日志] 检测到代码语法，跳过: {}", sql);
                    }
                }
            }

            int finalSize = sqlStatements.size();
            log.info("📊 [SQL提取] 提取完成，SQL集合从 {} 变为 {}, 新增 {} 条SQL", initialSize, finalSize, finalSize - initialSize);
            if (finalSize > 0) {
                log.info("📊 [SQL提取] 当前所有SQL: {}", sqlStatements);
            }
        }

        /**
         * 清理SQL语句，移除不必要的标记和格式
         */
        private String cleanSqlStatement(String sql) {
            if (sql == null) {
                log.info("🧹 [SQL清理] 输入SQL为null，返回空字符串");
                return "";
            }

            log.info("🧹 [SQL清理] 原始SQL: {}", sql);

            // 移除SQL代码块标记
            String beforeCodeBlock = sql;
            sql = sql.replaceAll("```[sS][qQ][lL]", "").replaceAll("```", "");
            if (!beforeCodeBlock.equals(sql)) {
                log.info("🧹 [SQL清理] 移除代码块标记后: {}", sql);
            }

            // 移除多余的空白字符
            String beforeWhitespace = sql;
            sql = sql.replaceAll("\\s+", " ");
            if (!beforeWhitespace.equals(sql)) {
                log.info("🧹 [SQL清理] 清理空白字符后: {}", sql);
            }

            // 确保语句以分号结束（如果不是以分号结尾）
            sql = sql.trim();
            if (!sql.isEmpty() && !sql.endsWith(";")) {
                sql += ";";
                log.info("🧹 [SQL清理] 添加分号后: {}", sql);
            }

            log.info("🧹 [SQL清理] 最终清理结果: {}", sql);
            return sql;
        }

        /**
         * 从消息中提取输出结果
         *
         * @param message 消息内容
         */
        private void extractOutputFromMessage(String message) {
            if (message == null || message.trim().isEmpty()) {
                log.info("⚠️  [输出提取] 消息为空，跳过");
                return;
            }

            // 检查是否已经处理过这条消息
            if (isMessageProcessed(message, "OUTPUT")) {
                log.info("⚠️  [输出提取] 消息已处理过，跳过重复提取");
                return;
            }

            log.info("🎯 [输出提取] 开始分析消息: \n{}", message);
            int initialSize = finalOutputs.size();

            // 专注于提取实际的执行结果，不提取函数调用代码

            // 1. 从"=== [NODE RESULT] ==="这类实际执行结果中提取
            if (message.contains("=== [NODE RESULT] ===") || message.contains("NODE RESULT")) {
                log.info("📋 [实际结果] 检测到NODE RESULT执行结果，开始提取...");

                // 匹配 "=== [NODE RESULT] === 实际数据"
                Pattern nodeResultPattern = Pattern.compile("===\\s*\\[NODE RESULT\\]\\s*===\\s*(.*?)(?=\\n|$)",
                        Pattern.MULTILINE);
                Matcher nodeResultMatcher = nodeResultPattern.matcher(message);
                while (nodeResultMatcher.find()) {
                    String nodeResult = nodeResultMatcher.group(1).trim();
                    log.info("📋 [实际结果] 从NODE RESULT中发现: {}", nodeResult);
                    if (!nodeResult.isEmpty() && !finalOutputs.contains(nodeResult)) {
                        finalOutputs.add(nodeResult);
                        log.info("✅ [实际结果] 成功添加NODE RESULT: {}", nodeResult);
                    } else {
                        log.info("⚠️  [实际结果] NODE RESULT为空或重复，跳过");
                    }
                }
            }

            // 2. 检测IndicatorBlock格式的实际数据输出（JSON格式的业务结果）
            if (message.contains("IndicatorBlock") ||
                    (message.contains("type") && message.contains("label") && message.contains("value")
                            && message.contains("unit"))) {
                log.info("📋 [业务数据] 检测到IndicatorBlock业务数据，开始提取...");

                // 匹配完整的IndicatorBlock JSON对象
                Pattern indicatorPattern = Pattern.compile(
                        "\\{[^{}]*(?:type[^{}]*IndicatorBlock|IndicatorBlock[^{}]*type)[^{}]*\\}", Pattern.MULTILINE);
                Matcher indicatorMatcher = indicatorPattern.matcher(message);
                while (indicatorMatcher.find()) {
                    String indicatorContent = indicatorMatcher.group().trim();
                    log.info("📋 [业务数据] 发现IndicatorBlock数据: {}", indicatorContent);
                    if (!finalOutputs.contains(indicatorContent)) {
                        finalOutputs.add(indicatorContent);
                        log.info("✅ [业务数据] 成功添加IndicatorBlock: {}", indicatorContent);
                    } else {
                        log.info("⚠️  [业务数据] IndicatorBlock重复，跳过");
                    }
                }
            }

            // 3. 检测其他包含业务数据的JSON结果
            if (message.contains("{") && message.contains("}") &&
                    (message.contains("label") || message.contains("value") || message.contains("data") ||
                            message.contains("企业") || message.contains("数量") || message.contains("营业收入") ||
                            message.contains("利润") || message.contains("行业"))) {
                log.info("📋 [JSON结果] 检测到包含业务数据的JSON，开始提取...");

                // 匹配包含业务字段的JSON对象
                Pattern businessJsonPattern = Pattern.compile("\\{[^{}]*(?:label|value|data|企业|数量|营业收入|利润|行业)[^{}]*\\}",
                        Pattern.MULTILINE);
                Matcher businessJsonMatcher = businessJsonPattern.matcher(message);
                while (businessJsonMatcher.find()) {
                    String jsonContent = businessJsonMatcher.group().trim();
                    log.info("📋 [JSON结果] 发现业务JSON: {}", jsonContent);
                    // 确保这不是代码中的参数，而是实际数据
                    if (!jsonContent.contains("vis_") && !jsonContent.contains("gen_")
                            && !finalOutputs.contains(jsonContent)) {
                        finalOutputs.add(jsonContent);
                        log.info("✅ [JSON结果] 成功添加业务JSON: {}", jsonContent);
                    } else {
                        log.info("⚠️  [JSON结果] JSON包含代码语法或重复，跳过");
                    }
                }
            }

            // 4. 检测包含明确数值结果的输出
            if (message.contains("结果") || message.contains("输出") || message.contains("数据")) {
                log.info("📋 [数值结果] 检测到数值结果相关信息，开始提取...");

                // 查找包含具体数值的结果行
                Pattern dataResultPattern = Pattern.compile("(?:结果|输出|数据)[:\\s]*([0-9.,]+[^\\n]*?)(?=\\n|$)",
                        Pattern.MULTILINE);
                Matcher dataResultMatcher = dataResultPattern.matcher(message);
                while (dataResultMatcher.find()) {
                    String dataResult = dataResultMatcher.group(1).trim();
                    log.info("📋 [数值结果] 发现数值结果: {}", dataResult);
                    if (!dataResult.isEmpty() && dataResult.length() > 3 && !finalOutputs.contains(dataResult)) {
                        finalOutputs.add(dataResult);
                        log.info("✅ [数值结果] 成功添加数值结果: {}", dataResult);
                    } else {
                        log.info("⚠️  [数值结果] 数值结果过短或重复，跳过");
                    }
                }
            }

            // 5. 检测reportNodeResult方法的实际输出参数
            if (message.contains("reportNodeResult") && message.contains("(")) {
                log.info("📋 [方法输出] 检测到reportNodeResult方法调用，提取参数...");

                // 匹配reportNodeResult方法的参数
                Pattern methodOutputPattern = Pattern.compile("reportNodeResult\\s*\\(\\s*([^)]+)\\s*\\)",
                        Pattern.MULTILINE);
                Matcher methodOutputMatcher = methodOutputPattern.matcher(message);
                while (methodOutputMatcher.find()) {
                    String methodOutput = methodOutputMatcher.group(1).trim();
                    log.info("📋 [方法输出] 发现方法输出参数: {}", methodOutput);
                    // 确保这是实际数据而不是变量名
                    if (!methodOutput.contains("resultPayload") && !methodOutput.isEmpty()
                            && !finalOutputs.contains(methodOutput)) {
                        finalOutputs.add(methodOutput);
                        log.info("✅ [方法输出] 成功添加方法输出: {}", methodOutput);
                    } else {
                        log.info("⚠️  [方法输出] 方法输出是变量名或重复，跳过");
                    }
                }
            }

            int finalSize = finalOutputs.size();
            log.info("📊 [输出提取] 提取完成，输出集合从 {} 变为 {}, 新增 {} 条输出", initialSize, finalSize, finalSize - initialSize);
            if (finalSize > 0) {
                log.info("📊 [输出提取] 当前所有输出: {}", finalOutputs);
            }
        }

        /**
         * 获取收集到的所有SQL语句
         *
         * @return SQL语句列表
         */
        public List<String> getSqlStatements() {
            // 优先从TestDataCollector获取真实数据
            if (testDataCollector != null) {
                List<String> realSqlStatements = testDataCollector.getCollectedSQL();
                log.info("🔄 从TestDataCollector获取SQL数据: {}", realSqlStatements.size());

                // 合并真实数据和传统收集的数据
                Set<String> allSql = new HashSet<>();
                allSql.addAll(realSqlStatements);
                allSql.addAll(sqlStatements);

                return allSql.stream()
                        .filter(sql -> sql != null && !sql.trim().isEmpty())
                        .collect(Collectors.toList());
            }

            // 回退到传统方式
            return new ArrayList<>(sqlStatements);
        }

        /**
         * 获取收集到的所有最终输出结果
         *
         * @return 输出结果列表
         */
        public List<String> getFinalOutputs() {
            // 优先从TestDataCollector获取真实数据
            if (testDataCollector != null) {
                List<String> realVisualizations = testDataCollector.getCollectedVisualizations();
                log.info("🔄 从TestDataCollector获取可视化数据: {}", realVisualizations.size());

                // 合并真实数据和传统收集的数据
                Set<String> allOutputs = new HashSet<>();
                allOutputs.addAll(realVisualizations);
                allOutputs.addAll(finalOutputs);

                return allOutputs.stream()
                        .filter(output -> output != null && !output.trim().isEmpty())
                        .collect(Collectors.toList());
            }

            // 回退到传统方式
            return new ArrayList<>(finalOutputs);
        }

        /**
         * 获取格式化的SQL集合字符串
         *
         * @return 格式化的SQL字符串，多个SQL之间用换行分隔
         */
        public String getSqlCollectionString() {
            if (sqlStatements.isEmpty()) {
                return "";
            }
            return String.join("\n", sqlStatements);
        }

        /**
         * 获取格式化的最终输出结果字符串
         *
         * @return 格式化的输出结果字符串
         */
        public String getFinalOutputString() {
            if (finalOutputs.isEmpty()) {
                return "";
            }
            return String.join("\n", finalOutputs);
        }

        @Override
        public void reportOverReply(String reply) {
            if (reply != null) {
                log.debug("Over Reply: {}", reply);
            }
        }

        @Override
        public void reportComplete() {
            log.debug("Complete");
        }

        @Override
        public void reportCompleteAndClose() {
            log.debug("Complete and close");
        }

        @Override
        public void reportThinking(String thinking) {
            if (thinking != null) {
                log.info("=== [THINKING] === {}", thinking);
                log.info("🔍 [THINKING检测] 消息长度: {}", thinking.length());

                // 检查思考内容中是否包含SQL或输出结果 - 增强检测条件
                boolean containsSql = thinking.contains("gen_sql") || thinking.contains("SQL")
                        || thinking.contains("sql")
                        || thinking.contains("【定制任务执行】查询单行业数据SQL生成结果")
                        || thinking.contains("【定制任务执行】")
                        || thinking.contains("SELECT") || thinking.contains("select")
                        || thinking.contains("```sql") || thinking.contains("```SQL")
                        || thinking.contains("FROM") || thinking.contains("from")
                        || thinking.contains("WHERE") || thinking.contains("where")
                        || thinking.contains("```Python") || thinking.contains("```python")
                        || thinking.contains("exec_sql") || thinking.contains("result =");

                log.info("🔍 [THINKING检测] SQL检测结果: {}", containsSql);

                if (containsSql) {
                    log.info("🔍 [SQL检测] reportThinking中发现SQL相关信息，开始提取...");
                    extractSqlFromMessage(thinking);
                    log.info("📊 [SQL状态] 当前SQL集合大小: {}", sqlStatements.size());
                }

                boolean containsOutput = thinking.contains("NODE RESULT") || thinking.contains("IndicatorBlock")
                        || thinking.contains("reportNodeResult") || thinking.contains("输出结果")
                        || thinking.contains("执行结果") || thinking.contains("业务数据")
                        || (thinking.contains("label") && thinking.contains("value") && thinking.contains("type"))
                        || thinking.contains("steps_summary") || thinking.contains("总结")
                        || thinking.contains("执行成功") || thinking.contains("生成完成") || thinking.contains("输出完成")
                        || thinking.contains("IndicatorBlock") || thinking.contains("type")
                        || thinking.contains("label") || thinking.contains("value") || thinking.contains("unit")
                        || thinking.contains("vis_textbox") || thinking.contains("vis_textblock")
                        || thinking.contains("vis_table") || thinking.contains("vis_single_bar")
                        || (thinking.contains("[") && thinking.contains("]")
                                && (thinking.contains("营业收入") || thinking.contains("利润") || thinking.contains("企业")))
                        || (thinking.contains("{") && thinking.contains("}")
                                && (thinking.contains("title") || thinking.contains("data")));

                log.info("🎯 [THINKING检测] 输出检测结果: {}", containsOutput);

                if (containsOutput) {
                    log.info("🎯 [输出检测] reportThinking中发现输出相关信息，开始提取...");
                    extractOutputFromMessage(thinking);
                    log.info("📈 [输出状态] 当前输出集合大小: {}", finalOutputs.size());
                }
            }
        }

        @Override
        public void reportTaskPlan(String[] planContent) {
            if (planContent != null && planContent.length > 0) {
                log.debug("Task Plan: {}", String.join(", ", planContent));
            }
        }

        @Override
        public void reportAnswer(String answer) {
            if (answer != null) {
                log.info("=== [ANSWER] === {}", answer);
                log.info("🔍 [ANSWER检测] 消息长度: {}", answer.length());

                // 检查答案中是否包含SQL或输出结果 - 增强检测条件
                boolean containsSql = answer.contains("gen_sql") || answer.contains("SQL") || answer.contains("sql")
                        || answer.contains("【定制任务执行】查询单行业数据SQL生成结果")
                        || answer.contains("【定制任务执行】")
                        || answer.contains("SELECT") || answer.contains("select")
                        || answer.contains("```sql") || answer.contains("```SQL")
                        || answer.contains("FROM") || answer.contains("from")
                        || answer.contains("WHERE") || answer.contains("where");

                log.info("🔍 [ANSWER检测] SQL检测结果: {}", containsSql);

                if (containsSql) {
                    log.info("🔍 [SQL检测] reportAnswer中发现SQL相关信息，开始提取...");
                    extractSqlFromMessage(answer);
                    log.info("📊 [SQL状态] 当前SQL集合大小: {}", sqlStatements.size());
                }

                boolean containsOutput = answer.contains("NODE RESULT") || answer.contains("IndicatorBlock")
                        || answer.contains("reportNodeResult") || answer.contains("输出结果")
                        || answer.contains("执行结果") || answer.contains("业务数据")
                        || (answer.contains("label") && answer.contains("value") && answer.contains("type"))
                        || answer.contains("执行成功") || answer.contains("生成完成") || answer.contains("输出完成")
                        || (answer.contains("{") && answer.contains("}")
                                && (answer.contains("企业") || answer.contains("数量") || answer.contains("营业收入")));

                log.info("🎯 [ANSWER检测] 输出检测结果: {}", containsOutput);

                if (containsOutput) {
                    log.info("🎯 [输出检测] reportAnswer中发现输出相关信息，开始提取...");
                    extractOutputFromMessage(answer);
                    log.info("📈 [输出状态] 当前输出集合大小: {}", finalOutputs.size());
                }
            }
        }

        @Override
        public void reportReply(String reply) {
            if (reply != null) {
                log.info("=== [REPLY] === {}", reply);
                log.info("🔍 [REPLY检测] 消息长度: {}", reply.length());

                // 检查回复中是否包含SQL或输出结果 - 增强检测条件
                boolean containsSql = reply.contains("gen_sql") || reply.contains("SQL") || reply.contains("sql")
                        || reply.contains("【定制任务执行】查询单行业数据SQL生成结果")
                        || reply.contains("【定制任务执行】")
                        || reply.contains("SELECT") || reply.contains("select")
                        || reply.contains("```sql") || reply.contains("```SQL")
                        || reply.contains("FROM") || reply.contains("from")
                        || reply.contains("WHERE") || reply.contains("where");

                log.info("🔍 [REPLY检测] SQL检测结果: {}", containsSql);

                if (containsSql) {
                    log.info("🔍 [SQL检测] reportReply中发现SQL相关信息，开始提取...");
                    extractSqlFromMessage(reply);
                    log.info("📊 [SQL状态] 当前SQL集合大小: {}", sqlStatements.size());
                }

                boolean containsOutput = reply.contains("NODE RESULT") || reply.contains("IndicatorBlock")
                        || reply.contains("reportNodeResult") || reply.contains("输出结果")
                        || reply.contains("执行结果") || reply.contains("业务数据")
                        || (reply.contains("label") && reply.contains("value") && reply.contains("type"))
                        || reply.contains("执行成功") || reply.contains("生成完成") || reply.contains("输出完成")
                        || (reply.contains("{") && reply.contains("}")
                                && (reply.contains("企业") || reply.contains("数量") || reply.contains("营业收入")));

                log.info("🎯 [REPLY检测] 输出检测结果: {}", containsOutput);

                if (containsOutput) {
                    log.info("🎯 [输出检测] reportReply中发现输出相关信息，开始提取...");
                    extractOutputFromMessage(reply);
                    log.info("📈 [输出状态] 当前输出集合大小: {}", finalOutputs.size());
                }
            }
        }

        @Override
        public void reportTree(String treeJson) {
            if (treeJson != null) {
                log.debug("Tree: {}", treeJson);

                // 检查树结构中是否包含SQL或输出结果
                if (treeJson.contains("gen_sql") || treeJson.contains("SQL") ||
                        treeJson.contains("【定制任务执行】查询单行业数据SQL生成结果") ||
                        treeJson.contains("SELECT") || treeJson.contains("select") || treeJson.contains("```sql")) {
                    extractSqlFromMessage(treeJson);
                }

                if (treeJson.contains("NODE RESULT") || treeJson.contains("IndicatorBlock") ||
                        treeJson.contains("reportNodeResult") || treeJson.contains("输出结果") ||
                        treeJson.contains("执行结果") || treeJson.contains("业务数据") ||
                        (treeJson.contains("label") && treeJson.contains("value") && treeJson.contains("type")) ||
                        treeJson.contains("执行成功") || treeJson.contains("生成完成") || treeJson.contains("输出完成") ||
                        (treeJson.contains("{") && treeJson.contains("}") &&
                                (treeJson.contains("企业") || treeJson.contains("数量") || treeJson.contains("营业收入")))) {
                    extractOutputFromMessage(treeJson);
                }
            }
        }

        @Override
        public void reportRecommend(String recommend) {
            if (recommend != null) {
                log.debug("Recommend: {}", recommend);

                // 检查推荐中是否包含SQL或输出结果
                if (recommend.contains("gen_sql") || recommend.contains("SQL") ||
                        recommend.contains("【定制任务执行】查询单行业数据SQL生成结果") ||
                        recommend.contains("SELECT") || recommend.contains("select") || recommend.contains("```sql")) {
                    extractSqlFromMessage(recommend);
                }

                if (recommend.contains("NODE RESULT") || recommend.contains("IndicatorBlock") ||
                        recommend.contains("reportNodeResult") || recommend.contains("输出结果") ||
                        recommend.contains("执行结果") || recommend.contains("业务数据") ||
                        (recommend.contains("label") && recommend.contains("value") && recommend.contains("type")) ||
                        recommend.contains("执行成功") || recommend.contains("生成完成") || recommend.contains("输出完成") ||
                        (recommend.contains("{") && recommend.contains("}") &&
                                (recommend.contains("企业") || recommend.contains("数量") || recommend.contains("营业收入")))) {
                    extractOutputFromMessage(recommend);
                }
            }
        }

        @Override
        public void reportJson(String json) {
            if (json != null) {
                log.debug("Json: {}", json);

                // 检查JSON中是否包含SQL或输出结果
                if (json.contains("gen_sql") || json.contains("SQL") ||
                        json.contains("【定制任务执行】查询单行业数据SQL生成结果") ||
                        json.contains("SELECT") || json.contains("select") || json.contains("```sql")) {
                    extractSqlFromMessage(json);
                }

                if (json.contains("NODE RESULT") || json.contains("IndicatorBlock") ||
                        json.contains("reportNodeResult") || json.contains("输出结果") ||
                        json.contains("执行结果") || json.contains("业务数据") ||
                        (json.contains("label") && json.contains("value") && json.contains("type")) ||
                        json.contains("执行成功") || json.contains("生成完成") || json.contains("输出完成") ||
                        (json.contains("{") && json.contains("}") &&
                                (json.contains("企业") || json.contains("数量") || json.contains("营业收入")))) {
                    extractOutputFromMessage(json);
                }
            }
        }
    }
}
