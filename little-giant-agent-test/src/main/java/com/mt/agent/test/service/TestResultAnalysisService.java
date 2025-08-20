package com.mt.agent.test.service;

import com.mt.agent.test.model.ExecutorTestResult;
import com.mt.agent.test.util.ExcelUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 测试结果分析服务
 * 专门处理测试结果的统计和分析功能
 *
 * @author MT Agent Team
 * @version 1.0
 * @since 2025-01-15
 */
@Slf4j
@Service
public class TestResultAnalysisService {

    @Autowired
    private ExcelUtil excelUtil;

    // 常量定义
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * 执行器测试结果统计方法
     * 
     * @param resultFileName 结果文件名
     */
    public void statisticsExecutorResult(String resultFileName) {
        log.info("=== 开始执行器测试结果统计，文件: {} ===", resultFileName);

        try {
            // 1. 读取测试结果
            List<ExecutorTestResult> results = excelUtil.readExecutorTestResults(resultFileName);
            log.info("读取到 {} 条测试结果", results.size());

            // 2. 进行结果分析
            List<ExecutorTestResult> analyzedResults = new ArrayList<>();
            for (ExecutorTestResult result : results) {
                ExecutorTestResult analyzed = analyzeExecutorResult(result);
                analyzedResults.add(analyzed);
            }

            // 3. 生成统计报告
            String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            String statisticsFileName = resultFileName.replace("executor-result-", "executor-statistics-")
                    .replace(".xlsx", "-" + timestamp + ".xlsx");

            // 4. 统计分类信息
            Map<String, Integer> categoryCount = new HashMap<>();
            Map<String, List<String>> categoryQuestions = new HashMap<>();

            for (ExecutorTestResult result : analyzedResults) {
                String category = result.getSqlErrorLog();
                categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);

                String questionKey = result.getQuestionId() + "-" + result.getRoundNumber();
                categoryQuestions.computeIfAbsent(category, k -> new ArrayList<>()).add(questionKey);
            }

            // 5. 生成统计报告Excel
            writeStatisticsReport(statisticsFileName, categoryCount, categoryQuestions, analyzedResults.size());

            log.info("=== 执行器测试结果统计完成，结果已保存到: {} ===", statisticsFileName);

        } catch (Exception e) {
            log.error("执行器测试结果统计发生异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 分析单个执行器测试结果
     * 
     * @param result 测试结果
     * @return 分析后的结果
     */
    private ExecutorTestResult analyzeExecutorResult(ExecutorTestResult result) {
        ExecutorTestResult analyzed = new ExecutorTestResult();
        // 复制原始数据
        analyzed.setQuestionId(result.getQuestionId());
        analyzed.setRoundNumber(result.getRoundNumber());
        analyzed.setUserInput(result.getUserInput());
        analyzed.setHistoryInput(result.getHistoryInput());
        analyzed.setPythonCode(result.getPythonCode());
        analyzed.setSqlStatement(result.getSqlStatement());
        analyzed.setSqlGenerateTime(result.getSqlGenerateTime());
        analyzed.setSqlExecuteSuccess(result.getSqlExecuteSuccess());
        analyzed.setSqlExecuteResult(result.getSqlExecuteResult());

        // 分析错误类型
        if (result.getSqlExecuteSuccess() != null && result.getSqlExecuteSuccess()) {
            analyzed.setSqlErrorLog("SQL成功执行");
        } else {
            String errorType = analyzeErrorType(result);
            analyzed.setSqlErrorLog(errorType);
        }

        return analyzed;
    }

    /**
     * 分析错误类型
     * 
     * @param result 测试结果
     * @return 错误类型描述
     */
    private String analyzeErrorType(ExecutorTestResult result) {
        String errorLog = result.getSqlErrorLog();
        if (errorLog == null || errorLog.trim().isEmpty()) {
            return "SQL无法执行（未知错误）";
        }

        // 检查是否是字段匹配问题
        if (isFieldMismatchError(result)) {
            return "SQL无法执行 - SQL查询字段与Python代码实际使用不匹配";
        }

        // 检查是否是数据库字段不存在问题
        if (isDatabaseFieldNotExistError(errorLog)) {
            return "SQL无法执行 - SQL查询字段在数据库不存在";
        }

        // 其他错误
        return "SQL无法执行 - 其他（需人工排查）";
    }

    /**
     * 检查是否是字段匹配错误
     * 
     * @param result 测试结果
     * @return true表示是字段匹配错误
     */
    private boolean isFieldMismatchError(ExecutorTestResult result) {
        String pythonCode = result.getPythonCode();
        String sqlStatement = result.getSqlStatement();

        if (pythonCode == null || sqlStatement == null) {
            return false;
        }

        try {
            // 从Python代码中提取变量名
            Set<String> pythonVariables = extractPythonVariables(pythonCode);

            // 从SQL语句中提取字段名
            Set<String> sqlFields = extractSqlFields(sqlStatement);

            // 检查是否存在不匹配的字段
            for (String variable : pythonVariables) {
                if (!sqlFields.contains(variable)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            log.error("检查字段匹配时发生异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查是否是数据库字段不存在错误
     * 
     * @param errorLog 错误日志
     * @return true表示是数据库字段不存在错误
     */
    private boolean isDatabaseFieldNotExistError(String errorLog) {
        // 常见的数据库字段不存在错误信息
        String[] fieldNotExistKeywords = {
                "Unknown column",
                "不存在的列",
                "Column not found",
                "Invalid column name",
                "字段不存在"
        };

        for (String keyword : fieldNotExistKeywords) {
            if (errorLog.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 从Python代码中提取变量名
     * 
     * @param pythonCode Python代码
     * @return 变量名集合
     */
    private Set<String> extractPythonVariables(String pythonCode) {
        Set<String> variables = new HashSet<>();

        // 提取形如 data['field_name'] 的变量
        Pattern pattern = Pattern.compile("data\\['([^']+)'\\]");
        Matcher matcher = pattern.matcher(pythonCode);

        while (matcher.find()) {
            variables.add(matcher.group(1));
        }

        return variables;
    }

    /**
     * 从SQL语句中提取字段名
     * 
     * @param sqlStatement SQL语句
     * @return 字段名集合
     */
    private Set<String> extractSqlFields(String sqlStatement) {
        Set<String> fields = new HashSet<>();

        // 简单的字段提取逻辑，可能需要根据实际情况调整
        Pattern pattern = Pattern.compile("SELECT\\s+(.+?)\\s+FROM", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(sqlStatement);

        if (matcher.find()) {
            String selectClause = matcher.group(1);
            String[] fieldArray = selectClause.split(",");

            for (String field : fieldArray) {
                String cleanField = field.trim().replaceAll("`", "");
                if (cleanField.contains(" AS ")) {
                    cleanField = cleanField.split(" AS ")[0].trim();
                }
                fields.add(cleanField);
            }
        }

        return fields;
    }

    /**
     * 写入统计报告
     * 
     * @param filePath          文件路径
     * @param categoryCount     分类计数
     * @param categoryQuestions 分类问题
     * @param totalCount        总数
     */
    private void writeStatisticsReport(String filePath, Map<String, Integer> categoryCount,
            Map<String, List<String>> categoryQuestions, int totalCount) {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                java.io.FileOutputStream fileOut = new java.io.FileOutputStream(filePath)) {

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("统计报告");

            // 创建标题行
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = { "结果分类类型", "次数", "占比", "分类关联的问题编号以及测试轮数" };
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 写入数据行
            int rowNum = 1;
            for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                String category = entry.getKey();
                Integer count = entry.getValue();
                double percentage = (double) count / totalCount * 100;

                row.createCell(0).setCellValue(category);
                row.createCell(1).setCellValue(count);
                row.createCell(2).setCellValue(String.format("%.2f%%", percentage));

                // 关联的问题编号
                List<String> questions = categoryQuestions.get(category);
                String questionList = questions != null ? String.join(", ", questions) : "";
                row.createCell(3).setCellValue(questionList);
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(fileOut);
            log.info("统计报告已写入: {}", filePath);

        } catch (Exception e) {
            log.error("写入统计报告失败: {}", e.getMessage(), e);
            throw new RuntimeException("写入统计报告失败", e);
        }
    }
}