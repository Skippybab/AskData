package com.mt.agent.test.executor;

import com.mt.agent.test.service.ExecutorTestService;
import com.mt.agent.test.service.TestExecutionService;
import com.mt.agent.test.service.TestResultAnalysisService;
import com.mt.agent.test.util.SampleDataGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

/**
 * 执行器测试类
 * 专门用于执行器相关的测试
 *
 * @author MT Agent Team
 * @version 1.0
 * @since 2025-01-15
 */
@SpringBootTest
@ActiveProfiles("test")
@ComponentScan(basePackages = {
        "com.mt.agent.test",
        "com.mt.agent.sysUtil",
        "com.mt.agent.common",
        "com.mt.agent.config"
})
@Slf4j
public class ExecutorTest {

    @Autowired
    private ExecutorTestService executorTestService;

    @Autowired
    private TestExecutionService testExecutionService;

    @Autowired
    private TestResultAnalysisService testResultAnalysisService;

    @Autowired
    private SampleDataGenerator sampleDataGenerator;

    /**
     * 测试执行器 Case 1a
     * 数据集测试1次
     */
    @Test
    public void testExecutorCase1a() {
        log.info("开始执行器测试 Case 1a - 数据集测试1次");
        executorTestService.executeTest(1, "case1a", "test-executor.xlsx");
    }

    /**
     * 测试执行器 Case 1b
     * 数据集测试1次
     */
    @Test
    public void testExecutorCase1b() {
        log.info("开始执行器测试 Case 1b - 数据集测试1次");
        executorTestService.executeTest(1, "case1b", "test-executor.xlsx");
    }

    /**
     * 测试执行器 Case 1c
     * 数据集重复测试2次（用于验证重复测试功能）
     */
    @Test
    public void testExecutorCase1c() {
        log.info("开始执行器测试 Case 1c - 数据集重复测试10次");
        executorTestService.executeTest(10, "case1c", "test-executor.xlsx");
    }

    /**
     * 测试执行器 Case 1d
     */
    @Test
    public void testExecutorCase1d() {
        log.info("开始执行器测试 Case 1d");
        executorTestService.executeTest(1, "case1d", "test-executor.xlsx");
    }

    /**
     * 测试执行器 Case 1e
     */
    @Test
    public void testExecutorCase1e() {
        log.info("开始执行器测试 Case 1e");
        executorTestService.executeTest(1, "case1e", "test-executor.xlsx");
    }
    /**
     * 测试执行器 Case 2c
     */
    @Test
    public void testExecutorCase2c() {
        log.info("开始执行器测试 Case 2c - 数据集重复测试10次");
        executorTestService.executeTest(10, "case2c", "test-executor.xlsx");
    }
    /**
     * 测试执行器 Case 3c
     */
    @Test
    public void testExecutorCase3c() {
        log.info("开始执行器测试 Case 3c - 数据集重复测试10次");
        executorTestService.executeTest(10, "case3c", "test-executor-test.xlsx");
    }
    /**
     * 测试执行器 Case 4c
     */
    @Test
    public void testExecutorCase4c() {
        log.info("开始执行器测试 Case 4c - 数据集重复测试30次");
        executorTestService.executeTest(10, "case4c", "test-executor-test.xlsx");
    }
    /**
     * 通过统一入口测试执行器
     */
    @Test
    public void testExecutorThroughUnifiedEntry() {
        log.info("通过统一入口测试执行器");
        testExecutionService.executeTestPlan("test-executor.xlsx", "executor", "case1a", 1);
    }

    /**
     * 多配置批量测试
     */
    @Test
    public void testExecutorBatchTest() {
        log.info("开始执行器批量测试");
        String[] testConfigs = { "case1a", "case1b", "case1c", "case1d", "case1e" };

        for (String config : testConfigs) {
            try {
                log.info("开始测试配置: {}", config);
                executorTestService.executeTest(1, config, "test-executor.xlsx");
                log.info("完成测试配置: {}", config);
            } catch (Exception e) {
                log.error("测试配置 {} 失败: {}", config, e.getMessage(), e);
            }
        }
    }

    /**
     * 重复测试功能验证
     * 测试数据集重复测试3次的功能
     */
    @Test
    public void testExecutorMultipleRounds() {
        log.info("开始执行器重复测试功能验证");
        try {
            // 对case1a配置进行3轮重复测试
            log.info("执行case1a配置 - 数据集重复测试3次");
            executorTestService.executeTest(3, "case1a", "test-executor.xlsx");
            log.info("重复测试完成，预期生成包含3轮测试结果的文件");
        } catch (Exception e) {
            log.error("重复测试功能验证失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 生成执行器测试示例数据
     */
    @Test
    public void generateExecutorSampleData() {
        log.info("开始生成执行器测试示例数据");
        try {
            sampleDataGenerator.generateExecutorSampleData("test-executor-sample.xlsx");
            log.info("执行器测试示例数据生成完成");
        } catch (Exception e) {
            log.error("生成执行器测试示例数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 测试结果统计分析
     * 注意：需要先运行测试生成结果文件，然后修改resultFileName参数
     */
    @Test
    public void testResultStatistics() {
        log.info("开始测试结果统计分析");
        try {
            // 这里需要替换为实际的结果文件名
            String resultFileName = "executor-result-case1c-rounds10-20250708_154837.xlsx";
            testResultAnalysisService.statisticsExecutorResult(resultFileName);
        } catch (Exception e) {
            log.error("测试结果统计分析失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 测试Excel字段设置的完整性
     * 验证SQL描述文本和表名字段在各种情况下都能正确设置
     */
    @Test
    public void testExcelFieldsCompleteness() {
        log.info("开始测试Excel字段设置完整性");
        try {
            executorTestService.testExcelFieldsCompleteness();
            log.info("Excel字段设置完整性测试完成");
        } catch (Exception e) {
            log.error("Excel字段设置完整性测试失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 测试gen_sql参数解析功能
     * 验证修复后的解析逻辑是否能正确处理位置参数和命名参数
     */
    @Test
    public void testGenSqlParsingFunction() {
        log.info("开始测试gen_sql参数解析功能");
        try {
            executorTestService.testGenSqlParamsParsing();
            log.info("gen_sql参数解析功能测试完成");
        } catch (Exception e) {
            log.error("gen_sql参数解析功能测试失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 完整的执行器测试流程
     */
    @Test
    public void fullExecutorTestWorkflow() {
        log.info("开始完整的执行器测试流程");
        try {
            // 2. 执行测试
            log.info("步骤2: 执行测试");
            executorTestService.executeTest(1, "case1a", "test-executor.xlsx");

            // 3. 注意：结果统计需要在有实际结果文件后手动运行
            log.info("步骤3: 结果统计需要手动运行 testResultStatistics 方法");

            log.info("完整的执行器测试流程完成");
        } catch (Exception e) {
            log.error("完整的执行器测试流程失败: {}", e.getMessage(), e);
        }
    }
}