package com.mt.agent.test.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 执行器测试结果模型
 * 对应executor-result-caseXX.xlsx中的数据结构
 *
 * @author MT Agent Team
 * @version 1.0
 * @since 2025-01-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutorTestResult {
    /**
     * 问题编号
     */
    private String questionId;

    /**
     * 测试轮数
     */
    private Integer roundNumber;

    /**
     * SQL编号（SQL1、SQL2等，表示Python代码中的第几个gen_sql调用）
     */
    private String sqlNumber;

    /**
     * 用户输入
     */
    private String userInput;

    /**
     * 历史输入
     */
    private String historyInput;

    /**
     * Python代码
     */
    private String pythonCode;

    /**
     * SQL描述文本
     */
    private String sqlDescription;

    /**
     * 查询的表名
     */
    private String tableName;

    /**
     * 生成的SQL语句
     */
    private String sqlStatement;

    /**
     * SQL生成花费时间（毫秒）
     */
    private Long sqlGenerateTime;

    /**
     * SQL是否执行成功
     */
    private Boolean sqlExecuteSuccess;

    /**
     * SQL执行结果
     */
    private String sqlExecuteResult;

    /**
     * SQL错误日志
     */
    private String sqlErrorLog;
}