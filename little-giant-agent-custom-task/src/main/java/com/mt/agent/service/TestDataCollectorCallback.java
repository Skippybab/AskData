package com.mt.agent.service;

import lombok.extern.slf4j.Slf4j;

/**
 * 测试数据收集回调接口
 * 提供静态方法供PythonDirectExecutorService调用
 */
@Slf4j
public class TestDataCollectorCallback {

    private static SQLCollector sqlCollector;
    private static VisualizationCollector visualizationCollector;

    /**
     * SQL收集器接口
     */
    public interface SQLCollector {
        void collectSQL(String sql);
    }

    /**
     * 可视化收集器接口
     */
    public interface VisualizationCollector {
        void collectVisualization(String functionName, Object result);
    }

    /**
     * 注册SQL收集器
     */
    public static void registerSQLCollector(SQLCollector collector) {
        sqlCollector = collector;
        log.info("🔧 [回调注册] SQL收集器已注册: {}", collector.getClass().getName());
    }

    /**
     * 注册可视化收集器
     */
    public static void registerVisualizationCollector(VisualizationCollector collector) {
        visualizationCollector = collector;
        log.info("🔧 [回调注册] 可视化收集器已注册: {}", collector.getClass().getName());
    }

    /**
     * 收集SQL
     */
    public static void collectSQL(String sql) {
        if (sqlCollector != null) {
            try {
                sqlCollector.collectSQL(sql);
                log.info("📊 [回调收集] SQL收集成功");
            } catch (Exception e) {
                log.error("🔥 [回调收集] SQL收集失败", e);
            }
        } else {
            log.debug("⚠️ [回调收集] SQL收集器未注册");
        }
    }

    /**
     * 收集可视化结果
     */
    public static void collectVisualization(String functionName, Object result) {
        if (visualizationCollector != null) {
            try {
                visualizationCollector.collectVisualization(functionName, result);
                log.info("📈 [回调收集] 可视化收集成功");
            } catch (Exception e) {
                log.error("🔥 [回调收集] 可视化收集失败", e);
            }
        } else {
            log.debug("⚠️ [回调收集] 可视化收集器未注册");
        }
    }

    /**
     * 清理注册的收集器
     */
    public static void clearCollectors() {
        sqlCollector = null;
        visualizationCollector = null;
        log.info("🧹 [回调清理] 收集器已清理");
    }
}