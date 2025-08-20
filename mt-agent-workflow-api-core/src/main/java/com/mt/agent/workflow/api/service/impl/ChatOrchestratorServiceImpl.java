package com.mt.agent.workflow.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.mt.agent.workflow.api.dto.PythonExecutionResult;
import com.mt.agent.workflow.api.entity.ChatMessage;
import com.mt.agent.workflow.api.service.ChatOrchestratorService;
import com.mt.agent.workflow.api.service.ChatService;
import com.mt.agent.workflow.api.service.DifyService;
import com.mt.agent.workflow.api.service.PythonExecutorService;
import com.mt.agent.workflow.api.service.TableInfoService;
import com.mt.agent.workflow.api.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatOrchestratorServiceImpl implements ChatOrchestratorService {

    private final DifyService difyService;
    private final TableInfoService tableInfoService;
    private final ChatService chatService;
    private final ChatMessageMapper messageMapper;
    private final ObjectMapper objectMapper;
    private final PythonExecutorService pythonExecutorService;

    private enum EventType {
        LLM_TOKEN("llm_token"),
        DONE("done"),
        ERROR("error");
        final String value;
        EventType(String value) {
            this.value = value;
        }
    }

    @Override
    public void processDataQuestion(Long sessionId, Long userId, String question, Long dbConfigId, Long tableId, SseEmitter emitter) {
        log.info("开始处理数据问答, sessionId: {}, userId: {}, dbConfigId: {}", sessionId, userId, dbConfigId);

        try {
            List<ChatMessage> historyMessages = chatService.getSessionMessages(sessionId, userId);
            saveUserMessage(sessionId, userId, question);

            String allTableNames = tableInfoService.getStandardTableNameFormat(dbConfigId, tableId, userId);
            if (allTableNames == null || allTableNames.isBlank()) {
                sendError(emitter, "没有找到可用的数据表。请先在数据管理中配置并启用表。");
                return;
            }

            List<Map<String, String>> history = historyMessages.stream()
                .map(msg -> Map.of("role", msg.getRole(), "content", msg.getContent()))
                .collect(Collectors.toList());

            String lastReply = historyMessages.stream()
                .filter(m -> "assistant".equals(m.getRole()))
                .map(ChatMessage::getContent)
                .reduce((first, second) -> second)
                .orElse(null);

            String userIdentifier = "user_" + userId;

            difyService.blockingChat(allTableNames, question, history, lastReply, userIdentifier)
                .subscribe(
                    responseData -> handleDifyResponse(responseData, sessionId, userId, dbConfigId, emitter),
                    error -> {
                        log.error("Dify API调用失败: {}", error.getMessage(), error);
                        sendError(emitter, "调用大模型服务失败: " + error.getMessage());
                    }
                );
        } catch (Exception e) {
            log.error("处理数据问答的同步部分时发生严重错误: {}", e.getMessage(), e);
            sendError(emitter, "处理请求时发生内部错误: " + e.getMessage());
        }
    }

    private void handleDifyResponse(String responseData, Long sessionId, Long userId, Long dbConfigId, SseEmitter emitter) {
        try {
            log.info("收到Dify响应，开始处理");
            StringBuilder thinkingContent = new StringBuilder();
            StringBuilder pythonCode = new StringBuilder();

            processDifyContent(responseData, thinkingContent, pythonCode, emitter);

            if (pythonCode.length() > 0) {
                // 1. 先保存包含思考过程和Python代码的初始消息
                ChatMessage initialMessage = saveInitialAssistantMessage(sessionId, userId, thinkingContent.toString(), pythonCode.toString());
                
                // 2. 执行代码
                PythonExecutionResult result = pythonExecutorService.executePythonCodeWithResult(initialMessage.getId(), dbConfigId);

                // 3. 更新消息并向前端发送结果
                updateMessageAndSendResult(initialMessage, result, emitter);
            } else {
                // 如果没有Python代码，只保存思考过程
                saveInitialAssistantMessage(sessionId, userId, thinkingContent.toString(), null);
            }

            emitter.send(SseEmitter.event().name(EventType.DONE.value).data("{\"status\":\"success\"}"));
            emitter.complete();
            log.info("数据问答处理完成, sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("处理Dify响应或执行Python代码失败: {}", e.getMessage(), e);
            sendError(emitter, "处理响应数据失败: " + e.getMessage());
        }
    }

    private void processDifyContent(String responseData, StringBuilder thinkingContent, StringBuilder pythonCode, SseEmitter emitter) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseData);
        if (rootNode.has("data") && rootNode.get("data").has("outputs") && rootNode.get("data").get("outputs").has("code")) {
            String codeContent = rootNode.get("data").get("outputs").get("code").asText();
            
            Pattern thinkPattern = Pattern.compile("<think>(.*?)</think>", Pattern.DOTALL);
            Matcher thinkMatcher = thinkPattern.matcher(codeContent);
            if (thinkMatcher.find()) {
                String thinking = thinkMatcher.group(1).trim();
                thinkingContent.append(thinking);
                sendSseMessage(emitter, EventType.LLM_TOKEN, Map.of("content", thinking, "type", "thinking"));
            }

            Pattern codePattern = Pattern.compile("```Python\\s*(.*?)\\s*```", Pattern.DOTALL);
            Matcher codeMatcher = codePattern.matcher(codeContent);
            if (codeMatcher.find()) {
                pythonCode.append(codeMatcher.group(1).trim());
            }
        }
    }

    private void updateMessageAndSendResult(ChatMessage message, PythonExecutionResult result, SseEmitter emitter) {
        message.setExecutionStatus(result.isSuccess() ? 1 : 2);
        if (result.isSuccess()) {
            message.setExecutionResult(result.getData());
            try {
                sendSseMessage(emitter, EventType.LLM_TOKEN, Map.of("content", result.getData(), "type", "sql_result"));
            } catch (Exception e) {
                log.error("发送SSE结果失败", e);
            }
        } else {
            message.setErrorMessage(result.getErrorMessage());
            message.setExecutionResult(result.getErrorMessage()); // Also store error in result field for visibility
            sendError(emitter, "Python代码执行失败: " + result.getErrorMessage());
        }
        messageMapper.updateById(message);
    }

    private ChatMessage saveInitialAssistantMessage(Long sessionId, Long userId, String thinkingContent, String pythonCode) {
        ChatMessage message = new ChatMessage();
        message.setTenantId(0L);
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole("assistant");
        message.setContent(thinkingContent != null && !thinkingContent.isEmpty() ? thinkingContent : "AI正在处理中...");
        message.setContentType("text");
        message.setStatus(0); // 状态：处理中
        message.setCreatedAtMs(System.currentTimeMillis());
        message.setThinkingContent(thinkingContent);
        message.setPythonCode(pythonCode);
        if (pythonCode != null && !pythonCode.isEmpty()) {
            message.setExecutionStatus(0); // 执行状态：执行中
        }
        messageMapper.insert(message);
        chatService.updateSessionMessageCount(sessionId);
        return message;
    }

    private ChatMessage saveUserMessage(Long sessionId, Long userId, String content) {
        ChatMessage message = new ChatMessage();
        message.setTenantId(0L);
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole("user");
        message.setContent(content);
        message.setContentType("text");
        message.setStatus(1);
        message.setCreatedAtMs(System.currentTimeMillis());
        messageMapper.insert(message);
        return message;
    }

    private void sendSseMessage(SseEmitter emitter, EventType eventType, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventType.value).data(objectMapper.writeValueAsString(data)));
        } catch (Exception e) {
            log.warn("发送SSE消息失败: event={}, data={}, error={}", eventType, data, e.getMessage());
        }
    }

    private void sendError(SseEmitter emitter, String errorMessage) {
        sendSseMessage(emitter, EventType.ERROR, Map.of("error", errorMessage));
        try {
            emitter.complete();
        } catch (Exception e) {
            log.warn("完成Emitter时出错: {}", e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public String processDataQuestionSync(Long sessionId, Long userId, String question, Long dbConfigId, Long tableId) {
        log.info("🔍 [数据问答] 开始处理数据问答(同步版本), sessionId: {}, userId: {}, dbConfigId: {}, tableId: {}", sessionId, userId, dbConfigId, tableId);
        log.info("🔍 [数据问答] 用户问题: {}", question);
        
        StringBuilder responseBuilder = new StringBuilder();
        
        // 设置整体超时时间（4分钟，比前端超时时间短）
        long startTime = System.currentTimeMillis();
        long timeoutMs = 4 * 60 * 1000; // 4分钟
        
        try {
            // 1. 保存用户消息
            log.info("🔍 [数据问答] 步骤1: 保存用户消息");
            ChatMessage userMessage = saveUserMessage(sessionId, userId, question);
            log.info("🔍 [数据问答] 用户消息保存成功, messageId: {}", userMessage.getId());
            
            // 2. 获取表信息
            log.info("🔍 [数据问答] 步骤2: 获取表信息");
            String tableInfo = tableInfoService.getStandardTableNameFormat(dbConfigId, tableId, userId);
            if (tableInfo == null || tableInfo.trim().isEmpty()) {
                log.error("🔍 [数据问答] 获取表信息失败: 表信息为空");
                return "event: error\ndata: {\"error\":\"未找到表信息\"}\n\n";
            }
            log.info("🔍 [数据问答] 表信息获取成功, 长度: {}", tableInfo.length());
            log.debug("🔍 [数据问答] 表信息内容: {}", tableInfo.substring(0, Math.min(200, tableInfo.length())) + "...");
            
            // 3. 调用Dify服务
            log.info("🔍 [数据问答] 步骤3: 调用Dify服务");
            
            // 检查超时
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                log.error("🔍 [数据问答] 处理超时，已耗时: {}ms", System.currentTimeMillis() - startTime);
                return "event: error\ndata: {\"error\":\"处理超时，请稍后重试\"}\n\n";
            }
            
            List<Map<String, String>> history = new ArrayList<>();
            String lastReply = null;
            String userIdentifier = "user_" + userId;
            
            log.info("🔍 [数据问答] Dify调用参数: userIdentifier={}, historySize={}, lastReply={}", userIdentifier, history.size(), lastReply);
            String difyResponse = difyService.blockingChat(tableInfo, question, history, lastReply, userIdentifier)
                .block(); // 阻塞等待响应
            if (difyResponse == null || difyResponse.trim().isEmpty()) {
                log.error("🔍 [数据问答] Dify服务返回空响应");
                return "event: error\ndata: {\"error\":\"Dify服务返回空响应\"}\n\n";
            }
            log.info("🔍 [数据问答] Dify响应接收成功, 长度: {}", difyResponse.length());
            log.debug("🔍 [数据问答] Dify响应内容: {}", difyResponse.substring(0, Math.min(500, difyResponse.length())) + "...");
            
            // 4. 处理Dify响应
            log.info("🔍 [数据问答] 步骤4: 处理Dify响应");
            StringBuilder thinkingContent = new StringBuilder();
            StringBuilder pythonCode = new StringBuilder();
            
            try {
                JsonNode rootNode = objectMapper.readTree(difyResponse);
                log.info("🔍 [数据问答] Dify响应JSON解析成功");
                
                if (rootNode.has("data") && rootNode.get("data").has("outputs") && rootNode.get("data").get("outputs").has("code")) {
                    String codeContent = rootNode.get("data").get("outputs").get("code").asText();
                    log.info("🔍 [数据问答] 提取到代码内容, 长度: {}", codeContent.length());
                    log.debug("🔍 [数据问答] 代码内容: {}", codeContent);
                    
                    Pattern thinkPattern = Pattern.compile("<think>(.*?)</think>", Pattern.DOTALL);
                    Matcher thinkMatcher = thinkPattern.matcher(codeContent);
                    if (thinkMatcher.find()) {
                        String thinking = thinkMatcher.group(1).trim();
                        thinkingContent.append(thinking);
                        log.info("🔍 [数据问答] 提取到思考内容, 长度: {}", thinking.length());
                        log.debug("🔍 [数据问答] 思考内容: {}", thinking.substring(0, Math.min(200, thinking.length())) + "...");
                        try {
                            Map<String, String> dataMap = new HashMap<>();
                            dataMap.put("content", thinking);
                            dataMap.put("type", "thinking");
                            String jsonData = objectMapper.writeValueAsString(dataMap);
                            responseBuilder.append("event: llm_token\ndata: ").append(jsonData).append("\n\n");
                        } catch (Exception e) {
                            log.error("🔍 [数据问答] 序列化思考内容失败: {}", e.getMessage(), e);
                            // 降级处理：使用简单转义
                            String escapedThinking = thinking.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
                            responseBuilder.append("event: llm_token\ndata: {\"content\":\"").append(escapedThinking).append("\",\"type\":\"thinking\"}\n\n");
                        }
                    } else {
                        log.warn("🔍 [数据问答] 未找到思考内容标签");
                    }

                    Pattern codePattern = Pattern.compile("```Python\\s*(.*?)\\s*```", Pattern.DOTALL);
                    Matcher codeMatcher = codePattern.matcher(codeContent);
                    if (codeMatcher.find()) {
                        String extractedCode = codeMatcher.group(1).trim();
                        pythonCode.append(extractedCode);
                        log.info("🔍 [数据问答] 提取到Python代码, 长度: {}", extractedCode.length());
                        log.debug("🔍 [数据问答] Python代码: {}", extractedCode);
                    } else {
                        log.warn("🔍 [数据问答] 未找到Python代码块");
                    }
                } else {
                    log.error("🔍 [数据问答] Dify响应格式不正确，缺少data.outputs.code字段");
                }
            } catch (Exception e) {
                log.error("🔍 [数据问答] 解析Dify响应失败: {}", e.getMessage(), e);
                return "event: error\ndata: {\"error\":\"解析Dify响应失败: " + e.getMessage() + "\"}\n\n";
            }
            
            // 5. 保存初始助手消息
            log.info("🔍 [数据问答] 步骤5: 保存初始助手消息");
            log.info("🔍 [数据问答] 思考内容长度: {}, Python代码长度: {}", thinkingContent.length(), pythonCode.length());
            ChatMessage initialMessage = saveInitialAssistantMessage(sessionId, userId, thinkingContent.toString(), pythonCode.toString());
            log.info("🔍 [数据问答] 初始助手消息保存成功, messageId: {}", initialMessage.getId());
            
            // 6. 执行Python代码
            if (pythonCode.length() > 0) {
                log.info("🔍 [数据问答] 步骤6: 执行Python代码");
                
                // 检查超时
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    log.error("🔍 [数据问答] 处理超时，已耗时: {}ms", System.currentTimeMillis() - startTime);
                    return "event: error\ndata: {\"error\":\"处理超时，请稍后重试\"}\n\n";
                }
                
                log.info("🔍 [数据问答] 开始执行Python代码, messageId: {}, dbConfigId: {}", initialMessage.getId(), dbConfigId);
                
                PythonExecutionResult result = pythonExecutorService.executePythonCodeWithResult(initialMessage.getId(), dbConfigId);
                
                log.info("🔍 [数据问答] Python执行完成, 成功: {}, 数据长度: {}", result.isSuccess(), 
                        result.getData() != null ? result.getData().length() : 0);
                log.debug("🔍 [数据问答] Python执行结果: {}", result.getData());
                
                // 7. 更新消息并构建响应
                log.info("🔍 [数据问答] 步骤7: 更新消息并构建响应");
                initialMessage.setExecutionStatus(result.isSuccess() ? 1 : 2);
                if (result.isSuccess()) {
                    initialMessage.setExecutionResult(result.getData());
                    
                    // 构建响应内容
                    String responseContent;
                    if (result.getData() != null && !result.getData().trim().isEmpty()) {
                        String rawData = result.getData();
                        log.info("🔍 [数据问答] 原始Python执行结果, 长度: {}", rawData.length());
                        
                        // 检测是否为Python字典列表格式
                        if (rawData.contains("查询结果:") && rawData.contains("[{") && rawData.contains("}]")) {
                            try {
                                // 提取Python字典列表部分
                                int startIndex = rawData.indexOf("[{");
                                int endIndex = rawData.lastIndexOf("}]") + 2;
                                if (startIndex != -1 && endIndex != -1) {
                                    String dictListStr = rawData.substring(startIndex, endIndex);
                                    log.info("🔍 [数据问答] 检测到Python字典列表, 长度: {}", dictListStr.length());
                                    
                                    // 构建包含元数据的JSON响应
                                    Map<String, Object> dataResponse = new HashMap<>();
                                    dataResponse.put("rawData", rawData);
                                    dataResponse.put("parsedData", dictListStr);
                                    dataResponse.put("dataType", "python_dict_list");
                                    dataResponse.put("message", "数据解析成功，请在前端查看表格展示");
                                    
                                    responseContent = objectMapper.writeValueAsString(dataResponse);
                                    log.info("🔍 [数据问答] 构建数据响应成功, 长度: {}", responseContent.length());
                                } else {
                                    responseContent = rawData;
                                }
                            } catch (Exception e) {
                                log.error("🔍 [数据问答] 解析Python字典列表失败: {}", e.getMessage(), e);
                                responseContent = rawData;
                            }
                        } else {
                            responseContent = rawData;
                        }
                        log.info("🔍 [数据问答] 使用处理后的Python执行结果作为响应, 长度: {}", responseContent.length());
                    } else {
                        // 如果执行结果为空，使用思考内容作为响应
                        responseContent = thinkingContent.toString();
                        if (responseContent.trim().isEmpty()) {
                            responseContent = "查询执行完成，但未返回具体数据。";
                        }
                        log.info("🔍 [数据问答] Python执行结果为空，使用思考内容作为响应, 长度: {}", responseContent.length());
                    }
                    
                    try {
                        Map<String, String> dataMap = new HashMap<>();
                        dataMap.put("content", responseContent);
                        dataMap.put("type", "sql_result");
                        String jsonData = objectMapper.writeValueAsString(dataMap);
                        log.info("🔍 [数据问答] SQL结果JSON序列化成功, 长度: {}", jsonData.length());
                        log.debug("🔍 [数据问答] SQL结果JSON内容: {}", jsonData);
                        responseBuilder.append("event: llm_token\ndata: ").append(jsonData).append("\n\n");
                    } catch (Exception e) {
                        log.error("🔍 [数据问答] 序列化SQL结果失败: {}", e.getMessage(), e);
                        log.error("🔍 [数据问答] 原始SQL结果内容: {}", responseContent);
                        // 降级处理：使用简单转义
                        String escapedContent = responseContent.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
                        String fallbackJson = "{\"content\":\"" + escapedContent + "\",\"type\":\"sql_result\"}";
                        log.info("🔍 [数据问答] 使用降级处理, JSON长度: {}", fallbackJson.length());
                        responseBuilder.append("event: llm_token\ndata: ").append(fallbackJson).append("\n\n");
                    }
                } else {
                    initialMessage.setErrorMessage(result.getErrorMessage());
                    initialMessage.setExecutionResult(result.getErrorMessage());
                    log.error("🔍 [数据问答] Python执行失败: {}", result.getErrorMessage());
                    responseBuilder.append("event: error\ndata: {\"error\":\"Python代码执行失败: ").append(result.getErrorMessage().replace("\"", "\\\"")).append("\"}\n\n");
                }
                messageMapper.updateById(initialMessage);
                log.info("🔍 [数据问答] 消息更新完成");
            } else {
                log.warn("🔍 [数据问答] 没有Python代码需要执行");
                // 如果没有Python代码，只返回思考内容
                String responseContent = thinkingContent.toString();
                if (responseContent.trim().isEmpty()) {
                    responseContent = "AI正在分析您的问题...";
                }
                try {
                    Map<String, String> dataMap = new HashMap<>();
                    dataMap.put("content", responseContent);
                    dataMap.put("type", "thinking");
                    String jsonData = objectMapper.writeValueAsString(dataMap);
                    responseBuilder.append("event: llm_token\ndata: ").append(jsonData).append("\n\n");
                } catch (Exception e) {
                    log.error("🔍 [数据问答] 序列化思考内容失败: {}", e.getMessage(), e);
                    // 降级处理：使用简单转义
                    String escapedContent = responseContent.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
                    responseBuilder.append("event: llm_token\ndata: {\"content\":\"").append(escapedContent).append("\",\"type\":\"thinking\"}\n\n");
                }
            }
            
            // 8. 添加完成事件
            log.info("🔍 [数据问答] 步骤8: 添加完成事件");
            
            // 最终超时检查
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                log.error("🔍 [数据问答] 处理超时，已耗时: {}ms", System.currentTimeMillis() - startTime);
                return "event: error\ndata: {\"error\":\"处理超时，请稍后重试\"}\n\n";
            }
            
            responseBuilder.append("event: done\ndata: {\"status\":\"success\"}\n\n");
            
            log.info("🔍 [数据问答] 数据问答处理完成(同步版本), sessionId: {}, 响应长度: {}, 总耗时: {}ms", 
                    sessionId, responseBuilder.length(), System.currentTimeMillis() - startTime);
            log.debug("🔍 [数据问答] 最终响应: {}", responseBuilder.toString());
            return responseBuilder.toString();
            
        } catch (Exception e) {
            log.error("🔍 [数据问答] 处理数据问答失败(同步版本): {}", e.getMessage(), e);
            return "event: error\ndata: {\"error\":\"处理数据问答失败: " + e.getMessage() + "\"}\n\n";
        }
    }
}
