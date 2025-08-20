package com.mt.agent.enums.consensus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ContextFields {
    YEAR("year", "年份"),
    CITY("city", "城市"),
    REGION("region", "地区"),
    INDUSTRY("industry", "行业"),
    ENTERPRISE("enterprise", "企业"),
    INDEX("index", "指标");

    private final String fieldName;
    private final String name;

    public static ContextFields getByFieldName(String fieldName) {
        for (ContextFields field : values()) {
            if (field.fieldName.equals(fieldName)) {
                return field;
            }
        }
        return null;
    }
}
