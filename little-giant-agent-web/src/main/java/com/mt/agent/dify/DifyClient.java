package com.mt.agent.dify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.agent.dify.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Dify API客户端
 * 提供完整的Dify智能体API调用功能
 *
 * @author wsx
 * @date 2025/6/30
 */
@Slf4j
@Component
public class DifyClient {

    private final DifyConfig difyConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private OkHttpClient httpClient;

    /**
     * 默认构造函数（用于Spring依赖注入）
     */
    public DifyClient(DifyConfig difyConfig) {
        this.difyConfig = difyConfig;
    }

    /**
     * 初始化HTTP客户端
     */
    @PostConstruct
    public void init() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(difyConfig.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(difyConfig.getReadTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(difyConfig.getTimeout(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build();

        log.info("DifyClient初始化完成，Base URL: {}", difyConfig.getBaseUrl());
    }

    /**
     * 销毁HTTP客户端资源
     */
    @PreDestroy
    public void destroy() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }

    /**
     * 发送聊天消息（阻塞式）
     *
     * @param request 聊天请求
     * @return 聊天响应JSON对象
     * @throws IOException 当请求失败时抛出异常
     */
    public JSONObject sendMessage(DifyChatRequest request) throws IOException {
        // 设置为阻塞模式
        request.setResponseMode("blocking");

        String requestJson = objectMapper.writeValueAsString(request);
        log.debug("发送Dify聊天请求: {}", requestJson);

        RequestBody body = RequestBody.create(requestJson, MediaType.get("application/json; charset=utf-8"));
        Request httpRequest = buildRequest(difyConfig.getBaseUrl() + "/chat-messages", body);

        try (Response response = httpClient.newCall(httpRequest).execute()) {
//            if (!response.isSuccessful()) {
//                throw new IOException("Dify API调用失败: " + response.code() + " " + response.message());
//            }

            String responseBody = response.body().string();
            log.debug("收到Dify聊天响应: {}", responseBody);

            return JSONUtil.parseObj(responseBody);
        }
    }

    /**
     * 发送聊天消息（阻塞式）- 灵活参数版本
     * 使用Map作为参数载体，支持动态参数传递
     *
     * @param params 参数Map，支持以下键：
     *               - query: 用户输入/提问内容（必需）
     *               - user: 用户标识（可选）
     *               - conversation_id: 会话ID（可选）
     *               - response_mode: 响应模式，默认"blocking"（可选）
     *               - auto_generate_name: 自动生成标题，默认true（可选）
     *               - inputs: 允许传入App定义的各变量值（可选）
     *               - files: 文件列表（可选）
     * @return 聊天响应JSON对象
     * @throws IOException 当请求失败时抛出异常
     */
    public JSONObject sendMessage(Map<String, Object> params) throws IOException {

        // 设置默认值
        Map<String, Object> requestParams = new HashMap<>(params);
        requestParams.putIfAbsent("response_mode", "blocking");
        requestParams.putIfAbsent("auto_generate_name", true);

        String requestJson = objectMapper.writeValueAsString(requestParams);
        log.debug("发送Dify灵活参数聊天请求: {}", requestJson);

        RequestBody body = RequestBody.create(requestJson, MediaType.get("application/json; charset=utf-8"));
        Request httpRequest = buildRequest(difyConfig.getBaseUrl() + "/chat-messages", body);

        try (Response response = httpClient.newCall(httpRequest).execute()) {
//            if (!response.isSuccessful()) {
//                throw new IOException("Dify API调用失败: " + response.code() + " " + response.message());
//            }

            String responseBody = response.body().string();
            log.debug("收到Dify灵活参数聊天响应: {}", responseBody);
            JSONObject entries = JSONUtil.parseObj(responseBody);
            log.debug("Dify灵活参数聊天响应: {}", entries);
            return entries;
        }
    }

    /**
     * 发送聊天消息（阻塞式）- 简化版本
     * 只需要提供查询内容，其他参数使用默认值
     *
     * @param query 用户输入/提问内容
     * @return 聊天响应JSON对象
     * @throws IOException 当请求失败时抛出异常
     */
    public JSONObject sendMessage(String query) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        return sendMessage(params);
    }

    /**
     * 发送聊天消息（阻塞式）- 带用户标识版本
     *
     * @param query 用户输入/提问内容
     * @param user  用户标识
     * @return 聊天响应JSON对象
     * @throws IOException 当请求失败时抛出异常
     */
    public JSONObject sendMessage(String query, String user) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        params.put("user", user);
        return sendMessage(params);
    }

    /**
     * 发送聊天消息（流式）
     *
     * @param request 聊天请求
     * @return 流式响应Flux
     */
    public Flux<JSONObject> sendMessageStream(DifyChatRequest request) {
        return Flux.create(sink -> {
            try {
                // 设置为流式模式
                request.setResponseMode("streaming");

                String requestJson = objectMapper.writeValueAsString(request);
                log.debug("发送Dify流式聊天请求: {}", requestJson);

                RequestBody body = RequestBody.create(requestJson, MediaType.get("application/json; charset=utf-8"));
                Request httpRequest = buildRequest(difyConfig.getBaseUrl() + "/chat-messages", body);

                Call call = httpClient.newCall(httpRequest);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        log.error("Dify流式请求失败", e);
                        sink.error(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        log.info("收到Dify流式响应: {}", response.toString());
                        if (!response.isSuccessful()) {
                            sink.error(new IOException("Dify API调用失败: " + response.code() + " " + response.message()));
                            return;
                        }

                        try (ResponseBody responseBody = response.body()) {
                            if (responseBody == null) {
                                sink.error(new IOException("响应体为空"));
                                return;
                            }

                            // 直接从响应流中逐行读取数据，实现真正的流式处理
                            try (BufferedReader reader = new BufferedReader(responseBody.charStream())) {
                                String line;
                                log.info("开始读取Dify流式响应数据...");

                                while ((line = reader.readLine()) != null) {

                                    if (line.startsWith("data: ")) {
                                        String data = line.substring(6).trim();
                                        System.out.println("data:" + data);
                                        // 跳过空行和特殊标记
                                        if (data.isEmpty() || "[DONE]".equals(data)) {
                                            log.debug("跳过空行或结束标记: {}", data);
                                            continue;
                                        }

                                        try {
                                            JSONObject streamResponse = JSONUtil.parseObj(data);

                                            // 立即发送到下游
                                            sink.next(streamResponse);

                                            // 如果是错误事件，结束流
                                            String event = streamResponse.getStr("event");
                                            if ("error".equals(event)) {
                                                log.error("Dify流式响应错误: {}", streamResponse.getStr("error"));
                                                sink.error(new RuntimeException(streamResponse.getStr("error")));
                                                return;
                                            }

                                            // 如果是结束事件，完成流
                                            if ("message_end".equals(event)) {
                                                log.info("Dify流式响应完成");
                                                sink.complete();
                                                return;
                                            }

                                        } catch (Exception e) {
                                            log.warn("解析流式响应数据失败: {}", data, e);
                                            // 不中断流，继续处理下一条数据
                                        }
                                    }
                                }

                                log.info("流式响应读取完毕");
                                sink.complete();

                            } catch (Exception e) {
                                log.error("读取流式响应失败", e);
                                sink.error(e);
                            }
                        } catch (Exception e) {
                            log.error("处理Dify流式响应失败", e);
                            sink.error(e);
                        }
                    }
                });

                // 取消订阅时取消请求
                sink.onCancel(() -> {
                    if (!call.isCanceled()) {
                        call.cancel();
                    }
                });

            } catch (Exception e) {
                log.error("创建Dify流式请求失败", e);
                sink.error(e);
            }
        });
    }

    /**
     * 获取建议问题列表
     *
     * @param messageId 消息ID
     * @param user      用户ID
     * @return 建议问题响应JSON对象
     * @throws IOException 当请求失败时抛出异常
     */
    public JSONObject getSuggestedQuestions(String messageId, String user) throws IOException {
        String url = difyConfig.getBaseUrl() + "/messages/" + messageId + "/suggested";

        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if (user != null) {
            urlBuilder.addQueryParameter("user", user);
        }

        Request request = buildRequest(urlBuilder.build().toString(), null);

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("获取建议问题失败: " + response.code() + " " + response.message());
            }

            String responseBody = response.body().string();
            log.debug("收到建议问题响应: {}", responseBody);

            return JSONUtil.parseObj(responseBody);
        }
    }

    /**
     * 获取会话历史消息
     *
     * @param conversationId 会话ID
     * @param user           用户ID
     * @param firstId        第一条消息ID（用于分页）
     * @param limit          限制数量，默认20，范围1-100
     * @return 历史消息响应JSON对象
     * @throws IOException 当请求失败时抛出异常
     */
    public JSONObject getMessages(String conversationId, String user, String firstId, Integer limit)
            throws IOException {
        String url = difyConfig.getBaseUrl() + "/messages";

        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder()
                .addQueryParameter("conversation_id", conversationId)
                .addQueryParameter("user", user);

        if (firstId != null) {
            urlBuilder.addQueryParameter("first_id", firstId);
        }
        if (limit != null) {
            urlBuilder.addQueryParameter("limit", String.valueOf(limit));
        }

        Request request = buildRequest(urlBuilder.build().toString(), null);

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("获取历史消息失败: " + response.code() + " " + response.message());
            }

            String responseBody = response.body().string();
            log.debug("收到历史消息响应: {}", responseBody);

            return JSONUtil.parseObj(responseBody);
        }
    }

    /**
     * 获取会话列表
     *
     * @param user   用户ID
     * @param lastId 最后一条会话ID（用于分页）
     * @param limit  限制数量，默认20，最大100
     * @param pinned 是否置顶
     * @return 会话列表响应JSON对象
     * @throws IOException 当请求失败时抛出异常
     */
    public JSONObject getConversations(String user, String lastId, Integer limit, Boolean pinned) throws IOException {
        String url = difyConfig.getBaseUrl() + "/conversations";

        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder()
                .addQueryParameter("user", user);

        if (lastId != null) {
            urlBuilder.addQueryParameter("last_id", lastId);
        }
        if (limit != null) {
            urlBuilder.addQueryParameter("limit", String.valueOf(limit));
        }
        if (pinned != null) {
            urlBuilder.addQueryParameter("pinned", String.valueOf(pinned));
        }

        Request request = buildRequest(urlBuilder.build().toString(), null);

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("获取会话列表失败: " + response.code() + " " + response.message());
            }

            String responseBody = response.body().string();
            log.debug("收到会话列表响应: {}", responseBody);

            return JSONUtil.parseObj(responseBody);
        }
    }

    /**
     * 重命名会话
     *
     * @param conversationId 会话ID
     * @param name           新名称
     * @param user           用户ID
     * @param autoGenerate   是否自动生成
     * @return 更新结果JSON对象
     * @throws IOException 当请求失败时抛出异常
     */
    public JSONObject renameConversation(String conversationId, String name, String user, Boolean autoGenerate)
            throws IOException {
        String url = difyConfig.getBaseUrl() + "/conversations/" + conversationId + "/name";

        Map<String, Object> requestData = Map.of(
                "name", name != null ? name : "",
                "user", user,
                "auto_generate", autoGenerate != null ? autoGenerate : false);

        String requestJson = objectMapper.writeValueAsString(requestData);
        RequestBody body = RequestBody.create(requestJson, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .method("PATCH", body)
                .addHeader("Authorization", "Bearer " + difyConfig.getApiKey())
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("重命名会话失败: " + response.code() + " " + response.message());
            }

            String responseBody = response.body().string();
            return JSONUtil.parseObj(responseBody);
        }
    }

    /**
     * 构建HTTP请求
     *
     * @param url  请求URL
     * @param body 请求体（GET请求时为null）
     * @return Request对象
     */
    private Request buildRequest(String url, RequestBody body) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + difyConfig.getApiKey())
                .addHeader("Content-Type", "application/json");

        if (body != null) {
            builder.post(body);
        } else {
            builder.get();
        }

        return builder.build();
    }

    /**
     * 安全获取字符串值
     *
     * @param json         JSON对象
     * @param key          键名
     * @param defaultValue 默认值
     * @return 字符串值
     */
    public static String safeGetStr(JSONObject json, String key, String defaultValue) {
        if (json == null) {
            return defaultValue;
        }
        try {
            return json.getStr(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 安全获取整数值
     *
     * @param json         JSON对象
     * @param key          键名
     * @param defaultValue 默认值
     * @return 整数值
     */
    public static Integer safeGetInt(JSONObject json, String key, Integer defaultValue) {
        if (json == null) {
            return defaultValue;
        }
        try {
            return json.getInt(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 安全获取布尔值
     *
     * @param json         JSON对象
     * @param key          键名
     * @param defaultValue 默认值
     * @return 布尔值
     */
    public static Boolean safeGetBool(JSONObject json, String key, Boolean defaultValue) {
        if (json == null) {
            return defaultValue;
        }
        try {
            return json.getBool(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 检查客户端是否可用
     *
     * @return true表示可用，false表示不可用
     */
    public boolean isAvailable() {
        return difyConfig.getApiKey() != null &&
                !difyConfig.getApiKey().trim().isEmpty() &&
                difyConfig.getBaseUrl() != null &&
                !difyConfig.getBaseUrl().trim().isEmpty();
    }

    /**
     * 获取当前配置信息（脱敏）
     *
     * @return 配置信息字符串
     */
    public String getConfigInfo() {
        String maskedApiKey = difyConfig.getApiKey() != null && difyConfig.getApiKey().length() > 8
                ? difyConfig.getApiKey().substring(0, 8) + "****"
                : "****";
        return String.format("DifyClient[baseUrl=%s, apiKey=%s, timeout=%dms]",
                difyConfig.getBaseUrl(), maskedApiKey, difyConfig.getTimeout());
    }
}
