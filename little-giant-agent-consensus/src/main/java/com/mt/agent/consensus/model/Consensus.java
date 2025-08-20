package com.mt.agent.consensus.model;

import com.mt.agent.consensus.model.consensus.TaskInput;
import com.mt.agent.consensus.model.consensus.TaskName;
import com.mt.agent.consensus.model.consensus.TaskOutput;
import com.mt.agent.consensus.model.consensus.TaskSteps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 共识数据模型
 * 不再使用内部类的方式，将相关数据结构拆分成独立的类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Consensus implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 状态
     */
    private String status;

    /**
     * 任务名称相关信息
     */
    private TaskName taskName;

    /**
     * 任务输出相关信息
     */
    private TaskOutput taskOutput;

    /**
     * 任务输入相关信息
     */
    private TaskInput taskInput;

    /**
     * 任务步骤相关信息
     */
    private TaskSteps taskSteps;
}
