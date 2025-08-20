package com.mt.agent.test;

import com.mt.agent.buffer.util.BufferUtil;
import com.mt.agent.sysUtil.AISQLQueryUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
@Slf4j
public class SQLGenCase1ConfigBTest {

    @Autowired
    private AISQLQueryUtil sqlGenUtil;

    @Autowired
    private BufferUtil bufferUtil;

    /**
     * 测试结果记录类
     */
    public static class TestResult {
        private String userInput; // 用户输入
        private String historyInput; // 历史输入
        private String queryText; // gen_sql中的查询文本
        private String tableName; // gen_sql中的表名
        private String pythonCode; // Python代码
        private String generatedSql; // 生成的SQL
        private boolean success; // 执行状态
        private String errorMessage; // 错误信息

        // 成功构造函数
        public TestResult(String userInput, String historyInput, String queryText, String tableName,
                String pythonCode, String generatedSql) {
            this.userInput = userInput;
            this.historyInput = historyInput;
            this.queryText = queryText;
            this.tableName = tableName;
            this.pythonCode = pythonCode;
            this.generatedSql = generatedSql;
            this.success = true;
        }

        // 失败构造函数
        public TestResult(String userInput, String historyInput, String pythonCode, Exception ex) {
            this.userInput = userInput;
            this.historyInput = historyInput;
            this.pythonCode = pythonCode;
            this.errorMessage = ex.getMessage();
            this.success = false;
        }

        // Getter方法
        public String getUserInput() {
            return userInput;
        }

        public String getHistoryInput() {
            return historyInput;
        }

        public String getQueryText() {
            return queryText;
        }

        public String getTableName() {
            return tableName;
        }

        public String getPythonCode() {
            return pythonCode;
        }

        public String getGeneratedSql() {
            return generatedSql;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Excel行数据类
     */
    public static class ExcelRowData {
        private String userInput; // 第一列：用户输入
        private String historyInput; // 第二列：历史输入
        private String pythonCode; // 第三列：Python代码

        public ExcelRowData(String userInput, String historyInput, String pythonCode) {
            this.userInput = userInput;
            this.historyInput = historyInput;
            this.pythonCode = pythonCode;
        }

        public String getUserInput() {
            return userInput;
        }

        public String getHistoryInput() {
            return historyInput;
        }

        public String getPythonCode() {
            return pythonCode;
        }
    }

    /**
     * Gen_sql调用信息类
     */
    public static class GenSqlCall {
        private String queryText;
        private String tableName;

        public GenSqlCall(String queryText, String tableName) {
            this.queryText = queryText;
            this.tableName = tableName;
        }

        public String getQueryText() {
            return queryText;
        }

        public String getTableName() {
            return tableName;
        }
    }

    /**
     * 自动化测试方法
     * 1. 读取当前目录下的test.xlsx文件，读取三列数据
     * 2. 解析gen_sql调用，如果存在gen_sql调用，则调用sqlGenUtil.genSQLCase4
     * 3. 保存测试结果到Excel文件
     */
    @Test
    public void testAutomatedSQLGenerationCase1ConfigB() {
        log.info("开始执行自动化SQL生成测试（Case1ConfigB）");

        List<TestResult> testResults = new ArrayList<>();

        try {
            // 读取test.xlsx文件
            List<ExcelRowData> excelData = readExcelData("test-case2.xlsx");
            log.info("从Excel文件读取到{}条数据", excelData.size());

            // 处理每行数据
            for (int i = 0; i < excelData.size(); i++) {
                ExcelRowData rowData = excelData.get(i);
                log.info("处理第{}条数据", i + 1);

                try {
                    // 解析Python代码中的gen_sql调用
                    List<GenSqlCall> genSqlCalls = parseGenSqlCalls(rowData.getPythonCode());

                    if (genSqlCalls.isEmpty()) {
                        log.warn("第{}条Python代码中未找到gen_sql调用", i + 1);
                        continue;
                    }

                    // 处理每个gen_sql调用
                    for (GenSqlCall call : genSqlCalls) {
                        String generatedSql = sqlGenUtil.genSQLCase1b(
                                call.getQueryText(), // queryText
                                call.getTableName(), // tableName
                                rowData.getPythonCode(), // pythonCode
                                rowData.getHistoryInput(), // diagHistory
                                rowData.getUserInput() // question
                        );

                        TestResult result = new TestResult(
                                rowData.getUserInput(),
                                rowData.getHistoryInput(),
                                call.getQueryText(),
                                call.getTableName(),
                                rowData.getPythonCode(),
                                generatedSql);
                        testResults.add(result);
                        log.info("成功生成SQL: {}",
                                generatedSql.substring(0, Math.min(100, generatedSql.length())));
                    }

                } catch (Exception e) {
                    log.error("处理第{}条数据时发生错误: {}", i + 1, e.getMessage());
                    TestResult errorResult = new TestResult(
                            rowData.getUserInput(),
                            rowData.getHistoryInput(),
                            rowData.getPythonCode(),
                            e);
                    testResults.add(errorResult);
                }
            }

            // 保存结果到Excel文件
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String outputFileName = "sql_generation_Case1ConfigB_test_results_" + timestamp + ".xlsx";
            saveResultsToExcel(testResults, outputFileName);
            log.info("测试完成，结果已保存到{}文件", outputFileName);

        } catch (Exception e) {
            log.error("自动化测试执行失败", e);
            throw new RuntimeException("自动化测试执行失败", e);
        }
    }

    /**
     * 从Excel文件读取三列数据
     */
    private List<ExcelRowData> readExcelData(String fileName) throws IOException {
        List<ExcelRowData> excelData = new ArrayList<>();

        File file = new File(fileName);
        if (!file.exists()) {
            throw new IOException("找不到文件: " + fileName);
        }

        try (FileInputStream fis = new FileInputStream(file);
                Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue; // 跳过标题行

                String userInput = getCellValueAsString(row.getCell(0)); // 第一列：用户输入
                String historyInput = getCellValueAsString(row.getCell(1)); // 第二列：历史输入
                String pythonCode = getCellValueAsString(row.getCell(2)); // 第三列：Python代码

                // 确保至少有Python代码
                if (pythonCode != null && !pythonCode.trim().isEmpty()) {
                    excelData.add(new ExcelRowData(userInput, historyInput, pythonCode.trim()));
                }
            }
        }

        return excelData;
    }

    /**
     * 获取单元格值并转换为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * 解析Python代码中的gen_sql调用
     */
    private List<GenSqlCall> parseGenSqlCalls(String pythonCode) {
        List<GenSqlCall> calls = new ArrayList<>();

        // 正则表达式匹配gen_sql函数调用
        // 匹配格式：gen_sql("queryText", "tableName") 或 gen_sql('queryText', 'tableName')
        Pattern pattern = Pattern.compile("gen_sql\\s*\\(\\s*[\"']([^\"']*)[\"']\\s*,\\s*[\"']([^\"']*)[\"']\\s*\\)");
        Matcher matcher = pattern.matcher(pythonCode);

        while (matcher.find()) {
            String queryText = matcher.group(1);
            String tableName = matcher.group(2);
            calls.add(new GenSqlCall(queryText, tableName));
            log.debug("找到gen_sql调用: queryText={}, tableName={}", queryText, tableName);
        }

        return calls;
    }

    /**
     * 保存测试结果到Excel文件
     */
    private void saveResultsToExcel(List<TestResult> results, String fileName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Case4测试结果");

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("序号");
            headerRow.createCell(1).setCellValue("用户输入");
            headerRow.createCell(2).setCellValue("历史输入");
            headerRow.createCell(3).setCellValue("查询文本(queryText)");
            headerRow.createCell(4).setCellValue("表名(tableName)");
            headerRow.createCell(5).setCellValue("Python代码");
            headerRow.createCell(6).setCellValue("生成的SQL");
            headerRow.createCell(7).setCellValue("执行状态");
            headerRow.createCell(8).setCellValue("错误信息");

            // 填充数据行
            for (int i = 0; i < results.size(); i++) {
                TestResult result = results.get(i);
                Row dataRow = sheet.createRow(i + 1);

                dataRow.createCell(0).setCellValue(i + 1);
                dataRow.createCell(1).setCellValue(result.getUserInput() != null ? result.getUserInput() : "");
                dataRow.createCell(2).setCellValue(result.getHistoryInput() != null ? result.getHistoryInput() : "");
                dataRow.createCell(3).setCellValue(result.getQueryText() != null ? result.getQueryText() : "");
                dataRow.createCell(4).setCellValue(result.getTableName() != null ? result.getTableName() : "");
                dataRow.createCell(5).setCellValue(result.getPythonCode() != null ? result.getPythonCode() : "");
                dataRow.createCell(6).setCellValue(result.getGeneratedSql() != null ? result.getGeneratedSql() : "");
                dataRow.createCell(7).setCellValue(result.isSuccess() ? "成功" : "失败");
                dataRow.createCell(8).setCellValue(result.getErrorMessage() != null ? result.getErrorMessage() : "");
            }

            // 自动调整列宽
            for (int i = 0; i < 9; i++) {
                sheet.autoSizeColumn(i);
            }

            // 保存文件
            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                workbook.write(fos);
            }
        }
    }
}
