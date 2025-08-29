package com.mt.agent.workflow.api.controller;

import com.mt.agent.workflow.api.dto.DataQuestionResponse;
import com.mt.agent.workflow.api.service.ChatOrchestratorService;
import com.mt.agent.workflow.api.service.DbConfigService;
import com.mt.agent.workflow.api.util.Result;
import com.mt.agent.workflow.api.util.BufferUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import com.mt.agent.workflow.api.entity.DbConfig;
import com.mt.agent.workflow.api.entity.TableInfo;
import com.mt.agent.workflow.api.bottomReply.service.BottomReplyService;

/**
 * 数据问答控制器
 * 提供统一的数据问答接口，支持自然语言查询数据库
 */
@Slf4j
@RestController
@RequestMapping("/api/data-question")
@CrossOrigin
public class DataQuestionController {

    @Autowired
    private ChatOrchestratorService orchestratorService;
    
    @Autowired
    private DbConfigService dbConfigService;

    @Autowired
    private SchemaController schemaController;
    
    @Autowired
    private BufferUtil bufferUtil;
    
    @Autowired
    private BottomReplyService bottomReplyService;

    /**
     * 数据问答接口 - 阻塞式返回
     * 
     * @param requestBody 请求体包含：sessionId, question, dbConfigId, tableId
     * @return 返回结构化的查询结果
     */
    @PostMapping("/ask")
    public Result<DataQuestionResponse> askQuestion(@RequestBody Map<String, Object> requestBody,
                                                    HttpServletRequest request) {
        Long userId = 1L; // 使用默认用户ID
        bufferUtil.clearOutputResultCache(userId.toString());
        try {
            // 解析请求参数
            Long sessionId = Long.valueOf(requestBody.get("sessionId").toString());
            String question = (String) requestBody.get("question");
            Object dbConfigIdObj = requestBody.get("dbConfigId");
            Long dbConfigId = dbConfigIdObj != null ? Long.valueOf(dbConfigIdObj.toString()) : null;

            
            // 处理tableIds参数，支持数组或单个ID
            Object tableIdsObj = requestBody.get("tableIds");
            List<Long> tableIds = null;
            Long tableId = null; // 保持向后兼容的单个tableId
            String tableName = null;
            
            if (tableIdsObj != null) {
                if (tableIdsObj instanceof List) {
                    // 处理tableIds数组
                    tableIds = ((List<?>) tableIdsObj).stream()
                        .map(id -> Long.valueOf(id.toString()))
                        .collect(java.util.stream.Collectors.toList());
//                    log.info("📊 [数据问答] 使用表ID数组: {}", tableIds);
                    
                    // 为了兼容现有的方法签名，使用第一个表ID作为主表ID
                    if (!tableIds.isEmpty()) {
                        tableId = tableIds.get(0);
                    }
                } else {
                    // 兼容旧的单个tableId参数
                    String tableIdStr = tableIdsObj.toString();
                    try {
                        tableId = Long.valueOf(tableIdStr);
                        tableIds = java.util.Arrays.asList(tableId);
                        log.info("📊 [数据问答] 使用单个表ID: {}", tableId);
                    } catch (NumberFormatException e) {
                        // 如果不是数字，说明传递的是表名
                        tableName = tableIdStr;
                        log.info("📊 [数据问答] 使用表名: {}", tableName);
                    }
                }
            }
            
            // 兼容处理单个tableId参数（如果没有tableIds参数）
            if (tableIds == null && requestBody.get("tableId") != null) {
                Object tableIdObj = requestBody.get("tableId");
                String tableIdStr = tableIdObj.toString();
                try {
                    tableId = Long.valueOf(tableIdStr);
                    tableIds = java.util.Arrays.asList(tableId);
                    log.info("📊 [数据问答] 兼容模式使用表ID: {}", tableId);
                } catch (NumberFormatException e) {
                    tableName = tableIdStr;
                    log.info("📊 [数据问答] 兼容模式使用表名: {}", tableName);
                }
            }
            
//            log.info("📊 [数据问答] 解析参数: sessionId={}, question={}, dbConfigId={}, tableIds={}, tableId={}, tableName={}",
//                    sessionId, question, dbConfigId, tableIds, tableId, tableName);
            
            // 参数验证
            if (question == null || question.trim().isEmpty()) {
                log.error("📊 [数据问答] 问题内容为空");
                return Result.error("问题内容不能为空");
            }
            
            if (dbConfigId == null) {
                return Result.error("请选择数据库");
            }
            
            // 将dbConfigId和tableIds存入缓存，供后续Python执行时使用
            String userIdStr = userId.toString();
            bufferUtil.setFieldPermanent(userIdStr, "dbConfigId", dbConfigId.toString());
//            log.info("📊 [数据问答] 已将dbConfigId={}存入缓存，用户ID={}", dbConfigId, userIdStr);
            
            // 将选中的表ID列表存入缓存
            if (tableIds != null && !tableIds.isEmpty()) {
                String tableIdsJson = tableIds.stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(","));
                bufferUtil.setField(userIdStr, "current_table_ids", tableIdsJson, 24, java.util.concurrent.TimeUnit.HOURS);
//                log.info("📊 [数据问答] 已将tableIds={}存入缓存，用户ID={}", tableIdsJson, userIdStr);
            }
            
            // 调用编排服务处理数据问答
            DataQuestionResponse response = orchestratorService.processDataQuestionSync(sessionId, userId, question, dbConfigId, tableId, tableName);
            
            // 调用兜底回复服务
            try {
                // 准备兜底回复的参数
                String dialogHistory = ""; // 暂时为空，后续可以从数据库获取
                String executions = response.getResult() != null ? response.getResult() : "";
                String taskName = "智能数据问答";
                String bottomReplyResult = bottomReplyService.replyForExecution(question, dialogHistory, executions, taskName, userId.toString(), null);
                log.info("📊 [数据问答] 兜底回复处理成功: {}", bottomReplyResult);

                // 将兜底回复设置为主要的显示内容
                if (bottomReplyResult != null && !bottomReplyResult.trim().isEmpty()) {
                    response.setBottomReply(bottomReplyResult);
                    // 将兜底回复内容设置为主要的result，这样前端会显示兜底回复而不是原始查询结果
                    response.setResult(bottomReplyResult);
                    response.setResultType("text");
                }

            } catch (Exception e) {
                log.error("📊 [数据问答] 兜底回复处理失败: {}", e.getMessage());
            }
            
            // 返回处理结果
            if (response.isSuccess()) {
                log.info("📊 [数据问答] 数据问答处理成功");
                return Result.success(response);
            } else {
                log.error("📊 [数据问答] 数据问答处理失败: {}", response.getError());
                return Result.error(response.getError());
            }
            
        } catch (Exception e) {
            log.error("📊 [数据问答] 处理数据问答请求失败: {}", e.getMessage(), e);
            
            String errorMessage = "处理数据问答失败";
            if (e.getMessage() != null) {
                if (e.getMessage().contains("timeout")) {
                    errorMessage = "请求处理超时，请稍后重试";
                } else if (e.getMessage().contains("interrupt")) {
                    errorMessage = "请求处理被中断，请重试";
                }
            }
            
            return Result.error(errorMessage);
        }
    }

    /**
     * 调试接口：检查数据库和表的状态
     */
    @GetMapping("/debug/db-status/{dbConfigId}")
    public Result<Map<String, Object>> debugDbStatus(@PathVariable Long dbConfigId) {
        try {
            Map<String, Object> status = new HashMap<>();
            
            // 检查数据库配置
            DbConfig dbConfig = dbConfigService.getById(1L, dbConfigId);
            status.put("dbConfig", dbConfig != null ? Map.of(
                "id", dbConfig.getId(),
                "name", dbConfig.getName(),
                "dbType", dbConfig.getDbType(),
                "host", dbConfig.getHost(),
                "databaseName", dbConfig.getDatabaseName(),
                "status", dbConfig.getStatus()
            ) : null);
            
            // 检查表信息
            List<TableInfo> allTables = schemaController.listTables(dbConfigId).getData();
            status.put("allTablesCount", allTables != null ? allTables.size() : 0);
            
            List<TableInfo> enabledTables = schemaController.listEnabledTables(dbConfigId).getData();
            status.put("enabledTablesCount", enabledTables != null ? enabledTables.size() : 0);
            
            // 详细的表信息
            if (allTables != null) {
                List<Map<String, Object>> tableDetails = allTables.stream()
                    .map(table -> {
                        Map<String, Object> details = new HashMap<>();
                        details.put("id", table.getId());
                        details.put("name", table.getTableName());
                        details.put("enabled", table.getEnabled());
                        details.put("hasDdl", table.getTableDdl() != null && !table.getTableDdl().isEmpty());
                        details.put("ddlLength", table.getTableDdl() != null ? table.getTableDdl().length() : 0);
                        return details;
                    })
                    .collect(Collectors.toList());
                status.put("tableDetails", tableDetails);
            }
            
            return Result.success(status);
        } catch (Exception e) {
            log.error("调试数据库状态失败: {}", e.getMessage(), e);
            return Result.error("调试失败: " + e.getMessage());
        }
    }
}