package com.mt.agent.test.service;

import cn.hutool.json.JSONUtil;
import com.mt.agent.sysUtil.AISQLQueryUtil;
import com.mt.agent.test.model.ExecutorTestData;
import com.mt.agent.test.model.ExecutorTestResult;
import com.mt.agent.test.util.ExcelUtil;
import com.mt.agent.test.util.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 执行器测试服务
 * 专门处理执行器相关的测试逻辑
 *
 * @author MT Agent Team
 * @version 1.0
 * @since 2025-01-15
 */
@Slf4j
@Service
public class ExecutorTestService {

    @Autowired
    private AISQLQueryUtil aisqlQueryUtil;

    @Autowired
    private ExcelUtil excelUtil;

    @Autowired
    private ThreadPoolUtil threadPoolUtil;

    // 常量定义
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * 任务执行完成后的休眠时间（毫秒）
     * 用于避免API请求过快导致的限流问题
     */
    private static final long TASK_EXECUTION_SLEEP_MILLIS = 500L;

    /**
     * 通用执行器测试方法
     * 
     * @param roundNumber 数据集重复测试次数（如：2表示数据集测试两遍）
     * @param testConfig  测试配置
     */
    public void executeTest(int roundNumber, String testConfig, String fileName) {
        log.info("=== 开始执行器测试 {}，数据集将重复测试 {} 次 ===", testConfig, roundNumber);

        try {
            // 1. 读取测试数据（只读取一次）
            String datasetPath = fileName;
            List<ExecutorTestData> testDataList = excelUtil.readExecutorTestData(datasetPath);
            log.info("读取到 {} 条测试数据", testDataList.size());

            // 2. 准备结果文件
            String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            String resultFileName = String.format("executor-result-%s-rounds%d-%s.xlsx", testConfig, roundNumber,
                    timestamp);

            // 3. 准备计数器
            AtomicInteger totalCompletedTests = new AtomicInteger(0);
            AtomicInteger totalCompletedSQLs = new AtomicInteger(0);
            int totalTestCount = testDataList.size() * roundNumber;

            // 4. 对数据集进行指定次数的重复测试
            for (int currentRound = 1; currentRound <= roundNumber; currentRound++) {
                log.info("--- 开始第 {}/{} 轮数据集测试 ---", currentRound, roundNumber);

                // 5. 准备当前轮次的任务
                List<Future<List<ExecutorTestResult>>> futures = new ArrayList<>();
                for (ExecutorTestData testData : testDataList) {
                    final int roundNum = currentRound; // 为lambda表达式创建final变量
                    Future<List<ExecutorTestResult>> future = threadPoolUtil.submit(() -> {
                        try {
                            List<ExecutorTestResult> results = executeSingleTest(testData, roundNum, testConfig);

                            // 任务执行完成后休眠，避免API请求过快
                            Thread.sleep(TASK_EXECUTION_SLEEP_MILLIS);

                            return results;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.warn("任务休眠被中断: {}", e.getMessage());
                            return executeSingleTest(testData, roundNum, testConfig);
                        }
                    });
                    futures.add(future);
                }

                // 6. 即时处理完成的任务并保存结果
                for (Future<List<ExecutorTestResult>> future : futures) {
                    try {
                        List<ExecutorTestResult> results = future.get();

                        // 立即保存结果
                        excelUtil.appendExecutorTestResults(resultFileName, results);

                        // 更新计数器
                        int completedTests = totalCompletedTests.incrementAndGet();
                        int completedSQLs = totalCompletedSQLs.addAndGet(results.size());

                        log.info("第{}轮已完成 {}/{} 个测试用例（产生{}个SQL记录），总进度: {}/{} 个测试用例，总计产生{}个SQL记录",
                                currentRound, completedTests - (currentRound - 1) * testDataList.size(),
                                testDataList.size(),
                                results.size(), completedTests, totalTestCount, completedSQLs);

                    } catch (Exception e) {
                        log.error("处理第{}轮测试结果时发生异常: {}", currentRound, e.getMessage(), e);

                        // 即使发生异常，也要保存一个错误记录
                        ExecutorTestResult errorResult = new ExecutorTestResult();
                        errorResult.setQuestionId("ERROR_" + System.currentTimeMillis());
                        errorResult.setRoundNumber(currentRound);
                        errorResult.setSqlNumber("SQL1");
                        errorResult.setUserInput("测试异常，无法获取用户输入");
                        errorResult.setHistoryInput("测试异常，无法获取历史输入");
                        errorResult.setPythonCode("测试异常，无法获取Python代码");
                        errorResult.setSqlDescription("测试流程处理异常");
                        errorResult.setTableName("data_little_giant_business_info");
                        errorResult.setSqlExecuteSuccess(false);
                        errorResult.setSqlErrorLog("处理测试结果时发生异常: " + e.getMessage());

                        List<ExecutorTestResult> errorResults = new ArrayList<>();
                        errorResults.add(errorResult);

                        try {
                            excelUtil.appendExecutorTestResults(resultFileName, errorResults);
                        } catch (Exception saveException) {
                            log.error("保存错误记录失败: {}", saveException.getMessage(), saveException);
                        }
                    }
                }

                log.info("--- 第 {}/{} 轮数据集测试完成 ---", currentRound, roundNumber);
            }

            log.info("=== 执行器测试 {} 完成，共进行 {} 轮测试，总计 {} 个测试用例，产生 {} 个SQL记录，结果已保存到: {} ===",
                    testConfig, roundNumber, totalCompletedTests.get(), totalCompletedSQLs.get(), resultFileName);

        } catch (Exception e) {
            log.error("执行器测试 {} 发生异常: {}", testConfig, e.getMessage(), e);
        }
    }

    /**
     *
     * @@新增case
     *          执行单个测试用例，为每个gen_sql调用生成独立的记录
     * 
     * @param testData    测试数据
     * @param roundNumber 轮次号
     * @param testConfig  测试配置
     * @return ExecutorTestResult列表，每个gen_sql调用对应一个结果
     */
    private List<ExecutorTestResult> executeSingleTest(ExecutorTestData testData, int roundNumber, String testConfig) {
        List<ExecutorTestResult> results = new ArrayList<>();

        try {
            // 1. 解析Python代码中的所有gen_sql调用
            List<GenSqlParams> genSqlParamsList = parseAllGenSqlParams(testData.getPythonCode());

            // 2. 为每个gen_sql调用生成独立的测试结果
            for (GenSqlParams genSqlParams : genSqlParamsList) {
                ExecutorTestResult result = new ExecutorTestResult();
                result.setQuestionId(testData.getQuestionId());
                result.setRoundNumber(roundNumber);
                result.setSqlNumber(genSqlParams.getSqlNumber());
                result.setUserInput(testData.getUserInput());
                result.setHistoryInput(testData.getHistoryInput());
                result.setPythonCode(testData.getPythonCode());
                result.setSqlDescription(genSqlParams.getQueryText());
                result.setTableName(genSqlParams.getTableName());

                long startTime = System.currentTimeMillis();
                String sqlStatement = null;

                // 3. 根据配置调用对应的genSQLCase方法
                try {
                    String methodName = "genSQLCase" + testConfig;

                    // 根据testConfig调用不同的genSQLCase方法
                    switch (testConfig.toLowerCase()) {
                        case "case4c":
                            // case4c 使用genSQLCase2c方法
                            sqlStatement = aisqlQueryUtil.genSQLCase4c(
                                    genSqlParams.getQueryText(),
                                    genSqlParams.getTableName(),
                                    testData.getPythonCode(),
                                    testData.getHistoryInput(),
                                    testData.getUserInput());
                            break;
                        case "case3c":
                            // case3c 使用genSQLCase2c方法
                            sqlStatement = aisqlQueryUtil.genSQLCase3c(
                                    genSqlParams.getQueryText(),
                                    genSqlParams.getTableName(),
                                    testData.getPythonCode(),
                                    testData.getHistoryInput(),
                                    testData.getUserInput());
                            break;
                        case "case2c":
                            // case2c 使用genSQLCase2c方法
                            sqlStatement = aisqlQueryUtil.genSQLCase2c(
                                    genSqlParams.getQueryText(),
                                    genSqlParams.getTableName(),
                                    testData.getPythonCode(),
                                    testData.getHistoryInput(),
                                    testData.getUserInput());
                            break;
                        case "case1a":
                            // case1a 使用默认的genSQL方法
                            sqlStatement = aisqlQueryUtil.genSQLOld(
                                    genSqlParams.getQueryText(),
                                    genSqlParams.getTableName(),
                                    testData.getPythonCode());
                            methodName = "genSQL";
                            break;
                        case "case1b":
                            sqlStatement = aisqlQueryUtil.genSQLCase1b(
                                    genSqlParams.getQueryText(),
                                    genSqlParams.getTableName(),
                                    testData.getPythonCode(),
                                    testData.getHistoryInput(),
                                    testData.getUserInput());
                            break;
                        case "case1c":
                            sqlStatement = aisqlQueryUtil.genSQLCase1c(
                                    genSqlParams.getQueryText(),
                                    genSqlParams.getTableName(),
                                    testData.getPythonCode(),
                                    testData.getHistoryInput(),
                                    testData.getUserInput());
                            break;
                        case "case1d":
                            sqlStatement = aisqlQueryUtil.genSQLCase1d(
                                    genSqlParams.getQueryText(),
                                    genSqlParams.getTableName(),
                                    testData.getPythonCode(),
                                    testData.getHistoryInput(),
                                    testData.getUserInput());
                            break;
                        case "case1e":
                            sqlStatement = aisqlQueryUtil.genSQLCase1e(
                                    genSqlParams.getQueryText(),
                                    genSqlParams.getTableName(),
                                    testData.getPythonCode(),
                                    testData.getHistoryInput(),
                                    testData.getUserInput());
                            break;
                        default:
                            // 默认使用case1c
                            sqlStatement = aisqlQueryUtil.genSQLCase1c(
                                    genSqlParams.getQueryText(),
                                    genSqlParams.getTableName(),
                                    testData.getPythonCode(),
                                    testData.getHistoryInput(),
                                    testData.getUserInput());
                            methodName = "genSQLCase1c (default)";
                            break;
                    }

                    log.info("通过{}方法生成SQL成功 [{}]: \n{}", methodName, genSqlParams.getSqlNumber(), sqlStatement);

                } catch (Exception e) {
                    log.error("调用genSQLCase方法失败 [{}]: {}", genSqlParams.getSqlNumber(), e.getMessage(), e);
                    // 如果genSQLCase方法失败，记录错误并设置必要字段
                    long generateTime = System.currentTimeMillis() - startTime;
                    result.setSqlGenerateTime(generateTime);
                    result.setSqlExecuteSuccess(false);
                    result.setSqlErrorLog("调用genSQLCase方法失败: " + e.getMessage());
                    result.setSqlStatement(""); // 设置空的SQL语句
                    results.add(result);
                    continue;
                }

                result.setSqlStatement(sqlStatement);
                long generateTime = System.currentTimeMillis() - startTime;
                result.setSqlGenerateTime(generateTime);

                // 4. 执行SQL
                try {
                    List<Map<String, Object>> executeResult = aisqlQueryUtil.executeSQL(sqlStatement);
                    result.setSqlExecuteSuccess(true);
                    result.setSqlExecuteResult(JSONUtil.toJsonStr(executeResult));
                    log.info("SQL执行成功 [{}]，问题ID: {}", genSqlParams.getSqlNumber(), testData.getQuestionId());
                } catch (Exception e) {
                    result.setSqlExecuteSuccess(false);
                    result.setSqlErrorLog(e.getMessage());
                    log.error("SQL执行失败 [{}]，问题ID: {}, 错误: {}", genSqlParams.getSqlNumber(), testData.getQuestionId(),
                            e.getMessage());
                }

                results.add(result);
            }

        } catch (Exception e) {
            // 如果解析失败，创建一个默认的错误记录
            ExecutorTestResult errorResult = new ExecutorTestResult();
            errorResult.setQuestionId(testData.getQuestionId());
            errorResult.setRoundNumber(roundNumber);
            errorResult.setSqlNumber("SQL1");
            errorResult.setUserInput(testData.getUserInput());
            errorResult.setHistoryInput(testData.getHistoryInput());
            errorResult.setPythonCode(testData.getPythonCode());

            // 尝试从Python代码中提取简单的gen_sql参数信息用于显示
            try {
                // 简单匹配第一个gen_sql调用的参数，用于错误记录的显示
                Pattern simplePattern = Pattern.compile("gen_sql\\s*\\(([^)]+)\\)");
                Matcher simpleMatcher = simplePattern.matcher(testData.getPythonCode());
                if (simpleMatcher.find()) {
                    String params = simpleMatcher.group(1);
                    // 尝试提取第一个字符串参数作为查询描述
                    Pattern stringPattern = Pattern.compile("([\"'])((?:\\\\.|(?!\\1)[^\\\\])*?)\\1");
                    Matcher stringMatcher = stringPattern.matcher(params);
                    if (stringMatcher.find()) {
                        String firstParam = stringMatcher.group(2);
                        errorResult.setSqlDescription(firstParam);

                        // 尝试提取第二个字符串参数作为表名
                        if (stringMatcher.find()) {
                            String secondParam = stringMatcher.group(2);
                            errorResult.setTableName(secondParam);
                        } else {
                            errorResult.setTableName("data_little_giant_business_info");
                        }
                    } else {
                        errorResult.setSqlDescription("解析失败的查询请求");
                        errorResult.setTableName("data_little_giant_business_info");
                    }
                } else {
                    errorResult.setSqlDescription("未找到gen_sql调用");
                    errorResult.setTableName("data_little_giant_business_info");
                }
            } catch (Exception parseException) {
                log.debug("简单解析gen_sql参数失败: {}", parseException.getMessage());
                errorResult.setSqlDescription("解析异常的查询请求");
                errorResult.setTableName("data_little_giant_business_info");
            }

            errorResult.setSqlExecuteSuccess(false);
            errorResult.setSqlErrorLog("解析gen_sql参数时发生异常: " + e.getMessage());
            results.add(errorResult);
            log.error("执行测试时发生异常，问题ID: {}, 错误: {}", testData.getQuestionId(), e.getMessage());
        }

        return results;
    }

    /**
     * 解析Python代码中的所有gen_sql调用
     * 
     * @param pythonCode Python代码
     * @return List<GenSqlParams> 包含所有gen_sql调用的参数列表
     */
    private List<GenSqlParams> parseAllGenSqlParams(String pythonCode) {
        List<GenSqlParams> paramsList = new ArrayList<>();

        if (pythonCode == null || pythonCode.trim().isEmpty()) {
            return paramsList;
        }

        try {
            // 使用正则表达式匹配所有gen_sql方法调用
            Pattern pattern = Pattern.compile("gen_sql\\s*\\(([^)]+)\\)", Pattern.MULTILINE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(pythonCode);

            int sqlIndex = 1;
            while (matcher.find()) {
                String parametersStr = matcher.group(1);
                log.debug("找到第{}个gen_sql调用，参数: {}", sqlIndex, parametersStr);

                GenSqlParams params = new GenSqlParams();
                params.setSqlNumber("SQL" + sqlIndex);

                // 解析参数 - 支持位置参数和命名参数两种格式
                // 位置参数格式：gen_sql("查询文本", "表名", "历史记录")
                // 命名参数格式：gen_sql(query_text="查询文本", table_name="表名", diag_history="历史记录")

                // 先尝试解析位置参数格式
                if (parsePositionalParams(parametersStr, params)) {
                    log.debug("成功解析位置参数格式");
                } else {
                    // 如果位置参数解析失败，尝试命名参数格式
                    parseNamedParams(parametersStr, params);
                    log.debug("尝试解析命名参数格式");
                }

                // 如果没有明确的参数，使用默认值
                if (params.getQueryText() == null || params.getQueryText().trim().isEmpty()) {
                    params.setQueryText("数据查询请求");
                }
                if (params.getTableName() == null || params.getTableName().trim().isEmpty()) {
                    params.setTableName("data_little_giant_business_info");
                }
                if (params.getDiagHistory() == null) {
                    params.setDiagHistory("");
                }

                paramsList.add(params);
                sqlIndex++;

                log.info("解析第{}个gen_sql参数成功: queryText={}, tableName={}, diagHistory={}",
                        params.getSqlNumber(), params.getQueryText(), params.getTableName(), params.getDiagHistory());
            }

            // 如果没有找到任何gen_sql调用，创建一个默认的
            if (paramsList.isEmpty()) {
                GenSqlParams defaultParams = new GenSqlParams();
                defaultParams.setSqlNumber("SQL1");
                defaultParams.setQueryText("数据查询请求");
                defaultParams.setTableName("data_little_giant_business_info");
                defaultParams.setDiagHistory("");
                paramsList.add(defaultParams);
                log.info("未找到gen_sql调用，使用默认参数");
            }

        } catch (Exception e) {
            log.error("解析gen_sql参数时发生异常: {}", e.getMessage(), e);
            // 使用默认值
            GenSqlParams defaultParams = new GenSqlParams();
            defaultParams.setSqlNumber("SQL1");
            defaultParams.setQueryText("数据查询请求");
            defaultParams.setTableName("data_little_giant_business_info");
            defaultParams.setDiagHistory("");
            paramsList.add(defaultParams);
        }

        return paramsList;
    }

    /**
     * 解析位置参数格式的gen_sql调用
     * 
     * @param parametersStr 参数字符串
     * @param params        参数对象
     * @return 是否解析成功
     */
    private boolean parsePositionalParams(String parametersStr, GenSqlParams params) {
        try {
            // 使用正则表达式匹配字符串参数
            // 支持单引号和双引号，考虑转义字符
            Pattern stringPattern = Pattern.compile("([\"'])((?:\\\\.|(?!\\1)[^\\\\])*?)\\1");
            Matcher stringMatcher = stringPattern.matcher(parametersStr);

            List<String> extractedParams = new ArrayList<>();
            while (stringMatcher.find()) {
                String param = stringMatcher.group(2);
                // 处理转义字符
                param = param.replace("\\\"", "\"").replace("\\'", "'").replace("\\\\", "\\");
                extractedParams.add(param);
            }

            log.debug("从位置参数中提取到 {} 个参数: {}", extractedParams.size(), extractedParams);

            // 根据参数数量设置对应的值
            if (extractedParams.size() >= 1) {
                params.setQueryText(extractedParams.get(0));
            }
            if (extractedParams.size() >= 2) {
                params.setTableName(extractedParams.get(1));
            }
            if (extractedParams.size() >= 3) {
                params.setDiagHistory(extractedParams.get(2));
            }

            // 如果至少提取到一个参数，认为解析成功
            return extractedParams.size() > 0;

        } catch (Exception e) {
            log.debug("位置参数解析失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析命名参数格式的gen_sql调用
     * 
     * @param parametersStr 参数字符串
     * @param params        参数对象
     */
    private void parseNamedParams(String parametersStr, GenSqlParams params) {
        try {
            // 提取query_text
            Pattern queryPattern = Pattern.compile("query_text\\s*=\\s*[\"'](.*?)[\"']");
            Matcher queryMatcher = queryPattern.matcher(parametersStr);
            if (queryMatcher.find()) {
                params.setQueryText(queryMatcher.group(1));
            }

            // 提取table_name
            Pattern tablePattern = Pattern.compile("table_name\\s*=\\s*[\"'](.*?)[\"']");
            Matcher tableMatcher = tablePattern.matcher(parametersStr);
            if (tableMatcher.find()) {
                params.setTableName(tableMatcher.group(1));
            }

            // 提取diag_history
            Pattern historyPattern = Pattern.compile("diag_history\\s*=\\s*[\"'](.*?)[\"']");
            Matcher historyMatcher = historyPattern.matcher(parametersStr);
            if (historyMatcher.find()) {
                params.setDiagHistory(historyMatcher.group(1));
            }

        } catch (Exception e) {
            log.debug("命名参数解析失败: {}", e.getMessage());
        }
    }

    /**
     * 解析Python代码中的gen_sql参数（保留原方法以支持向后兼容）
     * 
     * @param pythonCode Python代码
     * @return GenSqlParams 包含解析出的参数
     */
    private GenSqlParams parseGenSqlParams(String pythonCode) {
        List<GenSqlParams> paramsList = parseAllGenSqlParams(pythonCode);
        return paramsList.isEmpty() ? new GenSqlParams() : paramsList.get(0);
    }

    /**
     * 测试gen_sql参数解析功能
     * 用于验证不同格式的gen_sql调用是否能正确解析
     */
    public void testGenSqlParamsParsing() {
        log.info("=== 开始测试gen_sql参数解析功能 ===");

        // 测试用例1：位置参数格式
        String testCode1 = """
                # 查询广州的数据
                result = gen_sql("查询2023年广州通用设备制造业小巨人企业的营业收入", "广州2021年到2023年小巨人企业年度经营数据")
                print(result)
                """;

        // 测试用例2：命名参数格式
        String testCode2 = """
                # 查询数据
                result = gen_sql(query_text="查询所有企业信息", table_name="data_little_giant_business_info", diag_history="历史记录")
                print(result)
                """;

        // 测试用例3：多个gen_sql调用
        String testCode3 = """
                # 多个查询
                result1 = gen_sql("查询企业总数", "企业数据表")
                result2 = gen_sql("查询营业收入", "财务数据表", "相关历史记录")
                result3 = gen_sql(query_text="查询地区分布", table_name="地区数据表")
                """;

        // 测试用例4：包含转义字符的参数
        String testCode4 = """
                result = gen_sql("查询名称包含'巨人'的企业", "企业基本信息表")
                """;

        // 执行测试
        testGenSqlParsingCase("位置参数格式", testCode1);
        testGenSqlParsingCase("命名参数格式", testCode2);
        testGenSqlParsingCase("多个gen_sql调用", testCode3);
        testGenSqlParsingCase("包含转义字符", testCode4);

        log.info("=== gen_sql参数解析功能测试完成 ===");
    }

    /**
     * 测试单个gen_sql解析用例
     * 
     * @param testName   测试用例名称
     * @param pythonCode Python代码
     */
    private void testGenSqlParsingCase(String testName, String pythonCode) {
        log.info("--- 测试用例: {} ---", testName);
        log.info("Python代码:\n{}", pythonCode);

        try {
            List<GenSqlParams> paramsList = parseAllGenSqlParams(pythonCode);
            log.info("解析结果: 找到 {} 个gen_sql调用", paramsList.size());

            for (int i = 0; i < paramsList.size(); i++) {
                GenSqlParams params = paramsList.get(i);
                log.info("第{}个调用 - SQL编号: {}, 查询文本: '{}', 表名: '{}', 历史记录: '{}'",
                        i + 1, params.getSqlNumber(), params.getQueryText(), params.getTableName(),
                        params.getDiagHistory());
            }

        } catch (Exception e) {
            log.error("解析失败: {}", e.getMessage(), e);
        }

        log.info(""); // 空行分隔
    }

    /**
     * 测试Excel字段设置的完整性
     * 验证SQL描述文本和表名字段是否在各种情况下都能正确设置
     */
    public void testExcelFieldsCompleteness() {
        log.info("=== 开始测试Excel字段设置完整性 ===");

        // 测试用例1：正常的gen_sql调用
        String normalCode = """
                result = gen_sql("查询2023年广州通用设备制造业小巨人企业的营业收入", "广州2021年到2023年小巨人企业年度经营数据")
                """;

        // 测试用例2：多个gen_sql调用
        String multipleCode = """
                result1 = gen_sql("查询企业总数", "企业基本信息表")
                result2 = gen_sql("查询营业收入汇总", "财务数据表", "包含历史对比")
                """;

        // 测试用例3：格式错误的代码（会触发异常处理）
        String errorCode = """
                # 这是一个错误的gen_sql调用
                result = gen_sql("查询企业信息" # 缺少闭合括号
                """;

        testExcelFieldsCase("正常gen_sql调用", normalCode);
        testExcelFieldsCase("多个gen_sql调用", multipleCode);
        testExcelFieldsCase("错误格式代码", errorCode);

        log.info("=== Excel字段设置完整性测试完成 ===");
    }

    /**
     * 测试Excel字符长度限制处理
     * 验证超长文本是否能正确截断以符合Excel单元格限制
     */
    public void testExcelCellLengthLimit() {
        log.info("=== 开始测试Excel字符长度限制处理 ===");

        // 生成超长的测试数据
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 40000; i++) { // 生成40000字符的文本，超过32767限制
            longText.append("这是测试数据");
        }
        String longTestText = longText.toString();

        // 测试用例1：超长Python代码
        String longPythonCode = """
                # 这是一个超长的Python代码示例
                """ + longTestText + """
                result = gen_sql("查询企业信息", "data_little_giant_business_info")
                print(result)
                """;

        // 测试用例2：超长用户输入
        String longUserInput = "用户查询：" + longTestText;

        // 测试用例3：超长历史输入
        String longHistoryInput = "历史对话：" + longTestText;

        try {
            // 创建包含超长文本的测试数据
            ExecutorTestData testData = new ExecutorTestData();
            testData.setQuestionId("LONG_TEXT_TEST_" + System.currentTimeMillis());
            testData.setUserInput(longUserInput);
            testData.setHistoryInput(longHistoryInput);
            testData.setPythonCode(longPythonCode);

            log.info("创建的测试数据字段长度:");
            log.info("- 用户输入长度: {} 字符", testData.getUserInput().length());
            log.info("- 历史输入长度: {} 字符", testData.getHistoryInput().length());
            log.info("- Python代码长度: {} 字符", testData.getPythonCode().length());

            // 执行测试并生成结果
            List<ExecutorTestResult> results = executeSingleTest(testData, 1, "case1a");

            // 创建测试结果文件
            String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            String testFileName = String.format("excel-length-limit-test-%s.xlsx", timestamp);

            log.info("开始写入Excel文件: {}", testFileName);

            // 写入Excel文件，验证字符长度限制处理
            excelUtil.writeExecutorTestResults(testFileName, results);

            log.info("✓ Excel文件写入成功，字符长度限制处理正常");
            log.info("结果文件已保存: {}", testFileName);

            // 验证写入的数据
            for (ExecutorTestResult result : results) {
                log.info("结果验证:");
                log.info("- 问题ID: {}", result.getQuestionId());
                log.info("- 用户输入是否被截断: {}", result.getUserInput().contains("...[文本已截断]"));
                log.info("- 历史输入是否被截断: {}", result.getHistoryInput().contains("...[文本已截断]"));
                log.info("- Python代码是否被截断: {}", result.getPythonCode().contains("...[文本已截断]"));
            }

        } catch (Exception e) {
            log.error("Excel字符长度限制测试失败: {}", e.getMessage(), e);
        }

        log.info("=== Excel字符长度限制处理测试完成 ===");
    }

    /**
     * 模拟超长SQL执行结果的测试
     * 测试SQL查询返回大量数据时的字符长度限制处理
     */
    public void testLongSqlResult() {
        log.info("=== 开始测试超长SQL执行结果处理 ===");

        try {
            // 创建模拟的超长SQL执行结果
            StringBuilder longResultBuilder = new StringBuilder();
            longResultBuilder.append("[");
            for (int i = 0; i < 5000; i++) { // 模拟5000条记录的查询结果
                if (i > 0)
                    longResultBuilder.append(",");
                longResultBuilder.append(String.format(
                        "{\"id\":%d,\"name\":\"企业%d\",\"revenue\":%d,\"description\":\"这是企业%d的详细描述信息，包含了大量的业务数据和财务指标等内容\"}",
                        i, i, i * 1000, i));
            }
            longResultBuilder.append("]");
            String longSqlResult = longResultBuilder.toString();

            // 创建包含超长SQL结果的测试记录
            ExecutorTestResult testResult = new ExecutorTestResult();
            testResult.setQuestionId("LONG_SQL_RESULT_TEST_" + System.currentTimeMillis());
            testResult.setRoundNumber(1);
            testResult.setSqlNumber("SQL1");
            testResult.setUserInput("查询所有企业信息");
            testResult.setHistoryInput("历史对话记录");
            testResult.setPythonCode("result = gen_sql('查询所有企业信息', 'data_little_giant_business_info')");
            testResult.setSqlDescription("查询所有企业信息");
            testResult.setTableName("data_little_giant_business_info");
            testResult.setSqlStatement("SELECT * FROM data_little_giant_business_info");
            testResult.setSqlGenerateTime(100L);
            testResult.setSqlExecuteSuccess(true);
            testResult.setSqlExecuteResult(longSqlResult); // 设置超长的SQL执行结果
            testResult.setSqlErrorLog("");

            log.info("SQL执行结果长度: {} 字符", longSqlResult.length());

            List<ExecutorTestResult> results = List.of(testResult);

            // 写入Excel文件
            String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            String testFileName = String.format("long-sql-result-test-%s.xlsx", timestamp);

            excelUtil.writeExecutorTestResults(testFileName, results);

            log.info("✓ 超长SQL结果Excel写入成功");
            log.info("结果文件已保存: {}", testFileName);
            log.info("SQL执行结果是否被截断: {}", testResult.getSqlExecuteResult().contains("...[文本已截断]"));

        } catch (Exception e) {
            log.error("超长SQL结果测试失败: {}", e.getMessage(), e);
        }

        log.info("=== 超长SQL执行结果处理测试完成 ===");
    }

    /**
     * 测试单个Excel字段设置用例
     * 
     * @param testName   测试用例名称
     * @param pythonCode Python代码
     */
    private void testExcelFieldsCase(String testName, String pythonCode) {
        log.info("--- 测试用例: {} ---", testName);

        try {
            // 创建模拟测试数据
            ExecutorTestData testData = new ExecutorTestData();
            testData.setQuestionId("TEST_" + System.currentTimeMillis());
            testData.setUserInput("测试用户输入：" + testName);
            testData.setHistoryInput("测试历史输入");
            testData.setPythonCode(pythonCode);

            // 执行单个测试
            List<ExecutorTestResult> results = executeSingleTest(testData, 1, "case1a");

            log.info("生成了 {} 条结果记录", results.size());

            for (int i = 0; i < results.size(); i++) {
                ExecutorTestResult result = results.get(i);
                log.info("记录 {}: ", i + 1);
                log.info("  问题ID: {}", result.getQuestionId());
                log.info("  SQL编号: {}", result.getSqlNumber());
                log.info("  用户输入: {}", result.getUserInput());
                log.info("  历史输入: {}", result.getHistoryInput());
                log.info("  SQL描述文本: '{}'", result.getSqlDescription());
                log.info("  查询的表名: '{}'", result.getTableName());
                log.info("  执行成功: {}", result.getSqlExecuteSuccess());

                // 验证必要字段是否非空
                boolean fieldsComplete = result.getQuestionId() != null &&
                        result.getSqlNumber() != null &&
                        result.getUserInput() != null &&
                        result.getHistoryInput() != null &&
                        result.getSqlDescription() != null &&
                        result.getTableName() != null;

                log.info("  字段完整性: {}", fieldsComplete ? "✓ 完整" : "✗ 缺失");
            }

        } catch (Exception e) {
            log.error("测试Excel字段设置时发生异常: {}", e.getMessage(), e);
        }

        log.info(""); // 空行分隔
    }

    /**
     * 内部类：gen_sql参数
     */
    private static class GenSqlParams {
        private String sqlNumber;
        private String queryText;
        private String tableName;
        private String diagHistory;

        public String getSqlNumber() {
            return sqlNumber;
        }

        public void setSqlNumber(String sqlNumber) {
            this.sqlNumber = sqlNumber;
        }

        public String getQueryText() {
            return queryText;
        }

        public void setQueryText(String queryText) {
            this.queryText = queryText;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getDiagHistory() {
            return diagHistory;
        }

        public void setDiagHistory(String diagHistory) {
            this.diagHistory = diagHistory;
        }
    }
}