package com.mt.agent.enums.nodeClassify;

import lombok.Getter;

/**
 * 可视化节点类型枚举
 */
@Getter
public enum DataQueryNodeType {

    INDUSTRY_YEAR_INDEX("dc1", "查询行业指标年规模"),
    INDUSTRY_LAST_YEAR_INDEX("dc2", "查询行业指标上一年规模"),
    INDUSTRY_INDEX_TREND("dc3", "查询行业指标趋势"),
    INDUSTRY_LAST_TWO_YEAR_INDEX("dc4", "查询行业指标前年规模"),
    INDUSTRY_REGION_INDEX("dc5", "查询行业指标区域数据"),
    ;

    private final String code;
    private final String name;

    DataQueryNodeType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static DataQueryNodeType getByCode(String code) {
        for (DataQueryNodeType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}