package com.mt.agent.coze;

import com.coze.openapi.client.chat.model.ChatEvent;
import com.coze.openapi.client.chat.model.ChatEventType;
import com.coze.openapi.client.connversations.message.model.MessageType;
import com.mt.agent.reporter.SubEventReporter;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 智能体对话事件处理工具类
 * 提供便捷的事件处理方法和常用的事件处理模式
 *
 * @author wsx
 * @date 2025/6/19
 */
@Slf4j
public class ChatEventHandler {

    public static void getCompleteNodeContentDemo(Flowable<ChatEvent> eventStream,
                                                             SubEventReporter reporter) {
        CountDownLatch latch = new CountDownLatch(1);
        ConcurrentHashMap<String, StringBuilder> resultContainer = new ConcurrentHashMap<>();

        log.info("开始处理智能体事件流...");
        eventStream.subscribe(
                event -> {
                    try{
                        if (event.getEvent().equals(ChatEventType.CONVERSATION_MESSAGE_DELTA)) {
                            if(event.getMessage().getType().equals(MessageType.ANSWER) && event.getMessage().getReasoningContent() == null) {
                                reporter.reportAnswer(event.getMessage().getContent());
                            }else if(event.getMessage().getType().equals(MessageType.ANSWER) && event.getMessage().getReasoningContent() != null) {
                                reporter.reportThinking(event.getMessage().getReasoningContent());
                            }
                        } else if (event.getEvent().equals(ChatEventType.CONVERSATION_MESSAGE_COMPLETED)) {
                            if(event.getMessage().getType().equals(MessageType.FOLLOW_UP)) {
                                reporter.reportRecommend(event.getMessage().getContent());
                            }
                        } else if (event.getEvent().equals(ChatEventType.DONE)) {
                             log.info("接收到智能体完成事件");
                            latch.countDown();
                        } else if (event.getEvent().equals(ChatEventType.CONVERSATION_CHAT_FAILED)) {
                            log.error("接收到智能体错误事件");
                            latch.countDown();
                        } else {
                        }
                    }catch (Exception e) {
                        log.error("",e);
                    }
                },
                throwable -> {
                    log.error("智能体事件流处理发生异常: {}", throwable.getMessage(), throwable);
                    latch.countDown();
                },
                () -> {
                    log.info("智能体事件流正常完成");
                    latch.countDown();
                });

        try {
            boolean completed = latch.await(100, TimeUnit.SECONDS);
            if (completed) {
                log.info("智能体处理完成，收集到 {} 个节点的数据", resultContainer.size());
            } else {
                log.error("智能体处理等待超时，收集到 {} 个节点的数据", resultContainer.size());
                throw new RuntimeException("等待超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("智能体处理被中断");
            throw new RuntimeException("等待被中断: ", e);
        }
    }

}
