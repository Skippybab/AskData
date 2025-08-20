package com.mt.agent.controller;

import com.mt.agent.dify.DifyClient;
import com.mt.agent.dify.DifyClientFactory;
import com.mt.agent.dify.DifyConfigManager;
import com.mt.agent.dify.DifyEventHandler;
import com.mt.agent.dify.model.*;
import com.mt.agent.model.Result;
import com.mt.agent.reporter.SubEventReporter;
import com.mt.agent.repository.history.service.IHisConversationMessagesService;
import com.mt.agent.utils.DateUtil;
import com.mt.agent.utils.ReactiveStreamUtils;
import com.mt.agent.utils.SessionUtil;
import com.mt.agent.utils.SseSentEventUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import cn.hutool.json.JSONObject;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Dify智能体聊天控制器
 * 提供基于Dify平台的智能体对话功能
 *
 * @author wsx
 * @date 2025/6/30
 */
@RestController
@RequestMapping("/api/dify")
@Slf4j
@RequiredArgsConstructor
public class ChatDifyController {

    private final DifyClient difyClient;
    private final DifyClientFactory difyClientFactory;
    private final DifyConfigManager difyConfigManager;
    private final IHisConversationMessagesService hisConversationMessagesService;

    /**
     * Dify智能体普通聊天（阻塞式）
     *
     * @param message        用户消息
     * @param conversationId 会话ID（可选）
     * @param httpRequest    HTTP请求对象
     * @return 聊天结果
     */
    @PostMapping("/chat")
    public Result chat(@RequestParam String message,
            @RequestParam(required = false) String conversationId,
            HttpServletRequest httpRequest) {
        log.info("[ChatDifyController:chat] 开始Dify聊天, 消息: {}, 会话ID: {}", message, conversationId);

        try {
            // 获取用户ID
            Long longUserId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
            String userId = longUserId != null ? longUserId.toString() : "anonymous";

            // 构建聊天请求
            DifyChatRequest request = DifyChatRequest.builder()
                    .query(message)
                    .user(userId)
                    .conversationId(conversationId)
                    .responseMode("blocking")
                    .autoGenerateName(true)
                    .build();

            // 调用Dify API
            JSONObject response = difyClient.sendMessage(request);

            String responseId = DifyClient.safeGetStr(response, "id", "");
            log.info("[ChatDifyController:chat] Dify聊天完成, 响应ID: {}", responseId);
            return Result.success(response);

        } catch (Exception e) {
            log.error("[ChatDifyController:chat] Dify聊天失败", e);
            return Result.error("智能体聊天失败: " + e.getMessage());
        }
    }

    /**
     * Dify智能体普通聊天（阻塞式）- 支持指定工作流
     *
     * @param message        用户消息
     * @param workflowName   工作流名称（可选，默认使用default）
     * @param conversationId 会话ID（可选）
     * @param httpRequest    HTTP请求对象
     * @return 聊天结果
     */
    @PostMapping("/chat/workflow")
    public Result chatWithWorkflow(@RequestParam String message,
            @RequestParam(required = false) String workflowName,
            @RequestParam(required = false) String conversationId,
            HttpServletRequest httpRequest) {
        log.info("[ChatDifyController:chatWithWorkflow] 开始Dify工作流聊天, 消息: {}, 工作流: {}, 会话ID: {}",
                message, workflowName, conversationId);

        try {
            // 获取指定工作流的客户端
            DifyClient workflowClient = difyClientFactory.getClient(workflowName);

            // 检查工作流是否可用
            if (!workflowClient.isAvailable()) {
                return Result.error("指定的工作流不可用: " + workflowName);
            }

            // 获取用户ID
            Long longUserId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
            String userId = longUserId != null ? longUserId.toString() : "anonymous";

            // 构建聊天请求
            DifyChatRequest request = DifyChatRequest.builder()
                    .query(message)
                    .user(userId)
                    .conversationId(conversationId)
                    .responseMode("blocking")
                    .autoGenerateName(true)
                    .build();

            // 调用指定工作流的Dify API
            JSONObject response = workflowClient.sendMessage(request);

            String responseId = DifyClient.safeGetStr(response, "id", "");
            log.info("[ChatDifyController:chatWithWorkflow] Dify工作流聊天完成, 工作流: {}, 响应ID: {}",
                    workflowName, responseId);

            // 在响应中添加工作流信息
            response.put("workflowName", workflowName != null ? workflowName : "default");
            response.put("clientInfo", workflowClient.getConfigInfo());

            return Result.success(response);

        } catch (Exception e) {
            log.error("[ChatDifyController:chatWithWorkflow] Dify工作流聊天失败, 工作流: {}", workflowName, e);
            return Result.error("智能体工作流聊天失败: " + e.getMessage());
        }
    }

    /**
     * Dify智能体流式聊天
     *
     * @param message        用户消息
     * @param conversationId 会话ID（可选）
     * @param httpRequest    HTTP请求对象
     * @return 流式响应
     */
    @GetMapping(value = "/chat/stream")
    public Flux<ServerSentEvent<?>> chatStream(String message,
            String conversationId,
            HttpServletRequest httpRequest) {
        log.info("[ChatDifyController:chatStream] 开始Dify流式聊天, 消息: {}, 会话ID: {}", message, conversationId);
        String dateTime = DateUtil.formatCurrentDateTime();
        try {
            // 获取用户ID
            Long longUserId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
            String userId = longUserId != null ? longUserId.toString() : "anonymous";

            return ReactiveStreamUtils.executeServerSentEventOperation(
                    () -> Flux.create(sink -> {
                        // 创建事件流发送器
                        SubEventReporter reporter = new SubEventReporter(sink);

                        Schedulers.boundedElastic().schedule(() -> {
                            Map<String, String> result = null;
                            try {
                                // 构建聊天请求
                                String cId = conversationId;
                                if (cId != null && cId.equals("null")) {
                                    cId = null;
                                }
                                DifyChatRequest request = DifyChatRequest.builder()
                                        .query(message)
                                        .user(userId)
                                        .conversationId(cId)
                                        .responseMode("streaming")
                                        .autoGenerateName(true)
                                        .build();

                                log.info("[ChatDifyController:chatStream] 开始处理流式响应...");

                                // 调用Dify流式API
                                Flux<JSONObject> streamResponse = difyClient.sendMessageStream(request);

                                // 处理流式响应 - 这里会等待直到流式处理完成
                                result = DifyEventHandler.handleDifyStreamEventsAsync(streamResponse, reporter);

                                log.info("[ChatDifyController:chatStream] 流式响应处理完成，开始保存会话信息...");

                                // 检查返回结果
                                if (result == null || result.get("conversation_id") == null) {
                                    log.warn("[ChatDifyController:chatStream] 流式处理返回的结果为空或缺少conversation_id");
                                    reporter.reportError("流式处理未返回有效的会话信息");
                                    return;
                                }

                                // 传递会话信息给前端
                                String conversationIdResult = result.get("conversation_id");
                                String messageIdResult = result.get("message_id");
                                reporter.reportJson(
                                        "{\"conversationId\":\"" + conversationIdResult + "\",\"messageId\":\""
                                                + messageIdResult + "\"}");
                                // 保存用户消息历史
                                try {
                                    hisConversationMessagesService.saveChatHistory(
                                            conversationIdResult,
                                            null, // 用户消息没有messageId
                                            message,
                                            null, // 用户消息没有思考过程
                                            conversationId == null || conversationId.equals("null"), // 是否新会话
                                            longUserId,
                                            "user", // 对应前端的配置
                                            dateTime);
                                } catch (Exception e) {
                                    log.error("[ChatDifyController:chatStream] 保存用户消息历史失败", e);
                                }

                                // 保存系统消息历史
                                try {
                                    hisConversationMessagesService.saveChatHistory(
                                            conversationIdResult,
                                            messageIdResult,
                                            result.get("answer"),
                                            result.get("think"),
                                            false, // 系统消息永远不是新会话
                                            longUserId,
                                            "system", // 注意：对应前端配置
                                            DateUtil.formatCurrentDateTime());
                                    log.info("[ChatDifyController:chatStream] 系统消息历史保存成功");
                                } catch (Exception e) {
                                    log.error("[ChatDifyController:chatStream] 保存系统消息历史失败", e);
                                    // 不影响主流程，继续执行
                                }

                                log.info("[ChatDifyController:chatStream] 会话信息保存完成");

                            } catch (Exception e) {
                                log.error("[ChatDifyController:chatStream] 流式聊天处理失败", e);
                                reporter.reportError("智能体流式聊天失败: " + e.getMessage());
                            } finally {
                                // 确保在最后关闭流
                                reporter.reportCompleteAndClose();
                            }
                        });
                    }),
                    e -> {
                        log.error("[ChatDifyController:chatStream] 流式聊天异常:", e);
                        return Flux.just(SseSentEventUtils.createErrorEvent("智能体聊天异常: " + e.getMessage()),
                                SseSentEventUtils.createCompleteEvent());
                    },
                    signalType -> log.info("【Dify智能体】：流式聊天结束"));
        } catch (Exception e) {
            log.error("[ChatDifyController:chatStream] 流式聊天启动失败:", e);
            return Flux.just(SseSentEventUtils.createErrorEvent("智能体聊天启动失败: " + e.getMessage()),
                    SseSentEventUtils.createCompleteEvent());
        }
    }

    /**
     * 获取建议问题列表
     *
     * @param messageId   消息ID
     * @param httpRequest HTTP请求对象
     * @return 建议问题列表
     */
    @GetMapping("/suggested-questions")
    public Result getSuggestedQuestions(@RequestParam String messageId, HttpServletRequest httpRequest) {
        log.info("[ChatDifyController:getSuggestedQuestions] 获取建议问题, 消息ID: {}", messageId);

        try {
            // 获取用户ID
            Long longUserId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
            String userId = longUserId != null ? longUserId.toString() : "anonymous";

            // 调用Dify API
            JSONObject response = difyClient.getSuggestedQuestions(messageId, userId);

            // 获取建议问题数量用于日志
            Object data = response.get("data");
            int questionCount = 0;
            if (data instanceof java.util.List) {
                questionCount = ((java.util.List<?>) data).size();
            }

            log.info("[ChatDifyController:getSuggestedQuestions] 获取建议问题完成, 数量: {}", questionCount);
            return Result.success(response);

        } catch (Exception e) {
            log.error("[ChatDifyController:getSuggestedQuestions] 获取建议问题失败", e);
            return Result.error("获取建议问题失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话历史消息
     *
     * @param conversationId 会话ID
     * @param firstId        第一条消息ID（分页用）
     * @param limit          限制数量
     * @param httpRequest    HTTP请求对象
     * @return 历史消息列表
     */
    @GetMapping("/messages")
    public Result getMessages(@RequestParam String conversationId,
            @RequestParam(required = false) String firstId,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            HttpServletRequest httpRequest) {
        log.info("[ChatDifyController:getMessages] 获取历史消息, 会话ID: {}, 限制: {}", conversationId, limit);

        try {
            // 获取用户ID
            Long longUserId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
            String userId = longUserId != null ? longUserId.toString() : "anonymous";

            // 调用Dify API
            JSONObject response = difyClient.getMessages(conversationId, userId, firstId, limit);

            // 获取消息数量用于日志
            Object data = response.get("data");
            int messageCount = 0;
            if (data instanceof java.util.List) {
                messageCount = ((java.util.List<?>) data).size();
            }

            log.info("[ChatDifyController:getMessages] 获取历史消息完成, 数量: {}", messageCount);
            return Result.success(response);

        } catch (Exception e) {
            log.error("[ChatDifyController:getMessages] 获取历史消息失败", e);
            return Result.error("获取历史消息失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话列表
     *
     * @param lastId      最后一条会话ID（分页用）
     * @param limit       限制数量
     * @param pinned      是否置顶
     * @param httpRequest HTTP请求对象
     * @return 会话列表
     */
    @GetMapping("/conversations")
    public Result getConversations(@RequestParam(required = false) String lastId,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestParam(required = false) Boolean pinned,
            HttpServletRequest httpRequest) {
        log.info("[ChatDifyController:getConversations] 获取会话列表, 限制: {}, 置顶: {}", limit, pinned);

        try {
            // 获取用户ID
            Long longUserId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
            String userId = longUserId != null ? longUserId.toString() : "anonymous";

            // 调用Dify API
            JSONObject response = difyClient.getConversations(userId, lastId, limit, pinned);

            // 获取会话数量用于日志
            Object data = response.get("data");
            int conversationCount = 0;
            if (data instanceof java.util.List) {
                conversationCount = ((java.util.List<?>) data).size();
            }

            log.info("[ChatDifyController:getConversations] 获取会话列表完成, 数量: {}", conversationCount);
            return Result.success(response);

        } catch (Exception e) {
            log.error("[ChatDifyController:getConversations] 获取会话列表失败", e);
            return Result.error("获取会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 重命名会话
     *
     * @param conversationId 会话ID
     * @param name           新名称
     * @param autoGenerate   是否自动生成名称
     * @param httpRequest    HTTP请求对象
     * @return 操作结果
     */
    @PatchMapping("/conversations/{conversationId}/name")
    public Result renameConversation(@PathVariable String conversationId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false, defaultValue = "false") Boolean autoGenerate,
            HttpServletRequest httpRequest) {
        log.info("[ChatDifyController:renameConversation] 重命名会话, 会话ID: {}, 新名称: {}", conversationId, name);

        try {
            // 获取用户ID
            Long longUserId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
            String userId = longUserId != null ? longUserId.toString() : "anonymous";

            // 调用Dify API
            JSONObject result = difyClient.renameConversation(conversationId, name, userId, autoGenerate);

            log.info("[ChatDifyController:renameConversation] 重命名会话完成");
            return Result.success(result);

        } catch (Exception e) {
            log.error("[ChatDifyController:renameConversation] 重命名会话失败", e);
            return Result.error("重命名会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有可用的工作流配置信息
     *
     * @return 工作流配置信息
     */
    @GetMapping("/workflows")
    public Result getWorkflows() {
        log.info("[ChatDifyController:getWorkflows] 获取工作流配置信息");

        try {
            Map<String, Object> result = new HashMap<>();
            result.put("availableWorkflows", difyConfigManager.getAvailableWorkflowNames());
            result.put("defaultWorkflow", "default");
            result.put("configSummary", difyConfigManager.getConfigSummary());
            result.put("factoryCacheStatus", difyClientFactory.getCacheStatus());

            return Result.success(result);
        } catch (Exception e) {
            log.error("[ChatDifyController:getWorkflows] 获取工作流配置信息失败", e);
            return Result.error("获取工作流配置信息失败: " + e.getMessage());
        }
    }

    /**
     * 检查指定工作流的可用性
     *
     * @param workflowName 工作流名称
     * @return 工作流可用性信息
     */
    @GetMapping("/workflow/{workflowName}/status")
    public Result checkWorkflowStatus(@PathVariable String workflowName) {
        log.info("[ChatDifyController:checkWorkflowStatus] 检查工作流状态: {}", workflowName);

        try {
            Map<String, Object> result = new HashMap<>();
            result.put("workflowName", workflowName);
            result.put("exists", difyConfigManager.hasWorkflow(workflowName));
            result.put("available", difyClientFactory.isWorkflowAvailable(workflowName));
            result.put("clientInfo", difyClientFactory.getWorkflowClientInfo(workflowName));

            return Result.success(result);
        } catch (Exception e) {
            log.error("[ChatDifyController:checkWorkflowStatus] 检查工作流状态失败: {}", workflowName, e);
            return Result.error("检查工作流状态失败: " + e.getMessage());
        }
    }

    /**
     * 检查Dify客户端状态
     *
     * @return 客户端状态信息
     */
    @GetMapping("/status")
    public Result getStatus() {
        Map<String, Object> statusInfo = new HashMap<>();
        statusInfo.put("available", difyClient.isAvailable());
        statusInfo.put("config", difyClient.getConfigInfo());

        return Result.success(statusInfo);
    }

    @PostMapping("/rag")
    public Result ragExtractWord(@RequestParam String message,
            @RequestParam(required = false) String conversationId,
            HttpServletRequest httpRequest) {
        log.info("[ChatDifyController:ragExtractWord] 开始关键词提取, 消息: {}, 会话ID: {}", message, conversationId);

        try {
            // 获取关键词提取专用工作流客户端
            DifyClient keyWordExtractionClient = difyClientFactory.getClient("keyWordExtraction");

            // 检查工作流是否可用
            if (!keyWordExtractionClient.isAvailable()) {
                log.error("[ChatDifyController:ragExtractWord] 关键词提取工作流不可用");
                return Result.error("关键词提取工作流不可用，请检查配置");
            }

            // 获取用户ID
            Long longUserId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
            String userId = longUserId != null ? longUserId.toString() : "anonymous";

            // 构建灵活的参数Map
            Map<String, Object> params = new HashMap<>();

            Map<String, Object> inputs = new HashMap<>();
            params.put("inputs", inputs);
            params.put("user", "zzq");
            params.put("query", message);

            // 调用关键词提取工作流API（使用灵活参数方式）
            JSONObject response = keyWordExtractionClient.sendMessage(params);

            String text = DifyClient.safeGetStr(response, "answer", "");

            log.info("[ChatDifyController:ragExtractWord] 关键词提取完成,提取内容: {}", text);

            // 在响应中添加工作流信息和处理标识
//            response.put("workflowName", "keyWordExtraction");
//            response.put("clientInfo", keyWordExtractionClient.getConfigInfo());
//            response.put("extractionType", "keyWordExtraction");
//            response.put("processTime", System.currentTimeMillis());

            return Result.success(text);

        } catch (Exception e) {
            log.error("[ChatDifyController:ragExtractWord] 关键词提取失败", e);
            return Result.error("关键词提取失败: " + e.getMessage());
        }
    }
}
