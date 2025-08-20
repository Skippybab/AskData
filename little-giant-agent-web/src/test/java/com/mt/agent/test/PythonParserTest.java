package com.mt.agent.test;

import com.mt.agent.model.workflow.JavaExecutable;
import com.mt.agent.router.util.PythonCodeParserUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Slf4j
public class PythonParserTest {

    @Test
    public void testPythonParser1() {
        log.info("\n--- 复杂嵌套调用示例 ---");

        String complexCode = """
                # 您的原始例子 - 现在可以正确解析
                data1 = vis_textblock("2023年行业总营收", ext_double_list_from_dataList(result_2, "营业收入")[0])
                
                # 多重嵌套函数调用
                display_chart(get_data(filter_by_year(data, 2023), "revenue"))
                
                # 混合参数类型
                complex_call(123, "text", get_data(), [1,2,3], {"key": get_value()})
                
                # 带索引的嵌套调用
                show_result(calculate_sum(get_values(data)[0:5]), format_currency(get_config()["currency"]))
                
                data2 = ["2021","2022","2023"]
                
                data3 = { "note": "世界" , data : result[0] }
                """;

        try {
            List<JavaExecutable> executables = PythonCodeParserUtil.parseJavaOrders(complexCode);

            log.info("✅ 成功解析 {} 个函数调用", executables.size());

            for (int i = 0; i < executables.size(); i++) {
                JavaExecutable exec = executables.get(i);
                log.info("  📋 函数{}: {}", i + 1, exec.getFunctionName());
                log.info("    参数数量: {}", exec.getInputs().size());
                for (int j = 0; j < exec.getInputs().size(); j++) {
                    String param = exec.getInputs().get(j);
                    if (param.length() > 50) {
                        param = param.substring(0, 47) + "...";
                    }
                    log.info("    参数{}: {}", j + 1, param);
                }
            }

        } catch (Exception e) {
            log.error("❌ 解析出错: {}", e.getMessage());
        }
    }

    @Test
    public void testPythonParser2() {
        log.info("🐛 用户原始问题修复演示");
        log.info("问题：字符串参数中的括号被截断");

        String userCode = """
                revenue_data = ext_double_list_from_dataList(result_2, "营业收入(元)")
                growth_rate_data = ext_double_list_from_dataList(result_2, "营业收入增长率(%)")
                """;

        log.info("输入代码:");
        log.info("{}", userCode);

        List<JavaExecutable> executables = PythonCodeParserUtil.parseJavaOrders(userCode);

        log.info("✅ 修复后的解析结果:");
        PythonCodeParserUtil.printParseResults(executables);

        // 验证修复效果
        if (executables.size() == 2) {
            String param1 = executables.get(0).getInputs().get(1);
            String param2 = executables.get(1).getInputs().get(1);

            if (param1.equals("\"营业收入(元)\"") && param2.equals("\"营业收入增长率(%)\"")) {
                log.info("🎉 修复成功！括号和引号完整保留");
            } else {
                log.error("❌ 修复失败，参数仍有问题:");
                log.error("  期望: \"营业收入(元)\"，实际: {}", param1);
                log.error("  期望: \"营业收入增长率(%)\"，实际: {}", param2);
            }
        } else {
            log.error("❌ 解析失败，应该有2个结果，实际: {}", executables.size());
        }
    }

}
