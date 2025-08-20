//package com.mt.agent.test.planner;
//
//import com.mt.agent.test.service.PlannerTestService;
//import com.mt.agent.test.service.TestExecutionService;
//import com.mt.agent.test.util.SampleDataGenerator;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.test.context.ActiveProfiles;
//
///**
// * 规划器测试类
// * 专门用于规划器相关的测试
// *
// * @author MT Agent Team
// * @version 1.0
// * @since 2025-01-15
// */
//@SpringBootTest
//@ActiveProfiles("test")
//@ComponentScan(basePackages = {
//        "com.mt.agent.test",
//        "com.mt.agent.consensus",
//        "com.mt.agent.coze",
//        "com.mt.agent.common",
//        "com.mt.agent.config"
//})
//@Slf4j
//public class PlannerTest {
//
//    @Autowired
//    private PlannerTestService plannerTestService;
//
//    @Autowired
//    private TestExecutionService testExecutionService;
//
//    @Autowired
//    private SampleDataGenerator sampleDataGenerator;
//
//    /**
//     * 测试规划器 Case 1a
//     * 数据集测试1次
//     */
//    @Test
//    public void testPlannerCase1a() {
//        log.info("开始规划器测试 Case 1a - 数据集测试1次");
//        plannerTestService.executeCase1aTest(1);
//    }
//
//    /**
//     * 测试规划器 Case 1b
//     * 数据集测试1次
//     */
//    @Test
//    public void testPlannerCase1b() {
//        log.info("开始规划器测试 Case 1b - 数据集测试1次");
//        plannerTestService.executeCase1bTest(1, "test-planner.xlsx");
//    }
//
//    /**
//     * 测试规划器 Case 1c
//     * 数据集重复测试2次（用于验证重复测试功能）
//     */
//    @Test
//    public void testPlannerCase1c() {
//        log.info("开始规划器测试 Case 1c - 数据集重复测试2次");
//        plannerTestService.executeCase1cTest(2, "test-planner.xlsx");
//    }
//
//    /**
//     * 测试规划器 Case 1d
//     * 数据集测试1次
//     */
//    @Test
//    public void testPlannerCase1d() {
//        log.info("开始规划器测试 Case 1d - 数据集测试1次");
//        plannerTestService.executeCase1dTest(1, "test-planner.xlsx");
//    }
//
//    /**
//     * 测试规划器 Case 1e
//     * 数据集测试1次
//     */
//    @Test
//    public void testPlannerCase1e() {
//        log.info("开始规划器测试 Case 1e - 数据集测试1次");
//        plannerTestService.executeCase1eTest(1, "test-planner.xlsx");
//    }
//
//    /**
//     * 测试规划器 Case 2a
//     * 数据集测试1次
//     */
//    @Test
//    public void testPlannerCase2a() {
//        log.info("开始规划器测试 Case 2a - 数据集测试1次");
//        plannerTestService.executeCase2aTest(1, "test-planner.xlsx");
//    }
//
//    /**
//     * 测试规划器 Case 3a
//     * 数据集测试1次
//     */
//    @Test
//    public void testPlannerCase3a() {
//        log.info("开始规划器测试 Case 3a - 数据集测试1次");
//        plannerTestService.executeCase3aTest(1, "test-planner.xlsx");
//    }
//
//    /**
//     * 通过统一入口测试规划器
//     */
//    @Test
//    public void testPlannerThroughUnifiedEntry() {
//        log.info("通过统一入口测试规划器");
//        testExecutionService.executeTestPlan("test-planner.xlsx", "planner", "case1a", 1);
//    }
//
//    /**
//     * 多配置批量测试
//     */
//    @Test
//    public void testPlannerBatchTest() {
//        log.info("开始规划器批量测试");
//        String[] testConfigs = { "case1a", "case1b", "case1c", "case1d", "case1e", "case2a", "case3a" };
//
//        for (String config : testConfigs) {
//            try {
//                log.info("开始测试配置: {}", config);
//                plannerTestService.executeTest(1, config, "test-planner.xlsx");
//                log.info("完成测试配置: {}", config);
//            } catch (Exception e) {
//                log.error("测试配置 {} 失败: {}", config, e.getMessage(), e);
//            }
//        }
//    }
//
//    /**
//     * 重复测试功能验证
//     * 测试数据集重复测试3次的功能
//     */
//    @Test
//    public void testPlannerMultipleRounds() {
//        log.info("开始规划器重复测试功能验证");
//        try {
//            // 对case1a配置进行3轮重复测试
//            log.info("执行case1a配置 - 数据集重复测试3次");
//            plannerTestService.executeTest(3, "case1a", "test-planner.xlsx");
//            log.info("重复测试完成，预期生成包含3轮测试结果的文件");
//        } catch (Exception e) {
//            log.error("重复测试功能验证失败: {}", e.getMessage(), e);
//        }
//    }
//
//    /**
//     * 生成规划器测试示例数据
//     */
//    @Test
//    public void generatePlannerSampleData() {
//        log.info("开始生成规划器测试示例数据");
//        try {
//            sampleDataGenerator.generatePlannerSampleData("test-planner-sample.xlsx");
//            log.info("规划器测试示例数据生成完成");
//        } catch (Exception e) {
//            log.error("生成规划器测试示例数据失败: {}", e.getMessage(), e);
//        }
//    }
//
//    /**
//     * 完整的规划器测试流程
//     */
//    @Test
//    public void fullPlannerTestWorkflow() {
//        log.info("开始完整的规划器测试流程");
//        try {
//            // 1. 生成示例数据
//            log.info("步骤1: 生成示例数据");
//            sampleDataGenerator.generatePlannerSampleData("test-planner.xlsx");
//
//            // 2. 执行测试
//            log.info("步骤2: 执行测试");
//            plannerTestService.executeTest(1, "case1a", "test-planner.xlsx");
//
//            log.info("完整的规划器测试流程完成");
//        } catch (Exception e) {
//            log.error("完整的规划器测试流程失败: {}", e.getMessage(), e);
//        }
//    }
//
//    /**
//     * 压力测试 - 多用户并发测试
//     */
//    @Test
//    public void plannerStressTest() {
//        log.info("开始规划器压力测试");
//        try {
//            // 生成测试数据
//            sampleDataGenerator.generatePlannerSampleData("test-planner-stress.xlsx");
//
//            // 执行多轮并发测试
//            for (int i = 1; i <= 5; i++) {
//                log.info("开始第{}轮压力测试", i);
//                plannerTestService.executeCase1aTest(i);
//                log.info("第{}轮压力测试完成", i);
//
//                // 短暂休息避免过度占用资源
//                Thread.sleep(1000);
//            }
//
//            log.info("规划器压力测试完成");
//        } catch (Exception e) {
//            log.error("规划器压力测试失败: {}", e.getMessage(), e);
//        }
//    }
//}