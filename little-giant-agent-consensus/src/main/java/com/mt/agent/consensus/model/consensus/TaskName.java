package com.mt.agent.consensus.model.consensus;

import com.mt.agent.enums.consensus.ConsensusStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 任务名称信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskName implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 状态：未知、已知、已确认
     */
    private ConsensusStatus status;
}