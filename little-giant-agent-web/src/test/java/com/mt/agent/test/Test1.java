package com.mt.agent.test;

import com.mt.agent.sysUtil.AISQLQueryUtil;
import com.mt.agent.sysUtil.ComputingUtil;
import com.mt.agent.sysUtil.DataUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
public class Test1 {

    @Autowired
    private AISQLQueryUtil aiSQLQueryUtil;

    @Test
    public void test1() {
        ExpressionParser parser = new SpelExpressionParser();
        log.info("{}", parser.parseExpression("'Hello'.concat( \"World\")").getValue());
        log.info("{}", parser.parseExpression("'Hello World'").getValue());
    }

    @Test
    public void test3() throws NoSuchMethodException {
        // 注册自定义静态方法
        StandardEvaluationContext context = new StandardEvaluationContext();
        Method parseInt = Integer.class.getDeclaredMethod("parseInt", String.class);
        context.registerFunction("parseInt", parseInt); // 注册为函数

        // 调用示例
        ExpressionParser parser = new SpelExpressionParser();
        int result = parser.parseExpression("#parseInt('123')").getValue(context, Integer.class);
        System.out.println(result); // 输出：123
    }

    @Test
    public void test4() throws NoSuchMethodException {
        // 注册ComputingUtil的静态方法到SpEL上下文
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 注册sum静态方法
        Method sumMethod = ComputingUtil.class.getDeclaredMethod("sum", List.class);
        context.registerFunction("sum", sumMethod);

        // 测试正常情况
        List<Object> data = List.of(1.0, 2.0, 3.0);
        context.setVariable("data", data);

        ExpressionParser parser = new SpelExpressionParser();

        try {
            // 执行SpEL表达式计算求和（调用静态方法）
            double sum = parser.parseExpression("#sum(#data)").getValue(context, double.class);
            log.info("sum: {}", sum);

            // 添加断言验证结果
            assertEquals(6.0, sum, 0.001, "求和结果应该等于6.0");

            // 测试空列表情况
            List<Object> emptyData = List.of();
            context.setVariable("data", emptyData);
            double emptySum = parser.parseExpression("#sum(#data)").getValue(context, double.class);
            assertEquals(0.0, emptySum, 0.001, "空列表求和应该等于0.0");

            // 测试单个元素
            List<Object> singleData = List.of(5.0);
            context.setVariable("data", singleData);
            double singleSum = parser.parseExpression("#sum(#data)").getValue(context, double.class);
            assertEquals(5.0, singleSum, 0.001, "单个元素求和应该等于自身");

            log.info("所有SpEL表达式测试通过");

        } catch (EvaluationException e) {
            fail("SpEL表达式执行失败: " + e.getMessage());
        } catch (ParseException e) {
            fail("SpEL表达式解析失败: " + e.getMessage());
        }
    }

    @Test
    public void testAISQLQueryUtilWithSpEL() {

        // 创建SpEL上下文并注册AISQLQueryUtil实例
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("aiSQLQueryUtil", aiSQLQueryUtil);

        // 设置测试参数
        String queryText = "查询通用设备制造业的营收数据，字段包含营收规模";
        context.setVariable("queryText", queryText);

        ExpressionParser parser = new SpelExpressionParser();

        try {
            // 使用SpEL表达式调用sqlGenSingleInd方法
            String sql = parser.parseExpression("#aiSQLQueryUtil.sqlGenSingleInd(#queryText)")
                    .getValue(context, String.class);

            log.info("SpEL调用结果 - 生成的SQL: {}", sql);

            // 验证结果
            assertNotNull(sql, "生成的SQL不应该为null");
            assertTrue(sql.trim().length() > 0, "生成的SQL不应该为空");
            assertTrue(sql.toLowerCase().contains("select"), "生成的SQL应该包含SELECT语句");
            assertEquals("SELECT * FROM industry_data WHERE industry_name = '制造业'", sql.trim(),
                    "生成的SQL应该与预期一致");

            log.info("AISQLQueryUtil SpEL表达式测试通过");

        } catch (EvaluationException e) {
            fail("SpEL表达式执行失败: " + e.getMessage());
        } catch (ParseException e) {
            fail("SpEL表达式解析失败: " + e.getMessage());
        } catch (Exception e) {
            fail("测试执行失败: " + e.getMessage());
        }
    }


    @Test
    public void testWithSpEL() {

        // 创建SpEL上下文并注册AISQLQueryUtil实例
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("aiSQLQueryUtil", aiSQLQueryUtil);

        // 设置测试参数
        String queryText = "查询通用设备制造业的营收数据，字段包含营收规模";
        context.setVariable("queryText", queryText);

        ExpressionParser parser = new SpelExpressionParser();

        String sql = parser.parseExpression("#aiSQLQueryUtil.sqlGenSingleInd(#queryText)")
                .getValue(context, String.class);
    }


    @Test
    public void testWithSpEL2() {

        // 创建SpEL上下文并注册AISQLQueryUtil实例
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("dataUtil", new DataUtil());

        // 设置测试参数
        List<Map<String, Object>>  dataList = new ArrayList<>();
        Map<String, Object> data1 = new HashMap<>();
        data1.put("营收", 99.0);
        data1.put("利润", 10.0);
        dataList.add(data1);
        Map<String, Object> data2 = new HashMap<>();
        data2.put("营收", 99.0);
        data2.put("利润", 10.0);
        dataList.add(data2);
        context.setVariable("dataList", dataList);

        ExpressionParser parser = new SpelExpressionParser();

        List value = parser.parseExpression("#dataUtil.extractDoubleListFromDataList(#dataList,\"营收\")")
                .getValue(context, List.class);
        log.info("函数执行结果: {}", value);
    }


    @Test
    public void testWithSpEL3() {

        // 创建SpEL上下文并注册AISQLQueryUtil实例
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("dataUtil", new DataUtil());

        // 设置测试参数
        List<Map<String, Object>>  dataList = new ArrayList<>();
        Map<String, Object> data1 = new HashMap<>();
        data1.put("营收", 99.0);
        data1.put("利润", 10.0);
        dataList.add(data1);
        Map<String, Object> data2 = new HashMap<>();
        data2.put("营收", 99.0);
        data2.put("利润", 10.0);
        dataList.add(data2);
        context.setVariable("dataList", dataList);

        ExpressionParser parser = new SpelExpressionParser();

        Double value = parser.parseExpression("#dataUtil.extractDoubleListFromDataList(#dataList,\"营收\").get(0)")
                .getValue(context, Double.class);
        log.info("函数执行结果: {}", value);
    }

}
