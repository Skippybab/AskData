package com.cultivate.ai.model;


import com.cultivate.ai.enums.SiliconFlowModelType;
import lombok.Builder;
import lombok.Data;

/**
 * 硅基流动AI请求配置类
 * 用于封装硅基流动平台所有可能的大模型调用参数
 */
@Data
@Builder
public class SiliconFlowRequestConfig {
    /**
     * 模型类型
     */
    @Builder.Default
    private SiliconFlowModelType modelType = SiliconFlowModelType.getDefaultModel();

    /**
     * 温度参数，控制输出的随机性
     * 范围0-1，越大随机性越高，创造性越强；越小越确定性
     */
    @Builder.Default
    private float temperature = 0.0f;

    /**
     * Top-P采样参数，控制输出的多样性
     * 范围0-1，越大多样性越高
     */
    @Builder.Default
    private double topP = 0.01;

    /**
     * Top-K采样参数，控制输出的多样性
     * 越大多样性越高
     */
    @Builder.Default
    private Integer topK = 50;

    /**
     * 频率惩罚参数，用于减少重复内容
     * 范围0-2，越大越减少重复
     */
    @Builder.Default
    private double frequencyPenalty = 0.0;

    /**
     * 最大生成token数
     * 如果为null则使用模型默认值
     */
    @Builder.Default
    private Integer maxTokens = 2048;

    /**
     * 是否启用思考过程
     * 只有支持思考能力的模型才能启用该功能
     */
    @Builder.Default
    private boolean enableThinking = false;

    /**
     * 是否启用流式输出
     * 用于流式调用时的渐进式返回
     */
    @Builder.Default
    private boolean stream = false;

    /**
     * 结果格式
     * message: 对话格式
     * text: 纯文本格式
     */
    @Builder.Default
    private String resultFormat = "message";

    /**
     * 是否自动切换模型
     * 当所选模型不支持某项功能时，是否自动切换到支持该功能的模型
     */
    @Builder.Default
    private boolean autoSwitchModel = false;

    /**
     * 调用超时时间（秒）
     * 用于控制等待模型响应的最大时间
     * 对于复杂问题或需要长时间思考的场景，建议设置更长的超时时间
     */
    @Builder.Default
    private int timeoutSeconds = 300; // 默认5分钟超时

    /**
     * 创建一个默认配置
     * 
     * @return 默认配置对象
     */
    public static SiliconFlowRequestConfig defaultConfig() {
        return SiliconFlowRequestConfig.builder().build();
    }

    /**
     * 创建一个针对思考模式优化的配置
     * 
     * @return 思考模式配置对象
     */
    public static SiliconFlowRequestConfig thinkingConfig() {
        return SiliconFlowRequestConfig.builder()
                .modelType(SiliconFlowModelType
                        .getModelWithCapability(com.cultivate.ai.enums.ModelCapability.SUPPORTS_THINKING))
                .enableThinking(true)
                .timeoutSeconds(600) // 思考模式默认10分钟超时
                .build();
    }

    /**
     * 创建一个针对流式输出优化的配置
     * 
     * @return 流式输出配置对象
     */
    public static SiliconFlowRequestConfig streamingConfig() {
        return SiliconFlowRequestConfig.builder()
                .stream(true)
                .timeoutSeconds(300) // 流式输出默认5分钟超时
                .build();
    }

    /**
     * 创建一个更具创造性的配置（高温度参数）
     * 
     * @return 创造性配置对象
     */
    public static SiliconFlowRequestConfig creativeConfig() {
        return SiliconFlowRequestConfig.builder()
                .temperature(0.8f)
                .topP(0.95)
                .build();
    }

    /**
     * 创建一个针对代码生成优化的配置
     * 
     * @return 代码生成配置对象
     */
    public static SiliconFlowRequestConfig codingConfig() {
        return SiliconFlowRequestConfig.builder()
                .temperature(0.1f)
                .topP(0.3)
                .build();
    }
}