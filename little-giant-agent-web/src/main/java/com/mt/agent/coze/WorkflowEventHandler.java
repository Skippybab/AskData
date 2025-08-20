package com.mt.agent.coze;

import cn.hutool.json.JSONObject;
import com.coze.openapi.client.workflows.run.model.WorkflowEvent;
import com.coze.openapi.client.workflows.run.model.WorkflowEventType;
import com.mt.agent.consensus.util.ConsensusUtil;
import com.mt.agent.reporter.StepResultData;
import com.mt.agent.reporter.SubEventReporter;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 工作流事件处理工具类
 * 提供便捷的事件处理方法和常用的事件处理模式
 *
 * @author MT Team
 * @date 2025/5/28
 */
@Slf4j
public class WorkflowEventHandler {

    /**
     * 获取指定节点的完整内容（可用于其他地方调用）
     *
     * @param eventStream     工作流事件流
     * @param targetNodeTitle 目标节点标题
     * @param reporter        前端事件报告器
     * @return 指定节点的完整内容
     */
    public static String getCompleteNodeContent(Flowable<WorkflowEvent> eventStream, String targetNodeTitle,
                                                SubEventReporter reporter) {
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder nodeContent = new StringBuilder();

        eventStream.subscribe(
                event -> {
                    if (event.getEvent().equals(WorkflowEventType.MESSAGE)) {
                        String nodeTitle = event.getMessage().getNodeTitle();
                        String content = event.getMessage().getContent();

                        log.info("【{}】: {}", nodeTitle, content);
                        // 判断是否需要返回执行步骤
                        if (nodeTitle.startsWith("step:")) {
                            reporter.reportStep(content);
                        }
                        // 判断是否为模型的思考过程
                        if (nodeTitle.startsWith("thinking:")) {
                            reporter.reportThinking(content);
                        }
                        // 判断是否需要返回执行结果
                        if (nodeTitle.startsWith("stepResult:")) {

                            if (content.equals("null")) {
                                reporter.reportStepResult(new StepResultData());
                            } else {
                                reporter.reportStepResult(new StepResultData(content));
                            }
                        }

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

    /**
     * @param eventStream 工作流事件流
     * @param reporter    前端事件报告器
     * @return 所有节点的完整内容
     */
    public static Map<String, String> getCompleteNodeContent(Flowable<WorkflowEvent> eventStream,
                                                             SubEventReporter reporter, ConsensusUtil consensusUtil, String userId) {
        CountDownLatch latch = new CountDownLatch(1);
        ConcurrentHashMap<String, StringBuilder> resultContainer = new ConcurrentHashMap<>();

        log.info("开始处理工作流事件流...");

        // 创建一个变量，用于存储方案规划的内容
        StringBuilder planContent = new StringBuilder();
        // 创建一个变量存储方案规划的标识，taskName: taskOutput: taskStep:
        AtomicReference<String> stepTag = new AtomicReference<>();

        eventStream.subscribe(
                event -> {
                    log.debug("接收到事件类型: {}", event.getEvent());
                    if (event.getEvent().equals(WorkflowEventType.MESSAGE)) {
                        String nodeTitle = event.getMessage().getNodeTitle();
                        String content = event.getMessage().getContent();

                        log.info("【coze的返回】【{}】: {}", nodeTitle, content);
                        // 判断是否需要返回执行步骤
                        if (nodeTitle.startsWith("step:")) {
                            reporter.reportStep(content);
                        }
                        // 判断是否为模型的思考过程
                        if (nodeTitle.startsWith("thinking:")) {
                            reporter.reportThinking(content);
                        }
                        // 判断是否需要返回执行结果
                        if (nodeTitle.startsWith("stepResult:")) {
                            if (content.equals("null")) {
                                reporter.reportStepResult(new StepResultData());
                            } else {
                                reporter.reportStepResult(new StepResultData(content));
                                reporter.reportStepResult(new StepResultData());
                            }
                        }

                        // 判断是否是流式返回的执行结果
                        if (nodeTitle.startsWith("streamStepResult:")) {

                            // 判断方案内容标识
                            if (content.startsWith("taskName:")) {
                                stepTag.set("taskName");
                                // 去除内容标识
                                content = content.substring(content.indexOf(":") + 1);
                            } else if (content.startsWith("taskOutput:")) {
                                stepTag.set("taskOutput");
                                // 去除内容标识
                                content = content.substring(content.indexOf(":") + 1);
                            } else if (content.startsWith("taskStep:")) {
                                stepTag.set("taskStep");
                                // 去除内容标识
                                content = content.substring(content.indexOf(":") + 1);
                            }

                            // 流式输出过程内容给前端
                            String finalContent = content;
                            if (content.contains("@")) {
                                // 如果结尾是@，则去除@
                                finalContent = content.replaceAll("@", "");
                            }
                            //返回前端思考过程中步骤结果的输出内容
                            reporter.reportStepResult(new StepResultData(finalContent));

                            // 判断是否为方案规划的内容，用于浮窗显示
                            if (nodeTitle.contains(":plan:")) {
                                parseTaskPlan(content, planContent, stepTag.get(), reporter, consensusUtil, userId);
                            }
                        }
                        if (nodeTitle.startsWith("isAgree:")) {
                            reporter.reportStepResult(new StepResultData(content));
                        }

                        // 收集节点的内容
                        resultContainer.computeIfAbsent(nodeTitle, k -> new StringBuilder()).append(content);
                    } else if (event.getEvent().equals(WorkflowEventType.DONE)) {
                        // log.info("接收到工作流完成事件");
                        latch.countDown();
                    } else if (event.getEvent().equals(WorkflowEventType.ERROR)) {
                        log.error("接收到工作流错误事件: {}", event.getError());
                        latch.countDown();
                    } else {
                        log.debug("接收到其他事件类型: {}", event.getEvent());
                    }
                },
                throwable -> {
                    log.error("工作流事件流处理发生异常: {}", throwable.getMessage(), throwable);
                    latch.countDown();
                },
                () -> {
                    log.info("工作流事件流正常完成");
                    latch.countDown();
                });

        try {
            boolean completed = latch.await(180, TimeUnit.SECONDS);
            if (completed) {
                log.info("工作流处理完成，收集到 {} 个节点的数据", resultContainer.size());
                // 处理完成，返回结果集
                Map<String, String> resultSet = new HashMap<>();
                Set<Map.Entry<String, StringBuilder>> entries = resultContainer.entrySet();
                for (Map.Entry<String, StringBuilder> entry : entries) {
                    String nodeTitle = entry.getKey();
                    StringBuilder contentBuilder = entry.getValue();
                    resultSet.put(nodeTitle, contentBuilder.toString());
                    log.debug("节点 [{}] 内容长度: {}", nodeTitle, contentBuilder.length());
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

    public static Map<String, String> getCompleteNodeContentV12(Flowable<WorkflowEvent> eventStream,
                                                             SubEventReporter reporter, ConsensusUtil consensusUtil, String userId) {
        CountDownLatch latch = new CountDownLatch(1);
        ConcurrentHashMap<String, StringBuilder> resultContainer = new ConcurrentHashMap<>();

        log.info("开始处理工作流事件流...");

        eventStream.subscribe(
                event -> {
                    //log.debug("接收到事件类型: {}", event.getEvent());

                    try{
                        if (event.getEvent().equals(WorkflowEventType.MESSAGE)) {
                            String nodeTitle = event.getMessage().getNodeTitle();
                            String content = event.getMessage().getContent();

                            //log.info("【coze的返回】【{}】: {}", nodeTitle, content);
                            if (nodeTitle == null) {
                                nodeTitle = "null";
                            } else if (nodeTitle.startsWith("start:")) {
                                reporter.reportThinking(content);
                            } else if (nodeTitle.startsWith("aim:")) {
                                reporter.reportStep(content);
                            } else if (nodeTitle.startsWith("router:")) {
                                reporter.reportThinking(content);
                            }else if (nodeTitle.startsWith("refuse:")) {
                                reporter.reportAnswer(content);
                            }else if (nodeTitle.startsWith("question:")) {
                                reporter.reportAnswer(content);
                            }
                            else if (nodeTitle.startsWith("answer:")) {
                                reporter.reportAnswer(content);
                            }
                            else if (nodeTitle.startsWith("plan:")) {
                                reporter.reportThinking(content);
                            }
                            else if (nodeTitle.startsWith("planResult:")) {
                                reporter.reportThinking(content);
                            }
                            else if (nodeTitle.startsWith("thinking:")) {
                                reporter.reportThinking(content);
                            }
                            else if (nodeTitle.startsWith("reply:")) {
                                reporter.reportThinking(content);
                            }
                            else if (nodeTitle.startsWith("SQL:")) {
                                reporter.reportThinking(content);
                            }
                            else if (nodeTitle.startsWith("SQLStream:")) {
                                reporter.reportThinking(content);
                            }
                            else if (nodeTitle.startsWith("caculate:")) {
                                reporter.reportThinking(content);
                            }
                            else if (nodeTitle.startsWith("result:")) {
                                reporter.reportAnswer(content);
                            }

                            // 收集节点的内容
                            resultContainer.computeIfAbsent(nodeTitle, k -> new StringBuilder()).append(content);
                        } else if (event.getEvent().equals(WorkflowEventType.DONE)) {
                            // log.info("接收到工作流完成事件");
                            latch.countDown();
                        } else if (event.getEvent().equals(WorkflowEventType.ERROR)) {
                            log.error("接收到工作流错误事件: {}", event.getError());
                            latch.countDown();
                        } else {
                            log.debug("接收到其他事件类型: {}", event.getEvent());
                        }
                    }catch (Exception e) {
                        log.error("",e);
                    }
                },
                throwable -> {
                    log.error("工作流事件流处理发生异常: {}", throwable.getMessage(), throwable);
                    latch.countDown();
                },
                () -> {
                    log.info("工作流事件流正常完成");
                    latch.countDown();
                });

        try {
            boolean completed = latch.await(180, TimeUnit.SECONDS);
            if (completed) {
                log.info("工作流处理完成，收集到 {} 个节点的数据", resultContainer.size());
                // 处理完成，返回结果集
                Map<String, String> resultSet = new HashMap<>();
                Set<Map.Entry<String, StringBuilder>> entries = resultContainer.entrySet();
                for (Map.Entry<String, StringBuilder> entry : entries) {
                    String nodeTitle = entry.getKey();
                    StringBuilder contentBuilder = entry.getValue();
                    resultSet.put(nodeTitle, contentBuilder.toString());
                    log.debug("节点 [{}] 内容长度: {}", nodeTitle, contentBuilder.length());
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
     * 解析任务规划内容
     *
     * @param content     内容
     * @param planContent 规划内容构建器
     * @param stepTag     步骤标签
     * @param reporter    事件报告器
     */
    private static void parseTaskPlan(String content, StringBuilder planContent, String stepTag,
                                      SubEventReporter reporter, ConsensusUtil consensusUtil, String userId) {


        // 去除分号后面的换行符
        String str = content.replaceAll("[\n\r]+$", "");

        boolean isComplate = str.endsWith("@");

        String[] split = str.split("@");
        for (int i = 0; i < split.length; i++) {

            planContent.append(split[i]);

            //  判断是否为最后一个内容，如果是且最后内容没有“@”标识则跳过
            if (i == split.length - 1 && !isComplate) {
                return;
            }

            // 使用正则表达式去除前导的换行符(\n)、任何空白符(\s)和连字符(-)
            String contentText=  planContent.toString();
            contentText = contentText.replaceAll("^[\\n\\s-]*", "");

            // 判断有没有与上一次规划好的内容相同
            String status = judgeSameContent(contentText, stepTag, consensusUtil, userId);


            // 返回前端数据，格式为字符串数组：[内容标识（taskName等）, 具体内容, 状态（待确认know、已确认confirmed）]
            reporter.reportTaskPlan(new String[]{stepTag, contentText, status});
            // 清空阻塞存储的内容
            planContent.delete(0, planContent.length());
        }

    }

    /**
     * 判断当前内容是否与上一次规划好的内容相同，相同则返回confirmed，否则返回know
     *
     * @param content
     * @param stepTag
     * @return
     * @author lfz
     * @date 2025/6/6 18:30
     */
    private static String judgeSameContent(String content, String stepTag, ConsensusUtil consensusUtil, String userId) {

        //如果是新的规划，则返回待确认
        JSONObject consensus = consensusUtil.getConsensus(userId);
        if (consensus.get("status").equals("UNKNOWN")) {
            return "know";
        }

        // 如果是调整规划方案，则判断是否与上一次规划好的内容相同
        if (stepTag.equals("taskName")) {

            String text = consensus.getByPath("taskName.name", String.class);
            return content.equals(text)? "confirmed" : "know";

        } else if (stepTag.equals("taskOutput")) {

            //判断任务输出里面有没有当前规划的输出
            List<JSONObject> text = consensus.getByPath("taskOutput.output", List.class);
            String status = "know";
            for (JSONObject o : text) {
                String name = (String) o.get("name");
                if ((name.contains( content))) {
                    status = "confirmed";
                    break;
                }
            }
            return status;

        } else if (stepTag.equals("taskStep")) {

            //判断任务步骤里面有没有当前规划的步骤
            List<JSONObject> text = consensus.getByPath("taskSteps.steps", List.class);
            String status = "know";
            for (JSONObject o : text) {
                String name = (String) o.get("name");
                if ((name.contains( content))) {
                    status = "confirmed";
                    break;
                }
            }
            return status;

        }

        return "know";
    }

}
