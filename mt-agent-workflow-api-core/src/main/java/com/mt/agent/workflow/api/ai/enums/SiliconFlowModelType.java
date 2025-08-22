package com.mt.agent.workflow.api.ai.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * 硅基流动平台模型类型枚举
 * 用于统一管理硅基流动平台支持的所有模型类型及其能力
 *
 * @author AI架构师
 */
public enum SiliconFlowModelType {

    /**
     * DeepSeek 系列模型
     */
    DEEPSEEK_R1("deepseek-ai/DeepSeek-R1", "硅基流动-DeepSeek-R1",
            EnumSet.of(ModelCapability.SUPPORTS_STREAMING, ModelCapability.THINKING_ONLY)),

    DEEPSEEK_R1_PRO("Pro/deepseek-ai/DeepSeek-R1", "硅基流动-DeepSeek-R1-Pro",
            EnumSet.of(ModelCapability.SUPPORTS_STREAMING, ModelCapability.THINKING_ONLY)),

    DEEPSEEK_V3("deepseek-ai/DeepSeek-V3", "硅基流动-DeepSeek-V3",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    DEEPSEEK_V3_PRO("Pro/deepseek-ai/DeepSeek-V3", "硅基流动-DeepSeek-V3-Pro",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    DEEPSEEK_R1_DISTILL_QWEN_32B("deepseek-ai/DeepSeek-R1-Distill-Qwen-32B", "硅基流动-DeepSeek-R1-Distill-Qwen-32B",
            EnumSet.of(ModelCapability.SUPPORTS_STREAMING, ModelCapability.SUPPORTS_THINKING)),

    DEEPSEEK_R1_DISTILL_QWEN_14B("deepseek-ai/DeepSeek-R1-Distill-Qwen-14B", "硅基流动-DeepSeek-R1-Distill-Qwen-14B",
            EnumSet.of(ModelCapability.SUPPORTS_STREAMING, ModelCapability.SUPPORTS_THINKING)),

    DEEPSEEK_R1_DISTILL_QWEN_7B("deepseek-ai/DeepSeek-R1-Distill-Qwen-7B", "硅基流动-DeepSeek-R1-Distill-Qwen-7B",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING,
                    ModelCapability.SUPPORTS_THINKING)),

    DEEPSEEK_R1_DISTILL_QWEN_7B_PRO("Pro/deepseek-ai/DeepSeek-R1-Distill-Qwen-7B",
            "硅基流动-DeepSeek-R1-Distill-Qwen-7B-Pro",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING,
                    ModelCapability.SUPPORTS_THINKING)),

    DEEPSEEK_V2_5("deepseek-ai/DeepSeek-V2.5", "硅基流动-DeepSeek-V2.5",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    DEEPSEEK_CODER_V2("deepseek-ai/DeepSeek-Coder-V2", "硅基流动-DeepSeek-Coder-V2",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    /**
     * Qwen 系列模型
     */
    QWEN_2_5_72B_INSTRUCT("Qwen/Qwen2.5-72B-Instruct", "硅基流动-Qwen2.5-72B-Instruct",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING,
                    ModelCapability.DEFAULT_MODEL)),

    QWEN_2_5_32B_INSTRUCT("Qwen/Qwen2.5-32B-Instruct", "硅基流动-Qwen2.5-32B-Instruct",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    QWEN_2_5_14B_INSTRUCT("Qwen/Qwen2.5-14B-Instruct", "硅基流动-Qwen2.5-14B-Instruct",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    QWEN_2_5_7B_INSTRUCT("Qwen/Qwen2.5-7B-Instruct", "硅基流动-Qwen2.5-7B-Instruct",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    QWEN_2_5_CODER_32B_INSTRUCT("Qwen/Qwen2.5-Coder-32B-Instruct", "硅基流动-Qwen2.5-Coder-32B-Instruct",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    QWEN_2_5_CODER_14B_INSTRUCT("Qwen/Qwen2.5-Coder-14B-Instruct", "硅基流动-Qwen2.5-Coder-14B-Instruct",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    QWEN_2_5_CODER_7B_INSTRUCT("Qwen/Qwen2.5-Coder-7B-Instruct", "硅基流动-Qwen2.5-Coder-7B-Instruct",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    QWEN_2_5_MATH_72B_INSTRUCT("Qwen/Qwen2.5-Math-72B-Instruct", "硅基流动-Qwen2.5-Math-72B-Instruct",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    QWEN_2_72B_INSTRUCT("Qwen/Qwen2-72B-Instruct", "硅基流动-Qwen2-72B-Instruct",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    QWEN_2_57B_A14B_INSTRUCT("Qwen/Qwen2-57B-A14B-Instruct", "硅基流动-Qwen2-57B-A14B-Instruct",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    QWEN_2_VL_72B_INSTRUCT("Qwen/Qwen2-VL-72B-Instruct", "硅基流动-Qwen2-VL-72B-Instruct",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    QWQ_32B_PREVIEW("Qwen/QwQ-32B-Preview", "硅基流动-QwQ-32B-Preview",
            EnumSet.of(ModelCapability.SUPPORTS_STREAMING, ModelCapability.THINKING_ONLY)),

    QWEN_3_235B_A22B("Qwen/Qwen3-235B-A22B", "硅基流动-Qwen3-235B-A22B",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),

    QWEN_3_32B("Qwen/Qwen3-32B", "硅基流动-Qwen3-32B",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING, ModelCapability.SUPPORTS_THINKING)),

    QWEN_3_30B_A3B("Qwen/Qwen3-30B-A3B", "硅基流动-Qwen3-30B-A3B",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING, ModelCapability.SUPPORTS_THINKING)),

    QWEN_3_14B("Qwen/Qwen3-14B", "硅基流动-Qwen3-14B",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING, ModelCapability.SUPPORTS_THINKING)),

    QWEN_3_8B("Qwen/Qwen3-8B", "硅基流动-Qwen3-8B",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING, ModelCapability.SUPPORTS_THINKING)),

    Qwen3_Embedding_8B("Qwen/Qwen3-Embedding-8B", "硅基流动-Qwen3-Embedding-8B",
            EnumSet.of(ModelCapability.SUPPORTS_SYNC, ModelCapability.SUPPORTS_STREAMING)),
    BAAI_BGE_LARGE_ZH_V1_5("BAAI/bge-large-zh-v1.5", "硅基流动-BaiDu-BGE-Large-ZH-V1.5",
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

    SiliconFlowModelType(String code, String name, Set<ModelCapability> capabilities) {
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
    public static SiliconFlowModelType getDefaultModel() {
        for (SiliconFlowModelType modelType : values()) {
            if (modelType.supportsCapability(ModelCapability.DEFAULT_MODEL)) {
                return modelType;
            }
        }
        // 如果没有标记为默认的模型，返回QWEN_2_5_72B_INSTRUCT作为备选
        return QWEN_2_5_72B_INSTRUCT;
    }

    /**
     * 获取支持指定能力的第一个模型
     *
     * @param capability 需要的能力
     * @return 支持该能力的模型，如果没有找到返回默认模型
     */
    public static SiliconFlowModelType getModelWithCapability(ModelCapability capability) {
        for (SiliconFlowModelType modelType : values()) {
            if (modelType.supportsCapability(capability)) {
                return modelType;
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
    public static SiliconFlowModelType fromCode(String code) {
        if (code == null) {
            return null;
        }

        for (SiliconFlowModelType modelType : SiliconFlowModelType.values()) {
            if (modelType.getCode().equals(code)) {
                return modelType;
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
    public static SiliconFlowModelType getThinkingOnlyModel() {
        for (SiliconFlowModelType modelType : values()) {
            if (modelType.isThinkingOnly()) {
                return modelType;
            }
        }
        return null;
    }
}