package com.mt.agent.consensus.model.consensus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 任务单个步骤信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskStep implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 步骤编号
     */
    private int stepNo;

    /**
     * 步骤名称
     */
    private String stepName;

    /**
     * 步骤输入参数需求
     */
    private List<String> requirements;

    /**
     * 步骤输出
     */
    private List<String> output;
}