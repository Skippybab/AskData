package com.mt.agent.enums;

import lombok.Getter;

/**
 * 节点类型枚举
 */
@Getter
public enum NodeType {
    DATA_QUERY("a", "数据查询节点"),
    CALCULATION("b", "计算节点"),
    VISUALIZATION("c", "可视化节点"),
    AI_CHAT("d", "文本理解节点");

    private final String code;
    private final String description;

    NodeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码获取枚举值
     *
     * @param code 编码
     * @return 枚举值
     */
    public static NodeType getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (NodeType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}