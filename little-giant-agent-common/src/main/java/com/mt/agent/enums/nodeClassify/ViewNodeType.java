package com.mt.agent.enums.nodeClassify;

import lombok.Getter;

/**
 * 可视化节点类型枚举
 */
@Getter
public enum ViewNodeType {

    METRIC_SCALE_BLOCK("vc1", "指标刻度块"),
    METRIC_SCALE_GROWTH_BLOCK("vc2", "指标刻度增长率块"),
    TREND_CHART("vc3", "趋势图"),
    METRIC_SCALE_BLOCK_YEAR("vc4", "带年份的指标刻度块"),
    BAR_LINE_CHART("vc5", "柱状折线混合图"),
    PIE_CHART("vc6", "饼图"),
    TEXT_LIST("vc9", "文本列表"),
    AI_MD_TEXT("vc10", "ai返回的文本，支持md格式解析");

    private final String code;
    private final String name;

    ViewNodeType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ViewNodeType getByCode(String code) {
        for (ViewNodeType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}