package com.mt.agent.utils;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import com.mt.agent.reporter.StepResultData;

/**
 * 服务器发送事件(SSE)工具类
 * 提供处理Server-Sent Events的通用方法
 *
 * @author lfz
 */
public class SseSentEventUtils {

    /**
     * 创建通用SSE事件
     *
     * @param eventName 事件名称
     * @param data      事件数据
     * @param <T>       数据类型
     * @return SSE事件对象
     */
    public static <T> ServerSentEvent<T> createEvent(String eventName, T data) {
        return ServerSentEvent.<T>builder()
                .event(eventName)
                .data(data)
                .build();
    }

    /**
     * 创建兜底回复事件
     *
     * @param message 消息内容
     * @return 消息事件对象
     */
    public static ServerSentEvent<String> createOverReplyEvent(String message) {
        return createEvent("overReply", message);
    }

    /**
     * 创建节点结果事件
     *
     * @param result 节点结果数据
     * @param <T>    数据类型
     * @return 节点结果事件对象
     */
    public static <T> ServerSentEvent<T> createNodeResultEvent(T result) {
        return createEvent("node_result", result);
    }

    /**
     * 创建意图事件
     *
     * @param intent 意图
     * @return 意图事件对象
     */
    public static ServerSentEvent<String> createIntentEvent(String intent) {
        return createEvent("intent", intent);
    }

    /**
     * 创建进度提示事件
     *
     * @param stepMessage 执行步骤
     * @return 进度事件对象
     */
    public static ServerSentEvent<String> createStepEvent(String stepMessage) {
        return createEvent("step", stepMessage);
    }

    /**
     * 创建模型思考过程事件
     *
     * @param thinkingContent 思考内容
     * @return 思考过程事件对象
     */
    public static ServerSentEvent<String> createThinkingEvent(String thinkingContent) {
        return createEvent("thinking", thinkingContent);
    }

    /**
     * 创建步骤结果事件
     *
     * @param resultData 步骤结果数据
     * @return 步骤结果事件对象
     */
    public static ServerSentEvent<StepResultData> createStepResultEvent(StepResultData resultData) {
        return createEvent("stepResult", resultData);
    }

    /**
     * 创建大模型文本结果返回事件
     *
     * @param result 大模型的结果
     * @param <T>    数据类型
     * @return 节点结果事件对象
     */
    public static <T> ServerSentEvent<T> createLLMMsgEvent(T result) {
        return createEvent("LLM", result);
    }

    /**
     * 创建错误事件
     *
     * @param errorMessage 错误信息
     * @return 错误事件对象
     */
    public static ServerSentEvent<String> createErrorEvent(String errorMessage) {
        return createEvent("error", errorMessage);
    }

    /**
     * 创建Tree事件
     *
     * @param message 消息内容
     * @return 消息事件对象
     */
    public static ServerSentEvent<String> createTreeEvent(String message) {
        return createEvent("tree", message);
    }

    /**
     * 创建answer事件
     *
     * @param message 消息内容
     * @return 消息事件对象
     */
    public static ServerSentEvent<String> createAnswerEvent(String message) {
        return createEvent("answer", message);
    }

    /**
     * 创建recommend事件
     *
     * @param message 消息内容
     * @return 消息事件对象
     */
    public static ServerSentEvent<String> createRecommendEvent(String message) {
        return createEvent("recommend", message);
    }

    /**
     * json
     *
     * @param message 消息内容
     * @return 消息事件对象
     */
    public static ServerSentEvent<String> createJsonEvent(String message) {
        return createEvent("json", message);
    }

    /**
     * 创建完成事件
     *
     * @return 完成事件对象
     */
    public static ServerSentEvent<String> createCompleteEvent() {
        return createEvent("complete", "complete");
    }

    /**
     * 创建包含单个错误事件的流
     *
     * @param errorMessage 错误信息
     * @return 包含错误事件的流
     */
    public static Flux<ServerSentEvent<?>> errorAndCompleteEventFlux(String errorMessage) {
        return Flux.just(
                createErrorEvent(errorMessage),
                createCompleteEvent());
    }

    /**
     * 创建包含单个大模型结果信息事件和完成事件的流
     *
     * @param resultMessage 大模型结果信息
     * @return 包含大模型结果信息事件和完成事件的流
     */
    public static Flux<ServerSentEvent<?>> llmMsgAndCompleteEventFlux(String resultMessage) {
        return Flux.just(
                createLLMMsgEvent(resultMessage),
                createCompleteEvent());
    }

    /**
     * 创建包含单个消息事件和完成事件的流
     *
     * @param message 消息内容
     * @return 包含消息和完成事件的流
     */
    public static Flux<ServerSentEvent<?>> messageAndCompleteEventFlux(String message) {
        return Flux.just(
                createOverReplyEvent(message),
                createCompleteEvent());
    }

    /**
     * 创建包含兜底回复事件和完成事件的流
     *
     * @param reply 回复内容
     * @return 包含回复和完成事件的流
     */
    public static Flux<ServerSentEvent<?>> replyAndCompleteEventFlux(String reply) {
        return Flux.just(
                createOverReplyEvent(reply),
                createCompleteEvent());
    }

    /**
     * 创建任务规划事件
     *
     * @param planContent 规划内容数组
     * @return 任务规划事件对象
     */
    public static ServerSentEvent<String[]> createTaskPlanEvent(String[] planContent) {
        return createEvent("task_plan", planContent);
    }

    /**
     * 创建回复事件
     * 用于流式输出回复内容，支持前端逐步接收和展示
     *
     * @param replyContent 回复内容
     * @return 回复事件对象
     */
    public static ServerSentEvent<String> createReplyEvent(String replyContent) {
        return createEvent("reply", replyContent);
    }

}
