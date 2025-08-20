package com.mt.agent.workflow.api.controller;

import com.mt.agent.workflow.api.entity.ApiConfig;
import com.mt.agent.workflow.api.entity.ChatSession;
import com.mt.agent.workflow.api.service.ApiConfigService;
import com.mt.agent.workflow.api.service.ChatService;
import com.mt.agent.workflow.api.service.ChatOrchestratorService;
import com.mt.agent.workflow.api.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 开放API控制器
 * 提供对外的数据问答API接口
 */
@Slf4j
@RestController
@RequestMapping("/open-api/v1")
@CrossOrigin
public class OpenApiController {
    
    @Autowired
    private ApiConfigService apiConfigService;
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private ChatOrchestratorService orchestratorService;
    
    // 用于存储API调用频率限制
    private final Map<String, AtomicLong> rateLimitMap = new ConcurrentHashMap<>();
    private final Map<String, Long> rateLimitTimestamp = new ConcurrentHashMap<>();
    
    /**
     * 数据问答API
     * @param apiPath API路径
     * @param requestBody 请求体，包含问题和API密钥
     * @return 查询结果
     */
    @PostMapping("/query/{apiPath}")
    public Result<Map<String, Object>> queryData(
            @PathVariable String apiPath,
            @RequestBody Map<String, Object> requestBody) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 获取并验证API密钥
            String apiKey = (String) requestBody.get("apiKey");
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return Result.error(401, "缺少API密钥");
            }
            
            // 2. 验证API配置
            ApiConfig apiConfig = apiConfigService.getApiConfigByPath(apiPath);
            if (apiConfig == null) {
                return Result.error(404, "API不存在");
            }
            
            if (!apiConfig.getApiKey().equals(apiKey)) {
                return Result.error(401, "API密钥无效");
            }
            
            if (apiConfig.getStatus() != 1) {
                return Result.error(403, "API已禁用");
            }
            
            // 3. 检查频率限制
            if (!checkRateLimit(apiPath, apiConfig.getRateLimit())) {
                return Result.error(429, "请求过于频繁，请稍后再试");
            }
            
            // 4. 获取查询问题
            String question = (String) requestBody.get("question");
            if (question == null || question.trim().isEmpty()) {
                return Result.error(400, "缺少查询问题");
            }
            
            // 5. 创建临时会话
            String sessionName = "API调用-" + apiPath;
            ChatSession session = chatService.createSession(
                apiConfig.getUserId(), 
                sessionName, 
                apiConfig.getDbConfigId(), 
                apiConfig.getTableId()
            );
            
            log.info("OpenAPI调用 - API路径: {}, 问题: {}, 会话ID: {}", apiPath, question, session.getId());
            
            // 6. 执行数据问答
            String sseResponse = orchestratorService.processDataQuestionSync(
                session.getId(),
                apiConfig.getUserId(),
                question,
                apiConfig.getDbConfigId(),
                apiConfig.getTableId()
            );
            
            // 7. 解析SSE响应
            Map<String, Object> result = parseSSEResponse(sseResponse);
            
            // 8. 更新调用统计
            apiConfigService.updateCallStatistics(apiConfig.getId());
            
            // 9. 添加元数据
            result.put("apiPath", apiPath);
            result.put("sessionId", session.getId());
            result.put("executionTime", System.currentTimeMillis() - startTime);
            
            log.info("OpenAPI调用成功 - API路径: {}, 耗时: {}ms", apiPath, System.currentTimeMillis() - startTime);
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("OpenAPI调用失败 - API路径: {}, 错误: {}", apiPath, e.getMessage(), e);
            return Result.error(500, "查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取API信息
     */
    @GetMapping("/info/{apiPath}")
    public Result<Map<String, Object>> getApiInfo(@PathVariable String apiPath) {
        try {
            ApiConfig apiConfig = apiConfigService.getApiConfigByPath(apiPath);
            if (apiConfig == null) {
                return Result.error(404, "API不存在");
            }
            
            Map<String, Object> info = new HashMap<>();
            info.put("apiName", apiConfig.getApiName());
            info.put("description", apiConfig.getDescription());
            info.put("status", apiConfig.getStatus() == 1 ? "启用" : "禁用");
            info.put("rateLimit", apiConfig.getRateLimit() + "次/分钟");
            info.put("timeout", apiConfig.getTimeout() + "秒");
            
            return Result.success(info);
        } catch (Exception e) {
            log.error("获取API信息失败", e);
            return Result.error(500, "获取失败：" + e.getMessage());
        }
    }
    
    /**
     * 检查频率限制
     */
    private boolean checkRateLimit(String apiPath, Integer limit) {
        if (limit == null || limit <= 0) {
            return true; // 无限制
        }
        
        String key = "rate_" + apiPath;
        long currentTime = System.currentTimeMillis();
        
        // 获取或初始化计数器
        AtomicLong counter = rateLimitMap.computeIfAbsent(key, k -> new AtomicLong(0));
        Long timestamp = rateLimitTimestamp.get(key);
        
        // 如果是新的时间窗口，重置计数器
        if (timestamp == null || currentTime - timestamp > 60000) { // 60秒窗口
            counter.set(1);
            rateLimitTimestamp.put(key, currentTime);
            return true;
        }
        
        // 检查是否超过限制
        if (counter.incrementAndGet() > limit) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 解析SSE响应
     */
    private Map<String, Object> parseSSEResponse(String sseResponse) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        
        if (sseResponse == null || sseResponse.isEmpty()) {
            result.put("error", "空响应");
            return result;
        }
        
        try {
            String[] lines = sseResponse.split("\n");
            StringBuilder thinking = new StringBuilder();
            StringBuilder sqlResult = new StringBuilder();
            boolean hasError = false;
            String errorMessage = "";
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                
                if (line.startsWith("event:") && line.contains("llm_token")) {
                    // 找到下一行的数据
                    if (i + 1 < lines.length && lines[i + 1].startsWith("data:")) {
                        String dataLine = lines[i + 1].substring(5).trim();
                        try {
                            // 简单的JSON解析
                            if (dataLine.contains("\"type\":\"thinking\"")) {
                                String content = extractContent(dataLine);
                                if (content != null) {
                                    thinking.append(content);
                                }
                            } else if (dataLine.contains("\"type\":\"sql_result\"")) {
                                String content = extractContent(dataLine);
                                if (content != null) {
                                    sqlResult.append(content);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("解析数据行失败: {}", dataLine);
                        }
                    }
                } else if (line.startsWith("event:") && line.contains("error")) {
                    hasError = true;
                    if (i + 1 < lines.length && lines[i + 1].startsWith("data:")) {
                        String dataLine = lines[i + 1].substring(5).trim();
                        errorMessage = extractError(dataLine);
                    }
                } else if (line.startsWith("event:") && line.contains("done")) {
                    result.put("success", true);
                }
            }
            
            if (hasError) {
                result.put("success", false);
                result.put("error", errorMessage);
            } else {
                result.put("thinking", thinking.toString());
                result.put("data", sqlResult.toString());
                
                // 尝试解析结构化数据
                String sqlResultStr = sqlResult.toString();
                if (sqlResultStr.contains("{") && sqlResultStr.contains("\"dataType\":\"python_dict_list\"")) {
                    try {
                        // 提取JSON部分
                        int startIdx = sqlResultStr.indexOf("{");
                        int endIdx = sqlResultStr.lastIndexOf("}") + 1;
                        if (startIdx >= 0 && endIdx > startIdx) {
                            String jsonStr = sqlResultStr.substring(startIdx, endIdx);
                            result.put("structuredData", jsonStr);
                        }
                    } catch (Exception e) {
                        log.warn("解析结构化数据失败", e);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("解析SSE响应失败", e);
            result.put("error", "响应解析失败");
        }
        
        return result;
    }
    
    /**
     * 从JSON字符串中提取content字段
     */
    private String extractContent(String json) {
        try {
            int startIdx = json.indexOf("\"content\":\"");
            if (startIdx < 0) return null;
            
            startIdx += 11; // "content":"的长度
            int endIdx = json.indexOf("\"", startIdx);
            
            if (endIdx > startIdx) {
                String content = json.substring(startIdx, endIdx);
                // 处理转义字符
                content = content.replace("\\n", "\n")
                               .replace("\\r", "\r")
                               .replace("\\t", "\t")
                               .replace("\\\"", "\"");
                return content;
            }
        } catch (Exception e) {
            log.warn("提取content失败: {}", json);
        }
        return null;
    }
    
    /**
     * 从JSON字符串中提取error字段
     */
    private String extractError(String json) {
        try {
            int startIdx = json.indexOf("\"error\":\"");
            if (startIdx < 0) return "未知错误";
            
            startIdx += 9; // "error":"的长度
            int endIdx = json.indexOf("\"", startIdx);
            
            if (endIdx > startIdx) {
                return json.substring(startIdx, endIdx);
            }
        } catch (Exception e) {
            log.warn("提取error失败: {}", json);
        }
        return "未知错误";
    }
}