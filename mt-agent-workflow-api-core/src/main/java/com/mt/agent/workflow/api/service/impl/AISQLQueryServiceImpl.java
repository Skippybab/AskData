package com.mt.agent.workflow.api.service.impl;

import com.mt.agent.workflow.api.service.AISQLQueryService;
import com.mt.agent.workflow.api.service.SchemaContextService;
import com.mt.agent.workflow.api.util.DifyWorkflowCaller;
import com.mt.agent.workflow.api.util.PromptTemplates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * AI SQL查询服务实现
 */
@Slf4j
@Service
public class AISQLQueryServiceImpl implements AISQLQueryService {
    
    @Autowired
    private SchemaContextService schemaContextService;
    
    @Autowired
    private DifyWorkflowCaller difyWorkflowCaller;
    
    @Value("${dify.nl2sql.base-url:http://113.45.193.155:8888/v1}")
    private String nl2sqlBaseUrl;
    
    @Value("${dify.nl2sql.api-key:app-E96pDaNHzay95rKx49Vppnwy}")
    private String nl2sqlApiKey;
    
    @Override
    public String generateSQL(String queryText, String tableName, String pythonCode, 
                            String historyStr, String question, String tables) {
        try {
            log.info("开始生成SQL，queryText: {}, tableName: {}", queryText, tableName);
            
            // 如果没有提供表结构信息，从数据库获取
            if (tables == null || tables.isEmpty()) {
                // TODO: 从SchemaContextService获取表结构
                tables = getDefaultTableSchema();
            }
            
            // 构建提示词
            String fullPrompt = buildPrompt(queryText, tableName, pythonCode, historyStr, question, tables);
            
            // 调用Dify生成SQL
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("prompt", fullPrompt);
            inputs.put("query_text", queryText);
            inputs.put("table_name", tableName);
            
            String userId = "sql_gen_user_" + System.currentTimeMillis();
            DifyWorkflowCaller.DifyWorkflowResponse response = difyWorkflowCaller.executeWorkflow(
                nl2sqlBaseUrl, nl2sqlApiKey, inputs, userId
            );
            
            if (!response.isSuccess()) {
                log.error("Dify SQL生成失败: {}", response.getErrorMessage());
                throw new RuntimeException("SQL生成失败: " + response.getErrorMessage());
            }
            
            String sql = response.getOutput("sql");
            if (sql == null || sql.trim().isEmpty()) {
                sql = response.getText();
            }
            
            // 清理SQL语句
            sql = cleanSQL(sql);
            
            // 验证SQL语句
            validateSQL(sql);
            
            log.info("SQL生成成功: {}", sql);
            return sql;
            
        } catch (Exception e) {
            log.error("生成SQL失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成SQL失败: " + e.getMessage());
        }
    }
    
    @Override
    public String generateSimpleSQL(String queryText, String tableName) {
        return generateSQL(queryText, tableName, null, null, queryText, null);
    }
    
    /**
     * 构建提示词
     */
    private String buildPrompt(String queryText, String tableName, String pythonCode, 
                              String historyStr, String question, String tables) {
        // 使用模板构建提示词
        String template = PromptTemplates.SQL_GENERATION_TEMPLATE;
        
        template = template.replace("{{query_text}}", queryText != null ? queryText : "");
        template = template.replace("{{table_name}}", tableName != null ? tableName : "");
        template = template.replace("{{py_codes}}", pythonCode != null ? pythonCode : "");
        template = template.replace("{{diag_history}}", historyStr != null ? historyStr : "");
        template = template.replace("{{question}}", question != null ? question : "");
        template = template.replace("{{tableSchema}}", tables != null ? tables : getDefaultTableSchema());
        
        return template;
    }
    
    /**
     * 获取默认表结构
     */
    private String getDefaultTableSchema() {
        // TODO: 从数据库动态获取表结构
        return """
            表名: data_table
            字段:
            - id: BIGINT, 主键
            - name: VARCHAR(100), 名称
            - value: DECIMAL(10,2), 数值
            - category: VARCHAR(50), 分类
            - created_time: DATETIME, 创建时间
            - status: INT, 状态(1-正常,0-禁用)
            """;
    }
    
    /**
     * 清理SQL语句
     */
    private String cleanSQL(String sql) {
        if (sql == null) return "";
        
        // 移除markdown代码块标记
        sql = sql.replace("```sql", "")
                .replace("```SQL", "")
                .replace("```", "")
                .trim();
        
        // 移除多余的空白字符
        sql = sql.replaceAll("\\s+", " ").trim();
        
        return sql;
    }
    
    /**
     * 验证SQL语句
     */
    private void validateSQL(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new RuntimeException("生成的SQL语句为空");
        }
        
        String lowerSql = sql.toLowerCase();
        
        // 只允许SELECT语句
        if (!lowerSql.startsWith("select")) {
            throw new RuntimeException("只支持SELECT查询语句");
        }
        
        // 防止SQL注入和危险操作
        String[] dangerousKeywords = {"drop", "delete", "update", "insert", "alter", "create", "truncate"};
        for (String keyword : dangerousKeywords) {
            if (lowerSql.contains(keyword)) {
                throw new RuntimeException("不允许执行包含 " + keyword + " 的SQL语句");
            }
        }
    }
}
