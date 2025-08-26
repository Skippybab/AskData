package com.mt.agent.workflow.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mt.agent.workflow.api.entity.ChatMessage;
import com.mt.agent.workflow.api.entity.ChatSession;
import com.mt.agent.workflow.api.entity.UserToolConfig;
import com.mt.agent.workflow.api.service.ChatService;
import com.mt.agent.workflow.api.service.ChatOrchestratorService;
import com.mt.agent.workflow.api.service.DbConfigService;
import com.mt.agent.workflow.api.util.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mt.agent.workflow.api.config.DifyConfig;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {

    @Autowired
    private ChatService chatService;
    @Autowired
    private ChatOrchestratorService orchestratorService;
    @Autowired
    private DbConfigService dbConfigService;
    @Autowired
    private HttpServletRequest request;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DifyConfig difyConfig;
    
    @Autowired
    private WebClient webClient;

    /**
     * 测试Dify API连接
     */
    @GetMapping("/test-dify")
    public Result<Map<String, Object>> testDifyConnection() {
        try {
            log.info("开始测试Dify API连接");
            
            // 构建测试请求
            String url = difyConfig.getNl2sql().getBaseUrl() + "/workflows/run";
            String apiKey = difyConfig.getNl2sql().getApiKey();
            
            log.info("测试Dify workflow API URL: {}", url);
            log.info("测试Dify workflow API Key: {}", apiKey);
            
            Map<String, Object> testRequestBody = new HashMap<>();
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("all_table_names", "test_table_schema");
            inputs.put("input", "测试查询");
            inputs.put("history", "null");
            inputs.put("last_reply", "null");
            
            testRequestBody.put("user", "test_user");
            testRequestBody.put("inputs", inputs);
            testRequestBody.put("response_mode", "blocking");
            
            log.info("测试请求体: {}", objectMapper.writeValueAsString(testRequestBody));
            
            // 发送测试请求
            String response = webClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(testRequestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // 使用blocking模式获取响应
            
            log.info("Dify workflow API测试响应: {}", response);
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Dify workflow API连接测试成功");
            result.put("url", url);
            result.put("response", response);
            result.put("timestamp", System.currentTimeMillis());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("测试Dify workflow API连接失败: {}", e.getMessage(), e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", "Dify workflow API连接测试失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            result.put("timestamp", System.currentTimeMillis());
            
            return Result.error("测试Dify workflow API连接失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户聊天会话列表
     */
    @GetMapping("/sessions")
    public Result<IPage<ChatSession>> getSessions(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            HttpServletRequest request) {
        Long userId = 1L; // 使用默认用户ID
        try {
            Page<ChatSession> page = new Page<>(current, size);
            IPage<ChatSession> sessions = chatService.getUserSessions(userId, page);
            
            // 为每个会话添加额外信息
            if (sessions.getRecords() != null) {
                for (ChatSession session : sessions.getRecords()) {
                    // 获取会话的第一条消息作为预览
                    List<ChatMessage> messages = chatService.getSessionMessages(session.getId(), userId);
                    if (!messages.isEmpty()) {
                        ChatMessage firstMessage = messages.get(0);
                        if ("user".equals(firstMessage.getRole())) {
                            String preview = firstMessage.getContent();
                            if (preview.length() > 50) {
                                preview = preview.substring(0, 50) + "...";
                            }
                            session.setSessionName(session.getSessionName() + " - " + preview);
                        }
                    }
                }
            }
            
            return Result.success(sessions);
        } catch (Exception e) {
            log.error("获取会话列表失败: {}", e.getMessage(), e);
            return Result.error("获取会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 创建新的聊天会话
     */
    @PostMapping("/sessions")
    public Result<ChatSession> createSession(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        Long userId = 1L; // 使用默认用户ID
        try {
            String sessionName = (String) requestBody.get("sessionName");
            Object dbConfigIdObj = requestBody.get("dbConfigId");
            Long dbConfigId = dbConfigIdObj != null ? Long.valueOf(dbConfigIdObj.toString()) : null;
            Object tableIdObj = requestBody.get("tableId");
            Long tableId = tableIdObj != null ? Long.valueOf(tableIdObj.toString()) : null;
            
            if (sessionName == null || sessionName.trim().isEmpty()) {
                sessionName = "新对话";
            }
            
            ChatSession session = chatService.createSession(userId, sessionName, dbConfigId, tableId);
            return Result.success(session);
        } catch (Exception e) {
            log.error("创建会话失败: {}", e.getMessage(), e);
            return Result.error("创建会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话消息列表
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ChatMessage>> getMessages(
            @PathVariable Long sessionId,
            HttpServletRequest request) {
        Long userId = 1L; // 使用默认用户ID
        try {
            List<ChatMessage> messages = chatService.getSessionMessages(sessionId, userId);
            return Result.success(messages);
        } catch (Exception e) {
            log.error("获取消息列表失败: {}", e.getMessage(), e);
            return Result.error("获取消息列表失败: " + e.getMessage());
        }
    }



    /**
     * 获取用户可用工具
     */
    @GetMapping("/user-tools")
    public Result<List<UserToolConfig>> getUserTools(HttpServletRequest request) {
        Long userId = 1L; // 使用默认用户ID
        try {
            List<UserToolConfig> tools = chatService.getUserTools(userId);
            return Result.success(tools);
        } catch (Exception e) {
            log.error("获取用户工具失败: {}", e.getMessage(), e);
            return Result.error("获取用户工具失败: " + e.getMessage());
        }
    }
    
    /**
     * 重命名会话
     */
    @PutMapping("/sessions/{sessionId}/title")
    public Result<Boolean> renameSession(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {
        Long userId = 1L; // 使用默认用户ID
        try {
            String newTitle = requestBody.get("title");
            if (newTitle == null || newTitle.trim().isEmpty()) {
                return Result.error("会话标题不能为空");
            }
            
            boolean success = chatService.renameSession(userId, sessionId, newTitle);
            return success ? Result.success(true) : Result.error("重命名失败");
        } catch (Exception e) {
            log.error("重命名会话失败: {}", e.getMessage(), e);
            return Result.error("重命名失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Boolean> deleteSession(
            @PathVariable Long sessionId,
            HttpServletRequest request) {
        Long userId = 1L; // 使用默认用户ID
        try {
            boolean success = chatService.deleteSession(userId, sessionId);
            return success ? Result.success(true) : Result.error("删除失败");
        } catch (Exception e) {
            log.error("删除会话失败: {}", e.getMessage(), e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}
