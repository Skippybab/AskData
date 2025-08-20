package com.mt.agent.bottomReply.service;

public interface BottomReplyService {

    String reply(String reason, String userId);

    String replyForExecution(String question, String dialogHistory, String executions, String taskName, String userId);

    public String replyForExecutionSilicon(String question, String dialogHistory, String executions, String taskName, String userId);
}
