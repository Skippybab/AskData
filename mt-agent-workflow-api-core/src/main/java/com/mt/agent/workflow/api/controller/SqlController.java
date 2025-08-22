package com.mt.agent.workflow.api.controller;

import com.mt.agent.workflow.api.entity.SqlExecution;
import com.mt.agent.workflow.api.infra.ExternalDbExecutor;
import com.mt.agent.workflow.api.service.SqlExecutionService;
import com.mt.agent.workflow.api.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sql")
@CrossOrigin
public class SqlController {

    @Autowired
    private SqlExecutionService sqlExecutionService;

    public static class ExecRequest {
        public Long dbConfigId;
        public String sql;
    }

    @PostMapping("/execute")
    public Result<SqlExecution> execute(@RequestBody ExecRequest req) {
        try {
            SqlExecution exec = sqlExecutionService.executeReadOnly(req.dbConfigId, req.sql);
            return Result.success(exec);
        } catch (Exception e) {
            return Result.error("执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行SQL并返回结果
     */
    @PostMapping("/execute-with-result")
    public Result<SqlExecutionService.SqlExecutionResult> executeWithResult(@RequestBody ExecRequest req) {
        try {
            SqlExecutionService.SqlExecutionResult result = sqlExecutionService.executeWithResult(req.dbConfigId, req.sql);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取执行记录详情
     */
    @GetMapping("/executions/{id}")
    public Result<SqlExecution> getExecution(@PathVariable Long id) {
        try {
            // TODO: 实现通过mapper获取执行记录详情
            return Result.error("功能待实现");
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取执行结果
     */
    @GetMapping("/executions/{id}/result")
    public Result<ExternalDbExecutor.QueryResult> getExecutionResult(@PathVariable Long id) {
        try {
            ExternalDbExecutor.QueryResult result = sqlExecutionService.getExecutionResult(id);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取结果失败: " + e.getMessage());
        }
    }
}


