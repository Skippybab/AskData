package com.mt.agent.workflow.api.ai.service.impl;

import com.mt.agent.workflow.api.ai.enums.AliModelType;
import com.mt.agent.workflow.api.ai.model.RequestConfig;
import com.mt.agent.workflow.api.ai.model.impl.DashScopeChatClient;
import com.mt.agent.workflow.api.ai.service.AiService;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AI服务实现类
 * 实现各种大模型调用方式，包括同步调用、流式调用、思考过程获取等
 */
@Slf4j
@Service
public class AiServiceImpl implements AiService {

    @Autowired
    private DashScopeChatClient dashScopeChatClient;

    // ======================= 同步调用方法 =======================

    @Override
    public String chat(String message) {
        return chat(message, RequestConfig.defaultConfig());
    }

    @Override
    public String chat(String message, AliModelType aliModelType) {
        return chat(message, RequestConfig.builder().aliModelType(aliModelType).build());
    }

    @Override
    public String chat(String message, RequestConfig config) {
//        log.debug("执行同步调用 - 模型: {}, 消息: {}", config.getAliModelType(), message);
        try {
            return dashScopeChatClient.createRequest()
                    .modelType(config.getAliModelType())
                    .message(message)
                    .temperature(config.getTemperature())
                    .topP(config.getTopP())
                    .topK(config.getTopK())
                    .enableThinking(config.isEnableThinking())
                    .incrementalOutput(config.isIncrementalOutput())
                    .resultFormat(config.getResultFormat())
                    .autoSwitchModel(config.isAutoSwitchModel())
                    .call();
        } catch (Exception e) {
            log.error("同步调用出错 - 消息: {}, 错误: {}", message, e.getMessage(), e);
            throw new RuntimeException("大模型调用失败: " + e.getMessage(), e);
        }
    }

    // ======================= 流式调用方法 =======================

    @Override
    public Flowable<String> chatStream(String message) {
        return chatStream(message, RequestConfig.streamingConfig());
    }

    @Override
    public Flowable<String> chatStream(String message, AliModelType aliModelType) {
        return chatStream(message, RequestConfig.builder()
                .aliModelType(aliModelType)
                .incrementalOutput(true)
                .build());
    }

    @Override
    public Flowable<String> chatStream(String message, RequestConfig config) {
        log.debug("执行流式调用 - 模型: {}, 消息: {}", config.getAliModelType(), message);
        try {
            // 确保增量输出开启
            config.setIncrementalOutput(true);

            return dashScopeChatClient.createRequest()
                    .modelType(config.getAliModelType())
                    .message(message)
                    .temperature(config.getTemperature())
                    .topP(config.getTopP())
                    .enableThinking(false)
                    .incrementalOutput(true) // 流式调用必须启用增量输出
                    .resultFormat(config.getResultFormat())
                    .autoSwitchModel(config.isAutoSwitchModel())
                    .stream();
        } catch (Exception e) {
            log.error("流式调用出错 - 消息: {}, 错误: {}", message, e.getMessage(), e);
            return Flowable.error(new RuntimeException("大模型流式调用失败: " + e.getMessage(), e));
        }
    }

    // ======================= 思考过程获取方法 =======================

    @Override
    public DashScopeChatClient.ThinkingResult chatWithThinking(String message) {
        return chatWithThinking(message, RequestConfig.thinkingConfig());
    }

    @Override
    public DashScopeChatClient.ThinkingResult chatWithThinking(String message, AliModelType aliModelType) {
        return chatWithThinking(message, RequestConfig.builder()
                .aliModelType(aliModelType)
                .enableThinking(true)
                .build());
    }

    @Override
    public DashScopeChatClient.ThinkingResult chatWithThinking(String message, RequestConfig config) {
        log.debug("执行思考过程获取 - 模型: {}, 消息: {}", config.getAliModelType(), message);
        try {
            // 确保思考过程开启
            config.setEnableThinking(true);

            // 使用流式获取后阻塞获取完整结果
            return chatStreamWithThinking(message, config).getCompleteResult();
        } catch (Exception e) {
            log.error("思考过程获取出错 - 消息: {}, 错误: {}", message, e.getMessage(), e);
            throw new RuntimeException("思考过程获取失败: " + e.getMessage(), e);
        }
    }

    // ======================= 流式思考过程获取方法 =======================

    @Override
    public DashScopeChatClient.ThinkingStreamResult chatStreamWithThinking(String message) {
        return chatStreamWithThinking(message, RequestConfig.thinkingConfig());
    }

    @Override
    public DashScopeChatClient.ThinkingStreamResult chatStreamWithThinking(String message, AliModelType aliModelType) {
        return chatStreamWithThinking(message, RequestConfig.builder()
                .aliModelType(aliModelType)
                .enableThinking(true)
                .incrementalOutput(true)
                .build());
    }

    @Override
    public DashScopeChatClient.ThinkingStreamResult chatStreamWithThinking(String message, RequestConfig config) {
        log.debug("执行流式思考过程获取 -模型: {},设置超时时间: {}秒",config.getAliModelType(),  config.getTimeoutSeconds());
        try {
            // 确保思考过程和增量输出开启
            config.setEnableThinking(true);
            config.setIncrementalOutput(true);

            // 估算复杂度，动态调整超时时间
            int estimatedTimeout = estimateTimeout(message, config);
            log.debug("估算的超时时间: {}秒 (配置值: {}秒)", estimatedTimeout, config.getTimeoutSeconds());

            return dashScopeChatClient.createRequest()
                    .modelType(config.getAliModelType())
                    .message(message)
                    .temperature(config.getTemperature())
                    .topP(config.getTopP())
                    .enableThinking(true) // 必须启用思考过程
                    .incrementalOutput(true) // 必须启用增量输出
                    .resultFormat("message") // 必须使用message格式
                    .autoSwitchModel(config.isAutoSwitchModel())
                    .streamWithThinking();
        } catch (Exception e) {
            log.error("流式思考过程获取出错 - 消息: {}, 错误: {}", message, e.getMessage(), e);
            throw new RuntimeException("流式思考过程获取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据消息内容和配置估算合理的超时时间
     * 
     * @param message 用户消息
     * @param config  请求配置
     * @return 估算的超时时间（秒）
     */
    private int estimateTimeout(String message, RequestConfig config) {
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
