package com.mt.agent.workflow.api.ai.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * 模型类型枚举
 * 用于统一管理所有支持的模型类型及其能力
 *
 * @author AI架构师
 */
public enum AliModelType {

    /**
     * 阿里云大模型
     */
    QWEN_PLUS("qwen-plus", "阿里云-通义千问plus",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING,
                    ModelCapability.DEFAULT_MODEL)),

    QWEN2_PLUS("qwen-plus-2025-01-25", "阿里云-通义千问2-plus",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    QWEN_MAX("qwen-max", "阿里云-通义千问max",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    QWEN_TURBO("qwen-turbo", "阿里云-通义千问turbo",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.STREAMING_ONLY)),

    DEEPSEEK_V3("deepseek-v3", "阿里云-deepseek_v3",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    DEEPSEEK_R1("deepseek-r1", "阿里云-deepseek_r1",
            EnumSet.of(ModelCapability.SUPPORTS_STREAMING, ModelCapability.THINKING_ONLY)),

    QWQ_32B("qwq-32b", "阿里云-qwq-32b",
            EnumSet.of(ModelCapability.SUPPORTS_STREAMING,ModelCapability.THINKING_ONLY)),

    DEEPSEEK_R1_DISTILL_QWEN_14B("deepseek-r1-distill-qwen-14b", "阿里云-deepseek_r1_distill_qwen_14b",
            EnumSet.of(ModelCapability.SUPPORTS_STREAMING, ModelCapability.SUPPORTS_THINKING)),

    DEEPSEEK_R1_DISTILL_QWEN_7B("deepseek-r1-distill-qwen-7b", "阿里云-deepseek_r1_distill_qwen_7b",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC,ModelCapability.SUPPORTS_STREAMING, ModelCapability.SUPPORTS_THINKING)),

    QWEN_25_72B_INSTRUCT("qwen2.5-72b-instruct", "阿里云-qwen2.5-72b-instruct",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    QWEN_25_32B_INSTRUCT("qwen2.5-32b-instruct", "阿里云-qwen2.5-32b-instruct",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    QWEN_25_14B_INSTRUCT("qwen2.5-14b-instruct", "阿里云-qwen2.5-14b-instruct",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    QWEN_25_7B_INSTRUCT("qwen2.5-7b-instruct", "阿里云-qwen2.5-7b-instruct",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    QWEN_3_4B_INSTRUCT("qwen3-4b", "阿里云-qwen3-4b",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.STREAMING_ONLY)),

    QWEN_3_8B_INSTRUCT("qwen3-8b", "阿里云-qwen3-8b",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.STREAMING_ONLY)),

    QWEN_3_14B_INSTRUCT("qwen3-14b", "阿里云-qwen3-14b",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.STREAMING_ONLY,
                    ModelCapability.SUPPORTS_THINKING)),

    QWEN_3_32B_INSTRUCT("qwen3-32b", "阿里云-qwen3-32b",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.STREAMING_ONLY,
                    ModelCapability.SUPPORTS_THINKING)),

    QWEN_3_235B_INSTRUCT("qwen3-235b-a22b", "阿里云-qwen3-235b",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.STREAMING_ONLY,
                    ModelCapability.SUPPORTS_THINKING)),

    QWEN_25_CODER_32B_INSTRUCT("qwen2.5-coder-32b-instruct", "阿里云-qwen2.5-coder-32b-instruct",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    ;

    /**
     * 模型类型编码
     */
    private final String code;

    /**
     * 模型类型名称
     */
    private final String name;

    /**
     * 模型支持的能力集合
     */
    private final Set<ModelCapability> capabilities;

    AliModelType(String code, String name, Set<ModelCapability> capabilities) {
        this.code = code;
        this.name = name;
        this.capabilities = capabilities;
    }

    /**
     * 获取模型类型编码
     *
     * @return 模型类型编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取模型类型名称
     *
     * @return 模型类型名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取模型支持的能力集合
     * 
     * @return 能力集合
     */
    public Set<ModelCapability> getCapabilities() {
        return capabilities;
    }

    /**
     * 检查模型是否支持指定能力
     * 
     * @param capability 要检查的能力
     * @return 如果支持返回true，否则返回false
     */
    public boolean supportsCapability(ModelCapability capability) {
        return capabilities.contains(capability);
    }

    /**
     * 获取默认推荐模型
     * 
     * @return 默认推荐的模型类型
     */
    public static AliModelType getDefaultModel() {
        for (AliModelType aliModelType : values()) {
            if (aliModelType.supportsCapability(ModelCapability.DEFAULT_MODEL)) {
                return aliModelType;
            }
        }
        // 如果没有标记为默认的模型，返回QWEN_TURBO作为备选
        return QWEN_TURBO;
    }

    /**
     * 获取支持指定能力的第一个模型
     * 
     * @param capability 需要的能力
     * @return 支持该能力的模型，如果没有找到返回默认模型
     */
    public static AliModelType getModelWithCapability(ModelCapability capability) {
        for (AliModelType aliModelType : values()) {
            if (aliModelType.supportsCapability(capability)) {
                return aliModelType;
            }
        }
        return getDefaultModel();
    }

    /**
     * 根据编码获取模型类型枚举
     *
     * @param code 模型类型编码
     * @return 对应的模型类型枚举，如果不存在则返回null
     */
    public static AliModelType fromCode(String code) {
        if (code == null) {
            return null;
        }

        for (AliModelType aliModelType : AliModelType.values()) {
            if (aliModelType.getCode().equals(code)) {
                return aliModelType;
            }
        }

        return null;
    }

    /**
     * 判断给定的编码是否为有效的模型类型
     *
     * @param code 模型类型编码
     * @return 如果是有效的模型类型则返回true，否则返回false
     */
    public static boolean isValidCode(String code) {
        return fromCode(code) != null;
    }

    /**
     * 检查模型是否仅支持思考模式
     * 
     * @return 如果仅支持思考模式返回true，否则返回false
     */
    public boolean isThinkingOnly() {
        return capabilities.contains(ModelCapability.THINKING_ONLY);
    }

    /**
     * 检查模型是否支持思考功能（包括仅支持思考和支持思考的模型）
     * 
     * @return 如果支持思考功能返回true，否则返回false
     */
    public boolean canThink() {
        return capabilities.contains(ModelCapability.SUPPORTS_THINKING) ||
                capabilities.contains(ModelCapability.THINKING_ONLY);
    }

    /**
     * 获取第一个仅支持思考模式的模型
     * 
     * @return 仅支持思考模式的模型，如果没有找到返回null
     */
    public static AliModelType getThinkingOnlyModel() {
        for (AliModelType aliModelType : values()) {
            if (aliModelType.isThinkingOnly()) {
                return aliModelType;
            }
        }
        return null;
    }
}
