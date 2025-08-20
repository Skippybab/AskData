package com.mt.agent.test;

import com.coze.openapi.client.workflows.run.model.WorkflowEvent;
import com.coze.openapi.client.workflows.run.model.WorkflowEventType;
import com.mt.agent.buffer.util.BufferUtil;
import com.mt.agent.consensus.util.ConsensusUtil;
import com.mt.agent.coze.CozeClient;
import com.mt.agent.router.service.RouterService;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * coze 调用测试
 *
 * @date 2025/5/28
 */
@SpringBootTest
@Slf4j
public class CozeTest3 {

    @Autowired
    private CozeClient cozeClient;

    @Autowired
    private BufferUtil bufferUtil;

    @Autowired
    private ConsensusUtil consensusUtil;

    @Autowired
    private RouterService routerService;

    @Test
    public void cozeTest() {
        Map<String, Object> data = new HashMap<>();
        data.put("content", "2023年医药制造业的营收情况");
        data.put("userId", "4");

        String workflowID = "7508921133278986259";
        bufferUtil.clearUserCache("4");
//        consensusUtil.clearUserConsensus("4");

        Flowable<WorkflowEvent> workflowEventFlowable = cozeClient.executeWorkflow(data, workflowID);

        Map<String,StringBuilder> completeNodeContent = new HashMap<>();

        handleEventWithCozeClient(workflowEventFlowable,completeNodeContent);
        cozeClient.destroy();


    }

    @Test
    public void cozeTestSaveChatHistory() {


        Map<String, Object> data = new HashMap<>();
        data.put("content", "你好111");
        data.put("userId", "1");
        data.put("conversationId", "1");
        data.put("role", "system");


        String workflowID = "7518982183785398282";


        Flowable<WorkflowEvent> workflowEventFlowable = cozeClient.executeWorkflow(data, workflowID);

        Map<String,StringBuilder> completeNodeContent = new HashMap<>();

        handleEventWithCozeClient(workflowEventFlowable,completeNodeContent);

    }
    /**
     * 处理CozeClient返回的工作流事件流（收集完整节点内容版本）
     *
     * @param eventStream 工作流事件流
     */
    private void handleEventWithCozeClient(Flowable<WorkflowEvent> eventStream,
            Map<String, StringBuilder> nodeContents) {
        CountDownLatch latch = new CountDownLatch(1);

        eventStream.subscribe(
                event -> {
                    // 判断事件类型并进行相应的处理
                    if (event.getEvent().equals(WorkflowEventType.MESSAGE)) {
                        String nodeTitle = event.getMessage().getNodeTitle();
                        String content = event.getMessage().getContent();

                        // 如果节点标题为空，使用默认值
                        if (nodeTitle == null || nodeTitle.trim().isEmpty()) {
                            nodeTitle = "未知节点";
                        }

                        // 收集节点内容
                        nodeContents.computeIfAbsent(nodeTitle, k -> new StringBuilder()).append(content);

                        // 实时显示流式输出（可选，用于调试）
                        log.debug("【{}】流式片段: {}", nodeTitle, content);

                    } else if (event.getEvent().equals(WorkflowEventType.ERROR)) {
                        latch.countDown();
                        log.error("收到错误事件: {}", event.getError());
                    } else if (event.getEvent().equals(WorkflowEventType.DONE)) {
                        latch.countDown();
                        log.info("工作流运行完成: {}", event.getMessage());

                    } else if (event.getEvent().equals(WorkflowEventType.INTERRUPT)) {
                        log.warn("收到中断事件，需要人工处理: {}", event.getInterrupt());
                        latch.countDown();
                    } else {
                        log.debug("收到其他类型事件: {}", event.getEvent());
                    }
                },
                throwable -> {
                    latch.countDown();
                    log.error("处理工作流事件时发生异常: ", throwable);

                    // 针对401错误给出具体建议
                    if (throwable.getMessage() != null && throwable.getMessage().contains("401")) {
                        log.error("认证失败(401)，请检查以下几点：");
                        log.error("1. Token是否有效且未过期");
                        log.error("2. Base URL是否正确（中国区域使用api.coze.cn，国际区域使用api.coze.com）");
                        log.error("3. Token是否有访问该工作流的权限");
                        log.error("4. 当前配置的Token: {}", cozeClient.getMaskedToken());
                    }
                },
                () -> {
                    latch.countDown();
                    log.info("工作流事件流处理完成");
                });

        try {
            // 等待流处理完成，最多等待60秒
            boolean completed = latch.await(60, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("等待超时，工作流可能仍在执行中");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待被中断: ", e);
        }
    }

    /**
     * 获取指定节点的完整内容（可用于其他地方调用）
     *
     * @param eventStream     工作流事件流
     * @param targetNodeTitle 目标节点标题
     * @return 指定节点的完整内容
     */
    public String getCompleteNodeContent(Flowable<WorkflowEvent> eventStream, String targetNodeTitle) {
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder nodeContent = new StringBuilder();

        eventStream.subscribe(
                event -> {
                    if (event.getEvent().equals(WorkflowEventType.MESSAGE)) {
                        String nodeTitle = event.getMessage().getNodeTitle();
                        String content = event.getMessage().getContent();

                        // 只收集目标节点的内容
                        if (targetNodeTitle.equals(nodeTitle)) {
                            nodeContent.append(content);
                        }
                    } else if (event.getEvent().equals(WorkflowEventType.DONE) ||
                            event.getEvent().equals(WorkflowEventType.ERROR)) {
                        latch.countDown();
                    }
                },
                throwable -> {
                    latch.countDown();
                },
                () -> latch.countDown());

        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return nodeContent.toString();
    }

}