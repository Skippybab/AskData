package com.mt.agent.test;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 创建测试用的Excel文件
 */
public class CreateTestExcelFile {

    @Test
    public void createTestExcelFile() throws IOException {
        // 创建Excel工作簿
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Python代码测试数据");

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Python代码");

            // 创建测试数据
            String[] testPythonCodes = {
                    // 示例1: 简单的gen_sql调用
                    "industry_list = [\"医药制造\", \"通用设备制造\"]\nfor industry in industry_list:\n    sql = gen_sql(\"获取医药制造业过去三年毛利率数据\", \"广州小巨人企业年度经营数据\")\n    result = exec_sql(sql)\n    vis_bar(result)",

                    // 示例2: 多个gen_sql调用
                    "# 查询不同行业数据\nmedical_sql = gen_sql(\"查询医药制造业2023年营收数据\", \"企业经营数据表\")\nmedical_data = exec_sql(medical_sql)\n\nequipment_sql = gen_sql(\"查询设备制造业2023年利润数据\", \"企业经营数据表\")\nequipment_data = exec_sql(equipment_sql)\n\nvis_comparison(medical_data, equipment_data)",

                    // 示例3: 复杂的业务逻辑
                    "# 三大行业对比分析\nindustries = [\"医药制造\", \"通用设备制造\", \"专用设备制造\"]\ndata_dict = {}\n\nfor industry in industries:\n    query_text = f\"获取{industry}业过去三年（2021-2023）各年度毛利率数据\"\n    sql = gen_sql(query_text, \"广州2021年到2023年小巨人企业年度经营数据\")\n    result = exec_sql(sql)",

                    // 示例4: 无gen_sql调用的代码（用于测试过滤功能）
                    "# 这段代码没有gen_sql调用，应该被跳过\ndata = [1, 2, 3, 4, 5]\nresult = sum(data)\nprint(f\"结果是: {result}\")",

                    // 示例5: 单引号的gen_sql调用
                    "company_sql = gen_sql('查询特定企业的基本信息', '企业基础信息表')\ncompany_data = exec_sql(company_sql)\n\nif company_data:\n    vis_table(company_data)\nelse:\n    vis_textbox(\"未找到企业数据\")"
            };

            // 填充测试数据
            for (int i = 0; i < testPythonCodes.length; i++) {
                Row dataRow = sheet.createRow(i + 1);
                Cell cell = dataRow.createCell(0);
                cell.setCellValue(testPythonCodes[i]);
            }

            // 自动调整列宽
            sheet.autoSizeColumn(0);

            // 保存文件
            try (FileOutputStream fos = new FileOutputStream("test.xlsx")) {
                workbook.write(fos);
                System.out.println("测试文件 test.xlsx 创建成功！");
            }
        }
    }
}