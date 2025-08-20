package com.mt.agent.test.util;

import com.mt.agent.test.model.ExecutorTestData;
import com.mt.agent.test.model.ExecutorTestResult;
import com.mt.agent.test.model.PlannerTestData;
import com.mt.agent.test.model.PlannerTestResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel工具类
 * 提供Excel文件的读取和写入功能
 *
 * @author MT Agent Team
 * @version 1.0
 * @since 2025-01-15
 */
@Slf4j
@Component
public class ExcelUtil {

    /**
     * Excel单元格最大字符数限制
     */
    private static final int EXCEL_MAX_CELL_LENGTH = 32767;

    /**
     * 截断后缀标识
     */
    private static final String TRUNCATED_SUFFIX = "...[文本已截断]";

    /**
     * 读取执行器测试数据
     *
     * @param filePath Excel文件路径
     * @return 测试数据列表
     */
    public List<ExecutorTestData> readExecutorTestData(String filePath) {
        List<ExecutorTestData> dataList = new ArrayList<>();

        try (FileInputStream fileInputStream = new FileInputStream(filePath);
                Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            // 跳过标题行，从第二行开始读取
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                // 检查是否为空行
                String questionId = getCellValue(row.getCell(0));
                String userInput = getCellValue(row.getCell(1));
                String historyInput = getCellValue(row.getCell(2));
                String pythonCode = getCellValue(row.getCell(3));

                // 如果问题编号和用户输入都为空，则跳过该行
                if (isEmptyString(questionId) && isEmptyString(userInput)) {
                    log.debug("跳过空行: 第{}行", i + 1);
                    continue;
                }

                ExecutorTestData data = new ExecutorTestData();
                data.setQuestionId(questionId);
                data.setUserInput(userInput);
                data.setHistoryInput(historyInput);
                data.setPythonCode(pythonCode);

                dataList.add(data);
            }

        } catch (IOException e) {
            log.error("读取执行器测试数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("读取执行器测试数据失败", e);
        }

        return dataList;
    }

    /**
     * 读取规划器测试数据
     *
     * @param filePath Excel文件路径
     * @return 测试数据列表
     */
    public List<PlannerTestData> readPlannerTestData(String filePath) {
        List<PlannerTestData> dataList = new ArrayList<>();

        try (FileInputStream fileInputStream = new FileInputStream(filePath);
                Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            // 跳过标题行，从第二行开始读取
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                PlannerTestData data = new PlannerTestData();
                data.setQuestionId(getCellValue(row.getCell(0)));
                data.setUserInput(getCellValue(row.getCell(1)));
                data.setHistoryInput(getCellValue(row.getCell(2)));
                data.setLastPlan(getCellValue(row.getCell(3)));
                data.setLastReply(getCellValue(row.getCell(4)));

                dataList.add(data);
            }

        } catch (IOException e) {
            log.error("读取规划器测试数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("读取规划器测试数据失败", e);
        }

        return dataList;
    }

    /**
     * 写入执行器测试结果
     *
     * @param filePath 输出文件路径
     * @param results  测试结果列表
     */
    public void writeExecutorTestResults(String filePath, List<ExecutorTestResult> results) {
        try (Workbook workbook = new XSSFWorkbook();
                FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {

            Sheet sheet = workbook.createSheet("执行器测试结果");

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = { "问题编号", "测试轮数", "SQL编号", "用户输入", "历史输入", "Python代码",
                    "SQL描述文本", "查询的表名", "SQL语句", "SQL生成花费时间", "SQL是否执行成功", "SQL执行结果", "SQL错误日志" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 写入数据行
            for (int i = 0; i < results.size(); i++) {
                Row row = sheet.createRow(i + 1);
                ExecutorTestResult result = results.get(i);

                row.createCell(0).setCellValue(limitCellText(result.getQuestionId()));
                row.createCell(1).setCellValue(result.getRoundNumber() != null ? result.getRoundNumber() : 0);
                row.createCell(2).setCellValue(limitCellText(result.getSqlNumber()));
                row.createCell(3).setCellValue(limitCellText(result.getUserInput()));
                row.createCell(4).setCellValue(limitCellText(result.getHistoryInput()));
                row.createCell(5).setCellValue(limitCellText(result.getPythonCode()));
                row.createCell(6).setCellValue(limitCellText(result.getSqlDescription()));
                row.createCell(7).setCellValue(limitCellText(result.getTableName()));
                row.createCell(8).setCellValue(limitCellText(result.getSqlStatement()));
                row.createCell(9).setCellValue(result.getSqlGenerateTime() != null ? result.getSqlGenerateTime() : 0L);
                row.createCell(10)
                        .setCellValue(result.getSqlExecuteSuccess() != null ? result.getSqlExecuteSuccess() : false);
                row.createCell(11)
                        .setCellValue(limitCellText(result.getSqlExecuteResult()));
                row.createCell(12).setCellValue(limitCellText(result.getSqlErrorLog()));
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(fileOutputStream);

        } catch (IOException e) {
            log.error("写入执行器测试结果失败: {}", e.getMessage(), e);
            throw new RuntimeException("写入执行器测试结果失败", e);
        }
    }

    /**
     * 写入规划器测试结果
     *
     * @param filePath 输出文件路径
     * @param results  测试结果列表
     */
    public void writePlannerTestResults(String filePath, List<PlannerTestResult> results) {
        try (Workbook workbook = new XSSFWorkbook();
                FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {

            Sheet sheet = workbook.createSheet("规划器测试结果");

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = { "问题编号", "测试轮数", "用户输入", "历史输入", "上一轮规划",
                    "针对上一轮的问题回复内容", "是否生成python代码", "python代码",
                    "规划花费时间", "python是否执行成功", "python执行结果", "执行异常错误日志" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 写入数据行
            for (int i = 0; i < results.size(); i++) {
                Row row = sheet.createRow(i + 1);
                PlannerTestResult result = results.get(i);

                row.createCell(0).setCellValue(result.getQuestionId());
                row.createCell(1).setCellValue(result.getRoundNumber());
                row.createCell(2).setCellValue(result.getUserInput());
                row.createCell(3).setCellValue(result.getHistoryInput());
                row.createCell(4).setCellValue(result.getLastPlan());
                row.createCell(5).setCellValue(result.getLastReply());
                row.createCell(6).setCellValue(result.getGeneratePythonCode());
                row.createCell(7).setCellValue(result.getPythonCode());
                row.createCell(8).setCellValue(result.getPlanTime());
                row.createCell(9).setCellValue(result.getPythonExecuteSuccess());
                row.createCell(10).setCellValue(result.getPythonExecuteResult());
                row.createCell(11).setCellValue(result.getErrorLog());
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(fileOutputStream);

        } catch (IOException e) {
            log.error("写入规划器测试结果失败: {}", e.getMessage(), e);
            throw new RuntimeException("写入规划器测试结果失败", e);
        }
    }

    /**
     * 追加写入执行器测试结果到Excel文件
     *
     * @param filePath 输出文件路径
     * @param results  要追加的测试结果列表
     */
    public synchronized void appendExecutorTestResults(String filePath, List<ExecutorTestResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        File file = new File(filePath);
        boolean fileExists = file.exists();

        try (FileInputStream fileInputStream = fileExists ? new FileInputStream(file) : null;
                Workbook workbook = fileExists ? new XSSFWorkbook(fileInputStream) : new XSSFWorkbook()) {

            Sheet sheet;
            int currentRowIndex = 0;

            if (fileExists && fileInputStream != null) {
                // 文件存在，获取已有的工作表
                sheet = workbook.getSheetAt(0);
                currentRowIndex = sheet.getLastRowNum() + 1;
            } else {
                // 文件不存在，创建新的工作表和标题行
                sheet = workbook.createSheet("执行器测试结果");
                Row headerRow = sheet.createRow(0);
                String[] headers = { "问题编号", "测试轮数", "SQL编号", "用户输入", "历史输入", "Python代码",
                        "SQL描述文本", "查询的表名", "SQL语句", "SQL生成花费时间", "SQL是否执行成功", "SQL执行结果", "SQL错误日志" };
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                }
                currentRowIndex = 1;
            }

            // 追加新的数据行
            for (ExecutorTestResult result : results) {
                Row row = sheet.createRow(currentRowIndex++);

                row.createCell(0).setCellValue(limitCellText(result.getQuestionId()));
                row.createCell(1).setCellValue(result.getRoundNumber() != null ? result.getRoundNumber() : 0);
                row.createCell(2).setCellValue(limitCellText(result.getSqlNumber()));
                row.createCell(3).setCellValue(limitCellText(result.getUserInput()));
                row.createCell(4).setCellValue(limitCellText(result.getHistoryInput()));
                row.createCell(5).setCellValue(limitCellText(result.getPythonCode()));
                row.createCell(6).setCellValue(limitCellText(result.getSqlDescription()));
                row.createCell(7).setCellValue(limitCellText(result.getTableName()));
                row.createCell(8).setCellValue(limitCellText(result.getSqlStatement()));
                row.createCell(9).setCellValue(result.getSqlGenerateTime() != null ? result.getSqlGenerateTime() : 0L);
                row.createCell(10)
                        .setCellValue(result.getSqlExecuteSuccess() != null ? result.getSqlExecuteSuccess() : false);
                row.createCell(11)
                        .setCellValue(limitCellText(result.getSqlExecuteResult()));
                row.createCell(12).setCellValue(limitCellText(result.getSqlErrorLog()));
            }

            // 自动调整列宽（仅在新文件时）
            if (!fileExists) {
                String[] headers = { "问题编号", "测试轮数", "SQL编号", "用户输入", "历史输入", "Python代码",
                        "SQL描述文本", "查询的表名", "SQL语句", "SQL生成花费时间", "SQL是否执行成功", "SQL执行结果", "SQL错误日志" };
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            // 写入文件
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                workbook.write(fileOutputStream);
            }

        } catch (IOException e) {
            log.error("追加写入执行器测试结果失败: {}", e.getMessage(), e);
            throw new RuntimeException("追加写入执行器测试结果失败", e);
        }
    }

    /**
     * 读取执行器测试结果（用于统计分析）
     *
     * @param filePath Excel文件路径
     * @return 测试结果列表
     */
    public List<ExecutorTestResult> readExecutorTestResults(String filePath) {
        List<ExecutorTestResult> results = new ArrayList<>();

        try (FileInputStream fileInputStream = new FileInputStream(filePath);
                Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            // 跳过标题行，从第二行开始读取
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                ExecutorTestResult result = new ExecutorTestResult();
                result.setQuestionId(getCellValue(row.getCell(0)));
                result.setRoundNumber(getIntCellValue(row.getCell(1)));
                result.setSqlNumber(getCellValue(row.getCell(2)));
                result.setUserInput(getCellValue(row.getCell(3)));
                result.setHistoryInput(getCellValue(row.getCell(4)));
                result.setPythonCode(getCellValue(row.getCell(5)));
                result.setSqlDescription(getCellValue(row.getCell(6)));
                result.setTableName(getCellValue(row.getCell(7)));
                result.setSqlStatement(getCellValue(row.getCell(8)));
                result.setSqlGenerateTime(getLongCellValue(row.getCell(9)));
                result.setSqlExecuteSuccess(getBooleanCellValue(row.getCell(10)));
                result.setSqlExecuteResult(getCellValue(row.getCell(11)));
                result.setSqlErrorLog(getCellValue(row.getCell(12)));

                results.add(result);
            }

        } catch (IOException e) {
            log.error("读取执行器测试结果失败: {}", e.getMessage(), e);
            throw new RuntimeException("读取执行器测试结果失败", e);
        }

        return results;
    }

    /**
     * 获取单元格字符串值
     */
    private String getCellValue(Cell cell) {
        if (cell == null)
            return "";

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
     * 获取单元格整数值
     */
    private Integer getIntCellValue(Cell cell) {
        if (cell == null)
            return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    /**
     * 获取单元格长整数值
     */
    private Long getLongCellValue(Cell cell) {
        if (cell == null)
            return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return (long) cell.getNumericCellValue();
            case STRING:
                try {
                    return Long.parseLong(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    /**
     * 获取单元格布尔值
     */
    private Boolean getBooleanCellValue(Cell cell) {
        if (cell == null)
            return null;

        switch (cell.getCellType()) {
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case STRING:
                return "true".equalsIgnoreCase(cell.getStringCellValue());
            default:
                return null;
        }
    }

    /**
     * 判断字符串是否为空（null、空字符串或只有空白字符）
     */
    private boolean isEmptyString(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 限制字符串长度以符合Excel单元格限制
     * 
     * @param text 原始文本
     * @return 处理后的文本
     */
    private String limitCellText(String text) {
        if (text == null) {
            return "";
        }

        if (text.length() <= EXCEL_MAX_CELL_LENGTH) {
            return text;
        }

        // 计算可用字符数（减去后缀长度）
        int availableLength = EXCEL_MAX_CELL_LENGTH - TRUNCATED_SUFFIX.length();

        // 截断文本并添加后缀
        String truncatedText = text.substring(0, availableLength) + TRUNCATED_SUFFIX;

        log.warn("文本长度 {} 超过Excel单元格限制 {}，已截断到 {} 字符",
                text.length(), EXCEL_MAX_CELL_LENGTH, truncatedText.length());

        return truncatedText;
    }
}