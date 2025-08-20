package com.mt.agent.enums;

import lombok.Getter;

/**
 * 参数来源类型
 */
@Getter
public enum ParamSourceType {
    DEFAULT_VALUE(1, "默认值"),
    INTENTION_SOURCE(2, "意图识别来源"),
    NODE_SOURCE(3, "节点来源"),
    BUFFER_SOURCE(4, "缓存区来源");

    private final int code;
    private final String desc;

    ParamSourceType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ParamSourceType getByCode(Integer code) {
        for (ParamSourceType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return null;
    }
}