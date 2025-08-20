package com.mt.agent.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.coze.openapi.client.chat.model.ChatEvent;
import com.coze.openapi.client.workflows.run.model.WorkflowEvent;
import com.mt.agent.bottomReply.service.BottomReplyService;
import com.mt.agent.buffer.util.BufferUtil;
import com.mt.agent.consensus.util.ConsensusUtil;
import com.mt.agent.coze.ChatEventHandler;
import com.mt.agent.coze.CozeClient;
import com.mt.agent.coze.WorkflowEventHandler;
import com.mt.agent.model.ChatMessage;
import com.mt.agent.model.Result;
import com.mt.agent.model.SaveChatHistoryRequest;
import com.mt.agent.model.exception.DataAccessException;
import com.mt.agent.model.exception.ExecutionFailureException;
import com.mt.agent.reporter.SubEventReporter;
import com.mt.agent.router.service.RouterService;
import com.mt.agent.service.AIChatService;
import com.mt.agent.utils.ReactiveStreamUtils;
import com.mt.agent.utils.SessionUtil;
import com.mt.agent.utils.SseSentEventUtils;
import io.reactivex.Flowable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final AIChatService aiChatService;
    private final ConsensusUtil consensusUtil;
    private final RouterService routerService;
    private final BottomReplyService bottomReplyService;
    private final BufferUtil bufferUtil;

    private final CozeClient cozeClient;

    private static final String CHAT_WORKFLOW_ID = "7529791404764839982";
    private static final String DELETE_WORKFLOW_ID = "7529791404764938286";
    private static final String SAVE_WORKFLOW_ID = "7529791404764905518";

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<?>> chat(String message, HttpServletRequest httpRequest) {
        log.info("[ChatController:chat] 开始聊天, 消息: {}", message);
        Long longUserId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
        String userId = longUserId.toString();
        try {
            return ReactiveStreamUtils.executeServerSentEventOperation(
                    () -> Flux.create(sink -> {
                        // 创建事件流发送器，使用简化构造函数
                        SubEventReporter reporter = new SubEventReporter(sink);

                        Schedulers.boundedElastic().schedule(() -> {
                            try {

                                // 获取共识数据并判断整体状态
                                String overallStatus = consensusUtil.getOverallConsensusStatus(userId);

                                if ("UNKNOWN".equals(overallStatus)) {
                                    log.info("[ChatController:chat] 获取共识整体状态为未知，进入规划新任务流程");
                                    // 1. 共识状态全部为未知时 [规划新任务]
                                    String ask = aiChatService.planNewTask(message, userId, reporter);

                                    // 向用户发送任务规划确认信息
                                    reporter.reportOverReply(ask);
                                    reporter.reportCompleteAndClose();

                                } else if ("KNOWN".equals(overallStatus) || "CONFIRMED".equals(overallStatus)) {
                                    // 共识状态存在已知或已确认时

                                    // 判断用户是否认同规划出来的方案
                                    boolean isAgree = aiChatService.judgeUserAgree(message, userId);

                                    if (isAgree) {
                                        log.info("[ChatController:chat] 用户同意规划方案");
                                        // 用户同意规划方案时就进入方案的步骤执行
                                        routerService.routeMatching(userId, reporter);
                                        reporter.reportCompleteAndClose();
                                        return;
                                    }
                                    // 用户不完全认同方案时，进入调整任务规划
                                    log.info("[ChatController:chat] 用户不同意规划方案，进入调整任务规划流程");
                                    String ask = aiChatService.adjustTaskPlan(message, userId, reporter);
                                    // 向用户发送任务规划调整确认信息
                                    reporter.reportOverReply(ask);
                                    reporter.reportCompleteAndClose();

                                }
                            } catch (Exception e) {
                                log.error("[ChatController:chat] 错误:", e);
                                String reply = bottomReplyService.reply("系统繁忙，请稍后再试", userId);
                                reporter.reportError(reply);
                                reporter.reportCompleteAndClose();
                            }
                        });
                    }),
                    e -> {
                        log.error("[ChatController:chat] 错误:", e);
                        String reply = bottomReplyService.reply("系统繁忙，请稍后再试", userId);
                        return Flux.just(SseSentEventUtils.createErrorEvent(reply),
                                SseSentEventUtils.createCompleteEvent());
                    },
                    signalType -> log.info("【网关】：向用户确认"));
        } catch (Exception e) {
            log.error("[ChatController:chat] 错误,", e);
            String reply = bottomReplyService.reply("系统繁忙，请稍后再试", userId);
            return Flux.just(SseSentEventUtils.createErrorEvent(reply),
                    SseSentEventUtils.createCompleteEvent());
        }
    }

    @GetMapping("/chatWithCoze")
    public Flux<ServerSentEvent<?>> chatWithCoze(String message, HttpServletRequest httpRequest) {
        log.info("【ChatController:chat】 开始聊天, 消息: {}", message);
        Long longUserId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
        String userId = longUserId.toString();
        try {
            return ReactiveStreamUtils.executeServerSentEventOperation(
                    () -> Flux.create(sink -> {
                        // 创建事件流发送器，使用简化构造函数
                        SubEventReporter reporter = new SubEventReporter(sink);

                        Schedulers.boundedElastic().schedule(() -> {
                            Map<String, Object> data = new HashMap<>();
                            data.put("content", message);
                            data.put("userId", userId);

                            reporter.reportStep("智能体开始解析");

                            // String workflowID = "7516741365884731403";
                            String workflowID = "7514634136641536051";

                            Flowable<WorkflowEvent> workflowEventFlowable = cozeClient.executeWorkflow(data,
                                    workflowID);

                            // 获取上一次规划好的共识
                            Map<String, String> completeNodeContent = WorkflowEventHandler
                                    .getCompleteNodeContent(workflowEventFlowable, reporter, consensusUtil, userId);

                            // 移除手动销毁调用，让Spring容器管理生命周期
                            // cozeClient.destroy();

                            // 如果可以执行
                            String workflowEnd = completeNodeContent.get("End");
                            log.info("【End】\n{}", workflowEnd);
                            JSONObject jsonObject = JSONUtil.parseObj(workflowEnd);
                            String code = jsonObject.getStr("code");

                            if (code.equals("001")) {
                                String reply = jsonObject.getStr("commentTranslation");
                                reporter.reportOverReply(reply);
                            } else if (code.equals("002")) {
                                // routerService.routeMatching(userId, reporter);
                                routerService.routeMatching(userId, reporter);
                            }
                            reporter.reportCompleteAndClose();

                        });
                    }),
                    e -> {
                        log.error("[ChatController:chat] 错误:", e);
                        String reply = bottomReplyService.reply("系统繁忙，请稍后再试", userId);
                        return Flux.just(SseSentEventUtils.createErrorEvent(reply),
                                SseSentEventUtils.createCompleteEvent());
                    },
                    signalType -> log.info("【网关】：向用户确认"));
        } catch (Exception e) {
            log.error("[ChatController:chat] 错误,", e);
            String reply = bottomReplyService.reply("系统繁忙，请稍后再试", userId);
            return Flux.just(SseSentEventUtils.createErrorEvent(reply),
                    SseSentEventUtils.createCompleteEvent());
        }
    }

    /**
     * 保存用户聊天历史
     *
     * @param request     包含用户ID和聊天历史的请求
     * @param httpRequest HTTP请求对象
     * @return 操作结果
     */
    @PostMapping("/chat/history")
    public Result saveChatHistory(@RequestBody SaveChatHistoryRequest request, HttpServletRequest httpRequest) {
        try {
            // 从会话获取用户ID
            Long userId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
            if (userId == null) {
                return Result.error("用户未登录");
            }

            // 保存聊天历史
            aiChatService.saveChatHistory(userId, request.getChatHistory());
            return Result.success("聊天历史保存成功");
        } catch (Exception e) {
            log.error("[ChatController:saveChatHistory] 错误:", e);
            return Result.error("系统繁忙，请稍后再试");
        }
    }

    /**
     * 加载用户聊天历史
     *
     * @param httpRequest HTTP请求对象
     * @return 聊天历史
     */
    @GetMapping("/chat/history")
    public Result loadChatHistory(HttpServletRequest httpRequest) {
        try {
            // 从会话获取用户ID
            Long userId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
            if (userId == null) {
                return Result.error("用户未登录");
            }

            // 加载聊天历史
            List<ChatMessage> chatHistory = aiChatService.loadChatHistory(userId);
            return Result.success(chatHistory);
        } catch (Exception e) {
            log.error("[ChatController:loadChatHistory] 错误:", e);
            return Result.error("系统繁忙，请稍后再试");
        }
    }

    /**
     * 市场版本v1.2对话入口
     *
     * @param message
     * @param httpRequest
     * @return
     */
    @GetMapping("/chatCozeForV12")
    public Flux<ServerSentEvent<?>> chatCozeForV12(String message, HttpServletRequest httpRequest) {
        log.info("【ChatController:chatCozeForV12】 开始聊天, 消息: {}", message);
        Long longUserId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
        String userId = longUserId.toString();
        try {
            return ReactiveStreamUtils.executeServerSentEventOperation(
                    () -> Flux.create(sink -> {
                        // 创建事件流发送器，使用简化构造函数
                        SubEventReporter reporter = new SubEventReporter(sink);

                        Schedulers.boundedElastic().schedule(() -> {
                            Map<String, Object> data = new HashMap<>();
                            data.put("input", message);
                            data.put("userId", userId);

                            String workflowID = "7515129132466208807";
                            Flowable<WorkflowEvent> workflowEventFlowable = cozeClient.executeWorkflow(data,
                                    workflowID);

                            Map<String, String> completeNodeContent = WorkflowEventHandler
                                    .getCompleteNodeContentV12(workflowEventFlowable, reporter, consensusUtil, userId);

                            // 如果可以执行
                            // String workflowEnd = completeNodeContent.get("结束");
                            // reporter.reportAnswer(workflowEnd);
                            // log.info("【End】\n{}", workflowEnd);
                            reporter.reportCompleteAndClose();

                        });
                    }),
                    e -> {
                        log.error("[ChatController:chatCozeForV12] 错误:", e);
                        return Flux.just(SseSentEventUtils.createErrorEvent("错误"),
                                SseSentEventUtils.createCompleteEvent());
                    },
                    signalType -> log.info("【网关】：向用户确认"));
        } catch (Exception e) {
            log.error("[ChatController:chatCoze] 错误,", e);
        }
        return null;
    }

    /**
     * 市场demo版本对话入口
     *
     * @param message
     * @param httpRequest
     * @return
     */
    @GetMapping("/chatCozeForDemo")
    public Flux<ServerSentEvent<?>> chatCozeForDemo(String message, HttpServletRequest httpRequest) {
        log.info("【ChatController:chatCozeForDemo】 开始聊天, 消息: {}", message);
        Long longUserId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
        String userId = longUserId.toString();
        try {
            return ReactiveStreamUtils.executeServerSentEventOperation(
                    () -> Flux.interval(Duration.ofMillis(800)).create(sink -> {
                        // 创建事件流发送器，使用简化构造函数
                        SubEventReporter reporter = new SubEventReporter(sink);

                        Schedulers.boundedElastic().schedule(() -> {
                            Map<String, Object> data = new HashMap<>();
                            data.put("input", message);
                            data.put("userId", userId);

                            String botId = "7518783817197502498";
                            Flowable<ChatEvent> workflowEventFlowable = cozeClient.botChat(userId, botId, message);

                            ChatEventHandler
                                    .getCompleteNodeContentDemo(workflowEventFlowable, reporter);
                            reporter.reportCompleteAndClose();

                        });
                    }),
                    e -> {
                        log.error("[ChatController:chatCozeForDemo] 错误:", e);
                        return Flux.just(SseSentEventUtils.createErrorEvent("错误"),
                                SseSentEventUtils.createCompleteEvent());
                    },
                    signalType -> log.info("【网关】：向用户确认"));
        } catch (Exception e) {
            log.error("[ChatController:chatCozeForDemo] 错误,", e);
        }
        return null;
    }

    /**
     * 市场版本对话入口
     *
     * @param message
     * @param httpRequest
     * @return
     */
    @GetMapping("/chatCozeForV13")
    public Flux<ServerSentEvent<?>> chatCozeForV13(String message, HttpServletRequest httpRequest) {
        log.info("【ChatController:chatCozeForV13】 开始聊天, 消息: {}", message);
        Long longUserId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
        String userId = longUserId.toString();
        try {
            return ReactiveStreamUtils.executeServerSentEventOperation(
                    () -> Flux.create(sink -> {
                        // 创建事件流发送器，使用简化构造函数
                        SubEventReporter reporter = new SubEventReporter(sink);

                        Schedulers.boundedElastic().schedule(() -> {
                            Map<String, Object> data = new HashMap<>();
                            data.put("input", message);
                            data.put("userId", userId);

                            String workflowID = "7518982183785283594";
                            // String workflowID = "7516741365884731403";
                            Flowable<WorkflowEvent> workflowEventFlowable = cozeClient.executeWorkflow(data,
                                    workflowID);

                            Map<String, String> completeNodeContent = WorkflowEventHandler
                                    .getCompleteNodeContentV12(workflowEventFlowable, reporter, consensusUtil, userId);

                            // 如果可以执行
                            String workflowEnd = completeNodeContent.get("End");
                            log.info("【End】\n{}", workflowEnd);
                            JSONObject jsonObject = JSONUtil.parseObj(workflowEnd);
                            String statusCode = jsonObject.getStr("status_code");
                            String targetAndView = jsonObject.getStr("target_and_view");
                            String taskName = getTaskName(targetAndView);
                            String planResult = getPlanResult(targetAndView);

                            String historyStr = jsonObject.getStr("historyStr");
                            log.info("【historyLog】\n{}", historyStr);

                            if (statusCode.equals("002")) {
                                // routerService.routeMatching(userId, reporter);
                                String finalPlanResult = planResult;
                                try {
                                    routerService.routeMatching(userId, reporter);
                                } catch (DataAccessException e) {
                                    // 数据访问异常 - 给兜底模块反馈"数据库查无数据"
                                    log.error("数据访问异常: {}", e.getMessage(), e);
                                    finalPlanResult = finalPlanResult + "\n【方案执行失败了！数据库查无数据】";
                                    handleExecutionResult(workflowEnd, historyStr, finalPlanResult, taskName, userId,
                                            reporter);

                                } catch (ExecutionFailureException e) {
                                    // 执行失败异常 - 使用异常消息中包含的具体描述
                                    log.error("执行失败异常: {}", e.getMessage(), e);
                                    finalPlanResult = finalPlanResult + "\n【方案执行失败了！" + e.getMessage() + "】";
                                    handleExecutionResult(workflowEnd, historyStr, finalPlanResult, taskName, userId,
                                            reporter);

                                } catch (Exception e) {
                                    // 其他未分类异常 - 通用错误处理
                                    log.error("未分类异常: {}", e.getMessage(), e);
                                    finalPlanResult = planResult + "\n【系统目前行动计划执行状态出现异常，执行失败！！！】";
                                    handleExecutionResult(workflowEnd, historyStr, finalPlanResult, taskName, userId,
                                            reporter);
                                }

                                handleExecutionResult(workflowEnd, historyStr, finalPlanResult, taskName, userId,
                                        reporter);
                            }

                            reporter.reportCompleteAndClose();

                        });
                    }),
                    e -> {
                        log.error("[ChatController:chatCozeForV13] 错误:", e);
                        return Flux.just(SseSentEventUtils.createErrorEvent("错误"),
                                SseSentEventUtils.createCompleteEvent());
                    },
                    signalType -> log.info("【网关】：对话结束"));
        } catch (Exception e) {
            log.error("[ChatController:chatCoze] 错误,", e);
        }
        return null;
    }

    @GetMapping("/chatCozeForV14")
    public Flux<ServerSentEvent<?>> chatCozeForV14(String message, HttpServletRequest httpRequest) {
        log.info("【ChatController:chatCozeForV14】 开始聊天, 消息: {}", message);
        Long longUserId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
        String userId = longUserId.toString();
        try {
            return ReactiveStreamUtils.executeServerSentEventOperation(
                    () -> Flux.create(sink -> {
                        // 创建事件流发送器，使用简化构造函数
                        SubEventReporter reporter = new SubEventReporter(sink);

                        Schedulers.boundedElastic().schedule(() -> {
                            Map<String, Object> data = new HashMap<>();
                            data.put("input", message);
                            data.put("userId", userId);

                            String workflowID = CHAT_WORKFLOW_ID;
                            Flowable<WorkflowEvent> workflowEventFlowable = cozeClient.executeWorkflow(data,
                                    workflowID);

                            Map<String, String> completeNodeContent = WorkflowEventHandler
                                    .getCompleteNodeContentV12(workflowEventFlowable, reporter, consensusUtil, userId);

                            // 解析结果
                            String workflowEnd = completeNodeContent.get("End");
                            if (workflowEnd != null) {
                                log.info("工作流结束节点: {}", workflowEnd);

                                JSONObject jsonObject = JSONUtil.parseObj(workflowEnd);
                                String statusCode = jsonObject.getStr("status_code");
                                String targetAndView = jsonObject.getStr("target_and_view");

                                // 提取任务名称和规划结果（用于后续处理）
                                String taskName = getTaskName(targetAndView);
                                String planResult = getPlanResult(targetAndView);

                                // 获取历史输入 - 从工作流返回的JSON中获取
                                String historyStr = jsonObject.getStr("historyStr");
                                bufferUtil.setField(userId, "historyStr", historyStr,-1, TimeUnit.DAYS);
                                bufferUtil.setField(userId, "question", message,-1, TimeUnit.DAYS);
                                if (statusCode.equals("002")) {
                                    String finalPlanResult = planResult;
                                    try {
                                        routerService.routeMatching(userId, reporter);
                                    } catch (DataAccessException e) {
                                        // 数据访问异常 - 给兜底模块反馈"数据库查无数据"
                                        log.error("数据访问异常: {}", e.getMessage(), e);
                                        finalPlanResult = finalPlanResult + "\n【方案执行失败了！数据库查无数据】";
                                        handleExecutionResult(workflowEnd, historyStr, finalPlanResult, taskName, userId,
                                                reporter);

                                    } catch (ExecutionFailureException e) {
                                        // 执行失败异常 - 使用异常消息中包含的具体描述
                                        log.error("执行失败异常: {}", e.getMessage(), e);
                                        finalPlanResult = finalPlanResult + "\n【方案执行失败了！" + e.getMessage() + "】";
                                        handleExecutionResult(workflowEnd, historyStr, finalPlanResult, taskName, userId,
                                                reporter);

                                    } catch (Exception e) {
                                        // 其他未分类异常 - 通用错误处理
                                        log.error("未分类异常: {}", e.getMessage(), e);
                                        finalPlanResult = planResult + "\n【系统目前行动计划执行状态出现异常，执行失败！！！】";
                                        handleExecutionResult(workflowEnd, historyStr, finalPlanResult, taskName, userId,
                                                reporter);
                                    }

                                    handleExecutionResult(workflowEnd, historyStr, finalPlanResult, taskName, userId,
                                            reporter);
                                }

                                reporter.reportCompleteAndClose();
                            } else {
                                throw new RuntimeException("工作流返回异常！");
                            }
                        });
                    }),
                    e -> {
                        log.error("[ChatController:chatCozeForV14] 错误:", e);
                        return Flux.just(SseSentEventUtils.createErrorEvent("错误"),
                                SseSentEventUtils.createCompleteEvent());
                    },
                    signalType -> log.info("【网关】：对话结束"));
        } catch (Exception e) {
            log.error("[ChatController:chatCoze] 错误,", e);
        }
        return null;
    }

    // 处理执行结果(成功、失败)
    private void handleExecutionResult(String workflowEnd, String historyStr, String finalPlanResult, String taskName,
            String userId, SubEventReporter reporter) {
        String reply = bottomReplyService.replyForExecution(workflowEnd, historyStr,
                finalPlanResult, taskName, userId);

        Pattern pattern = Pattern.compile(
                "【系统回复】[\\s]*(.*?)\\s*【追问用户】[\\s]*(.*)",
                Pattern.DOTALL);

        Matcher matcher = pattern.matcher(reply);
        String systemReply = "";
        String userFollowup = "";
        if (matcher.find() && matcher.groupCount() == 2) {
            systemReply = matcher.group(1).trim().replace("```","");
            userFollowup = matcher.group(2).trim().replace("```","");
        }
        // TODO
        reporter.reportAnswer(systemReply);
        reporter.reportAnswer("\n\n");
        reporter.reportAnswer(userFollowup);
        // 保存聊天历史
        saveChatHistory(userId, reply, bufferUtil.getPythonCode(userId),
                SAVE_WORKFLOW_ID);
    }

    String getTaskName(String input) {
        // 提取【任务目标】后面的内容
        Pattern taskTargetPattern = Pattern.compile("【任务目标】(.*)");
        Matcher taskTargetMatcher = taskTargetPattern.matcher(input);
        String taskTargetContent = "";
        if (taskTargetMatcher.find()) {
            taskTargetContent = taskTargetMatcher.group(1).trim();
        }

        return taskTargetContent;
    }

    String getPlanResult(String input) {
        // 提取【呈现形式】下一行到结束的内容
        Pattern presentationFormPattern = Pattern.compile("【呈现形式】\\s*([^\\S\\r\\n]*)(.*)", Pattern.DOTALL);
        Matcher presentationFormMatcher = presentationFormPattern.matcher(input);
        String presentationFormContent = "";
        if (presentationFormMatcher.find()) {
            presentationFormContent = presentationFormMatcher.group(2).trim();
        }

        return presentationFormContent;
    }

    void saveChatHistory(String userId, String content, String pyCode, String workflowID) {
        Map<String, Object> data = new HashMap<>();
        data.put("content", content);
        data.put("userId", userId);
        data.put("conversationId", userId);
        data.put("role", "system");
        data.put("py_code", pyCode);

        // String workflowID = "7517946042081263679";
        if (workflowID == null) {
            workflowID = SAVE_WORKFLOW_ID;
        }

        Flowable<WorkflowEvent> workflowEventFlowable = cozeClient.executeWorkflow(data, workflowID);

        Map<String, String> completeNodeContent = WorkflowEventHandler
                .getCompleteNodeContentV12(workflowEventFlowable, null, consensusUtil, userId);

    }

    /**
     * 市场版本对话入口
     *
     * @param message
     * @param httpRequest
     * @return
     */
    @GetMapping("/chatCoze")
    public Flux<ServerSentEvent<?>> chatCoze(String message, HttpServletRequest httpRequest) {
        log.info("【ChatController:chatCoze】 开始聊天, 消息: {}", message);
        Long longUserId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
        String userId = longUserId.toString();
        try {
            return ReactiveStreamUtils.executeServerSentEventOperation(
                    () -> Flux.create(sink -> {
                        // 创建事件流发送器，使用简化构造函数
                        SubEventReporter reporter = new SubEventReporter(sink);

                        Schedulers.boundedElastic().schedule(() -> {
                            Map<String, Object> data = new HashMap<>();
                            data.put("input", message);
                            data.put("userId", userId);

                            String workflowID = "7512815916032393225";
                            Flowable<WorkflowEvent> workflowEventFlowable = cozeClient.executeWorkflow(data,
                                    workflowID);

                            Map<String, String> completeNodeContent = WorkflowEventHandler
                                    .getCompleteNodeContentV12(workflowEventFlowable, reporter, consensusUtil, userId);

                            // 如果可以执行
                            String workflowEnd = completeNodeContent.get("结束");
                            log.info("【End】\n{}", workflowEnd);
                            reporter.reportCompleteAndClose();

                        });
                    }),
                    e -> {
                        log.error("[ChatController:chatCoze] 错误:", e);
                        return Flux.just(SseSentEventUtils.createErrorEvent("错误"),
                                SseSentEventUtils.createCompleteEvent());
                    },
                    signalType -> log.info("【网关】：向用户确认"));
        } catch (Exception e) {
            log.error("[ChatController:chatCoze] 错误,", e);
        }
        return null;
    }

    /**
     * 重置用户聊天历史
     *
     * @param httpRequest HTTP请求对象
     * @return 操作结果
     */
    @DeleteMapping("/newChat")
    public Result newChat(@RequestParam String workflowID, HttpServletRequest httpRequest) {
        try {
            // 从会话获取用户ID
            Long userIdLong = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);

            String userId = userIdLong.toString();

            if (workflowID == null) {
                workflowID = DELETE_WORKFLOW_ID;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            cozeClient.runWorkflow(data, workflowID);
            // 重置聊天历史
            return Result.success("聊天历史已成功重置");
        } catch (Exception e) {
            log.error("[ChatController:newChat] 错误:", e);
            return Result.error("系统繁忙，请稍后再试");
        }
    }

    /**
     * 重置用户聊天历史
     *
     * @param httpRequest HTTP请求对象
     * @return 操作结果
     */
    @DeleteMapping("/chat/history")
    public Result resetChatHistory(HttpServletRequest httpRequest) {
        try {
            // 从会话获取用户ID
            Long userId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
            if (userId == null) {
                return Result.error("用户未登录");
            }

            // 重置聊天历史
            aiChatService.resetChatHistory(userId);
            return Result.success("聊天历史已成功重置");
        } catch (Exception e) {
            log.error("[ChatController:resetChatHistory] 错误:", e);
            return Result.error("系统繁忙，请稍后再试");
        }
    }
}
