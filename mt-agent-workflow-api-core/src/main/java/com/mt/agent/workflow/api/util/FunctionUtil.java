package com.mt.agent.workflow.api.util;

import com.mt.agent.workflow.api.service.AISQLQueryService;
import com.mt.agent.workflow.api.service.SqlExecutionService;
import com.mt.agent.workflow.api.service.impl.SubEventReporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 功能工具类
 * 统一封装各种工具函数调用
 */
@Slf4j
@Component
public class FunctionUtil {
    
    @Autowired(required = false)
    private AISQLQueryService aiSQLQueryService;
    
    @Autowired(required = false)
    private SqlExecutionService sqlExecutionService;
    
    @Autowired
    private BufferUtil bufferUtil;
    
    /**
     * 生成SQL语句
     */
    public String genSQL(String queryText, String tableName, String userId) {
        // 从缓冲区获取上下文信息
        String pythonCode = bufferUtil.getPythonCode(userId);
        String historyStr = bufferUtil.getField(userId, "historyStr");
        String question = bufferUtil.getField(userId, "question");
        String tables = bufferUtil.getField(userId, "tables");
        
        // 调用AI服务生成SQL
        return aiSQLQueryService.generateSQL(queryText, tableName, pythonCode, historyStr, question, tables);
    }
    
    /**
     * 执行SQL语句
     */
    public List<Map<String, Object>> executeSQL(String sql, String userId) {
        log.info("🔧 [FunctionUtil] 开始执行SQL: {}, userId: {}", sql, userId);
        
        // 简单的SQL验证
        if (sql == null || sql.trim().isEmpty()) {
            log.error("🔧 [FunctionUtil] SQL语句为空");
            throw new IllegalArgumentException("SQL语句不能为空");
        }
        
        String lowerSql = sql.toLowerCase().trim();
        if (!lowerSql.startsWith("select")) {
            log.error("🔧 [FunctionUtil] 只支持SELECT查询语句: {}", sql);
            throw new IllegalArgumentException("只支持SELECT查询语句");
        }
        
        // 防止SQL注入
        if (lowerSql.contains("drop") || 
            lowerSql.contains("delete") || 
            lowerSql.contains("update") || 
            lowerSql.contains("insert")) {
            log.error("🔧 [FunctionUtil] 不允许执行修改数据的SQL语句: {}", sql);
            throw new IllegalArgumentException("不允许执行修改数据的SQL语句");
        }
        
        // 从BufferUtil获取数据库配置ID
        log.info("🔧 [FunctionUtil] 从BufferUtil获取数据库配置ID, userId: {}", userId);
        Long dbConfigId = getDbConfigIdFromBuffer(userId);
        if (dbConfigId == null) {
            log.error("🔧 [FunctionUtil] 无法获取数据库配置ID, userId: {}", userId);
            throw new IllegalArgumentException("无法获取数据库配置ID，请确保已正确设置数据库配置");
        }
        log.info("🔧 [FunctionUtil] 获取到数据库配置ID: {}", dbConfigId);
        
        // 执行SQL并返回结果
        try {
            log.info("🔧 [FunctionUtil] 调用sqlExecutionService执行SQL");
            var result = sqlExecutionService.executeWithResult(dbConfigId, sql);
            log.info("🔧 [FunctionUtil] SQL执行成功, 返回行数: {}", result.queryResult.rows.size());
            log.debug("🔧 [FunctionUtil] SQL执行结果: {}", result.queryResult.rows);
            return result.queryResult.rows;
        } catch (Exception e) {
            log.error("🔧 [FunctionUtil] SQL执行失败: {}", e.getMessage(), e);
            throw new RuntimeException("SQL执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 从BufferUtil获取数据库配置ID
     */
    private Long getDbConfigIdFromBuffer(String userId) {
        log.info("🔧 [FunctionUtil] 从BufferUtil获取dbConfigId, userId: {}", userId);
        try {
            Object dbConfigIdObj = bufferUtil.getField(userId, "dbConfigId");
            log.info("🔧 [FunctionUtil] 从BufferUtil获取到dbConfigId对象: {}, 类型: {}", 
                    dbConfigIdObj, dbConfigIdObj != null ? dbConfigIdObj.getClass().getSimpleName() : "null");
            
            if (dbConfigIdObj instanceof Long) {
                Long result = (Long) dbConfigIdObj;
                log.info("🔧 [FunctionUtil] 返回Long类型dbConfigId: {}", result);
                return result;
            } else if (dbConfigIdObj instanceof String) {
                Long result = Long.valueOf((String) dbConfigIdObj);
                log.info("🔧 [FunctionUtil] 转换String到Long类型dbConfigId: {}", result);
                return result;
            } else if (dbConfigIdObj instanceof Integer) {
                Long result = ((Integer) dbConfigIdObj).longValue();
                log.info("🔧 [FunctionUtil] 转换Integer到Long类型dbConfigId: {}", result);
                return result;
            } else {
                log.warn("🔧 [FunctionUtil] dbConfigId对象类型不支持: {}", 
                        dbConfigIdObj != null ? dbConfigIdObj.getClass().getSimpleName() : "null");
            }
        } catch (Exception e) {
            log.warn("🔧 [FunctionUtil] 从BufferUtil获取dbConfigId失败: {}", e.getMessage());
        }
        log.error("🔧 [FunctionUtil] 无法获取有效的dbConfigId");
        return null;
    }
    
    /**
     * 获取当前用户ID（从线程上下文或默认值）
     */
    private String getCurrentUserId() {
        // 这里可以从ThreadLocal或其他上下文获取当前用户ID
        // 暂时返回默认值，实际项目中应该从安全上下文获取
        return "1";
    }
    
    /**
     * 步骤总结
     */
    public String stepSummary(String summaryTitle) {
        // TODO: 实现步骤总结逻辑
        return "执行步骤总结: " + summaryTitle;
    }
    
    /**
     * 可视化文本框
     */
    public void visTextBox(String text, SubEventReporter reporter) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "textbox");
        result.put("content", text);
        reporter.reportNodeResult(result);
    }
    
    /**
     * 可视化文本块
     */
    public void visTextBlock(String nameField, Double valueField, SubEventReporter reporter) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "textblock");
        result.put("name", nameField);
        result.put("value", valueField);
        reporter.reportNodeResult(result);
    }
    
    /**
     * 可视化单柱状图
     */
    public void visSingleBar(String title, List<String> xData, List<Double> yData, SubEventReporter reporter) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "bar_chart");
        result.put("title", title);
        result.put("xData", xData);
        result.put("yData", yData);
        reporter.reportNodeResult(result);
    }
    
    /**
     * 可视化双柱状图
     */
    public void visClusteredBar(String title, List<String> xData, String barALabel,
                                String barBLabel, List<Double> barAYData, List<Double> barBYData,
                                SubEventReporter reporter) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "clustered_bar");
        result.put("title", title);
        result.put("xData", xData);
        result.put("barALabel", barALabel);
        result.put("barBLabel", barBLabel);
        result.put("barAData", barAYData);
        result.put("barBData", barBYData);
        reporter.reportNodeResult(result);
    }
    
    /**
     * 可视化饼图
     */
    public void visPieChart(String title, List<String> pieTags, List<Double> pieData, SubEventReporter reporter) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "pie_chart");
        result.put("title", title);
        result.put("labels", pieTags);
        result.put("data", pieData);
        reporter.reportNodeResult(result);
    }
    
    /**
     * 可视化表格
     */
    public void visTable(String title, List<Map<String, Object>> data, SubEventReporter reporter) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "table");
        result.put("title", title);
        result.put("data", data);
        reporter.reportNodeResult(result);
    }
}
