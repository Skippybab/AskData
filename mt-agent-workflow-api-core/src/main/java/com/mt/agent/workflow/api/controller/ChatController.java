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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mt.agent.workflow.api.config.DifyConfig;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

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
     * 发送消息 - 阻塞式返回
     */
    @PostMapping(value = "/send")
    public String sendMessage(@RequestBody Map<String, Object> requestBody,
                                   HttpServletRequest request) {
        log.info("📨 [ChatController] 收到发送消息请求");
        log.debug("📨 [ChatController] 请求体: {}", requestBody);
        
        Long userId = 1L; // 使用默认用户ID
        log.info("📨 [ChatController] 用户ID: {}", userId);
        
        try {
            // 解析请求参数
            Long sessionId = Long.valueOf(requestBody.get("sessionId").toString());
            String content = (String) requestBody.get("question");
            Object dbConfigIdObj = requestBody.get("dbConfigId");
            Long dbConfigId = dbConfigIdObj != null ? Long.valueOf(dbConfigIdObj.toString()) : null;
            
            // 处理tableId参数，可能是数字ID或表名字符串
            Object tableIdObj = requestBody.get("tableId");
            Long tableId = null;
            if (tableIdObj != null) {
                String tableIdStr = tableIdObj.toString();
                // 尝试解析为Long，如果失败则保持为null（表示传递的是表名）
                try {
                    tableId = Long.valueOf(tableIdStr);
                } catch (NumberFormatException e) {
                    // 如果不是数字，说明传递的是表名，暂时设置为null
                    // 后续可以根据表名查询表ID
                    log.info("📨 [ChatController] tableId参数是表名: {}", tableIdStr);
                    tableId = null;
                }
            }
            
            log.info("📨 [ChatController] 解析参数: sessionId={}, content={}, dbConfigId={}, tableId={}", 
                    sessionId, content, dbConfigId, tableId);
            
            if (content == null || content.trim().isEmpty()) {
                log.error("📨 [ChatController] 消息内容为空");
                return "event: error\ndata: {\"error\":\"消息内容不能为空\"}\n\n";
            }
            
            // 移除权限校验，实现最小闭环
            // if (dbConfigId != null && !dbConfigService.checkAccess(userId, dbConfigId, "use")) {
            //     return "event: error\ndata: {\"error\":\"没有访问该数据库的权限\"}\n\n";
            // }
            
            // 同步处理消息
            if (dbConfigId != null && tableId != null) {
                // 数据问答流程
                log.info("📨 [ChatController] 开始数据问答流程");
                String result = orchestratorService.processDataQuestionSync(sessionId, userId, content, dbConfigId, tableId);
                log.info("📨 [ChatController] 数据问答流程完成, 响应长度: {}", result.length());
                log.debug("📨 [ChatController] 响应内容: {}", result);
                return result;
            } else {
                // 普通聊天（后续扩展）
                log.info("📨 [ChatController] 开始普通聊天流程");
                String result = chatService.sendMessageSync(sessionId, userId, content, null);
                log.info("📨 [ChatController] 普通聊天流程完成, 响应长度: {}", result.length());
                return result;
            }
            
        } catch (Exception e) {
            log.error("📨 [ChatController] 发送消息失败: {}", e.getMessage(), e);
            String errorMessage = "发送消息失败";
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                errorMessage = "请求处理超时，请稍后重试";
            } else if (e.getMessage() != null && e.getMessage().contains("interrupt")) {
                errorMessage = "请求处理被中断，请重试";
            }
            return "event: error\ndata: {\"error\":\"" + errorMessage + "\"}\n\n";
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
