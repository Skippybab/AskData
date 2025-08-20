package com.mt.agent.test.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规划器测试数据模型
 * 对应test-planner.xlsx中的数据结构
 *
 * @author MT Agent Team
 * @version 1.0
 * @since 2025-01-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlannerTestData {
    /**
     * 问题编号 (格式: 1-1 表示第一轮对话的第一次提问)
     */
    private String questionId;

    /**
     * 用户输入
     */
    private String userInput;

    /**
     * 历史输入
     */
    private String historyInput;

    /**
     * 上一轮规划
     */
    private String lastPlan;

    /**
     * 针对上一轮的问题回复内容
     */
    private String lastReply;
}