package com.mt.agent.ai.service;

import com.mt.agent.ai.enums.SiliconFlowModelType;
import com.mt.agent.ai.model.SiliconFlowRequestConfig;
import com.mt.agent.ai.model.impl.SiliconFlowChatClient;
import io.reactivex.Flowable;

/**
 * 硅基流动AI服务接口
 * 提供多种大模型调用方式，包括同步调用、流式调用、思考过程获取等
 */
public interface SiliconFlowAiService {
    /**
     * 同步调用 - 使用默认配置
     *
     * @param message 用户消息
     * @return 模型回复
     */
    String chat(String message);

    /**
     * 同步调用 - 指定模型类型
     *
     * @param message   用户消息
     * @param modelType 模型类型
     * @return 模型回复
     */
    String chat(String message, SiliconFlowModelType modelType);

    /**
     * 同步调用 - 自定义配置
     *
     * @param message 用户消息
     * @param config  请求配置
     * @return 模型回复
     */
    String chat(String message, SiliconFlowRequestConfig config);

    /**
     * 流式调用 - 使用默认配置
     *
     * @param message 用户消息
     * @return 流式回复
     */
    Flowable<String> chatStream(String message);

    /**
     * 流式调用 - 指定模型类型
     *
     * @param message   用户消息
     * @param modelType 模型类型
     * @return 流式回复
     */
    Flowable<String> chatStream(String message, SiliconFlowModelType modelType);

    /**
     * 流式调用 - 自定义配置
     *
     * @param message 用户消息
     * @param config  请求配置
     * @return 流式回复
     */
    Flowable<String> chatStream(String message, SiliconFlowRequestConfig config);

    /**
     * 获取思考过程 - 使用默认配置
     * 以同步方式返回思考过程和最终回复
     *
     * @param message 用户消息
     * @return 包含思考过程和最终回复的结果对象
     */
    SiliconFlowChatClient.ThinkingResult chatWithThinking(String message);

    /**
     * 获取思考过程 - 指定模型类型
     * 以同步方式返回思考过程和最终回复
     *
     * @param message   用户消息
     * @param modelType 模型类型
     * @return 包含思考过程和最终回复的结果对象
     */
    SiliconFlowChatClient.ThinkingResult chatWithThinking(String message, SiliconFlowModelType modelType);

    /**
     * 获取思考过程 - 自定义配置
     * 以同步方式返回思考过程和最终回复
     *
     * @param message 用户消息
     * @param config  请求配置
     * @return 包含思考过程和最终回复的结果对象
     */
    SiliconFlowChatClient.ThinkingResult chatWithThinking(String message, SiliconFlowRequestConfig config);

    /**
     * 流式获取思考过程 - 使用默认配置
     * 同时获取思考过程流和内容流
     *
     * @param message 用户消息
     * @return 包含思考过程和内容的流式结果对象
     */
    SiliconFlowChatClient.ThinkingStreamResult chatStreamWithThinking(String message);

    /**
     * 流式获取思考过程 - 指定模型类型
     * 同时获取思考过程流和内容流
     *
     * @param message   用户消息
     * @param modelType 模型类型
     * @return 包含思考过程和内容的流式结果对象
     */
    SiliconFlowChatClient.ThinkingStreamResult chatStreamWithThinking(String message, SiliconFlowModelType modelType);

    /**
     * 流式获取思考过程 - 自定义配置
     * 同时获取思考过程流和内容流
     *
     * @param message 用户消息
     * @param config  请求配置
     * @return 包含思考过程和内容的流式结果对象
     */
    SiliconFlowChatClient.ThinkingStreamResult chatStreamWithThinking(String message, SiliconFlowRequestConfig config);
}