package com.mt.agent.enums.nodeClassify;

import lombok.Getter;

/**
 * 计算节点类型枚举
 */
@Getter
public enum CalculateNodeType {

    GROWTH_RATE("cc1", "计算增长率"),
    CHANGER("cc2", "计算变化量"),
    PERCENTAGE("cc3", "计算占比"),
    GROWTH_RATE_TREND("cc4", "计算增长率趋势"),
    AVG("cc5", "计算平均值"),
    MAX_GROWTH_RATE("cc6", "计算增长率并找到最大值"),
    MIN_GROWTH_RATE("cc7", "计算增长率并找到最小值"),
    ;


    private final String code; // 类型编码
    private final String name; // 类型名称

    CalculateNodeType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static CalculateNodeType getByCode(String code) {
        for (CalculateNodeType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

}