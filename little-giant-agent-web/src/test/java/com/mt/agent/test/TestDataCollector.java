package com.mt.agent.test;

import com.mt.agent.service.TestDataCollectorCallback;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 测试数据收集器
 * 用于在真实执行过程中收集SQL生成和可视化结果
 */
@Slf4j
@Component
public class TestDataCollector
        implements TestDataCollectorCallback.SQLCollector, TestDataCollectorCallback.VisualizationCollector {

    /**
     * 测试数据结构 - 直接作为实例变量，避免ThreadLocal问题
     */
    @Data
    public static class TestData {
        private List<String> sqlStatements = new ArrayList<>();
        private List<String> visualizationResults = new ArrayList<>();
        private boolean testMode = false;
    }

    private TestData testData = new TestData();

    /**
     * 开启测试模式
     */
    public void enableTestMode() {
        testData = new TestData();
        testData.setTestMode(true);

        // 注册回调
        TestDataCollectorCallback.registerSQLCollector(this);
        TestDataCollectorCallback.registerVisualizationCollector(this);

        log.info("🧪 测试数据收集器已启用");
    }

    /**
     * 关闭测试模式
     */
    public void disableTestMode() {
        if (testData != null) {
            testData.setTestMode(false);
        }

        // 注意：不要立即清理回调注册，因为可能还有其他地方需要获取数据
        // 回调注册会在新的测试开始时被覆盖，或者在测试完全结束时清理
        // TestDataCollectorCallback.clearCollectors();

        log.info("🧪 测试数据收集器已关闭");
    }

    /**
     * 完全清理测试数据和回调注册
     */
    public void clearAll() {
        if (testData != null) {
            testData.setTestMode(false);
        }

        // 清理回调注册
        TestDataCollectorCallback.clearCollectors();

        log.info("🧹 测试数据收集器已完全清理");
    }

    /**
     * 检查是否处于测试模式
     */
    public boolean isTestMode() {
        return testData != null && testData.isTestMode();
    }

    /**
     * 收集SQL语句 - 实现回调接口
     */
    @Override
    public void collectSQL(String sql) {
        if (testData != null && sql != null && !sql.trim().isEmpty()) {
            testData.getSqlStatements().add(sql.trim());
            log.info("📊 收集SQL: {}", sql.substring(0, Math.min(sql.length(), 100)) + "...");
        }
    }

    /**
     * 收集可视化结果 - 实现回调接口
     */
    @Override
    public void collectVisualization(String functionName, Object result) {
        if (testData != null && result != null) {
            String resultStr = formatVisualizationResult(functionName, result);
            testData.getVisualizationResults().add(resultStr);
            log.info("📈 收集可视化: {}", resultStr);
        }
    }

    /**
     * 格式化可视化结果
     */
    private String formatVisualizationResult(String functionName, Object result) {
        if (result instanceof Map) {
            Map<String, Object> resultMap = (Map<String, Object>) result;

            // 根据type字段确定可视化类型
            String type = (String) resultMap.get("type");
            String label = (String) resultMap.get("label");
            Object value = resultMap.get("value");
            String unit = (String) resultMap.get("unit");

            if ("IndicatorBlock".equals(type)) {
                return String.format("{type=IndicatorBlock, label=%s, value=%s, unit=%s}",
                        label, value, unit != null ? unit : "");
            } else if ("BarChart".equals(type)) {
                return String.format("{type=BarChart, title=%s}",
                        resultMap.get("title"));
            } else if ("PieChart".equals(type)) {
                return String.format("{type=PieChart, title=%s}",
                        resultMap.get("title"));
            } else if ("MDText".equals(type)) {
                return String.format("{type=MDText, content=%s}",
                        resultMap.get("content"));
            } else if ("Table".equals(type)) {
                return String.format("{type=Table, title=%s}",
                        resultMap.get("title"));
            }
        }

        // 默认格式
        return String.format("{function=%s, result=%s}",
                functionName, result.toString());
    }

    /**
     * 获取收集的SQL语句
     */
    public List<String> getCollectedSQL() {
        return testData != null ? new ArrayList<>(testData.getSqlStatements()) : new ArrayList<>();
    }

    /**
     * 获取收集的可视化结果
     */
    public List<String> getCollectedVisualizations() {
        return testData != null ? new ArrayList<>(testData.getVisualizationResults()) : new ArrayList<>();
    }

    /**
     * 清空收集的数据
     */
    public void clearCollectedData() {
        if (testData != null) {
            testData.getSqlStatements().clear();
            testData.getVisualizationResults().clear();
            log.info("🧹 清空测试数据收集器");
        }
    }

    /**
     * 获取收集数据的统计信息
     */
    public String getCollectionStats() {
        if (testData != null) {
            return String.format("SQL数量: %d, 可视化数量: %d",
                    testData.getSqlStatements().size(),
                    testData.getVisualizationResults().size());
        }
        return "SQL数量: 0, 可视化数量: 0";
    }
}