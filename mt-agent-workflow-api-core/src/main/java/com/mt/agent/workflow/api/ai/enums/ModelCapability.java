package com.mt.agent.workflow.api.ai.enums;

/**
 * 模型能力枚举
 * 用于标记模型支持的功能和能力
 */
public enum ModelCapability {
    /**
     * 支持同步调用（非流式输出）
     */
    SUPPORTS_SYNC("支持同步调用"),

    /**
     * 支持流式输出
     */
    SUPPORTS_STREAMING("支持流式输出"),

    /**
     * 仅支持流式输出，不支持同步调用
     */
    STREAMING_ONLY("仅支持流式输出"),

    /**
     * 支持思考模式
     */
    SUPPORTS_THINKING("支持思考模式"),

    /**
     * 仅支持思考模式，不支持普通对话
     */
    THINKING_ONLY("仅支持思考模式"),

    /**
     * 建议作为默认选择的模型
     */
    DEFAULT_MODEL("默认推荐模型");

    private final String description;

    ModelCapability(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}