package com.mt.agent.consensus.model.consensus;

import com.mt.agent.enums.consensus.ConsensusStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 任务输出信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskOutput implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 输出项列表
     */
    private List<OutputItem> output;

    /**
     * 状态：未知、已知、已确认
     */
    private ConsensusStatus status;
}