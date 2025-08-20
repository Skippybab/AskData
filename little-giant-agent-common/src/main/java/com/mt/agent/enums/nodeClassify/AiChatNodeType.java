package com.mt.agent.enums.nodeClassify;

import lombok.Getter;

@Getter
public enum AiChatNodeType {

    TEXT_LIST("ac1", "系统功能问询");

    private final String code;
    private final String name;

    AiChatNodeType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static AiChatNodeType getByCode(String code) {
        for (AiChatNodeType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

}
