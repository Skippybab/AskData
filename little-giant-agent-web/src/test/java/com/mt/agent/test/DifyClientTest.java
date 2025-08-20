package com.mt.agent.test;

import com.mt.agent.dify.DifyClient;
import com.mt.agent.dify.DifyEventHandler;
import com.mt.agent.dify.model.*;
import com.mt.agent.reporter.SubEventReporter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import cn.hutool.json.JSONObject;

import java.io.IOException;

/**
 * Dify客户端测试类
 * 提供Dify API调用的使用示例
 *
 * @author wsx
 * @date 2025/6/30
 */
@SpringBootTest
@Slf4j
public class DifyClientTest {

    @Autowired
    private DifyClient difyClient;

    /**
     * 测试普通聊天功能
     */
    @Test
    public void testChat() {
        try {
            // 构建聊天请求
            DifyChatRequest request = DifyChatRequest.builder()
                    .query("你好，请介绍一下你自己")
                    .user("test_user_001")
                    .responseMode("blocking")
                    .autoGenerateName(true)
                    .build();

            // 发送聊天请求
            JSONObject response = difyClient.sendMessage(request);
            
            String responseId = DifyClient.safeGetStr(response, "id", "");
            String conversationId = DifyClient.safeGetStr(response, "conversation_id", "");
            String answer = DifyClient.safeGetStr(response, "answer", "");
            
            log.info("聊天响应ID: {}", responseId);
            log.info("会话ID: {}", conversationId);
            log.info("回答内容: {}", answer);
            
        } catch (IOException e) {
            log.error("聊天测试失败", e);
        }
    }

    /**
     * 测试流式聊天功能
     */
    @Test
    public void testStreamChat() {
        try {
            // 构建聊天请求
            DifyChatRequest request = DifyChatRequest.builder()
                    .query("请详细介绍一下人工智能的发展历史")
                    .user("test_user_001")
                    .responseMode("streaming")
                    .autoGenerateName(true)
                    .build();

            // 发送流式聊天请求
            Flux<JSONObject> streamResponse = difyClient.sendMessageStream(request);
            
            // 创建模拟的事件报告器
            SubEventReporter mockReporter = createMockReporter();
            
            // 处理流式响应
            DifyEventHandler.handleDifyStreamEvents(streamResponse, mockReporter);
            
        } catch (Exception e) {
            log.error("流式聊天测试失败", e);
        }
    }

    /**
     * 测试获取建议问题
     */
    @Test
    public void testGetSuggestedQuestions() {
        try {
            // 这里需要一个真实的消息ID，通常来自之前的聊天响应
            String messageId = "test_message_id";
            String userId = "test_user_001";
            
            JSONObject response = difyClient.getSuggestedQuestions(messageId, userId);
            
            Object dataObj = response.get("data");
            int questionCount = 0;
            if (dataObj instanceof java.util.List) {
                java.util.List<?> dataList = (java.util.List<?>) dataObj;
                questionCount = dataList.size();
                log.info("建议问题数量: {}", questionCount);
                dataList.forEach(question -> log.info("建议问题: {}", question));
            } else {
                log.info("建议问题数量: {}", questionCount);
            }
            
        } catch (IOException e) {
            log.error("获取建议问题测试失败", e);
        }
    }

    /**
     * 测试获取会话历史消息
     */
    @Test
    public void testGetMessages() {
        try {
            String conversationId = "test_conversation_id";
            String userId = "test_user_001";
            
            JSONObject response = difyClient.getMessages(conversationId, userId, null, 20);
            
            Object dataObj = response.get("data");
            Boolean hasMore = DifyClient.safeGetBool(response, "has_more", false);
            
            int messageCount = 0;
            if (dataObj instanceof java.util.List) {
                java.util.List<?> dataList = (java.util.List<?>) dataObj;
                messageCount = dataList.size();
                
                dataList.forEach(messageObj -> {
                    if (messageObj instanceof java.util.Map) {
                        java.util.Map<?, ?> messageMap = (java.util.Map<?, ?>) messageObj;
                        log.info("消息ID: {}, 问题: {}, 回答: {}", 
                                messageMap.get("id"), messageMap.get("query"), messageMap.get("answer"));
                    }
                });
            }
            
            log.info("历史消息数量: {}", messageCount);
            log.info("是否还有更多消息: {}", hasMore);
            
        } catch (IOException e) {
            log.error("获取历史消息测试失败", e);
        }
    }

    /**
     * 测试获取会话列表
     */
    @Test
    public void testGetConversations() {
        try {
            String userId = "test_user_001";
            
            JSONObject response = difyClient.getConversations(userId, null, 20, null);
            
            Object dataObj = response.get("data");
            Boolean hasMore = DifyClient.safeGetBool(response, "has_more", false);
            
            int conversationCount = 0;
            if (dataObj instanceof java.util.List) {
                java.util.List<?> dataList = (java.util.List<?>) dataObj;
                conversationCount = dataList.size();
                
                dataList.forEach(conversationObj -> {
                    if (conversationObj instanceof java.util.Map) {
                        java.util.Map<?, ?> conversationMap = (java.util.Map<?, ?>) conversationObj;
                        log.info("会话ID: {}, 名称: {}, 状态: {}", 
                                conversationMap.get("id"), conversationMap.get("name"), conversationMap.get("status"));
                    }
                });
            }
            
            log.info("会话数量: {}", conversationCount);
            log.info("是否还有更多会话: {}", hasMore);
            
        } catch (IOException e) {
            log.error("获取会话列表测试失败", e);
        }
    }

    /**
     * 测试客户端状态
     */
    @Test
    public void testClientStatus() {
        log.info("客户端是否可用: {}", difyClient.isAvailable());
        log.info("客户端配置信息: {}", difyClient.getConfigInfo());
    }

    /**
     * 创建模拟的事件报告器
     */
    private SubEventReporter createMockReporter() {
        return new SubEventReporter(null) {
            @Override
            public void reportStep(String message) {
                log.info("📋 [步骤] {}", message);
            }

            @Override
            public void reportAnswer(String answer) {
                log.info("🤖 [回答] {}", answer);
            }

            @Override
            public void reportError(String errorMessage) {
                log.error("❌ [错误] {}", errorMessage);
            }

            @Override
            public void reportThinking(String thinkingContent) {
                log.info("🤔 [思考] {}", thinkingContent);
            }

            @Override
            public void reportRecommend(String recommend) {
                log.info("💡 [推荐] {}", recommend);
            }
        };
    }
} 