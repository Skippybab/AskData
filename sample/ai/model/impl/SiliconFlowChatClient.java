package com.cultivate.ai.model.impl;

import com.cultivate.ai.enums.ModelCapability;
import com.cultivate.ai.enums.SiliconFlowModelType;
import com.cultivate.ai.exception.CapabilityNotSupportedException;
import com.cultivate.ai.exception.InvalidConfigurationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.reactivex.Flowable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 硅基流动大模型调用客户端
 * 支持链式调用、流式输出、推理过程获取等功能
 * 此客户端只处理单次对话，不维护对话历史
 */
@Slf4j
@Component
public class SiliconFlowChatClient {

    @Value("${spring.ai.siliconFlow.apikey}")
    private String apiKey;

    @Value("${spring.ai.siliconFlow.baseurl:https://api.siliconflow.cn/v1}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient;

    public SiliconFlowChatClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 创建请求构建器
     *
     * @return 请求构建器
     */
    public RequestBuilder createRequest() {
        log.debug("创建新的硅基流动请求构建器");
        return new RequestBuilder();
    }

    /**
     * 创建适用于思考模式的请求构建器
     * 
     * @return 已配置思考模式的请求构建器
     */
    public RequestBuilder forThinking() {
        log.debug("创建适用于思考模式的请求构建器");
        SiliconFlowModelType recommendedModel = SiliconFlowModelType
                .getModelWithCapability(ModelCapability.SUPPORTS_THINKING);
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
        SiliconFlowModelType recommendedModel = SiliconFlowModelType.getDefaultModel();
        log.debug("推荐用于流式输出的模型: {} ({})", recommendedModel.getName(), recommendedModel.getCode());

        return new RequestBuilder()
                .modelType(recommendedModel)
                .stream(true);
    }

    /**
     * 请求构建器，用于链式构建请求参数
     */
    public class RequestBuilder {
        private SiliconFlowModelType modelType = SiliconFlowModelType.getDefaultModel();
        private String userContent = "";
        private float temperature = 0.7f;
        private double topP = 0.7;
        private Integer topK = 50;
        private double frequencyPenalty = 0.0;
        private Integer maxTokens = null;
        private boolean enableThinking = false;
        private boolean stream = false;
        private String resultFormat = "message";
        private boolean autoSwitchModel = true;

        /**
         * 设置模型类型
         */
        public RequestBuilder modelType(SiliconFlowModelType modelType) {
            this.modelType = modelType;
            log.debug("设置模型类型: {}, 代码: {}", modelType.getName(), modelType.getCode());
            return this;
        }

        /**
         * 设置用户消息（单条）
         */
        public RequestBuilder message(String content) {
            this.userContent = content;
            log.debug("设置用户消息长度: {}", content.length());
            return this;
        }

        /**
         * 设置温度参数
         */
        public RequestBuilder temperature(float temperature) {
            this.temperature = temperature;
            log.debug("设置温度参数: {}", temperature);
            return this;
        }

        /**
         * 设置topP参数
         */
        public RequestBuilder topP(double topP) {
            this.topP = topP;
            log.debug("设置topP参数: {}", topP);
            return this;
        }

        /**
         * 设置topK参数
         */
        public RequestBuilder topK(Integer topK) {
            this.topK = topK;
            log.debug("设置topK参数: {}", topK);
            return this;
        }

        /**
         * 设置频率惩罚参数
         */
        public RequestBuilder frequencyPenalty(double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            log.debug("设置频率惩罚参数: {}", frequencyPenalty);
            return this;
        }

        /**
         * 设置最大token数
         */
        public RequestBuilder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            log.debug("设置最大token数: {}", maxTokens);
            return this;
        }

        /**
         * 设置是否获取思考过程
         */
        public RequestBuilder enableThinking(boolean enableThinking) {
            this.enableThinking = enableThinking;
            log.debug("设置是否获取思考过程: {}", enableThinking);
            return this;
        }

        /**
         * 设置是否流式输出
         */
        public RequestBuilder stream(boolean stream) {
            this.stream = stream;
            log.debug("设置是否流式输出: {}", stream);
            return this;
        }

        /**
         * 设置是否启用模型自动切换
         */
        public RequestBuilder autoSwitchModel(boolean autoSwitch) {
            this.autoSwitchModel = autoSwitch;
            log.debug("设置是否启用模型自动切换: {}", autoSwitch);
            return this;
        }

        /**
         * 验证当前配置的模型是否支持指定能力
         */
        public RequestBuilder validateCapability(ModelCapability capability) {
            if (!modelType.supportsCapability(capability)) {
                if (autoSwitchModel) {
                    SiliconFlowModelType newModel = SiliconFlowModelType.getModelWithCapability(capability);
                    log.warn("模型 {} 不支持 {} 能力，已自动切换为 {}",
                            modelType.getName(), capability.getDescription(), newModel.getName());
                    this.modelType = newModel;
                } else {
                    throw new CapabilityNotSupportedException(capability,
                            "模型 " + modelType.getName() + " 不支持 " + capability.getDescription());
                }
            }
            return this;
        }

        /**
         * 验证所有需要的能力
         */
        public RequestBuilder validateCapabilities() {
            if (enableThinking) {
                validateCapability(ModelCapability.SUPPORTS_THINKING);
            }
            if (stream) {
                validateCapability(ModelCapability.SUPPORTS_STREAMING);
            }
            return this;
        }

        /**
         * 构建请求体
         */
        private String buildRequestBody() throws Exception {
            // 验证用户内容是否为空
            if (userContent == null || userContent.trim().isEmpty()) {
                throw new InvalidConfigurationException("用户消息内容不能为空");
            }

            // 构建消息体
            String messageJson = String.format("""
                    {
                        "model": "%s",
                        "messages": [
                            {
                                "role": "user",
                                "content": "%s"
                            }
                        ],
                        "temperature": %f,
                        "top_p": %f,
                        "top_k": %d,
                        "frequency_penalty": %f,
                        "stream": %b
                    """,
                    modelType.getCode(),
                    userContent.replace("\"", "\\\"").replace("\n", "\\n"),
                    temperature,
                    topP,
                    topK,
                    frequencyPenalty,
                    stream);

            if (maxTokens != null) {
                messageJson += String.format(", \"max_tokens\": %d", maxTokens);
            }

            messageJson += "}";

            log.debug("构建请求体: {}", messageJson);
            return messageJson;
        }

        /**
         * 执行同步调用（非流式）
         */
        public String call() {
            // 检查模型是否仅支持思考模式
            if (modelType.isThinkingOnly()) {
                if (autoSwitchModel) {
                    SiliconFlowModelType newModel = SiliconFlowModelType
                            .getModelWithCapability(ModelCapability.SUPPORTS_SYNC);
                    log.warn("模型 {} 仅支持思考模式，不支持普通对话，已自动切换为 {}",
                            modelType.getName(), newModel.getName());
                    this.modelType = newModel;
                } else {
                    throw new CapabilityNotSupportedException(ModelCapability.SUPPORTS_SYNC,
                            "该模型仅支持思考模式，请使用streamWithThinking()方法或启用自动切换模型");
                }
            }

            // 验证模型能力
            validateCapabilities();

            // 确保非流式调用
            this.stream = false;

            log.debug("开始同步调用 - 模型: {}", modelType.getCode());

            try {
                String requestBody = buildRequestBody();

                Request request = new Request.Builder()
                        .url(baseUrl + "/chat/completions")
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "未知错误";
                        log.error("API调用失败，状态码: {}, 错误信息: {}", response.code(), errorBody);
                        throw new RuntimeException("硅基流动API调用失败: " + errorBody);
                    }

                    String responseBody = response.body().string();
                    log.debug("收到API响应: {}", responseBody);

                    // 解析响应
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    JsonNode choices = jsonNode.get("choices");
                    if (choices != null && choices.isArray() && choices.size() > 0) {
                        JsonNode message = choices.get(0).get("message");
                        if (message != null) {
                            JsonNode content = message.get("content");
                            if (content != null) {
                                return content.asText();
                            }
                        }
                    }

                    log.warn("响应中未找到有效的内容字段");
                    return null;
                }
            } catch (Exception e) {
                log.error("同步调用出错: {}", e.getMessage(), e);
                throw new RuntimeException("硅基流动模型调用失败: " + e.getMessage(), e);
            }
        }

        /**
         * 执行流式调用
         */
        public Flowable<String> stream() {
            // 验证模型能力
            validateCapabilities();

            // 确保流式调用
            this.stream = true;

            log.debug("开始流式调用 - 模型: {}", modelType.getCode());

            return Flowable.create(emitter -> {
                try {
                    String requestBody = buildRequestBody();

                    Request request = new Request.Builder()
                            .url(baseUrl + "/chat/completions")
                            .addHeader("Authorization", "Bearer " + apiKey)
                            .addHeader("Content-Type", "application/json")
                            .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                            .build();

                    try (Response response = httpClient.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            String errorBody = response.body() != null ? response.body().string() : "未知错误";
                            log.error("流式API调用失败，状态码: {}, 错误信息: {}", response.code(), errorBody);
                            emitter.onError(new RuntimeException("硅基流动流式API调用失败: " + errorBody));
                            return;
                        }

                        ResponseBody responseBody = response.body();
                        if (responseBody != null) {
                            String line;
                            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                                    new java.io.InputStreamReader(responseBody.byteStream()))) {

                                while ((line = reader.readLine()) != null && !emitter.isCancelled()) {
                                    if (line.startsWith("data: ")) {
                                        String data = line.substring(6).trim();
                                        if ("[DONE]".equals(data)) {
                                            emitter.onComplete();
                                            break;
                                        }

                                        try {
                                            JsonNode jsonNode = objectMapper.readTree(data);
                                            JsonNode choices = jsonNode.get("choices");
                                            if (choices != null && choices.isArray() && choices.size() > 0) {
                                                JsonNode delta = choices.get(0).get("delta");
                                                if (delta != null) {
                                                    JsonNode content = delta.get("content");
                                                    if (content != null && !content.isNull()) {
                                                        String contentStr = content.asText();
                                                        if (!contentStr.isEmpty()) {
                                                            emitter.onNext(contentStr);
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            log.debug("解析流式响应片段失败: {}", e.getMessage());
                                        }
                                    }
                                }
                            }
                        }

                        if (!emitter.isCancelled()) {
                            emitter.onComplete();
                        }
                    }
                } catch (Exception e) {
                    log.error("流式调用出错: {}", e.getMessage(), e);
                    if (!emitter.isCancelled()) {
                        emitter.onError(new RuntimeException("硅基流动流式模型调用失败: " + e.getMessage(), e));
                    }
                }
            }, io.reactivex.BackpressureStrategy.BUFFER);
        }

        /**
         * 执行流式调用并获取思考过程
         */
        public ThinkingStreamResult streamWithThinking() {
            // 确保启用思考过程
            this.enableThinking = true;

            // 验证模型是否支持思考过程
            if (!modelType.supportsCapability(ModelCapability.SUPPORTS_THINKING) &&
                    !modelType.supportsCapability(ModelCapability.THINKING_ONLY)) {
                if (autoSwitchModel) {
                    SiliconFlowModelType newModel = SiliconFlowModelType
                            .getModelWithCapability(ModelCapability.SUPPORTS_THINKING);
                    if (newModel == null || newModel == SiliconFlowModelType.getDefaultModel()) {
                        newModel = SiliconFlowModelType.getModelWithCapability(ModelCapability.THINKING_ONLY);
                    }
                    log.warn("模型 {} 不支持思考过程，已自动切换为 {}",
                            modelType.getName(), newModel.getName());
                    this.modelType = newModel;
                } else {
                    throw new CapabilityNotSupportedException(ModelCapability.SUPPORTS_THINKING,
                            "模型 " + modelType.getName() + " 不支持思考过程");
                }
            }

            // 确保流式调用
            this.stream = true;

            log.debug("开始流式思考过程调用 - 模型: {}", modelType.getCode());

            StringBuilder reasoningContent = new StringBuilder();
            StringBuilder finalContent = new StringBuilder();

            Flowable<ThinkingStreamResponse> responseFlowable = Flowable.create(emitter -> {
                try {
                    String requestBody = buildRequestBody();

                    Request request = new Request.Builder()
                            .url(baseUrl + "/chat/completions")
                            .addHeader("Authorization", "Bearer " + apiKey)
                            .addHeader("Content-Type", "application/json")
                            .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                            .build();

                    try (Response response = httpClient.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            String errorBody = response.body() != null ? response.body().string() : "未知错误";
                            log.error("思考过程流式API调用失败，状态码: {}, 错误信息: {}", response.code(), errorBody);
                            emitter.onError(new RuntimeException("硅基流动思考过程流式API调用失败: " + errorBody));
                            return;
                        }

                        ResponseBody responseBody = response.body();
                        if (responseBody != null) {
                            String line;
                            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                                    new java.io.InputStreamReader(responseBody.byteStream()))) {

                                while ((line = reader.readLine()) != null && !emitter.isCancelled()) {
                                    if (line.startsWith("data: ")) {
                                        String data = line.substring(6).trim();
                                        if ("[DONE]".equals(data)) {
                                            emitter.onComplete();
                                            break;
                                        }

                                        try {
                                            JsonNode jsonNode = objectMapper.readTree(data);
                                            JsonNode choices = jsonNode.get("choices");
                                            if (choices != null && choices.isArray() && choices.size() > 0) {
                                                JsonNode delta = choices.get(0).get("delta");
                                                if (delta != null) {
                                                    String reasoning = null;
                                                    String content = null;

                                                    // 获取推理内容
                                                    JsonNode reasoningNode = delta.get("reasoning_content");
                                                    if (reasoningNode != null && !reasoningNode.isNull()) {
                                                        reasoning = reasoningNode.asText();
                                                        reasoningContent.append(reasoning);
                                                    }

                                                    // 获取普通内容
                                                    JsonNode contentNode = delta.get("content");
                                                    if (contentNode != null && !contentNode.isNull()) {
                                                        content = contentNode.asText();
                                                        finalContent.append(content);
                                                    }

                                                    // 如果有内容，发送响应
                                                    if ((reasoning != null && !reasoning.isEmpty()) ||
                                                            (content != null && !content.isEmpty())) {
                                                        emitter.onNext(new ThinkingStreamResponse(
                                                                reasoning != null ? reasoning : "",
                                                                content != null ? content : "",
                                                                reasoningContent.toString(),
                                                                finalContent.toString()));
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            log.debug("解析思考过程流式响应片段失败: {}", e.getMessage());
                                        }
                                    }
                                }
                            }
                        }

                        if (!emitter.isCancelled()) {
                            emitter.onComplete();
                        }
                    }
                } catch (Exception e) {
                    log.error("思考过程流式调用出错: {}", e.getMessage(), e);
                    if (!emitter.isCancelled()) {
                        emitter.onError(new RuntimeException("硅基流动思考过程流式模型调用失败: " + e.getMessage(), e));
                    }
                }
            }, io.reactivex.BackpressureStrategy.BUFFER);

            return new ThinkingStreamResult(responseFlowable, reasoningContent, finalContent);
        }
    }

    /**
     * 包含思考过程的流式响应
     */
    public static class ThinkingStreamResponse {
        private final String incrementalReasoning;
        private final String incrementalContent;
        private final String fullReasoning;
        private final String fullContent;

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

        public Flowable<ThinkingStreamResponse> getResponseFlowable() {
            return responseFlowable;
        }

        public Flowable<String> getReasoningFlowable() {
            return responseFlowable
                    .filter(response -> !response.getIncrementalReasoning().isEmpty())
                    .map(ThinkingStreamResponse::getIncrementalReasoning);
        }

        public Flowable<String> getContentFlowable() {
            return responseFlowable
                    .filter(response -> !response.getIncrementalContent().isEmpty())
                    .map(ThinkingStreamResponse::getIncrementalContent);
        }

        public ThinkingResult getCompleteResult() {
            StringBuilder reasoningBuilder = new StringBuilder();
            StringBuilder contentBuilder = new StringBuilder();

            responseFlowable.blockingForEach(response -> {
                String incrementalReasoning = response.getIncrementalReasoning();
                String incrementalContent = response.getIncrementalContent();

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