package com.mt.agent.enums;

import lombok.Getter;

/**
 * 类描述
 *
 * @author lfz
 * @date 2025/3/25 19:18
 */
@Getter
public enum ViewComponentType {

    BARCHART("BarChart", "柱状图组件"),
    DOUBLE_BARCHART("DoubleBarChart", "二分组柱状图组件"),
    INDICATOR_BLOCK("IndicatorBlock", "指标规模信息块"),
    INDICATOR_CHANGE_BLOCK("IndicatorChangeBlock", "指标规模增长信息块"),
    BAR_LINE_CHART("BarLineChart", "柱状折线混合图"),
    PIE_CHART("PieChart", "饼图"),
    TEXT("text", "纯文本"),
    MD_TEXT("MdText", "md格式的文本"),
    TWO_D_TABLE("TwoDTable", "二维表格"),
    ;

    private final String code;
    private final String name;

    ViewComponentType(String code, String name) {
        this.code = code;
        this.name = name;
    }

}
