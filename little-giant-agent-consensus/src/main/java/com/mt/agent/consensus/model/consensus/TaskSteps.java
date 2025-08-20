package com.mt.agent.consensus.model.consensus;

import com.mt.agent.enums.consensus.ConsensusStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 任务步骤信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskSteps implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 步骤列表
     */
    private List<TaskStep> steps;

    /**
     * 状态：未知、已知、已确认
     */
    private ConsensusStatus status;
}