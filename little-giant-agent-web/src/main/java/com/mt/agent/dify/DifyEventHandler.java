package com.mt.agent.dify;

import com.mt.agent.reporter.SubEventReporter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import cn.hutool.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Dify事件处理器
 * 处理Dify的流式响应事件，并通过SubEventReporter发送给前端
 *
 * @author wsx
 * @date 2025/6/30
 */
@Slf4j
public class DifyEventHandler {

    /**
     * 处理Dify流式响应事件
     *
     * @param eventStream Dify流式响应事件流
     * @param reporter 事件报告器
     */
    public static void handleDifyStreamEvents(Flux<JSONObject> eventStream, SubEventReporter reporter) {
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder completeAnswer = new StringBuilder();

        log.info("开始处理Dify事件流...");

        eventStream.subscribe(
                event -> {
                    try {
                        String eventType = DifyClient.safeGetStr(event, "event", "");
                        log.debug("收到Dify事件: {}", eventType);

                        switch (eventType) {
                            case "message":
                                // 处理普通消息内容
                                String messageAnswer = DifyClient.safeGetStr(event, "answer", null);
                                if (messageAnswer != null) {
                                    String processedContent = processWhitespaceContent(messageAnswer);
                                    System.out.println("message:" + processedContent);
                                    completeAnswer.append(processedContent);
                                    reporter.reportAnswer(processedContent);
                                }
                                break;

                            case "agent_message":
                                // 处理智能体消息
                                String agentAnswer = DifyClient.safeGetStr(event, "answer", null);
                                if (agentAnswer != null) {
                                    String processedContent = processWhitespaceContent(agentAnswer);
                                    System.out.println("agent_message:" + processedContent);
                                    completeAnswer.append(processedContent);
                                    reporter.reportAnswer(processedContent);
                                }
                                break;

                            case "message_end":
                                // 消息结束
                                log.info("Dify消息流结束，完整内容长度: {}", completeAnswer.length());
                                latch.countDown();
                                break;

                            case "message_replace":
                                // 消息替换，清空之前的内容
                                log.debug("收到消息替换事件");
                                completeAnswer.setLength(0);
                                String replaceAnswer = DifyClient.safeGetStr(event, "answer", null);
                                System.out.println("message_replace:" + replaceAnswer);
                                if (replaceAnswer != null) {
                                    String processedContent = processWhitespaceContent(replaceAnswer);
                                    completeAnswer.append(processedContent);
                                    reporter.reportAnswer(processedContent);
                                }
                                break;

                            case "error":
                                // 错误处理
                                Integer errorCode = DifyClient.safeGetInt(event, "code", null);
                                String errorMessage = DifyClient.safeGetStr(event, "error", "未知错误");
                                log.error("Dify流式响应错误: code={}, message={}", errorCode, errorMessage);
                                reporter.reportError("Dify智能体错误: " + errorMessage);
                                latch.countDown();
                                break;

                            case "ping":
                                // 心跳事件，用于保持连接
                                log.debug("收到Dify心跳事件");
                                break;

                            default:
                                log.debug("收到未知事件类型: {}", eventType);
                                break;
                        }

                    } catch (Exception e) {
                        log.error("处理Dify事件失败", e);
                        reporter.reportError("处理响应事件失败: " + e.getMessage());
                    }
                },
                throwable -> {
                    log.error("Dify事件流处理发生异常: {}", throwable.getMessage(), throwable);
                    reporter.reportError("智能体响应异常: " + throwable.getMessage());
                    latch.countDown();
                },
                () -> {
                    log.info("Dify事件流正常完成");
                    latch.countDown();
                }
        );

        try {
            boolean completed = latch.await(300, TimeUnit.SECONDS);
            if (completed) {
                log.info("Dify事件处理完成，完整答案长度: {}", completeAnswer.length());
            } else {
                log.error("Dify事件处理等待超时");
                reporter.reportError("智能体响应超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Dify事件处理被中断");
            reporter.reportError("智能体响应被中断");
        }
    }

    /**
     * 处理Dify流式响应事件（异步版本，不阻塞）
     * @param eventStream Dify流式响应事件流
     * @param reporter 事件报告器
     */
    public static Map<String, String> handleDifyStreamEventsAsync(Flux<JSONObject> eventStream, SubEventReporter reporter) {
        log.info("开始异步处理Dify事件流...");
        CountDownLatch latch = new CountDownLatch(1);
        ConcurrentHashMap<String, String> resultContainer = new ConcurrentHashMap<>();
        AtomicReference<Boolean> inThinkingMode = new AtomicReference<>(false);
        eventStream.subscribe(
                event -> {
                    try {
                        String eventType = DifyClient.safeGetStr(event, "event", "");
                        if (!resultContainer.containsKey("conversation_id")) {
                            resultContainer.put("conversation_id", DifyClient.safeGetStr(event, "conversation_id", null));
                            resultContainer.put("message_id", DifyClient.safeGetStr(event, "message_id", null));
                        }

                        switch (eventType) {
                            case "message":
                                // 处理普通消息内容
                                String messageAnswer = DifyClient.safeGetStr(event, "answer", null);
                                if (messageAnswer != null) {
                                    String processedContent = processWhitespaceContent(messageAnswer);
                                    reporter.reportAnswer(processedContent);
                                    // 保存到结果容器
                                    if (!resultContainer.containsKey("answer")) {
                                        resultContainer.put("answer", processedContent);
                                    } else {
                                        resultContainer.put("answer", resultContainer.get("answer") + processedContent);
                                    }
                                }
                                break;

                            case "agent_message":
                                // 处理智能体消息，支持thinking标签解析
                                String agentAnswer = DifyClient.safeGetStr(event, "answer", null);
                                if (agentAnswer != null) {
                                    // 检查是否包含thinking标签
                                    if (agentAnswer.contains("<think>")) {
                                        // 开始思考模式
                                        inThinkingMode.set(true);
                                        break;
                                    }
                                    if (agentAnswer.contains("</think>")) {
                                        // 结束思考模式
                                        inThinkingMode.set(false);
                                        break;
                                    }

                                    // 根据当前模式处理内容
                                    if (inThinkingMode.get()) {
                                        reporter.reportThinking(agentAnswer);
                                        if (!resultContainer.containsKey("think")) {
                                            resultContainer.put("think", agentAnswer);
                                        } else {
                                            resultContainer.put("think", resultContainer.get("think") + agentAnswer);
                                        }
                                    } else {
                                        reporter.reportAnswer(agentAnswer);
                                        if (!resultContainer.containsKey("answer")) {
                                            resultContainer.put("answer", agentAnswer);
                                        } else {
                                            resultContainer.put("answer", resultContainer.get("answer") + agentAnswer);
                                        }
                                    }
                                }
                                break;

                            case "message_end":
                                // 消息结束，关闭流
                                log.info("Dify消息流结束");
                                latch.countDown();
                                break;

                            case "message_replace":
                                // 消息替换
                                log.debug("收到消息替换事件");
                                String replaceAnswer = DifyClient.safeGetStr(event, "answer", null);
                                if (replaceAnswer != null) {
                                    // 重置状态
                                    inThinkingMode.set(false);
                                    String processedContent = processWhitespaceContent(replaceAnswer);
                                    reporter.reportAnswer(processedContent);
                                    // 更新结果容器
                                    resultContainer.put("answer", processedContent);
                                }
                                break;

                            case "error":
                                // 错误处理
                                Integer errorCode = DifyClient.safeGetInt(event, "code", null);
                                String errorMessage = DifyClient.safeGetStr(event, "error", "未知错误");
                                log.error("Dify流式响应错误: code={}, message={}", errorCode, errorMessage);
                                reporter.reportError("Dify智能体错误: " + errorMessage);
                                latch.countDown();
                                break;

                            case "ping":
                                // 心跳事件，用于保持连接
                                log.debug("收到Dify心跳事件");
                                break;

                            default:
                                log.debug("收到未知事件类型: {}", eventType);
                                break;
                        }
                    } catch (Exception e) {
                        log.error("处理Dify事件失败", e);
                        reporter.reportError("处理响应事件失败: " + e.getMessage());
                    }
                },
                throwable -> {
                    log.error("Dify事件流处理发生异常: {}", throwable.getMessage(), throwable);
                    reporter.reportError("智能体响应异常: " + throwable.getMessage());
                    latch.countDown();
                },
                () -> {
                    log.info("Dify事件流正常完成");
                    // 注意：这里不调用reportCompleteAndClose，因为message_end事件已经调用了
                    latch.countDown();
                }
        );

        try {
            boolean completed = latch.await(180, TimeUnit.SECONDS);
            if (completed) {
                // 处理完成，返回结果集
                Map<String, String> resultSet = new HashMap<>();
                Set<Map.Entry<String, String>> entries = resultContainer.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    String key = entry.getKey();
                    String contentBuilder = entry.getValue();
                    resultSet.put(key, contentBuilder);
                }
                return resultSet;
            } else {
                log.error("工作流处理等待超时，收集到 {} 个节点的数据", resultContainer.size());
                throw new RuntimeException("等待超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("工作流处理被中断");
            throw new RuntimeException("等待被中断: ", e);
        }
    }

    /**
     * 处理空白字符内容，确保单个空格在前端正确显示
     *
     * @param content 原始内容
     * @return 处理后的内容
     */
    private static String processWhitespaceContent(String content) {
        if (content == null) {
            return null;
        }

        // 如果内容只是单个普通空格，替换为非断行空格
        if (" ".equals(content)) {
            log.debug("检测到单个空格，替换为非断行空格");
            return "\u00A0";
        }

        // 其他情况保持原样
        return content;
    }
}
