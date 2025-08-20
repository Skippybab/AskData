package com.mt.agent.test;

import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import com.mt.agent.JavaFileExecutor;
import com.mt.agent.router.service.RouterService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

/**
 * 执行器测试
 * 
 * @date on 2022/5/13 11:01
 */
@SpringBootTest
@Slf4j
public class ExecuteTest {

    @Autowired
    private RouterService routerService;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 测试RouterService的routeMatching方法
     */
    @Test
    public void testJavaFileExcutor() {


        // 执行测试
        log.info("开始执行RouterService.routeMatching...");
        routerService.routeMatching("1", null);
        log.info("完成执行RouterService.routeMatching");
    }



    @Test
    public void testFlowableExecution() throws Exception {
        JavaFileExecutor executor = new JavaFileExecutor();


        // 定义一个简单的返回Flowable的类
        String javaCode = """
                package com.mt.agent.test;

                import io.reactivex.rxjava3.core.Flowable;

                public class FlowableTest {
                    public Flowable<String> generateNumbers() {
                        return Flowable.range(1, 5)
                            .map(i -> "数字 " + i/0);
                    }
                }
                """;

        // 执行流式方法
        executor.executeJavaCode(javaCode, "generateNumbers", message -> {
            log.info("收到流消息: {}", message);
        });
    }


    @Test
    public void testSql() {

        String sql="SELECT  SUM(total_revenue)   as '营收' FROM data_little_giant_business_info WHERE industry_code LIKE '%通用设备制造业%'";
        List<Map<String, Object>> result = SqlRunner.db().selectList(sql);
        if (!result.isEmpty()) {
            // 获取第一行数据的字段信息
            Map<String, Object> firstRow = result.get(0);
            firstRow.keySet().forEach(fieldName -> {
                Object value = firstRow.get(fieldName);
                System.out.println("字段名：" + fieldName + " 类型：" + value.getClass());
            });
        }

    }

}
