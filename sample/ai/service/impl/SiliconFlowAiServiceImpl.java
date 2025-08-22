package com.cultivate.ai.service.impl;

import com.cultivate.ai.enums.SiliconFlowModelType;
import com.cultivate.ai.model.SiliconFlowRequestConfig;
import com.cultivate.ai.model.impl.SiliconFlowChatClient;
import com.cultivate.ai.service.SiliconFlowAiService;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 硅基流动AI服务实现类
 * 实现各种大模型调用方式，包括同步调用、流式调用、思考过程获取等
 */
@Slf4j
@Service
public class SiliconFlowAiServiceImpl implements SiliconFlowAiService {
    private static final Integer DefaultMaxTokens = 2000;

    @Autowired
    private SiliconFlowChatClient siliconFlowChatClient;

    // ======================= 同步调用方法 =======================

    @Override
    public String chat(String message) {
        return chat(message, SiliconFlowRequestConfig.defaultConfig());
    }

    @Override
    public String chat(String message, SiliconFlowModelType modelType) {
        return chat(message, SiliconFlowRequestConfig.builder().modelType(modelType).build());
    }

    @Override
    public String chat(String message, SiliconFlowRequestConfig config) {
        log.debug("执行硅基流动同步调用 - 模型: {}, 消息长度: {}", config.getModelType().getCode(), message.length());
        try {
            return siliconFlowChatClient.createRequest()
                    .modelType(config.getModelType())
                    .message(message)
                    .temperature(config.getTemperature())
                    .topP(config.getTopP())
                    .topK(config.getTopK())
                    .frequencyPenalty(config.getFrequencyPenalty())
                    .maxTokens(config.getMaxTokens())
                    .enableThinking(config.isEnableThinking())
                    .stream(false) // 同步调用
                    .autoSwitchModel(config.isAutoSwitchModel())
                    .call();
        } catch (Exception e) {
            log.error("硅基流动同步调用出错 - 消息: {}, 错误: {}", message, e.getMessage(), e);
            throw new RuntimeException("硅基流动大模型调用失败: " + e.getMessage(), e);
        }
    }

    // ======================= 流式调用方法 =======================

    @Override
    public Flowable<String> chatStream(String message) {
        return chatStream(message, SiliconFlowRequestConfig.streamingConfig());
    }

    @Override
    public Flowable<String> chatStream(String message, SiliconFlowModelType modelType) {
        return chatStream(message, SiliconFlowRequestConfig.builder()
                .modelType(modelType)
                .stream(true)
                .build());
    }

    @Override
    public Flowable<String> chatStream(String message, SiliconFlowRequestConfig config) {
        log.debug("执行硅基流动流式调用 - 模型: {}, 消息长度: {}", config.getModelType().getCode(), message.length());
        try {
            return siliconFlowChatClient.createRequest()
                    .modelType(config.getModelType())
                    .message(message)
                    .temperature(config.getTemperature())
                    .topP(config.getTopP())
                    .topK(config.getTopK())
                    .frequencyPenalty(config.getFrequencyPenalty())
                    .maxTokens(config.getMaxTokens())
                    .enableThinking(false) // 流式调用不启用思考过程
                    .stream(true) // 流式调用
                    .autoSwitchModel(config.isAutoSwitchModel())
                    .stream();
        } catch (Exception e) {
            log.error("硅基流动流式调用出错 - 消息: {}, 错误: {}", message, e.getMessage(), e);
            return Flowable.error(new RuntimeException("硅基流动大模型流式调用失败: " + e.getMessage(), e));
        }
    }

    // ======================= 思考过程获取方法 =======================

    @Override
    public SiliconFlowChatClient.ThinkingResult chatWithThinking(String message) {
        return chatWithThinking(message, SiliconFlowRequestConfig.thinkingConfig());
    }

    @Override
    public SiliconFlowChatClient.ThinkingResult chatWithThinking(String message, SiliconFlowModelType modelType) {
        return chatWithThinking(message, SiliconFlowRequestConfig.builder()
                .modelType(modelType)
                .enableThinking(true)
                .build());
    }

    @Override
    public SiliconFlowChatClient.ThinkingResult chatWithThinking(String message, SiliconFlowRequestConfig config) {
        log.debug("执行硅基流动思考过程获取 - 模型: {}, 消息长度: {}", config.getModelType().getCode(), message.length());
        try {
            // 确保思考过程开启
            config.setEnableThinking(true);

            // 使用流式获取后阻塞获取完整结果
            return chatStreamWithThinking(message, config).getCompleteResult();
        } catch (Exception e) {
            log.error("硅基流动思考过程获取出错 - 消息: {}, 错误: {}", message, e.getMessage(), e);
            throw new RuntimeException("硅基流动思考过程获取失败: " + e.getMessage(), e);
        }
    }

    // ======================= 流式思考过程获取方法 =======================

    @Override
    public SiliconFlowChatClient.ThinkingStreamResult chatStreamWithThinking(String message) {
        return chatStreamWithThinking(message, SiliconFlowRequestConfig.thinkingConfig());
    }

    @Override
    public SiliconFlowChatClient.ThinkingStreamResult chatStreamWithThinking(String message,
            SiliconFlowModelType modelType) {
        return chatStreamWithThinking(message, SiliconFlowRequestConfig.builder()
                .modelType(modelType)
                .enableThinking(true)
                .stream(true)
                .build());
    }

    @Override
    public SiliconFlowChatClient.ThinkingStreamResult chatStreamWithThinking(String message,
            SiliconFlowRequestConfig config) {
        log.debug("执行硅基流动流式思考过程获取 - 模型: {}, 超时时间: {}秒",
                config.getModelType().getCode(), config.getTimeoutSeconds());
        try {
            // 确保思考过程和流式输出开启
            config.setEnableThinking(true);
            config.setStream(true);

            // 估算复杂度，动态调整超时时间
            int estimatedTimeout = estimateTimeout(message, config);
            log.debug("估算的超时时间: {}秒 (配置值: {}秒)", estimatedTimeout, config.getTimeoutSeconds());

            return siliconFlowChatClient.createRequest()
                    .modelType(config.getModelType())
                    .message(message)
                    .temperature(config.getTemperature())
                    .topP(config.getTopP())
                    .topK(config.getTopK())
                    .frequencyPenalty(config.getFrequencyPenalty())
                    .maxTokens(config.getMaxTokens())
                    .enableThinking(true) // 必须启用思考过程
                    .stream(true) // 必须启用流式输出
                    .autoSwitchModel(config.isAutoSwitchModel())
                    .streamWithThinking();
        } catch (Exception e) {
            log.error("硅基流动流式思考过程获取出错 - 消息: {}, 错误: {}", message, e.getMessage(), e);
            throw new RuntimeException("硅基流动流式思考过程获取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据消息内容和配置估算合理的超时时间
     * 
     * @param message 用户消息
     * @param config  请求配置
     * @return 估算的超时时间（秒）
     */
    private int estimateTimeout(String message, SiliconFlowRequestConfig config) {
        // 1. 基础超时时间来自配置
        int baseTimeout = config.getTimeoutSeconds();

        // 2. 根据消息长度适当调整（每1000字符增加60秒）
        int lengthFactor = Math.min(300, message.length() / 1000 * 60);

        // 3. 思考模式需要更多时间
        int thinkingFactor = config.isEnableThinking() ? 240 : 0;

        // 4. 计算总超时时间（确保至少有配置的时间）
        int estimatedTimeout = Math.max(baseTimeout, baseTimeout + lengthFactor + thinkingFactor);

        // 5. 合理上限，避免设置过长超时
        return Math.min(estimatedTimeout, 1800); // 最多30分钟
    }
}