package com.mt.agent.model.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务步骤定义
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskStep implements Serializable {

    /**
     * 执行步骤编号
     */
    private String num;

    /**
     * 任务步骤描述
     */
    private String stepName;

    /**
     * 对应的功能或可视化工具名称
     */
    private String funName;

    /**
     * 入参列表
     */
    private List<String> inputs;

    /**
     * 出参列表
     */
    private List<String> outputs;

} 