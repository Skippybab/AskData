package com.mt.agent.enums.nodeClassify;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 查询结果类型枚举
 */
@Getter
@AllArgsConstructor
public enum ResultType {
    SUM(1, "汇总数据"),
    TREND(2, "趋势数据"),
    REGION(3, "区域数据"),
    LIST(4, "列表数据"),
    STRING_DATA(5, "一个对象值，包含String和data"),
    INDEX_LIST(6, "支持分析的指标列表"),
    ;

    private final int code;
    private final String desc;

    public static ResultType getByCode(int code) {
        for (ResultType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
} 