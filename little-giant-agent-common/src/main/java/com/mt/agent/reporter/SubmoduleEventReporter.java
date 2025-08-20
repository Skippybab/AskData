package com.mt.agent.reporter;

/**
 * 子模块事件报告器接口
 * 用于从业务逻辑内部报告进度、结果或错误，以便流式传输到客户端。
 */
public interface SubmoduleEventReporter {

    /**
     * 报告进度信息。
     *
     * @param message 进度消息
     */
    void reportStep(String message);

    /**
     * 报告步骤结果信息。
     *
     * @param resultData 步骤结果数据
     */
    void reportStepResult(StepResultData resultData);

    /**
     * 报告工作流执行节点结果对象。
     *
     * @param resultPayload 结果数据
     */
    void reportNodeResult(Object resultPayload);

    /**
     * 报告错误信息和关联的异常。
     *
     * @param errorMessage 错误描述
     * @param throwable    关联的异常 (可以为 null)
     */
    void reportError(String errorMessage, Throwable throwable);

    /**
     * 报告错误信息。
     *
     * @param errorMessage 错误描述
     */
    void reportError(String errorMessage);

    /**
     * 报告兜底回复信息。
     *
     * @param replyMessage 兜底回复内容
     */
    void reportOverReply(String replyMessage);

    /**
     * 报告任务完成。
     */
    void reportComplete();

    /**
     * 报告任务完成并关闭事件流。
     *
     * 该方法将发送一个完成事件，然后关闭底层反应式流，表明没有更多事件会发送
     */
    void reportCompleteAndClose();

    /**
     * 报告模型思考过程。
     *
     * @param thinkingContent 思考内容
     */
    void reportThinking(String thinkingContent);

    /**
     * 报告任务规划内容。
     *
     * @param planContent 规划内容数组，格式为字符串数组：[内容标识（taskName等）, 具体内容, 状态（待确认know、已确认confirmed）]
     */
    void reportTaskPlan(String[] planContent);

    /**
     * 报告树形构造
     *
     * @param treeJson 树形字符串
     */
    void reportTree(String treeJson);

    /**
     * 报告执行结果
     *
     * @param result 结果
     */
    void reportAnswer(String result);

    /**
     * 报告回复内容
     * 用于流式输出回复内容，支持前端逐步接收和展示
     *
     * @param replyContent 回复内容
     */
    void reportReply(String replyContent);

    /**
     * 报告推荐
     *
     * @param recommend 推荐
     */
    void reportRecommend(String recommend);

    /**
     * 报告json参数
     *
     * @param json json字符串
     */
    void reportJson(String json);
}
