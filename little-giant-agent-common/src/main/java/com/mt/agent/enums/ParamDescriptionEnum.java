package com.mt.agent.enums;

import lombok.Getter;

/**
 * 参数描述枚举
 * 用于存储工作流参数的字段名称和描述信息
 */
@Getter
public enum ParamDescriptionEnum {
    YEAR("year", "用户想要查询的年份"),
    INDUSTRY("industry", "用户最想要了解的行业"),
    CITY("city", "用户想要了解的行业所在的城市"),
    REGION("region", "用户想要了解的行业所在的区县");

    private final String fieldName;
    private final String description;

    ParamDescriptionEnum(String fieldName, String description) {
        this.fieldName = fieldName;
        this.description = description;
    }

    /**
     * 根据字段名获取对应的枚举值
     *
     * @param fieldName 字段名
     * @return 对应的枚举值，如果未找到则返回null
     */
    public static ParamDescriptionEnum getByFieldName(String fieldName) {
        if (fieldName == null) {
            return null;
        }
        for (ParamDescriptionEnum param : values()) {
            if (param.getFieldName().equals(fieldName)) {
                return param;
            }
        }
        return null;
    }
}