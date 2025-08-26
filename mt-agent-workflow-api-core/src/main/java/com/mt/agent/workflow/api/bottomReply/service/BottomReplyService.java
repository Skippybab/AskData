package com.mt.agent.workflow.api.bottomReply.service;

import io.reactivex.Flowable;

public interface BottomReplyService {

    /**
     * 兜底回复
     *
     * @param question  用户提问问题
     * @param dialogHistory 对话历史
     * @param executions 执行结果
     * @param taskName 任务名称
     * @param userId 用户ID
     * @return
     */
    String replyForExecution(String question, String dialogHistory, String executions, String taskName, String userId, String background);

}
