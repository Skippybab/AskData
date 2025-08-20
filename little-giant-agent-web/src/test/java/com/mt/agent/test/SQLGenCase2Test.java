package com.mt.agent.test;

import com.mt.agent.buffer.util.BufferUtil;
import com.mt.agent.sysUtil.AISQLQueryUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
@Slf4j
public class SQLGenCase2Test {
    @Autowired
    private AISQLQueryUtil sqlGenUtil;
    @Autowired
    private BufferUtil bufferUtil;

    /**
     * 测试结果记录类
     */
    public static class TestResult {
        private String queryText;
        private String tableName;
        private String pythonCode;
        private String generatedSql;
        private boolean success;
        private String errorMessage;

        // 构造函数
        public TestResult(String queryText, String tableName, String pythonCode, String generatedSql) {
            this.queryText = queryText;
            this.tableName = tableName;
            this.pythonCode = pythonCode;
            this.generatedSql = generatedSql;
            this.success = true;
        }

        public TestResult(String queryText, String tableName, String pythonCode, Exception ex) {
            this.queryText = queryText;
            this.tableName = tableName;
            this.pythonCode = pythonCode;
            this.errorMessage = ex.getMessage();
            this.success = false;
        }

        // Getter方法
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

    @Test
    public void testSQLGen() {
        String queryText = "获取医药制造业过去三年（2021-2023）各年度毛利率数据,计算毛利率不要通过多个毛利率求平均来计算";
        String tableName = "广州2021年到2023年小巨人企业年度经营数据";
        String pythonCode = """
                ```Python
                industry_list = ["医药制造", "通用设备制造", "专用设备制造"]
                year_range = [2021, 2022, 2023]
                data_dict = {}

                for industry in industry_list:
                    sql = gen_sql(f"获取{industry}业过去三年（2021-2023）各年度毛利率数据", "广州2021年到2023年小巨人企业年度经营数据")
                    result = exec_sql(sql)
                    if len(result) != 3:
                        vis_textbox(f"{industry}行业数据不完整，无法进行三年对比")
                        continue
                    try:
                        sorted_data = sorted(result, key=lambda x: x["年份"])
                        data_dict[industry] = {
                            "years": [str(item["年份"]) for item in sorted_data],
                            "rates": [float(item["毛利率_"]) for item in sorted_data]
                        }
                    except (KeyError, ValueError) as e:
                        vis_textbox(f"{industry}行业数据格式异常")

                if len(data_dict) < 2:
                    vis_textbox("至少两个行业数据完整才能进行比较")
                else:
                    common_years = set(data_dict["医药制造"]["years"]) & set(data_dict["通用设备制造"]["years"]) & set(data_dict["专用设备制造"]["years"])
                    if len(common_years) < 3:
                        vis_textbox("存在行业年份数据不匹配")
                    else:
                        vis_clustered_bar(
                            title="三大行业毛利率三年对比",
                            x_labels=data_dict["医药制造"]["years"],
                            bar_a_label="医药制造",
                            bar_a_label="通用设备制造",
                            group_a=data_dict["医药制造"]["rates"],
                            group_b=data_dict["通用设备制造"]["rates"]
                        )
                        vis_clustered_bar(
                            title="三大行业毛利率三年对比",
                            x_labels=data_dict["医药制造"]["years"],
                            bar_a_label="医药制造",
                            bar_a_label="专用设备制造",
                            group_a=data_dict["医药制造"]["rates"],
                            group_b=data_dict["专用设备制造"]["rates"]
                        )
                ```
                """;
        testSQLGen(queryText, tableName, pythonCode);
    }

    private void testSQLGen(String queryText, String tableName, String pyCode) {
        String sql = sqlGenUtil.genSQLOld(queryText, tableName, pyCode);
    }

    /**
     * 自动化测试方法
     * 1. 读取当前目录下的test.xlsx文件，读取第一列的Python代码
     * 2. 解析Python代码，如果存在gen_sql调用，则调用sqlGenUtil.genSQL
     * 3. 保存测试结果到Excel文件
     */
    @Test
    public void testAutomatedSQLGeneration() {
        log.info("开始执行自动化SQL生成测试");

        List<TestResult> testResults = new ArrayList<>();

        try {
            // 读取test.xlsx文件
            List<String> pythonCodes = readPythonCodesFromExcel("test.xlsx");
            log.info("从Excel文件读取到{}条Python代码", pythonCodes.size());

            // 处理每个Python代码
            for (int i = 0; i < pythonCodes.size(); i++) {
                String pythonCode = pythonCodes.get(i);
                log.info("处理第{}条Python代码", i + 1);

                try {
                    // 解析Python代码中的gen_sql调用
                    List<GenSqlCall> genSqlCalls = parseGenSqlCalls(pythonCode);

                    if (genSqlCalls.isEmpty()) {
                        log.warn("第{}条Python代码中未找到gen_sql调用", i + 1);
                        continue;
                    }

                    // 处理每个gen_sql调用
                    for (GenSqlCall call : genSqlCalls) {
                        String generatedSql = sqlGenUtil.genSQLOld(call.getQueryText(), call.getTableName(), pythonCode);
                        TestResult result = new TestResult(call.getQueryText(), call.getTableName(), pythonCode,
                                generatedSql);
                        testResults.add(result);
                        log.info("成功生成SQL: {}", generatedSql.substring(0, Math.min(100, generatedSql.length())));
                    }

                } catch (Exception e) {
                    log.error("处理第{}条Python代码时发生错误: {}", i + 1, e.getMessage());
                    TestResult errorResult = new TestResult("", "", pythonCode, e);
                    testResults.add(errorResult);
                }
            }

            // 保存结果到Excel文件
            saveResultsToExcel(testResults, "sql_generation_test_results.xlsx");
            log.info("测试完成，结果已保存到sql_generation_test_results.xlsx文件");

        } catch (Exception e) {
            log.error("自动化测试执行失败", e);
            throw new RuntimeException("自动化测试执行失败", e);
        }
    }

    /**
     * 从Excel文件读取Python代码
     */
    private List<String> readPythonCodesFromExcel(String fileName) throws IOException {
        List<String> pythonCodes = new ArrayList<>();

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

                Cell cell = row.getCell(0);
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    String pythonCode = cell.getStringCellValue();
                    if (pythonCode != null && !pythonCode.trim().isEmpty()) {
                        pythonCodes.add(pythonCode.trim());
                    }
                }
            }
        }

        return pythonCodes;
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
            Sheet sheet = workbook.createSheet("测试结果");

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("序号");
            headerRow.createCell(1).setCellValue("查询文本");
            headerRow.createCell(2).setCellValue("表名");
            headerRow.createCell(3).setCellValue("Python代码");
            headerRow.createCell(4).setCellValue("生成的SQL");
            headerRow.createCell(5).setCellValue("执行状态");
            headerRow.createCell(6).setCellValue("错误信息");

            // 填充数据行
            for (int i = 0; i < results.size(); i++) {
                TestResult result = results.get(i);
                Row dataRow = sheet.createRow(i + 1);

                dataRow.createCell(0).setCellValue(i + 1);
                dataRow.createCell(1).setCellValue(result.getQueryText() != null ? result.getQueryText() : "");
                dataRow.createCell(2).setCellValue(result.getTableName() != null ? result.getTableName() : "");
                dataRow.createCell(3).setCellValue(result.getPythonCode() != null ? result.getPythonCode() : "");
                dataRow.createCell(4).setCellValue(result.getGeneratedSql() != null ? result.getGeneratedSql() : "");
                dataRow.createCell(5).setCellValue(result.isSuccess() ? "成功" : "失败");
                dataRow.createCell(6).setCellValue(result.getErrorMessage() != null ? result.getErrorMessage() : "");
            }

            // 自动调整列宽
            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
            }

            // 保存文件
            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                workbook.write(fos);
            }
        }
    }
}
