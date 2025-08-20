package com.mt.agent.consensus.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 共识状态管理
 *
 * @author lfz
 * @date 2025/5/17 14:41
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsensusStatusManage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 未有任务
     */
    public static final int NO_TASK = 0;

    /**
     * 任务进行中
     */
    public static final int TASK_IN_PROGRESS = 1;

    /**
     * 任务完成
     */
    public static final int TASK_COMPLETED = 2;

    /**
     * 共识对话状态
     * 0.未有任务 1.任务进行中 2.任务完成
     */
    private Integer dialogStatus;

    /**
     * 当前共识序号
     */
    private Integer currentConsensusId;

    /**
     * 共识管理区
     */
    private List<ConsensusItem> consensusItems;


}
