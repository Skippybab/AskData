package com.cultivate.ai.model.impl;


import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.cultivate.ai.enums.AliModelType;
import com.cultivate.ai.enums.ModelCapability;
import com.cultivate.ai.exception.CapabilityNotSupportedException;
import com.cultivate.ai.exception.InvalidConfigurationException;
import io.reactivex.Flowable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 阿里云通义千问大模型调用客户端
 * 支持链式调用、流式输出、推理过程获取等功能
 * 此客户端只处理单次对话，不维护对话历史
 */
@Slf4j
@Component
public class DashScopeChatClient {

    @Value("${spring.ai.dashscope.apikey}")
    private String apiKey;

    /**
     * 创建请求构建器
     *
     * @return 请求构建器
     */
    public RequestBuilder createRequest() {
        // log.debug("创建新的请求构建器");
        // log.debug("当前API密钥: {}", apiKey.substring(0, Math.min(5, apiKey.length())) +
        // "***");
        return new RequestBuilder();
    }

    /**
     * 创建适用于思考模式的请求构建器
     * 
     * @return 已配置思考模式的请求构建器
     */
    public RequestBuilder forThinking() {
        log.debug("创建适用于思考模式的请求构建器");
        AliModelType recommendedModel = AliModelType.getModelWithCapability(ModelCapability.SUPPORTS_THINKING);
        log.debug("推荐用于思考模式的模型: {} ({})", recommendedModel.getName(), recommendedModel.getCode());

        return new RequestBuilder()
                .modelType(recommendedModel)
                .enableThinking(true);
    }

    /**
     * 创建适用于流式输出的请求构建器
     * 
     * @return 已配置流式输出的请求构建器
     */
    public RequestBuilder forStreaming() {
        log.debug("创建适用于流式输出的请求构建器");
        AliModelType recommendedModel = AliModelType.getDefaultModel();
        log.debug("推荐用于流式输出的模型: {} ({})", recommendedModel.getName(), recommendedModel.getCode());

        return new RequestBuilder()
                .modelType(recommendedModel);
    }

    /**
     * 请求构建器，用于链式构建请求参数
     */
    public class RequestBuilder {
        private AliModelType aliModelType = AliModelType.getDefaultModel(); // 默认使用推荐模型
        private String userContent = ""; // 用户输入内容
        private float temperature = 0.0f; // 默认温度
        private double topP = 0.05; // 默认topP
        private Integer topK = 50;
        private Integer maxTokens = null; // 默认不限制
        /**
         * 是否启用思考过程
         * - true: 模型会输出推理过程和思考内容，仅支持特定模型如QWEN_3_14B等
         * - false: 模型只输出最终回复内容
         * 使用场景：需要查看模型的推理链路时、希望了解模型思考过程时启用
         */
        private boolean enableThinking = false; // 默认不开启思考过程

        /**
         * 是否启用增量输出
         * - true: 模型生成的内容会逐步返回，提供更好的用户体验
         * - false: 模型仅在生成完整内容后一次性返回
         * 使用场景：流式UI界面、需要更好用户体验时启用，API调试时可关闭
         */
        private boolean incrementalOutput = false; // 默认不开启增量输出

        /**
         * 结果格式
         * - "message": 返回消息格式，适用于对话场景
         * - "text": 纯文本格式，适用于文本生成场景
         * 使用场景：根据下游应用的需求选择合适的格式
         */
        private String resultFormat = "message"; // 默认返回message格式

        /**
         * 是否自动切换模型
         * - true: 当所选模型不支持某种能力（如思考过程）时，自动切换到支持的模型
         * - false: 严格使用指定模型，若能力不匹配则抛出异常
         * 使用场景：生产环境建议启用以提高容错性，测试特定模型时建议关闭
         */
        private boolean autoSwitchModel = true; // 默认启用模型自动切换

        /**
         * 设置模型类型
         */
        public RequestBuilder modelType(AliModelType aliModelType) {
            this.aliModelType = aliModelType;
            // log.debug("设置模型类型: {}, 代码: {}", aliModelType.getName(), aliModelType.getCode());
            return this;
        }

        /**
         * 设置用户消息（单条）
         */
        public RequestBuilder message(String content) {
            this.userContent = content;
            // log.debug("设置用户消息: {}", content);
            return this;
        }

        /**
         * 设置温度参数
         */
        public RequestBuilder temperature(float temperature) {
            this.temperature = temperature;
            // log.debug("设置温度参数: {}", temperature);
            return this;
        }

        /**
         * 设置topP参数
         */
        public RequestBuilder topP(double topP) {
            this.topP = topP;
            // log.debug("设置topP参数: {}", topP);
            return this;
        }

        /**
         * 获取topK参数
         */
        public RequestBuilder topK(Integer topK) {
            this.topK = topK;
            // log.debug("设置topK参数: {}", topK);
            return this;
        }

        /**
         * 设置最大token数
         */
        public RequestBuilder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            // log.debug("设置最大token数: {}", maxTokens);
            return this;
        }

        /**
         * 设置是否获取思考过程
         */
        public RequestBuilder enableThinking(boolean enableThinking) {
            this.enableThinking = enableThinking;
            // log.debug("设置是否获取思考过程: {}", enableThinking);
            return this;
        }

        /**
         * 设置是否增量输出
         */
        public RequestBuilder incrementalOutput(boolean incrementalOutput) {
            this.incrementalOutput = incrementalOutput;
            // log.debug("设置是否增量输出: {}", incrementalOutput);
            return this;
        }

        /**
         * 设置结果格式
         */
        public RequestBuilder resultFormat(String resultFormat) {
            this.resultFormat = resultFormat;
            // log.debug("设置结果格式: {}", resultFormat);
            return this;
        }

        /**
         * 设置是否启用模型自动切换
         * 如果启用，当模型不支持某项能力时，会自动切换到支持该能力的模型
         */
        public RequestBuilder autoSwitchModel(boolean autoSwitch) {
            this.autoSwitchModel = autoSwitch;
            // log.debug("设置是否启用模型自动切换: {}", autoSwitch);
            return this;
        }

        /**
         * 验证当前配置的模型是否支持指定能力
         * 
         * @param capability 需要验证的能力
         * @return 请求构建器自身，用于链式调用
         * @throws CapabilityNotSupportedException 如果模型不支持该能力且未启用自动切换
         */
        public RequestBuilder validateCapability(ModelCapability capability) {
            if (!aliModelType.supportsCapability(capability)) {
                if (autoSwitchModel) {
                    AliModelType newModel = AliModelType.getModelWithCapability(capability);
                    log.warn("模型 {} 不支持 {} 能力，已自动切换为 {}",
                            aliModelType.getName(), capability.getDescription(), newModel.getName());
                    this.aliModelType = newModel;
                } else {
                    throw new CapabilityNotSupportedException(aliModelType, capability);
                }
            }
            return this;
        }

        /**
         * 验证所有需要的能力
         * 
         * @return 请求构建器自身，用于链式调用
         */
        public RequestBuilder validateCapabilities() {
            // 如果启用了思考模式，验证模型是否支持
            if (enableThinking) {
                validateCapability(ModelCapability.SUPPORTS_THINKING);
            }

            return this;
        }

        /**
         * 构建生成参数
         */
        private GenerationParam buildParam() {
            // 创建用户消息对象
            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(userContent)
                    .build();

            log.debug("选用模型: {}", aliModelType.getCode());
            // log.debug("API密钥前缀: {}", apiKey.substring(0, Math.min(5, apiKey.length())));

            GenerationParam paramBuilder = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(aliModelType.getCode())
                    .messages(Arrays.asList(userMsg))
                    .temperature(temperature)
                    .topP(topP)
                    .topK(topK)
                    .enableThinking(enableThinking)
                    .incrementalOutput(incrementalOutput)
                    .resultFormat(resultFormat)
                    .build();

            if (maxTokens != null) {
                paramBuilder.setMaxTokens(maxTokens);
                log.debug("设置最大token数: {}", maxTokens);
            }

            // log.debug("构建参数完成: {}", paramBuilder);
            return paramBuilder;
        }

        /**
         * 执行同步调用（非流式）
         *
         * @return 模型响应文本
         */
        public String call() {
            // 检查模型是否仅支持思考模式
            if (aliModelType.isThinkingOnly()) {
                if (autoSwitchModel) {
                    // 自动切换到支持同步调用的模型
                    AliModelType newModel = AliModelType.getModelWithCapability(ModelCapability.SUPPORTS_SYNC);
                    log.warn("模型 {} 仅支持思考模式，不支持普通对话，已自动切换为 {}",
                            aliModelType.getName(), newModel.getName());
                    this.aliModelType = newModel;
                } else {
                    throw new CapabilityNotSupportedException(aliModelType, ModelCapability.SUPPORTS_SYNC,
                            "该模型仅支持思考模式，请使用streamWithThinking()方法或启用自动切换模型");
                }
            }

            // 验证模型是否支持同步调用
            validateCapability(ModelCapability.SUPPORTS_SYNC);

            // 验证所有其他能力配置
            validateCapabilities();

            log.debug("模型调用 - 模型: {},温度: {},TopP: {},TopK: {},是否思考: {},是否增量输出: {}", aliModelType.getCode(), temperature,
                    topP, topK, enableThinking, incrementalOutput);
            // log.debug("发送消息：\n{}", userContent);

            try {
                // 验证用户内容是否为空
                if (userContent == null || userContent.trim().isEmpty()) {
                    log.error("用户消息内容为空");
                    throw new InvalidConfigurationException("用户消息内容不能为空");
                }

                Generation gen = new Generation();
                GenerationParam param = buildParam();

                GenerationResult result = gen.call(param);

                if (result == null) {
                    log.error("模型API返回结果为null");
                    return null;
                }

                String text = result.getOutput().getText();

                // 如果text为null，尝试从choices中获取内容
                if (text == null && result.getOutput().getChoices() != null
                        && !result.getOutput().getChoices().isEmpty()) {
                    Message message = result.getOutput().getChoices().get(0).getMessage();
                    if (message != null) {
                        text = message.getContent();
                        // log.info("从message.content获取文本响应: {}", text);
                    }
                }

                // 如果仍然为null，记录警告
                if (text == null) {
                    log.warn("响应文本内容为null，但API调用成功，可能是模型或API格式变更");
                }

                // log.info("获取到的文本响应: \n{}", text);

                return text;
            } catch (NoApiKeyException e) {
                log.error("API密钥异常: {}", e.getMessage(), e);
                throw new RuntimeException("模型调用失败，API密钥错误: " + e.getMessage(), e);
            } catch (InputRequiredException e) {
                log.error("输入参数异常: {}", e.getMessage(), e);
                throw new RuntimeException("模型调用失败，输入参数错误: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("模型调用未知异常: {}", e.getMessage(), e);
                throw new RuntimeException("模型调用失败: " + e.getMessage(), e);
            }
        }

        /**
         * 执行流式调用
         *
         * @return 流式结果
         */
        public Flowable<String> stream() {
            this.enableThinking = false;
            this.incrementalOutput = false;
            // 验证模型是否支持流式调用（支持SUPPORTS_STREAMING或STREAMING_ONLY）
            if (!aliModelType.supportsCapability(ModelCapability.SUPPORTS_STREAMING) &&
                    !aliModelType.supportsCapability(ModelCapability.STREAMING_ONLY)) {
                if (autoSwitchModel) {
                    // 优先选择支持流式的模型，如果没有则选择仅流式的模型
                    AliModelType newModel = AliModelType.getModelWithCapability(ModelCapability.SUPPORTS_STREAMING);
                    if (newModel == null || newModel == AliModelType.getDefaultModel()) {
                        newModel = AliModelType.getModelWithCapability(ModelCapability.STREAMING_ONLY);
                    }
                    log.warn("模型 {} 不支持流式调用，已自动切换为 {}",
                            aliModelType.getName(), newModel.getName());
                    this.aliModelType = newModel;
                } else {
                    throw new CapabilityNotSupportedException(aliModelType, ModelCapability.SUPPORTS_STREAMING);
                }
            }

            // 验证所有其他能力配置
            validateCapabilities();

            log.debug("模型调用 - 模型: {},温度: {},TopP: {},TopK: {},是否思考: {},是否增量输出: {}", aliModelType.getCode(), temperature,
                    topP, topK, enableThinking, incrementalOutput);
            // log.debug("发送消息：\n{}", userContent);

            try {
                // 验证用户内容是否为空
                if (userContent == null || userContent.trim().isEmpty()) {
                    log.error("用户消息内容为空");
                    throw new InvalidConfigurationException("用户消息内容不能为空");
                }

                this.incrementalOutput = true;

                Generation gen = new Generation();
                GenerationParam param = buildParam();

                log.debug("开始流式调用模型API...");
                Flowable<GenerationResult> results = gen.streamCall(param);
                return results
                        .map(response -> {
                            String content = null;
                            // 尝试从choices获取内容
                            if (response.getOutput().getChoices() != null
                                    && !response.getOutput().getChoices().isEmpty()) {
                                Message message = response.getOutput().getChoices().get(0).getMessage();
                                if (message != null) {
                                    content = message.getContent();
                                }
                            }

                            // 如果content为null，尝试从text获取
                            if (content == null) {
                                content = response.getOutput().getText();
                            }

                            // 如果仍然为null，返回空字符串避免NPE
                            if (content == null) {
                                // log.debug("流式响应片段为null，这是正常现象（可能是连接建立或状态信息）");
                                content = "";
                            }

                            // 添加更详细的调试信息
                            if (!content.isEmpty()) {
                                log.debug("收到流式响应片段: {}", content);
                            }
                            return content;
                        })
                        // 过滤掉空内容片段，只返回有实际内容的片段
                        .filter(content -> content != null && !content.trim().isEmpty());
            } catch (NoApiKeyException e) {
                log.error("API密钥异常: {}", e.getMessage(), e);
                throw new RuntimeException("流式模型调用失败，API密钥错误: " + e.getMessage(), e);
            } catch (InputRequiredException e) {
                log.error("输入参数异常: {}", e.getMessage(), e);
                throw new RuntimeException("流式模型调用失败，输入参数错误: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("流式模型调用未知异常: {}", e.getMessage(), e);
                throw new RuntimeException("流式模型调用失败: " + e.getMessage(), e);
            }
        }

        /**
         * 执行流式调用并获取思考过程（仅对支持思考过程的模型有效）
         *
         * @return 流式结果（包含思考过程和最终回复）
         */
        public ThinkingStreamResult streamWithThinking() {
            // 确保启用思考过程
            this.enableThinking = true;

            // 验证模型是否支持思考过程（支持SUPPORTS_THINKING或THINKING_ONLY）
            if (!aliModelType.supportsCapability(ModelCapability.SUPPORTS_THINKING) &&
                    !aliModelType.supportsCapability(ModelCapability.THINKING_ONLY)) {
                if (autoSwitchModel) {
                    // 优先选择支持思考的模型，如果没有则选择仅思考的模型
                    AliModelType newModel = AliModelType.getModelWithCapability(ModelCapability.SUPPORTS_THINKING);
                    if (newModel == null || newModel == AliModelType.getDefaultModel()) {
                        newModel = AliModelType.getModelWithCapability(ModelCapability.THINKING_ONLY);
                    }
                    log.warn("模型 {} 不支持思考过程，已自动切换为 {}",
                            aliModelType.getName(), newModel.getName());
                    this.aliModelType = newModel;
                } else {
                    throw new CapabilityNotSupportedException(aliModelType, ModelCapability.SUPPORTS_THINKING);
                }
            }

            log.debug("流式模型调用（包含思考过程） - 模型: {},温度: {},TopP: {},TopK: {},是否思考: {},是否增量输出: {}", aliModelType.getCode(),
                    temperature, topP, topK, enableThinking, incrementalOutput);
            // log.debug("发送消息：\n{}", userContent);

            // 验证用户内容是否为空
            if (userContent == null || userContent.trim().isEmpty()) {
                log.error("用户消息内容为空");
                throw new InvalidConfigurationException("用户消息内容不能为空");
            }

            // 确保正确的结果格式和增量输出
            this.resultFormat = "message";
            this.incrementalOutput = true;

            try {
                Generation gen = new Generation();
                GenerationParam param = buildParam();

                log.debug("开始流式调用模型API（含思考过程）...");
                Flowable<GenerationResult> results = gen.streamCall(param);

                StringBuilder reasoningContent = new StringBuilder();
                StringBuilder finalContent = new StringBuilder();

                Flowable<ThinkingStreamResponse> responseFlowable = results.map(result -> {
                    String reasoning = null;
                    String content = null;

                    // 尝试从choices获取思考过程和内容
                    if (result.getOutput().getChoices() != null && !result.getOutput().getChoices().isEmpty()) {
                        Message message = result.getOutput().getChoices().get(0).getMessage();
                        if (message != null) {
                            reasoning = message.getReasoningContent();
                            content = message.getContent();
                        }
                    }

                    // 使用空字符串代替null，避免NPE
                    reasoning = reasoning != null ? reasoning : "";
                    content = content != null ? content : "";

                    if (!reasoning.isEmpty()) {
                        // log.debug("收到思考过程片段: {}", reasoning);
                        reasoningContent.append(reasoning);
                    }

                    if (!content.isEmpty()) {
                        // log.debug("收到内容片段: {}", content);
                        finalContent.append(content);
                    }

                    return new ThinkingStreamResponse(reasoning, content, reasoningContent.toString(),
                            finalContent.toString());
                });

                return new ThinkingStreamResult(responseFlowable, reasoningContent, finalContent);
            } catch (NoApiKeyException e) {
                log.error("API密钥异常: {}", e.getMessage(), e);
                throw new RuntimeException("思考过程流式模型调用失败，API密钥错误: " + e.getMessage(), e);
            } catch (InputRequiredException e) {
                log.error("输入参数异常: {}", e.getMessage(), e);
                throw new RuntimeException("思考过程流式模型调用失败，输入参数错误: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("思考过程流式模型调用未知异常: {}", e.getMessage(), e);
                throw new RuntimeException("思考过程流式模型调用失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 包含思考过程的流式响应
     */
    public static class ThinkingStreamResponse {
        private final String incrementalReasoning; // 增量思考过程
        private final String incrementalContent; // 增量内容
        private final String fullReasoning; // 完整思考过程
        private final String fullContent; // 完整内容

        public ThinkingStreamResponse(String incrementalReasoning, String incrementalContent,
                String fullReasoning, String fullContent) {
            this.incrementalReasoning = incrementalReasoning != null ? incrementalReasoning : "";
            this.incrementalContent = incrementalContent != null ? incrementalContent : "";
            this.fullReasoning = fullReasoning;
            this.fullContent = fullContent;
        }

        public String getIncrementalReasoning() {
            return incrementalReasoning;
        }

        public String getIncrementalContent() {
            return incrementalContent;
        }

        public String getFullReasoning() {
            return fullReasoning;
        }

        public String getFullContent() {
            return fullContent;
        }
    }

    /**
     * 包含思考过程的流式结果
     */
    public static class ThinkingStreamResult {
        private final Flowable<ThinkingStreamResponse> responseFlowable;
        private final StringBuilder reasoningBuilder;
        private final StringBuilder contentBuilder;

        public ThinkingStreamResult(Flowable<ThinkingStreamResponse> responseFlowable,
                StringBuilder reasoningBuilder, StringBuilder contentBuilder) {
            this.responseFlowable = responseFlowable;
            this.reasoningBuilder = reasoningBuilder;
            this.contentBuilder = contentBuilder;
        }

        /**
         * 获取流式响应
         */
        public Flowable<ThinkingStreamResponse> getResponseFlowable() {
            return responseFlowable;
        }

        /**
         * 只获取思考过程流
         */
        public Flowable<String> getReasoningFlowable() {
            return responseFlowable
                    .filter(response -> !response.getIncrementalReasoning().isEmpty())
                    .map(ThinkingStreamResponse::getIncrementalReasoning);
        }

        /**
         * 只获取内容流
         */
        public Flowable<String> getContentFlowable() {
            return responseFlowable
                    .filter(response -> !response.getIncrementalContent().isEmpty())
                    .map(ThinkingStreamResponse::getIncrementalContent);
        }

        /**
         * 阻塞获取最终完整结果
         */
        public ThinkingResult getCompleteResult() {
            StringBuilder reasoningBuilder = new StringBuilder();
            StringBuilder contentBuilder = new StringBuilder();

            // 不再依赖fullContent，而是直接使用增量内容按顺序构建
            responseFlowable.blockingForEach(response -> {
                // 获取增量思考过程和增量内容
                String incrementalReasoning = response.getIncrementalReasoning();
                String incrementalContent = response.getIncrementalContent();

                // 直接按顺序追加增量内容
                if (incrementalReasoning != null && !incrementalReasoning.isEmpty()) {
                    reasoningBuilder.append(incrementalReasoning);
                }

                if (incrementalContent != null && !incrementalContent.isEmpty()) {
                    contentBuilder.append(incrementalContent);
                }
            });

            return new ThinkingResult(reasoningBuilder.toString(), contentBuilder.toString());
        }
    }

    /**
     * 思考过程结果
     */
    @Getter
    public static class ThinkingResult {
        private final String reasoning;
        private final String content;

        public ThinkingResult(String reasoning, String content) {
            this.reasoning = reasoning;
            this.content = content;
        }
    }
}
