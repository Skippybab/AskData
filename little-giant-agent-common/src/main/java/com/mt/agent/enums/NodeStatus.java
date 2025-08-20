package com.mt.agent.enums;

import lombok.Getter;

/**
 * 节点状态枚举
 */
@Getter
public enum NodeStatus {
    INACTIVE(0, "未激活"),
    ACTIVATED(1, "已激活"),
    RUNNING(2, "执行中"),
    COMPLETED(3, "执行完成"),
    FAILED(4, "执行失败");

    private final Integer code;
    private final String description;

    NodeStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码获取枚举值
     * 
     * @param code 编码
     * @return 枚举值
     */
    public static NodeStatus getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (NodeStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}