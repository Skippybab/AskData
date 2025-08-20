package com.mt.agent.test.util;

import com.mt.agent.test.model.ExecutorTestData;
import com.mt.agent.test.model.PlannerTestData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 示例数据生成器
 * 用于生成测试用的Excel文件示例数据
 *
 * @author MT Agent Team
 * @version 1.0
 * @since 2025-01-15
 */
@Slf4j
@Component
public class SampleDataGenerator {

    @Autowired
    private ExcelUtil excelUtil;

    /**
     * 生成执行器测试示例数据
     *
     * @param filePath 输出文件路径
     */
    public void generateExecutorSampleData(String filePath) {
        List<ExecutorTestData> sampleData = new ArrayList<>();

        // 示例数据1
        ExecutorTestData data1 = new ExecutorTestData();
        data1.setQuestionId("1");
        data1.setUserInput("查询2023年营收前10的企业");
        data1.setHistoryInput("无历史输入");
        data1.setPythonCode("import pandas as pd\n" +
                "# 生成SQL查询\n" +
                "query = gen_sql(\n" +
                "    query_text=\"查询2023年营收前10的企业\",\n" +
                "    table_name=\"data_little_giant_business_info\",\n" +
                "    diag_history=\"\"\n" +
                ")\n" +
                "df = pd.read_sql(query, connection)\n" +
                "top_companies = df.nlargest(10, 'total_revenue')\n" +
                "print(top_companies[['company_name', 'total_revenue']])");
        sampleData.add(data1);

        // 示例数据2
        ExecutorTestData data2 = new ExecutorTestData();
        data2.setQuestionId("2");
        data2.setUserInput("统计各行业的平均利润率");
        data2.setHistoryInput("上次查询了营收前10企业");
        data2.setPythonCode("import pandas as pd\n" +
                "# 生成SQL查询\n" +
                "query = gen_sql(\n" +
                "    query_text=\"统计各行业的平均利润率\",\n" +
                "    table_name=\"data_little_giant_business_info\",\n" +
                "    diag_history=\"上次查询了营收前10企业\"\n" +
                ")\n" +
                "df = pd.read_sql(query, connection)\n" +
                "avg_profit = df.groupby('industry_code')['profit_margin_pct'].mean()\n" +
                "print(avg_profit.sort_values(ascending=False))");
        sampleData.add(data2);

        // 示例数据3
        ExecutorTestData data3 = new ExecutorTestData();
        data3.setQuestionId("3");
        data3.setUserInput("查找研发费用占比超过10%的企业数量");
        data3.setHistoryInput("之前分析了行业利润率");
        data3.setPythonCode("import pandas as pd\n" +
                "# 生成SQL查询\n" +
                "query = gen_sql(\n" +
                "    query_text=\"查找研发费用占比超过10%的企业数量\",\n" +
                "    table_name=\"data_little_giant_business_info\",\n" +
                "    diag_history=\"之前分析了行业利润率\"\n" +
                ")\n" +
                "df = pd.read_sql(query, connection)\n" +
                "high_rd = df[df['rd_expense_ratio'] > 0.10]\n" +
                "print(f'研发费用占比超过10%的企业数量: {len(high_rd)}')");
        sampleData.add(data3);

        // 写入Excel文件
        try {
            // 使用特殊的方法写入测试数据
            writeExecutorTestData(filePath, sampleData);
            log.info("执行器示例数据已生成到: {}", filePath);
        } catch (Exception e) {
            log.error("生成执行器示例数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 生成规划器测试示例数据
     *
     * @param filePath 输出文件路径
     */
    public void generatePlannerSampleData(String filePath) {
        List<PlannerTestData> sampleData = new ArrayList<>();

        // 示例数据1 - 第一轮对话的第一次提问
        PlannerTestData data1 = new PlannerTestData();
        data1.setQuestionId("1-1");
        data1.setUserInput("我想了解2023年医药制造业的整体情况");
        data1.setHistoryInput("");
        data1.setLastPlan("");
        data1.setLastReply("");
        sampleData.add(data1);

        // 示例数据2 - 第一轮对话的第二次提问
        PlannerTestData data2 = new PlannerTestData();
        data2.setQuestionId("1-2");
        data2.setUserInput("请具体分析一下营收分布情况");
        data2.setHistoryInput("用户：我想了解2023年医药制造业的整体情况\n助手：为您查询了医药制造业的基本统计信息");
        data2.setLastPlan("查询医药制造业企业数量、总营收、平均规模等基本信息");
        data2.setLastReply("2023年医药制造业共有1250家企业，总营收达到15000亿元");
        sampleData.add(data2);

        // 示例数据3 - 第二轮对话的第一次提问
        PlannerTestData data3 = new PlannerTestData();
        data3.setQuestionId("2-1");
        data3.setUserInput("比较一下医药制造业和通用设备制造业的研发投入");
        data3.setHistoryInput("");
        data3.setLastPlan("");
        data3.setLastReply("");
        sampleData.add(data3);

        // 写入Excel文件
        try {
            // 使用特殊的方法写入测试数据
            writePlannerTestData(filePath, sampleData);
            log.info("规划器示例数据已生成到: {}", filePath);
        } catch (Exception e) {
            log.error("生成规划器示例数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 写入执行器测试数据到Excel
     */
    private void writeExecutorTestData(String filePath, List<ExecutorTestData> testData) {
        // 转换为结果格式以便写入（复用现有的写入方法）
        List<com.mt.agent.test.model.ExecutorTestResult> results = new ArrayList<>();
        for (ExecutorTestData data : testData) {
            com.mt.agent.test.model.ExecutorTestResult result = new com.mt.agent.test.model.ExecutorTestResult();
            result.setQuestionId(data.getQuestionId());
            result.setRoundNumber(1);
            result.setUserInput(data.getUserInput());
            result.setHistoryInput(data.getHistoryInput());
            result.setPythonCode(data.getPythonCode());
            // 其他字段保持空
            results.add(result);
        }

        // 创建自定义的Excel写入方法
        writeExecutorDataAsTemplate(filePath, testData);
    }

    /**
     * 写入规划器测试数据到Excel
     */
    private void writePlannerTestData(String filePath, List<PlannerTestData> testData) {
        // 转换为结果格式以便写入
        List<com.mt.agent.test.model.PlannerTestResult> results = new ArrayList<>();
        for (PlannerTestData data : testData) {
            com.mt.agent.test.model.PlannerTestResult result = new com.mt.agent.test.model.PlannerTestResult();
            result.setQuestionId(data.getQuestionId());
            result.setRoundNumber(1);
            result.setUserInput(data.getUserInput());
            result.setHistoryInput(data.getHistoryInput());
            result.setLastPlan(data.getLastPlan());
            result.setLastReply(data.getLastReply());
            // 其他字段保持空
            results.add(result);
        }

        // 创建自定义的Excel写入方法
        writePlannerDataAsTemplate(filePath, testData);
    }

    /**
     * 将执行器测试数据写入为模板格式
     */
    private void writeExecutorDataAsTemplate(String filePath, List<ExecutorTestData> testData) {
        try {
            org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("执行器测试数据");

            // 创建标题行
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = { "问题编号", "用户输入", "历史输入", "Python代码" };
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 写入数据行
            for (int i = 0; i < testData.size(); i++) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(i + 1);
                ExecutorTestData data = testData.get(i);

                row.createCell(0).setCellValue(data.getQuestionId());
                row.createCell(1).setCellValue(data.getUserInput());
                row.createCell(2).setCellValue(data.getHistoryInput());
                row.createCell(3).setCellValue(data.getPythonCode());
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 写入文件
            java.io.FileOutputStream fileOut = new java.io.FileOutputStream(filePath);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();

        } catch (Exception e) {
            throw new RuntimeException("写入执行器测试数据失败", e);
        }
    }

    /**
     * 将规划器测试数据写入为模板格式
     */
    private void writePlannerDataAsTemplate(String filePath, List<PlannerTestData> testData) {
        try {
            org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("规划器测试数据");

            // 创建标题行
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = { "问题编号", "用户输入", "历史输入", "上一轮规划", "针对上一轮的问题回复内容" };
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 写入数据行
            for (int i = 0; i < testData.size(); i++) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(i + 1);
                PlannerTestData data = testData.get(i);

                row.createCell(0).setCellValue(data.getQuestionId());
                row.createCell(1).setCellValue(data.getUserInput());
                row.createCell(2).setCellValue(data.getHistoryInput());
                row.createCell(3).setCellValue(data.getLastPlan());
                row.createCell(4).setCellValue(data.getLastReply());
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 写入文件
            java.io.FileOutputStream fileOut = new java.io.FileOutputStream(filePath);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();

        } catch (Exception e) {
            throw new RuntimeException("写入规划器测试数据失败", e);
        }
    }

    /**
     * 生成所有示例数据
     */
    public void generateAllSampleData() {
        generateExecutorSampleData("test-executor.xlsx");
        generatePlannerSampleData("test-planner.xlsx");
        log.info("所有示例数据生成完成");
    }
}