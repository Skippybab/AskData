package com.mt.agent.workflow.api.service;

import com.mt.agent.workflow.api.entity.SqlExecution;
import com.mt.agent.workflow.api.infra.ExternalDbExecutor;

public interface SqlExecutionService {
    
    /**
     * 执行只读SQL查询
     * @param dbConfigId 数据库配置ID
     * @param sql SQL语句
     * @return 执行记录
     */
    SqlExecution executeReadOnly(Long dbConfigId, String sql);
    
    /**
     * 执行SQL并返回结果
     * @param dbConfigId 数据库配置ID
     * @param sql SQL语句
     * @return 执行记录和结果
     */
    SqlExecutionResult executeWithResult(Long dbConfigId, String sql);
    
    /**
     * 获取执行结果
     * @param executionId 执行记录ID
     * @return 查询结果
     */
    ExternalDbExecutor.QueryResult getExecutionResult(Long executionId);
    
    /**
     * SQL执行结果封装类
     */
    class SqlExecutionResult {
        public SqlExecution execution;
        public ExternalDbExecutor.QueryResult queryResult;
        
        public SqlExecutionResult(SqlExecution execution, ExternalDbExecutor.QueryResult queryResult) {
            this.execution = execution;
            this.queryResult = queryResult;
        }
    }
}


