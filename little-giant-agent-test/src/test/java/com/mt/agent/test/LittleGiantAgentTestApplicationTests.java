package com.mt.agent.test;

import com.mt.agent.test.service.TestExecutionService;
import com.mt.agent.test.util.SampleDataGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

/**
 * 小巨人代理测试应用程序测试类
 * 
 * 这是整体项目的模块测试类入口，提供示例数据生成和统一测试入口
 * 具体的测试功能已分离到专门的测试类中：
 * - 执行器测试：{@link com.mt.agent.test.executor.ExecutorTest}
 * - 规划器测试：{@link com.mt.agent.test.planner.PlannerTest}
 * 
 * @author MT Agent Team
 * @version 2.0
 * @since 2025-01-15
 */
@SpringBootTest
@ActiveProfiles("test")
@ComponentScan(basePackages = {
        "com.mt.agent.test",
        "com.mt.agent.ai",
        "com.mt.agent.consensus",
        "com.mt.agent.sysUtil",
        "com.mt.agent.coze",
        "com.mt.agent.common",
        "com.mt.agent.config"
})
@Slf4j
class LittleGiantAgentTestApplicationTests {

    @Autowired
    private TestExecutionService testExecutionService;

    @Autowired
    private SampleDataGenerator sampleDataGenerator;

    /**
     * 生成示例测试数据
     */
    @Test
    public void generateSampleData() {
        log.info("=== 开始生成示例测试数据 ===");
        sampleDataGenerator.generateAllSampleData();
        log.info("=== 示例测试数据生成完成 ===");
    }

    /**
     * 生成执行器测试示例数据
     */
    @Test
    public void generateExecutorSampleData() {
        log.info("开始生成执行器测试示例数据...");
        try {
            sampleDataGenerator.generateExecutorSampleData("test-executor-sample.xlsx");
            log.info("执行器测试示例数据生成完成");
        } catch (Exception e) {
            log.error("生成执行器测试示例数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 生成规划器示例数据
     */
    @Test
    public void generatePlannerSampleData() {
        log.info("=== 开始生成规划器示例数据 ===");
        sampleDataGenerator.generatePlannerSampleData("test-planner.xlsx");
        log.info("=== 规划器示例数据生成完成 ===");
    }

    /**
     * 统一测试入口示例
     * 
     * 注意：具体的测试功能已移至专门的测试类：
     * - 执行器测试：ExecutorTest
     * - 规划器测试：PlannerTest
     */
    @Test
    public void testUnifiedEntry() {
        log.info("=== 统一测试入口示例 ===");
        log.info("注意：具体的测试功能已移至专门的测试类：");
        log.info("- 执行器测试：com.mt.agent.test.executor.ExecutorTest");
        log.info("- 规划器测试：com.mt.agent.test.planner.PlannerTest");

        // 首先生成示例数据
        generateSampleData();

        // 通过统一入口执行测试
        testExecutionService.executeTestPlan("test-executor.xlsx", "executor", "case1a", 1);
        testExecutionService.executeTestPlan("test-planner.xlsx", "planner", "case1a", 1);
    }

    /**
     * 测试空行处理和错误处理
     */
    @Test
    public void testEmptyRowHandling() {
        log.info("=== 测试空行处理和错误处理 ===");
        log.info("注意：具体的测试功能已移至专门的测试类：");
        log.info("- 执行器测试：com.mt.agent.test.executor.ExecutorTest");
        log.info("- 规划器测试：com.mt.agent.test.planner.PlannerTest");
        log.info("请在相应的测试类中运行具体的测试方法");
    }

    /**
     * 显示测试架构说明
     */
    @Test
    public void showTestArchitecture() {
        log.info("=== 小巨人代理测试框架架构说明 ===");
        log.info("");
        log.info("📁 测试架构：");
        log.info("├── LittleGiantAgentTestApplicationTests (当前类)");
        log.info("│   ├── 示例数据生成");
        log.info("│   ├── 统一测试入口");
        log.info("│   └── 架构说明");
        log.info("│");
        log.info("├── 📦 Service层");
        log.info("│   ├── TestExecutionService (统一测试执行服务)");
        log.info("│   ├── ExecutorTestService (执行器测试服务)");
        log.info("│   ├── PlannerTestService (规划器测试服务)");
        log.info("│   └── TestResultAnalysisService (测试结果分析服务)");
        log.info("│");
        log.info("├── 📦 测试类");
        log.info("│   ├── ExecutorTest (执行器专用测试类)");
        log.info("│   └── PlannerTest (规划器专用测试类)");
        log.info("│");
        log.info("└── 📦 工具类");
        log.info("    ├── ExcelUtil (Excel读写工具)");
        log.info("    ├── PythonExecutor (Python执行器)");
        log.info("    ├── ThreadPoolUtil (线程池工具)");
        log.info("    └── SampleDataGenerator (示例数据生成器)");
        log.info("");
        log.info("🚀 使用建议：");
        log.info("1. 首先运行 generateSampleData() 生成测试数据");
        log.info("2. 执行器测试：运行 ExecutorTest 中的测试方法");
        log.info("3. 规划器测试：运行 PlannerTest 中的测试方法");
        log.info("4. 结果分析：运行 ExecutorTest.testResultStatistics()");
        log.info("");
        log.info("💡 核心特性：");
        log.info("- 多线程并发测试");
        log.info("- Excel数据驱动");
        log.info("- 结果统计分析");
        log.info("- 完整的错误处理");
        log.info("- 用户隔离支持");
        log.info("=== 架构说明完成 ===");
    }
}
