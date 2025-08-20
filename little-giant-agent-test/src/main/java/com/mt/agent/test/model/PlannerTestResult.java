package com.mt.agent.test.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规划器测试结果模型
 * 对应planner-result-caseXX.xlsx中的数据结构
 *
 * @author MT Agent Team
 * @version 1.0
 * @since 2025-01-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlannerTestResult {
    /**
     * 问题编号
     */
    private String questionId;

    /**
     * 测试轮数
     */
    private Integer roundNumber;

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

    /**
     * 是否生成python代码
     */
    private Boolean generatePythonCode;

    /**
     * Python代码
     */
    private String pythonCode;

    /**
     * 规划花费时间（毫秒）
     */
    private Long planTime;

    /**
     * Python是否执行成功
     */
    private Boolean pythonExecuteSuccess;

    /**
     * Python执行结果
     */
    private String pythonExecuteResult;

    /**
     * 执行异常错误日志
     */
    private String errorLog;
}