package com.mt.agent.test;

import com.mt.agent.buffer.util.BufferUtil;
import com.mt.agent.reporter.StepResultData;
import com.mt.agent.reporter.SubEventReporter;
import com.mt.agent.service.impl.PythonDirectExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.HashMap;

@SpringBootTest
@Slf4j
public class PythonCodeExecTest {

    @Autowired
    private PythonDirectExecutorService pythonDirectExecutorService;

    @Autowired
    private BufferUtil bufferUtil;

    String userId = "1";

    /**
     * 测试基本Python代码执行
     */
    @Test
    public void testBasicPythonExecution() {
        log.info("开始测试基本Python代码执行");

        String pythonCode = """
               ```python
                                 query_text = "2024年信息技术行业亏损企业名称及其资产负债率"
                                 table_name = "2023年到2025年广州市各企业年度营收数据表"
                                 sql_code = gen_sql(query_text, table_name)
                                 result_data = exec_sql(sql_code)
               
                                 filtered_data = []
                                 for i in range(len(result_data)):
                                     print(result_data)
                                     if i < len(result_data) - 1 and result_data[i]["年份"] == 2024 and result_data[i]["净利润总额_元"] < 0:
                                         prev_year_data = next((item for item in result_data if item["企业名称"] == result_data[i]["企业名称"] and item["年份"] == 2023), None)
                                         print(prev_year_data)
                                         if prev_year_data:
                                             current_ratio = float(result_data[i]["资产负债率_%"])
                                             previous_ratio = float(prev_year_data["资产负债率_%"])
                                             print(current_ratio)
                                             print(previous_ratio)
                                             if current_ratio > previous_ratio:
                                                 filtered_data.append({
                                                     "企业名称": result_data[i]["企业名称"],
                                                     "2023年资产负债率": previous_ratio,
                                                     "2024年资产负债率": current_ratio
                                                 })
               
                                 title = "2024年信息技术行业亏损且资产负债率同比增加的企业"
                                 vis_table(title, filtered_data)
                                 ```
               """;
//        String pythonCode = """
//               ```Python
//               # 获取2023年通用设备制造业企业的平均营收规模
//               sql_2023_avg_revenue = gen_sql("计算所属行业为'通用设备制造业'的企业在2023年的平均营业收入", "广州2021年到2023年小巨人企业年度经营数据")
//               result_2023_avg_revenue = exec_sql(sql_2023_avg_revenue)
//               value_2023_avg_revenue = result_2023_avg_revenue[0].get("AVG(`营业收入_元`)", 0) if result_2023_avg_revenue else 0
//
//               # 获取2022年通用设备制造业企业的平均营收规模
//               sql_2022_avg_revenue = gen_sql("计算所属行业为'通用设备制造业'的企业在2022年的平均营业收入", "广州2021年到2023年小巨人企业年度经营数据")
//               result_2022_avg_revenue = exec_sql(sql_2022_avg_revenue)
//               value_2022_avg_revenue = result_2022_avg_revenue[0].get("AVG(`营业收入_元`)", 0) if result_2022_avg_revenue else 0
//
//               # 计算平均营收增长率
//               if value_2022_avg_revenue != 0:
//                   avg_revenue_growth_rate = (value_2023_avg_revenue - value_2022_avg_revenue) / value_2022_avg_revenue * 100
//               else:
//                   avg_revenue_growth_rate = 0.0
//
//               vis_textblock("2023年行业平均营收规模（元）", float(value_2023_avg_revenue))
//               vis_textblock("2023年行业平均营收增长率(%)", avg_revenue_growth_rate)
//               ```
//               """;

        bufferUtil.savePythonCode(userId, pythonCode);

        pythonCode = pythonCode.replace("```python", "").replace("```Python", "").replace("```", "");

//                String pythonCode = """
//                # 生成查询2023年通用设备制造业小巨人企业数量的SQL语句
//                sql_2023 = sql_single_industry("查询2023年通用设备制造业小巨人企业数量，字段名称为企业数量")
//
//                # 生成查询2022年通用设备制造业小巨人企业数量的SQL语句
//                sql_2022 = sql_single_company("查询2022年通用设备制造业小巨人企业数量，字段名称为企业数量")
//
//                # 执行SQL语句，获取2023年的企业数量
//                result_2023 = sql_exc(sql_2023)
//
//                # 执行SQL语句，获取2022年的企业数量
//                result_2022 = sql_exc(sql_2022)
//
//                # 提取2023年的企业数量
//                count_2023 = result_2023[0]['企业数量']
//
//                # 提取2022年的企业数量
//                count_2022 = result_2022[0]['企业数量']
//
//                # 计算企业数量的变化
//                change = count_2023 - count_2022
//
//                # 计算变化率
//                change_rate = (change / count_2022) * 100
//                print(count_2023)
//                print(count_2022)
//                # 显示2023年企业数量
//                vis_textblock("2023年通用设备制造业小巨人企业数量", count_2023)
//
//                # 显示2022年企业数量
//                vis_textblock("2022年通用设备制造业小巨人企业数量", count_2022)
//
//                # 显示企业数量变化
//                vis_textblock("企业数量变化（2023 vs 2022）", f"{change}（{change_rate:.2f}%）")
//                """;

        HashMap<String, Object> paramMap = new HashMap<>();
        SubEventReporter reporter = createMockReporter();

        try {
            pythonDirectExecutorService.executePythonCode(pythonCode, paramMap, reporter, userId);
            log.info("基本Python代码执行测试完成");
        } catch (Exception e) {
            log.error("基本Python代码执行测试失败", e);
        }
    }

    /**
     * 测试带参数的Python代码执行
     */
    @Test
    public void testPythonWithParameters() {
        log.info("开始测试带参数的Python代码执行");

        String pythonCode = """
                ```Python
                # 获取医药制造业和通用设备制造业在2023年和2022年的相关数据
                sql_text_2023 = gen_sql("查询所属行业为'医药制造业'或'通用设备制造业'，年份为2023年的企业数据，按所属行业分组，统计营业收入总额、净利润总额、研发费用总额、出口额、员工总数", "广州2021年到2023年小巨人企业年度经营数据")
                sql_text_2022 = gen_sql("查询所属行业为'医药制造业'或'通用设备制造业'，年份为2022年的企业数据，按所属行业分组，统计营业收入总额、净利润总额、研发费用总额、出口额、员工总数", "广州2021年到2023年小巨人企业年度经营数据")
    
                result_2023 = exec_sql(sql_text_2023)
                result_2022 = exec_sql(sql_text_2022)
    
                # 提取医药制造业和通用设备制造业的数据
                industry_data_2023 = {}
                for row in result_2023:
                 industry = row["所属行业"]
                 revenue = float(row.get("营业收入_元", 0))
                 net_profit = float(row.get("净利润总额_元", 0))
                 r_and_d_expense = float(row.get("研发费用总额_元", 0))
                 export_amount = float(row.get("出口额_元", 0))
                 employee_count = float(row.get("全职员工数量_人", 0))
                 industry_data_2023[industry] = {
                 "revenue": revenue,
                 "net_profit": net_profit,
                 "r_and_d_expense": r_and_d_expense,
                 "export_amount": export_amount,
                 "employee_count": employee_count
                 }
    
                industry_data_2022 = {}
                for row in result_2022:
                 industry = row["所属行业"]
                 revenue = float(row.get("营业收入_元", 0))
                 net_profit = float(row.get("净利润总额_元", 0))
                 r_and_d_expense = float(row.get("研发费用总额_元", 0))
                 export_amount = float(row.get("出口额_元", 0))
                 employee_count = float(row.get("全职员工数量_人", 0))
                 industry_data_2022[industry] = {
                 "revenue": revenue,
                 "net_profit": net_profit,
                 "r_and_d_expense": r_and_d_expense,
                 "export_amount": export_amount,
                 "employee_count": employee_count
                 }
    
                # 计算差异并展示结果
                x_labels = ["医药制造业", "通用设备制造业"]
                revenue_diff = []
                net_profit_diff = []
                r_and_d_diff = []
                export_diff = []
                employee_diff = []
    
                for industry in x_labels:
                 rev_2023 = industry_data_2023.get(industry, {}).get("revenue", 0)
                 rev_2022 = industry_data_2022.get(industry, {}).get("revenue", 0)
                 rev_diff = rev_2023 - rev_2022 if rev_2022 != 0 else 0
                 revenue_diff.append(rev_diff)
    
                 profit_2023 = industry_data_2023.get(industry, {}).get("net_profit", 0)
                 profit_2022 = industry_data_2022.get(industry, {}).get("net_profit", 0)
                 profit_diff = profit_2023 - profit_2022 if profit_2022 != 0 else 0
                 net_profit_diff.append(profit_diff)
    
                 r_and_d_2023 = industry_data_2023.get(industry, {}).get("r_and_d_expense", 0)
                 r_and_d_2022 = industry_data_2022.get(industry, {}).get("r_and_d_expense", 0)
                 r_and_d_diff_val = r_and_d_2023 - r_and_d_2022 if r_and_d_2022 != 0 else 0
                 r_and_d_diff.append(r_and_d_diff_val)
    
                 export_2023 = industry_data_2023.get(industry, {}).get("export_amount", 0)
                 export_2022 = industry_data_2022.get(industry, {}).get("export_amount", 0)
                 export_diff_val = export_2023 - export_2022 if export_2022 != 0 else 0
                 export_diff.append(export_diff_val)
    
                 emp_2023 = industry_data_2023.get(industry, {}).get("employee_count", 0)
                 emp_2022 = industry_data_2022.get(industry, {}).get("employee_count", 0)
                 emp_diff = emp_2023 - emp_2022 if emp_2022 != 0 else 0
                 employee_diff.append(emp_diff)
    
                vis_clustered_bar(
                 "医药与通用设备制造业关键指标对比",
                 x_labels,
                 "2023年-2022年差值",
                 "2023年-2022年差值",
                 revenue_diff,
                 net_profit_diff
                )
    
                vis_clustered_bar(
                 "医药与通用设备制造业研发投入及出口额对比",
                 x_labels,
                 "2023年-2022年差值",
                 "2023年-2022年差值",
                 r_and_d_diff,
                 export_diff
                )
    
                vis_single_bar(
                 "医药与通用设备制造业员工数量变化",
                 x_labels,
                 employee_diff
                )
                ```
                """;

        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("username", "张三");
        paramMap.put("age", 25);
        paramMap.put("city", "北京");

        SubEventReporter reporter = createMockReporter();

        try {
            pythonDirectExecutorService.executePythonCode(pythonCode, paramMap, reporter, userId);
            log.info("带参数Python代码执行测试完成");
        } catch (Exception e) {
            log.error("带参数Python代码执行测试失败", e);
        }
    }

    /**
     * 测试可视化函数调用
     */
    @Test
    public void testVisualizationFunctions() {
        log.info("开始测试可视化函数调用");

        String pythonCode = """
                
                # 测试文本框
                vis_textbox("这是一个测试文本框内容")
                
                # 测试文本块
                vis_textblock("销售额", 1500000.50)
                
                # 测试单柱状图
                x_labels = ["一月", "二月", "三月", "四月"]
                y_data = [100.0, 120.0, 90.0, 150.0]
                vis_single_bar("月度销售额", x_labels, y_data)
                
                # 测试饼图
                pie_labels = ["产品A", "产品B", "产品C"]
                pie_data = [30.0, 45.0, 25.0]
                vis_pie_chart("产品销售占比", pie_labels, pie_data)
                
                print("哈哈哈")
                # 测试表格
                table_data = [
                    {"姓名": "张三", "年龄": 25, "部门": "技术部"},
                    {"姓名": "李四", "年龄": 30, "部门": "销售部"},
                    {"姓名": "王五", "年龄": 28, "部门": "市场部"}
                ]
                vis_table("员工信息表", table_data)
                
                report("可视化函数测试完成")
                """;

        HashMap<String, Object> paramMap = new HashMap<>();
        SubEventReporter reporter = createMockReporter();

        try {
            pythonDirectExecutorService.executePythonCode(pythonCode, paramMap, reporter, userId);
            log.info("可视化函数调用测试完成");
        } catch (Exception e) {
            log.error("可视化函数调用测试失败", e);
        }
    }

    /**
     * 测试数据分析功能
     */
    @Test
    public void testDataAnalysisFunctions() {
        log.info("开始测试数据分析功能");

        String pythonCode = """
                report("开始测试数据分析功能")
                
                # 测试预测功能
                historical_data = [100.0, 110.0, 120.0, 115.0, 125.0]
                forecast_steps = 3
                
                report(f"历史数据: {historical_data}")
                
                predicted_values = forecast_data(historical_data, forecast_steps)
                report(f"预测未来{forecast_steps}个时间点的数据: {predicted_values}")
                
                # 测试步骤总结功能
                step_list = ["数据加载成功", "数据清洗完成", "模型训练成功", "预测完成"]
                summary_title = "数据分析任务执行总结"
                
                summary = steps_summary(step_list, summary_title)
                report(f"任务执行总结:\n{summary}")
                
                report("数据分析功能测试完成")
                """;

        HashMap<String, Object> paramMap = new HashMap<>();
        SubEventReporter reporter = createMockReporter();

        try {
            pythonDirectExecutorService.executePythonCode(pythonCode, paramMap, reporter, userId);
            log.info("数据分析功能测试完成");
        } catch (Exception e) {
            log.error("数据分析功能测试失败", e);
        }
    }

    /**
     * 测试异常处理
     */
    @Test
    public void testErrorHandling() {
        log.info("开始测试异常处理");

        String pythonCode = """
                report("开始测试异常处理")
                
                try:
                    # 这里故意产生一个错误
                    result = 10 / 0
                except ZeroDivisionError as e:
                    report(f"捕获到除零错误: {e}")
                    report("错误已被正确处理")
                
                report("异常处理测试完成")
                """;

        HashMap<String, Object> paramMap = new HashMap<>();
        SubEventReporter reporter = createMockReporter();

        try {
            pythonDirectExecutorService.executePythonCode(pythonCode, paramMap, reporter, userId);
            log.info("异常处理测试完成");
        } catch (Exception e) {
            log.error("异常处理测试失败", e);
        }
    }

    /**
     * 测试复杂业务逻辑
     */
    @Test
    public void testComplexBusinessLogic() {
        log.info("开始测试复杂业务逻辑");

        String pythonCode = """
                report("开始执行复杂业务分析")
                
                # 模拟业务数据分析场景
                sales_data = sales_data if 'sales_data' in globals() else [
                    {"month": "2024-01", "revenue": 100000, "cost": 80000},
                    {"month": "2024-02", "revenue": 120000, "cost": 85000},
                    {"month": "2024-03", "revenue": 95000, "cost": 78000},
                    {"month": "2024-04", "revenue": 140000, "cost": 90000}
                ]
                
                report("开始分析销售数据")
                
                # 计算利润率
                for i, data in enumerate(sales_data):
                    profit = data["revenue"] - data["cost"]
                    profit_rate = (profit / data["revenue"]) * 100
                    sales_data[i]["profit"] = profit
                    sales_data[i]["profit_rate"] = round(profit_rate, 2)
                
                    report(f"{data['month']} 利润率: {profit_rate:.2f}%")
                
                # 生成图表数据
                months = [item["month"] for item in sales_data]
                revenues = [float(item["revenue"]) for item in sales_data]
                profits = [float(item["profit"]) for item in sales_data]
                
                # 绘制双柱状图
                vis_clustered_bar("月度收入与利润对比", months, "收入", "利润", revenues, profits)
                
                # 计算总体统计
                total_revenue = sum(revenues)
                total_profit = sum(profits)
                avg_profit_rate = sum([item["profit_rate"] for item in sales_data]) / len(sales_data)
                
                report(f"总收入: {total_revenue:,.2f}")
                report(f"总利润: {total_profit:,.2f}")
                report(f"平均利润率: {avg_profit_rate:.2f}%")
                
                # 展示详细表格
                vis_table("销售数据详情", sales_data)
                
                report("复杂业务分析完成")
                """;

        HashMap<String, Object> paramMap = new HashMap<>();
        SubEventReporter reporter = createMockReporter();

        try {
            pythonDirectExecutorService.executePythonCode(pythonCode, paramMap, reporter, userId);
            log.info("复杂业务逻辑测试完成");
        } catch (Exception e) {
            log.error("复杂业务逻辑测试失败", e);
        }
    }

    /**
     * 创建测试用的SubEventReporter
     */
    private SubEventReporter createMockReporter() {
        return Flux.<ServerSentEvent<?>>create(sink -> {
            // 空的事件发射器，仅用于测试
        }).as(flux -> new SubEventReporter(null) {
            @Override
            public void reportStep(String message) {
                System.out.println("📋 [步骤] " + message);
                log.info("[测试报告-步骤] {}", message);
            }

            @Override
            public void reportStepResult(StepResultData resultData) {
                System.out.println("✅ [步骤结果] " + (resultData != null ? resultData.toString() : "无结果"));
                log.info("[测试报告-步骤结果] {}", resultData);
            }

            @Override
            public void reportNodeResult(Object resultPayload) {
                System.out.println("🎯 [节点结果] " + (resultPayload != null ? resultPayload.toString() : "无结果"));
                log.info("[测试报告-节点结果] {}", resultPayload);
            }

            @Override
            public void reportError(String errorMessage, Throwable throwable) {
                System.out.println("❌ [错误] " + errorMessage);
                log.error("[测试报告-错误] {}", errorMessage, throwable);
            }

            @Override
            public void reportError(String errorMessage) {
                System.out.println("❌ [错误] " + errorMessage);
                log.error("[测试报告-错误] {}", errorMessage);
            }

            @Override
            public void reportOverReply(String replyMessage) {
                System.out.println("💬 [回复] " + replyMessage);
                log.info("[测试报告-回复] {}", replyMessage);
            }

            @Override
            public void reportComplete() {
                System.out.println("🎉 [任务完成]");
                log.info("[测试报告-任务完成]");
            }

            @Override
            public void reportCompleteAndClose() {
                System.out.println("🎉 [任务完成并关闭]");
                log.info("[测试报告-任务完成并关闭]");
            }

            @Override
            public void reportThinking(String thinkingContent) {
                System.out.println("🤔 [思考] " + thinkingContent);
                log.info("[测试报告-思考] {}", thinkingContent);
            }

            @Override
            public void reportTaskPlan(String[] planContent) {
                System.out.println("📋 [任务规划] " + java.util.Arrays.toString(planContent));
                log.info("[测试报告-任务规划] {}", java.util.Arrays.toString(planContent));
            }

            @Override
            public void reportTree(String treeJson) {
                System.out.println("🌳 [树结构] " + treeJson);
                log.info("[测试报告-树结构] {}", treeJson);
            }

            @Override
            public void reportAnswer(String result) {
                System.out.println("💡 [答案] " + result);
                log.info("[测试报告-答案] {}", result);
            }
        });
    }
}
