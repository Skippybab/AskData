package com.mt.agent.workflow.api.controller;

import com.mt.agent.workflow.api.dto.DataQuestionRequest;
import com.mt.agent.workflow.api.dto.DataQuestionResponse;
import com.mt.agent.workflow.api.service.ChatOrchestratorService;
import com.mt.agent.workflow.api.service.DbConfigService;
import com.mt.agent.workflow.api.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

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

    /**
     * 数据问答接口 - 阻塞式返回
     * 
     * @param requestBody 请求体包含：sessionId, question, dbConfigId, tableId
     * @return 返回结构化的查询结果
     */
    @PostMapping("/ask")
    public Result<DataQuestionResponse> askQuestion(@RequestBody Map<String, Object> requestBody,
                                                    HttpServletRequest request) {
        log.info("📊 [数据问答] 收到数据问答请求");
        log.debug("📊 [数据问答] 请求参数: {}", requestBody);
        
        Long userId = 1L; // 使用默认用户ID，后续可从token中获取
        
        try {
            // 解析请求参数
            Long sessionId = Long.valueOf(requestBody.get("sessionId").toString());
            String question = (String) requestBody.get("question");
            Object dbConfigIdObj = requestBody.get("dbConfigId");
            Long dbConfigId = dbConfigIdObj != null ? Long.valueOf(dbConfigIdObj.toString()) : null;
            
            // 处理tableId参数，可能是数字ID或表名字符串
            Object tableIdObj = requestBody.get("tableId");
            Long tableId = null;
            String tableName = null;
            
            if (tableIdObj != null) {
                String tableIdStr = tableIdObj.toString();
                try {
                    tableId = Long.valueOf(tableIdStr);
                    log.info("📊 [数据问答] 使用表ID: {}", tableId);
                } catch (NumberFormatException e) {
                    // 如果不是数字，说明传递的是表名
                    tableName = tableIdStr;
                    log.info("📊 [数据问答] 使用表名: {}", tableName);
                    // TODO: 根据表名查询表ID
                    // tableId = tableInfoService.getTableIdByName(dbConfigId, tableName);
                }
            }
            
            log.info("📊 [数据问答] 解析参数: sessionId={}, question={}, dbConfigId={}, tableId={}, tableName={}", 
                    sessionId, question, dbConfigId, tableId, tableName);
            
            // 参数验证
            if (question == null || question.trim().isEmpty()) {
                log.error("📊 [数据问答] 问题内容为空");
                return Result.error("问题内容不能为空");
            }
            
            if (dbConfigId == null) {
                log.error("📊 [数据问答] 数据库配置ID为空");
                return Result.error("请选择数据库");
            }
            
            // 调用编排服务处理数据问答
            log.info("📊 [数据问答] 开始处理数据问答");
            String responseJson = orchestratorService.processDataQuestionSync(sessionId, userId, question, dbConfigId, tableId);
            
            // 解析响应JSON
            DataQuestionResponse response = parseResponse(responseJson);
            
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
     * 健康检查接口
     */
    @GetMapping("/health")
    public Result<String> health() {
        log.info("📊 [数据问答] 健康检查");
        return Result.success("数据问答服务正常");
    }
    
    /**
     * 解析响应JSON字符串为DataQuestionResponse对象
     */
    private DataQuestionResponse parseResponse(String responseJson) {
        try {
            // 使用Jackson或其他JSON库解析
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(responseJson, DataQuestionResponse.class);
        } catch (Exception e) {
            log.error("解析响应JSON失败: {}", e.getMessage());
            DataQuestionResponse errorResponse = new DataQuestionResponse();
            errorResponse.setSuccess(false);
            errorResponse.setError("解析响应失败");
            return errorResponse;
        }
    }
}