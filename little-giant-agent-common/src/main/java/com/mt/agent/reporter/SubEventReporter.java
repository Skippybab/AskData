package com.mt.agent.reporter;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.FluxSink;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;
import java.util.function.Supplier;

import com.mt.agent.utils.SseSentEventUtils;

/**
 * SubmoduleEventReporter 的默认实现。
 * 通过 FluxSink 将事件推送到反应式流中。
 */
@Slf4j
public class SubEventReporter implements SubmoduleEventReporter {

    private final FluxSink<ServerSentEvent<?>> sink;
    private final Function<String, ServerSentEvent<?>> stepEventFactory;
    private final Function<StepResultData, ServerSentEvent<?>> stepResultEventFactory;
    private final Function<Object, ServerSentEvent<?>> resultEventFactory;
    private final Function<String, ServerSentEvent<?>> errorEventFactory;
    private final Function<String, ServerSentEvent<?>> overReplyEventFactory;
    private final Supplier<ServerSentEvent<?>> completeEventFactory;
    private final Function<String, ServerSentEvent<?>> thinkingEventFactory;
    private final Function<String[], ServerSentEvent<?>> taskPlanEventFactory;
    private final Function<String, ServerSentEvent<?>> treeEventFactory;
    private final Function<String, ServerSentEvent<?>> answerEventFactory;
    private final Function<String, ServerSentEvent<?>> replyEventFactory;
    private final Function<String, ServerSentEvent<?>> recommendEventFactory;
    private final Function<String, ServerSentEvent<?>> jsonEventFactory;

    /**
     * 简化版构造函数，使用默认的事件工厂方法。
     *
     * @param sink FluxSink 用于发射事件
     */
    public SubEventReporter(FluxSink<ServerSentEvent<?>> sink) {
        this(
                sink,
                SseSentEventUtils::createStepEvent,
                SseSentEventUtils::createStepResultEvent,
                SseSentEventUtils::createNodeResultEvent,
                SseSentEventUtils::createErrorEvent,
                SseSentEventUtils::createOverReplyEvent,
                SseSentEventUtils::createCompleteEvent,
                SseSentEventUtils::createThinkingEvent,
                SseSentEventUtils::createTaskPlanEvent,
                SseSentEventUtils::createTreeEvent,
                SseSentEventUtils::createAnswerEvent,
                SseSentEventUtils::createReplyEvent,
                SseSentEventUtils::createRecommendEvent,
                SseSentEventUtils::createJsonEvent);
    }

    /**
     * 扩展构造函数，支持兜底回复和任务完成事件。
     *
     * @param sink                   FluxSink 用于发射事件
     * @param stepEventFactory       用于创建进度事件的函数
     * @param stepResultEventFactory 用于创建步骤结果事件的函数
     * @param resultEventFactory     用于创建节点结果事件的函数
     * @param errorEventFactory      用于创建错误事件的函数
     * @param overReplyEventFactory  用于创建兜底回复事件的函数
     * @param completeEventFactory   用于创建任务完成事件的函数
     * @param thinkingEventFactory   用于创建思考过程事件的函数
     */
    public SubEventReporter(
            FluxSink<ServerSentEvent<?>> sink,
            Function<String, ServerSentEvent<?>> stepEventFactory,
            Function<StepResultData, ServerSentEvent<?>> stepResultEventFactory,
            Function<Object, ServerSentEvent<?>> resultEventFactory,
            Function<String, ServerSentEvent<?>> errorEventFactory,
            Function<String, ServerSentEvent<?>> overReplyEventFactory,
            Supplier<ServerSentEvent<?>> completeEventFactory,
            Function<String, ServerSentEvent<?>> thinkingEventFactory,
            Function<String[], ServerSentEvent<?>> taskPlanEventFactory,
            Function<String, ServerSentEvent<?>> treeEventFactory,
            Function<String, ServerSentEvent<?>> answerEventFactory,
            Function<String, ServerSentEvent<?>> replyEventFactory,
            Function<String, ServerSentEvent<?>> recommendEventFactory,
            Function<String, ServerSentEvent<?>> jsonEventFactory) {
        this.sink = sink;
        this.stepEventFactory = stepEventFactory;
        this.stepResultEventFactory = stepResultEventFactory;
        this.resultEventFactory = resultEventFactory;
        this.errorEventFactory = errorEventFactory;
        this.overReplyEventFactory = overReplyEventFactory;
        this.completeEventFactory = completeEventFactory;
        this.thinkingEventFactory = thinkingEventFactory;
        this.taskPlanEventFactory = taskPlanEventFactory;
        this.treeEventFactory = treeEventFactory;
        this.answerEventFactory = answerEventFactory;
        this.replyEventFactory = replyEventFactory;
        this.recommendEventFactory = recommendEventFactory;
        this.jsonEventFactory = jsonEventFactory;
    }

    @Override
    public void reportStep(String message) {
        if (isSinkCancelled())
            return;
        sink.next(stepEventFactory.apply(message));
    }

    @Override
    public void reportStepResult(StepResultData resultData) {
        if (isSinkCancelled())
            return;
        sink.next(stepResultEventFactory.apply(resultData));
    }

    @Override
    public void reportNodeResult(Object resultPayload) {
        if (isSinkCancelled())
            return;
        sink.next(resultEventFactory.apply(resultPayload));
    }

    @Override
    public void reportError(String errorMessage, Throwable throwable) {
        if (isSinkCancelled())
            return;
        sink.next(errorEventFactory.apply(errorMessage));
    }

    @Override
    public void reportError(String errorMessage) {
        reportError(errorMessage, null);
    }

    /**
     * 报告兜底回复，或普通的纯文本回复
     *
     * @param replyMessage 兜底回复消息
     */
    @Override
    public void reportOverReply(String replyMessage) {
        if (isSinkCancelled())
            return;
        if (overReplyEventFactory != null) {
            sink.next(overReplyEventFactory.apply(replyMessage));
        }
    }

    /**
     * 报告模型思考过程
     *
     * @param thinkingContent 思考内容
     */
    @Override
    public void reportThinking(String thinkingContent) {
        if (isSinkCancelled())
            return;
        if (thinkingEventFactory != null) {
            sink.next(thinkingEventFactory.apply(thinkingContent));
        }
    }

    /**
     * 报告任务完成
     */
    @Override
    public void reportComplete() {
        if (isSinkCancelled())
            return;
        if (completeEventFactory != null) {
            sink.next(completeEventFactory.get());
        }
    }

    /**
     * 报告任务完成并关闭事件流
     * <p>
     * 该方法将发送一个完成事件，然后关闭底层反应式流，表明没有更多事件会发送
     */
    @Override
    public void reportCompleteAndClose() {
        if (isSinkCancelled())
            return;

        // 首先发送完成事件
        reportComplete();

        // 然后关闭流
        sink.complete();
    }

    @Override
    public void reportTaskPlan(String[] planContent) {
        if (isSinkCancelled())
            return;
        sink.next(taskPlanEventFactory.apply(planContent));
    }

    public void reportTree(String treeJson) {
        if (isSinkCancelled())
            return;
        try {
            if (treeEventFactory != null) {
                sink.next(treeEventFactory.apply(treeJson));
            } else {
                log.warn("[EventReporter] treeJson is null, unable to report treeJson content: {}",
                        treeJson);
            }
        } catch (Exception e) {
            log.error("[EventReporter] Error applying treeJson for content: {}", treeJson, e);
        }
    }

    public void reportAnswer(String answer) {
        if (isSinkCancelled())
            return;
        try {
            if (answerEventFactory != null) {
                sink.next(answerEventFactory.apply(answer));
            } else {
                log.warn("[EventReporter] answer is null, unable to report answer content: {}",
                        answer);
            }
        } catch (Exception e) {
            log.error("[EventReporter] Error applying answer for content: {}", answer, e);
        }
    }

    @Override
    public void reportReply(String replyContent) {
        if (isSinkCancelled())
            return;
        try {
            if (replyEventFactory != null) {
                sink.next(replyEventFactory.apply(replyContent));
            } else {
                log.warn("[EventReporter] replyEventFactory is null, unable to report reply content: {}",
                        replyContent);
            }
        } catch (Exception e) {
            log.error("[EventReporter] Error applying reply for content: {}", replyContent, e);
        }
    }

    public void reportRecommend(String recommend) {
        if (isSinkCancelled())
            return;
        try {
            if (recommendEventFactory != null) {
                sink.next(recommendEventFactory.apply(recommend));
            } else {
                log.warn("[EventReporter] recommend is null, unable to report recommend content: {}",
                        recommend);
            }
        } catch (Exception e) {
            log.error("[EventReporter] Error applying recommend for content: {}", recommend, e);
        }
    }

    public void reportJson(String json) {
        if (isSinkCancelled())
            return;
        try {
            if (jsonEventFactory != null) {
                sink.next(jsonEventFactory.apply(json));
            } else {
                log.warn("[EventReporter] json is null, unable to report json content: {}",
                        json);
            }
        } catch (Exception e) {
            log.error("[EventReporter] Error applying json for content: {}", json, e);
        }
    }

    private boolean isSinkCancelled() {
        return sink.isCancelled();
    }

}
