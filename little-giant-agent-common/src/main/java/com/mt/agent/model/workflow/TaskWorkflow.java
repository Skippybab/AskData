package com.mt.agent.model.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 任务工作流定义
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskWorkflow implements Serializable {

    /**
     * 工作流名称
     */
    private String taskName;

    /**
     * 步骤列表
     */
    private List<TaskStep> steps;
} 