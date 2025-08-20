package com.mt.agent.service;

import com.mt.agent.model.ChatMessage;
import com.mt.agent.reporter.SubEventReporter;

import java.util.List;

public interface AIChatService {
    /**
     * 保存用户的聊天历史记录
     * 
     * @param userId      用户ID
     * @param chatHistory 聊天历史记录列表
     */
    void saveChatHistory(Long userId, List<ChatMessage> chatHistory);

    /**
     * 加载用户的聊天历史记录
     * 
     * @param userId 用户ID
     * @return 聊天历史记录列表
     */
    List<ChatMessage> loadChatHistory(Long userId);

    /**
     * 重置用户的聊天历史记录
     *
     * @param userId 用户ID
     */
    void resetChatHistory(Long userId);

    /**
     * 规划新任务
     * 当共识状态全部为未知时调用此方法
     *
     * @param message  用户消息
     * @param userId   用户ID
     * @param reporter 事件报告器
     * @return 任务规划结果
     */
    String planNewTask(String message, String userId, SubEventReporter reporter);

    /**
     * 调整任务规划
     * 当共识状态存在已知但未全部确认时调用此方法
     *
     * @param message  用户消息
     * @param userId   用户ID
     * @param reporter 事件报告器
     * @return 调整后的任务规划结果
     */
    String adjustTaskPlan(String message, String userId, SubEventReporter reporter);

    /**
     * 判断用户是否同意任务规划
     *
     * @param message 用户消息
     * @param userId  用户ID
     * @return 是否同意任务规划
     */
    boolean judgeUserAgree(String message, String userId);
}
