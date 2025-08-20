package com.mt.agent.consensus.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsensusItem implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 未知
     */
    public static final int UNKNOWN = 0;

    /**
     * 待确认
     */
    public static final int KNOWN = 1;

    /**
     * 已确认
     */
    public static final int CONFIRMED = 2;

    /**
     * 共识名称
     */
    private String name;

    /**
     * 共识序号
     */
    private Integer id;

    /**
     * 共识状态
     * 0.未开始 1.执行中 2.待确认 3.已确认 4.执行完成
     */
    private Integer status;

    /**
     * 任务参数列表
     */
    private List<ConsensusParameterItem> parameters;



}
