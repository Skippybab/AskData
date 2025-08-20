package com.mt.agent.enums.consensus;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 共识状态枚举
 * 用于标识任务名称、输入、输出、步骤等共识信息的状态
 */
@Getter
@AllArgsConstructor
public enum ConsensusStatus {

    UNKNOWN("未知", "共识信息尚未获取或无效"),
    KNOWN("已知", "共识信息已获取但尚未确认"),
    CONFIRMED("已确认", "共识信息已确认并可用于后续处理");

    /**
     * 状态名称
     */
    private final String name;

    /**
     * 状态描述
     */
    private final String description;
}