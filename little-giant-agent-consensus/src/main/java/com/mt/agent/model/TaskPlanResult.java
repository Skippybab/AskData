package com.mt.agent.model;

import com.mt.agent.model.workflow.TaskWorkflow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 任务规划结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskPlanResult implements Serializable {

    /**
     * 是否执行成功
     */
    private Boolean success;

    /**
     * 追问文本
     */
    private String followUpQuestion;

    /**
     * 工作流信息
     */
    private TaskWorkflow workflow;

}