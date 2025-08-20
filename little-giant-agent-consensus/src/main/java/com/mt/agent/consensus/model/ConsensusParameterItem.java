package com.mt.agent.consensus.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsensusParameterItem implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 未知
     */
    public static final int UNKNOWN = 0;

    /**
     * 待确认
     */
    public static final int TO_BE_CONFIRMED = 1;

    /**
     * 已确认
     */
    public static final int CONFIRMED = 2;



    /**
     * 参数名称
     */
    private String name;

    /**
     * 状态
     * 0.未知 1.待确认 2.已确认
     */
    private Integer status;


}
