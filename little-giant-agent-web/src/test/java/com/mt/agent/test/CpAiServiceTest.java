package com.mt.agent.test;

import com.mt.agent.ai.enums.AliModelType;
import com.mt.agent.ai.enums.SiliconFlowModelType;
import com.mt.agent.ai.service.AiService;
import com.mt.agent.ai.service.SiliconFlowAiService;
import io.reactivex.Flowable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 双平台AI服务对比测试类
 * 对比DashScope和SiliconFlow平台相同模型的性能和效果
 */
@SpringBootTest
@Slf4j
public class CpAiServiceTest {

    @Autowired
    private SiliconFlowAiService siliconFlowAiService;

    @Autowired
    private AiService aiService;

    /**
     * 测试结果数据类
     */
    @Data
    @AllArgsConstructor
    public static class TestResult {
        private String platform; // 平台名称
        private String model; // 模型名称
        private String question; // 测试问题
        private String response; // 模型回复
        private long responseTime; // 响应时间(ms)
        private int responseLength; // 回复长度(字符数)
        private double throughput; // 吞吐速度(字符/秒)
        private boolean success; // 是否成功
        private String errorMessage; // 错误信息
    }

    /**
     * 测试问题列表
     */
    private final List<String> testQuestions = List.of(
            "请简要介绍人工智能的发展历程，不超过200字",
            "什么是量子计算？请用通俗易懂的语言解释",
            "分析一下区块链技术的优缺点",
            "请写一个快速排序算法的Java实现",
            "如何解决城市交通拥堵问题？请提供3个建议");

    /**
     * Qwen3-8B模型对比测试
     */
    @Test
    public void testQwen3_8B_Comparison() {
        log.info("=== Qwen3-8B 模型对比测试 ===");

        List<TestResult> dashScopeResults = new ArrayList<>();
        List<TestResult> siliconFlowResults = new ArrayList<>();

        for (String question : testQuestions) {
            log.info("测试问题: {}", question);

            // 测试DashScope
            TestResult dashScopeResult = testDashScopeSync(question, AliModelType.QWEN_3_8B_INSTRUCT);
            dashScopeResults.add(dashScopeResult);

            // 测试SiliconFlow
            TestResult siliconFlowResult = testSiliconFlowSync(question, SiliconFlowModelType.QWEN_3_8B);
            siliconFlowResults.add(siliconFlowResult);

            // 添加间隔避免频率限制
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 输出对比结果
        generateComparisonReport("Qwen3-8B", dashScopeResults, siliconFlowResults);
    }

    /**
     * Qwen3-14B模型对比测试
     */
    @Test
    public void testQwen3_14B_Comparison() {
        log.info("=== Qwen3-14B 模型对比测试 ===");

        List<TestResult> dashScopeResults = new ArrayList<>();
        List<TestResult> siliconFlowResults = new ArrayList<>();

        for (String question : testQuestions) {
            log.info("测试问题: {}", question);

            // 测试DashScope
            TestResult dashScopeResult = testDashScopeSync(question, AliModelType.QWEN_3_14B_INSTRUCT);
            dashScopeResults.add(dashScopeResult);

            // 测试SiliconFlow
            TestResult siliconFlowResult = testSiliconFlowSync(question, SiliconFlowModelType.QWEN_3_14B);
            siliconFlowResults.add(siliconFlowResult);

            // 添加间隔避免频率限制
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 输出对比结果
        generateComparisonReport("Qwen3-14B", dashScopeResults, siliconFlowResults);
    }

    /**
     * Qwen3-32B模型对比测试
     */
    @Test
    public void testQwen3_32B_Comparison() {
        log.info("=== Qwen3-32B 模型对比测试 ===");

        List<TestResult> dashScopeResults = new ArrayList<>();
        List<TestResult> siliconFlowResults = new ArrayList<>();

        for (String question : testQuestions) {
            log.info("测试问题: {}", question);

            // 测试DashScope
            TestResult dashScopeResult = testDashScopeSync(question, AliModelType.QWEN_3_32B_INSTRUCT);
            dashScopeResults.add(dashScopeResult);

            // 测试SiliconFlow
            TestResult siliconFlowResult = testSiliconFlowSync(question, SiliconFlowModelType.QWEN_3_32B);
            siliconFlowResults.add(siliconFlowResult);

            // 添加间隔避免频率限制
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 输出对比结果
        generateComparisonReport("Qwen3-32B", dashScopeResults, siliconFlowResults);
    }

    /**
     * 流式调用对比测试
     */
    @Test
    public void testStreamingComparison() {
        log.info("=== 流式调用对比测试 ===");

        String testQuestion = "请详细分析人工智能在未来10年的发展趋势，包括技术突破、应用场景和挑战";

        log.info("测试问题: {}", testQuestion);

        // 测试DashScope流式调用
        TestResult dashScopeStreamResult = testDashScopeStream(testQuestion, AliModelType.QWEN_3_14B_INSTRUCT);

        // 测试SiliconFlow流式调用
        TestResult siliconFlowStreamResult = testSiliconFlowStream(testQuestion, SiliconFlowModelType.QWEN_3_14B);

        // 输出流式调用对比
        log.info("\n" + "=".repeat(80));
        log.info("流式调用对比结果:");
        log.info("=".repeat(80));

        printSingleResult(dashScopeStreamResult);
        log.info("-".repeat(80));
        printSingleResult(siliconFlowStreamResult);

        // 对比分析
        if (dashScopeStreamResult.isSuccess() && siliconFlowStreamResult.isSuccess()) {
            log.info("\n流式调用性能对比:");
            log.info("DashScope 响应时间: {}ms | SiliconFlow 响应时间: {}ms",
                    dashScopeStreamResult.getResponseTime(),
                    siliconFlowStreamResult.getResponseTime());
            log.info("DashScope 吞吐速度: {}字符/秒 | SiliconFlow 吞吐速度: {}字符/秒",
                    String.format("%.2f", dashScopeStreamResult.getThroughput()),
                    String.format("%.2f", siliconFlowStreamResult.getThroughput()));

            String fasterPlatform = dashScopeStreamResult.getResponseTime() < siliconFlowStreamResult.getResponseTime()
                    ? "DashScope"
                    : "SiliconFlow";
            log.info("流式调用速度: {} 更快", fasterPlatform);
        }
    }

    /**
     * 全面性能压力测试
     */
    @Test
    public void testPerformanceStressTest() {
        log.info("=== 全面性能压力测试 ===");

        String stressTestQuestion = "解释一下机器学习的基本概念";
        int testRounds = 3; // 每个模型测试3轮

        log.info("压力测试配置: 问题='{}', 测试轮数={}", stressTestQuestion, testRounds);

        // 测试所有模型组合
        AliModelType[] dashScopeModels = {
                AliModelType.QWEN_3_8B_INSTRUCT,
                AliModelType.QWEN_3_14B_INSTRUCT,
                AliModelType.QWEN_3_32B_INSTRUCT
        };

        SiliconFlowModelType[] siliconFlowModels = {
                SiliconFlowModelType.QWEN_3_8B,
                SiliconFlowModelType.QWEN_3_14B,
                SiliconFlowModelType.QWEN_3_32B
        };

        for (int i = 0; i < dashScopeModels.length; i++) {
            AliModelType dashScopeModel = dashScopeModels[i];
            SiliconFlowModelType siliconFlowModel = siliconFlowModels[i];

            log.info("\n测试模型对: {} vs {}", dashScopeModel.getName(), siliconFlowModel.getName());

            List<TestResult> dashScopeRounds = new ArrayList<>();
            List<TestResult> siliconFlowRounds = new ArrayList<>();

            // 多轮测试
            for (int round = 1; round <= testRounds; round++) {
                log.info("第{}轮测试...", round);

                TestResult dashScopeResult = testDashScopeSync(stressTestQuestion, dashScopeModel);
                dashScopeRounds.add(dashScopeResult);

                TestResult siliconFlowResult = testSiliconFlowSync(stressTestQuestion, siliconFlowModel);
                siliconFlowRounds.add(siliconFlowResult);

                // 轮次间隔
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 计算平均性能
            generateAveragePerformanceReport(dashScopeModel.getName(), siliconFlowModel.getName(),
                    dashScopeRounds, siliconFlowRounds);
        }
    }

    /**
     * 测试DashScope同步调用
     */
    private TestResult testDashScopeSync(String question, AliModelType modelType) {
        long startTime = System.currentTimeMillis();

        try {
            String response = aiService.chat(question, modelType);
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            int responseLength = response.length();
            double throughput = (responseLength * 1000.0) / responseTime; // 字符/秒

            return new TestResult(
                    "DashScope",
                    modelType.getName(),
                    question,
                    response,
                    responseTime,
                    responseLength,
                    throughput,
                    true,
                    null);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;

            log.error("DashScope {} 调用失败: {}", modelType.getName(), e.getMessage());
            return new TestResult(
                    "DashScope",
                    modelType.getName(),
                    question,
                    null,
                    responseTime,
                    0,
                    0.0,
                    false,
                    e.getMessage());
        }
    }

    /**
     * 测试SiliconFlow同步调用
     */
    private TestResult testSiliconFlowSync(String question, SiliconFlowModelType modelType) {
        long startTime = System.currentTimeMillis();

        try {
            String response = siliconFlowAiService.chat(question, modelType);
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            int responseLength = response.length();
            double throughput = (responseLength * 1000.0) / responseTime; // 字符/秒

            return new TestResult(
                    "SiliconFlow",
                    modelType.getName(),
                    question,
                    response,
                    responseTime,
                    responseLength,
                    throughput,
                    true,
                    null);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;

            log.error("SiliconFlow {} 调用失败: {}", modelType.getName(), e.getMessage());
            return new TestResult(
                    "SiliconFlow",
                    modelType.getName(),
                    question,
                    null,
                    responseTime,
                    0,
                    0.0,
                    false,
                    e.getMessage());
        }
    }

    /**
     * 测试DashScope流式调用
     */
    private TestResult testDashScopeStream(String question, AliModelType modelType) {
        long startTime = System.currentTimeMillis();
        AtomicLong firstTokenTime = new AtomicLong(0);
        StringBuilder responseBuilder = new StringBuilder();

        try {
            CountDownLatch latch = new CountDownLatch(1);

            Flowable<String> streamResponse = aiService.chatStream(question, modelType);

            streamResponse
                    .doOnNext(chunk -> {
                        if (firstTokenTime.get() == 0) {
                            firstTokenTime.set(System.currentTimeMillis());
                        }
                        responseBuilder.append(chunk);
                    })
                    .doOnComplete(() -> latch.countDown())
                    .doOnError(error -> latch.countDown())
                    .subscribe();

            // 等待流式调用完成
            latch.await(60, TimeUnit.SECONDS);

            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            long firstTokenLatency = firstTokenTime.get() - startTime;

            String response = responseBuilder.toString();
            int responseLength = response.length();
            double throughput = (responseLength * 1000.0) / responseTime;

            log.info("DashScope {} 流式调用 - 首Token延迟: {}ms", modelType.getName(), firstTokenLatency);

            return new TestResult(
                    "DashScope-Stream",
                    modelType.getName(),
                    question,
                    response,
                    responseTime,
                    responseLength,
                    throughput,
                    true,
                    null);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;

            log.error("DashScope {} 流式调用失败: {}", modelType.getName(), e.getMessage());
            return new TestResult(
                    "DashScope-Stream",
                    modelType.getName(),
                    question,
                    null,
                    responseTime,
                    0,
                    0.0,
                    false,
                    e.getMessage());
        }
    }

    /**
     * 测试SiliconFlow流式调用
     */
    private TestResult testSiliconFlowStream(String question, SiliconFlowModelType modelType) {
        long startTime = System.currentTimeMillis();
        AtomicLong firstTokenTime = new AtomicLong(0);
        StringBuilder responseBuilder = new StringBuilder();

        try {
            CountDownLatch latch = new CountDownLatch(1);

            Flowable<String> streamResponse = siliconFlowAiService.chatStream(question, modelType);

            streamResponse
                    .doOnNext(chunk -> {
                        if (firstTokenTime.get() == 0) {
                            firstTokenTime.set(System.currentTimeMillis());
                        }
                        responseBuilder.append(chunk);
                    })
                    .doOnComplete(() -> latch.countDown())
                    .doOnError(error -> latch.countDown())
                    .subscribe();

            // 等待流式调用完成
            latch.await(60, TimeUnit.SECONDS);

            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            long firstTokenLatency = firstTokenTime.get() - startTime;

            String response = responseBuilder.toString();
            int responseLength = response.length();
            double throughput = (responseLength * 1000.0) / responseTime;

            log.info("SiliconFlow {} 流式调用 - 首Token延迟: {}ms", modelType.getName(), firstTokenLatency);

            return new TestResult(
                    "SiliconFlow-Stream",
                    modelType.getName(),
                    question,
                    response,
                    responseTime,
                    responseLength,
                    throughput,
                    true,
                    null);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;

            log.error("SiliconFlow {} 流式调用失败: {}", modelType.getName(), e.getMessage());
            return new TestResult(
                    "SiliconFlow-Stream",
                    modelType.getName(),
                    question,
                    null,
                    responseTime,
                    0,
                    0.0,
                    false,
                    e.getMessage());
        }
    }

    /**
     * 生成对比报告
     */
    private void generateComparisonReport(String modelName, List<TestResult> dashScopeResults,
            List<TestResult> siliconFlowResults) {
        log.info("\n" + "=".repeat(100));
        log.info("{} 模型对比报告", modelName);
        log.info("=".repeat(100));

        for (int i = 0; i < dashScopeResults.size(); i++) {
            TestResult dashScopeResult = dashScopeResults.get(i);
            TestResult siliconFlowResult = siliconFlowResults.get(i);

            log.info("\n问题 {}: {}", i + 1, dashScopeResult.getQuestion());
            log.info("-".repeat(100));

            // DashScope结果
            printSingleResult(dashScopeResult);

            log.info("-".repeat(50));

            // SiliconFlow结果
            printSingleResult(siliconFlowResult);

            // 对比分析
            if (dashScopeResult.isSuccess() && siliconFlowResult.isSuccess()) {
                log.info("\n📊 性能对比:");
                log.info("响应时间: DashScope {}ms vs SiliconFlow {}ms ({}更快)",
                        dashScopeResult.getResponseTime(),
                        siliconFlowResult.getResponseTime(),
                        dashScopeResult.getResponseTime() < siliconFlowResult.getResponseTime() ? "DashScope "
                                : "SiliconFlow ");

                log.info("回复长度: DashScope {}字符 vs SiliconFlow {}字符",
                        dashScopeResult.getResponseLength(),
                        siliconFlowResult.getResponseLength());

                log.info("吞吐速度: DashScope {}字符/秒 vs SiliconFlow {}字符/秒",
                        String.format("%.2f", dashScopeResult.getThroughput()),
                        String.format("%.2f", siliconFlowResult.getThroughput()));
            }
        }

        // 总体统计
        generateOverallStatistics(modelName, dashScopeResults, siliconFlowResults);
    }

    /**
     * 打印单个测试结果
     */
    private void printSingleResult(TestResult result) {
        log.info("🔹 平台: {} | 模型: {}", result.getPlatform(), result.getModel());
        log.info("📊 性能: 响应时间={}ms, 回复长度={}字符, 吞吐速度={}字符/秒",
                result.getResponseTime(), result.getResponseLength(),
                String.format("%.2f", result.getThroughput()));

        if (result.isSuccess()) {
            log.info("✅ 状态: 成功");
            log.info("💬 回复预览: {}",
                    result.getResponse().length() > 100
                            ? result.getResponse().substring(0, 100) + "..."
                            : result.getResponse());
        } else {
            log.info("❌ 状态: 失败");
            log.info("🚫 错误: {}", result.getErrorMessage());
        }
    }

    /**
     * 生成总体统计
     */
    private void generateOverallStatistics(String modelName, List<TestResult> dashScopeResults,
            List<TestResult> siliconFlowResults) {
        log.info("\n" + "=".repeat(50));
        log.info("📈 {} 总体统计", modelName);
        log.info("=".repeat(50));

        // DashScope统计
        double dashScopeAvgTime = dashScopeResults.stream()
                .filter(TestResult::isSuccess)
                .mapToLong(TestResult::getResponseTime)
                .average().orElse(0.0);

        double dashScopeAvgThroughput = dashScopeResults.stream()
                .filter(TestResult::isSuccess)
                .mapToDouble(TestResult::getThroughput)
                .average().orElse(0.0);

        long dashScopeSuccessCount = dashScopeResults.stream()
                .mapToLong(result -> result.isSuccess() ? 1 : 0)
                .sum();

        // SiliconFlow统计
        double siliconFlowAvgTime = siliconFlowResults.stream()
                .filter(TestResult::isSuccess)
                .mapToLong(TestResult::getResponseTime)
                .average().orElse(0.0);

        double siliconFlowAvgThroughput = siliconFlowResults.stream()
                .filter(TestResult::isSuccess)
                .mapToDouble(TestResult::getThroughput)
                .average().orElse(0.0);

        long siliconFlowSuccessCount = siliconFlowResults.stream()
                .mapToLong(result -> result.isSuccess() ? 1 : 0)
                .sum();

        log.info("DashScope - 成功率: {}/{}, 平均响应时间: {}ms, 平均吞吐速度: {}字符/秒",
                dashScopeSuccessCount, dashScopeResults.size(),
                String.format("%.2f", dashScopeAvgTime),
                String.format("%.2f", dashScopeAvgThroughput));

        log.info("SiliconFlow - 成功率: {}/{}, 平均响应时间: {}ms, 平均吞吐速度: {}字符/秒",
                siliconFlowSuccessCount, siliconFlowResults.size(),
                String.format("%.2f", siliconFlowAvgTime),
                String.format("%.2f", siliconFlowAvgThroughput));

        // 综合对比
        if (dashScopeSuccessCount > 0 && siliconFlowSuccessCount > 0) {
            String fasterPlatform = dashScopeAvgTime < siliconFlowAvgTime ? "DashScope" : "SiliconFlow";
            String higherThroughputPlatform = dashScopeAvgThroughput > siliconFlowAvgThroughput ? "DashScope"
                    : "SiliconFlow";

            log.info("🏆 {} 响应速度更快", fasterPlatform);
            log.info("🚀 {} 吞吐量更高", higherThroughputPlatform);
        }
    }

    /**
     * 生成平均性能报告
     */
    private void generateAveragePerformanceReport(String dashScopeModelName, String siliconFlowModelName,
            List<TestResult> dashScopeRounds, List<TestResult> siliconFlowRounds) {

        log.info("\n📊 {}轮平均性能对比:", dashScopeRounds.size());

        // 计算DashScope平均值
        double dashScopeAvgTime = dashScopeRounds.stream()
                .filter(TestResult::isSuccess)
                .mapToLong(TestResult::getResponseTime)
                .average().orElse(0.0);

        double dashScopeAvgThroughput = dashScopeRounds.stream()
                .filter(TestResult::isSuccess)
                .mapToDouble(TestResult::getThroughput)
                .average().orElse(0.0);

        // 计算SiliconFlow平均值
        double siliconFlowAvgTime = siliconFlowRounds.stream()
                .filter(TestResult::isSuccess)
                .mapToLong(TestResult::getResponseTime)
                .average().orElse(0.0);

        double siliconFlowAvgThroughput = siliconFlowRounds.stream()
                .filter(TestResult::isSuccess)
                .mapToDouble(TestResult::getThroughput)
                .average().orElse(0.0);

        log.info("{}: 平均响应时间={}ms, 平均吞吐速度={}字符/秒",
                dashScopeModelName,
                String.format("%.2f", dashScopeAvgTime),
                String.format("%.2f", dashScopeAvgThroughput));
        log.info("{}: 平均响应时间={}ms, 平均吞吐速度={}字符/秒",
                siliconFlowModelName,
                String.format("%.2f", siliconFlowAvgTime),
                String.format("%.2f", siliconFlowAvgThroughput));

        double speedImprovement = ((siliconFlowAvgTime - dashScopeAvgTime) / siliconFlowAvgTime) * 100;
        log.info("速度优势: {} {}%",
                speedImprovement > 0 ? "DashScope领先" : "SiliconFlow领先",
                String.format("%.2f", Math.abs(speedImprovement)));
    }
}
