package com.mt.agent.test.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 执行器测试数据模型
 * 对应test-executor.xlsx中的数据结构
 *
 * @author MT Agent Team
 * @version 1.0
 * @since 2025-01-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutorTestData {
    /**
     * 问题编号
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
     * Python代码
     */
    private String pythonCode;
}