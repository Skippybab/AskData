package com.mt.agent.enums.consensus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonStatus {
    // 1: 初始状态, 2: 共识补充中 3: 执行中, 4: 确认问题
    INIT(1, "初始状态"),
    CONSENSUS_SUPPLEMENT(2, "共识补充中"),
    EXECUTING(3, "执行中"),
    CONFIRM_QUESTION(4, "确认问题");

    private final int type;
    private final String name;

}
