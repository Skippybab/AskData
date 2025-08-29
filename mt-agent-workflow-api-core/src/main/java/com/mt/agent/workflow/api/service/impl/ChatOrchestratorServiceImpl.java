package com.mt.agent.workflow.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.mt.agent.workflow.api.dto.DataQuestionResponse;
import com.mt.agent.workflow.api.dto.PythonExecutionResult;
import com.mt.agent.workflow.api.entity.ChatMessage;
import com.mt.agent.workflow.api.entity.ChatSession;
import com.mt.agent.workflow.api.service.ChatOrchestratorService;
import com.mt.agent.workflow.api.service.ChatService;
import com.mt.agent.workflow.api.service.DifyService;
import com.mt.agent.workflow.api.service.PythonExecutorService;
import com.mt.agent.workflow.api.service.TableInfoService;
import com.mt.agent.workflow.api.mapper.ChatMessageMapper;
import com.mt.agent.workflow.api.util.BufferUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final BufferUtil bufferUtil;



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
    
    @Override
    @Transactional
    public DataQuestionResponse processDataQuestionSync(Long sessionId, Long userId, String question, Long dbConfigId, Long tableId) {

        // 设置整体超时时间（4分钟，比前端超时时间短）
        long startTime = System.currentTimeMillis();
        long timeoutMs = 4 * 60 * 1000; // 4分钟
        
        DataQuestionResponse response = DataQuestionResponse.success(sessionId, null);
        
        try {
            ChatSession session = chatService.getSessionById(sessionId, userId);
            if (session == null) {
                // 创建指定ID的新会话，保持用户传入的sessionId
                String sessionName = "数据问答-" + System.currentTimeMillis();
                try {
                    session = chatService.createSessionWithId(sessionId, userId, sessionName, dbConfigId, null);
                } catch (Exception e) {
                    session = chatService.createSession(userId, sessionName, dbConfigId, null);
                    sessionId = session.getId(); // 使用新创建的会话ID
                    response = DataQuestionResponse.success(sessionId, null);
                }
            } else {
                log.info("🔍 [数据问答] 会话验证成功, sessionId: {}, sessionName: {}", sessionId, session.getSessionName());
            }
            
            // 1. 保存用户消息
            saveUserMessage(sessionId, userId, question);
            
            // 将当前会话ID和问题存储到缓存，供Python执行时使用
            String userIdStr = userId.toString();
            bufferUtil.setField(userIdStr, "current_session_id", sessionId.toString(), -1, java.util.concurrent.TimeUnit.DAYS);
            bufferUtil.setField(userIdStr, "current_question", question, -1, java.util.concurrent.TimeUnit.DAYS);
            
            // 2. 获取表信息（按session存储的简化版本）
            String tableInfo, tableableSchema;
            
            // 从session缓存获取表信息，并检查是否与当前请求匹配
            String tableInfoKey = "session_table_info_" + sessionId;
            String tableSchemaKey = "session_table_schema_" + sessionId;
            String tableIdsKey = "session_table_ids_" + sessionId;
            
            tableInfo = bufferUtil.getField(userIdStr, tableInfoKey);
            tableableSchema = bufferUtil.getField(userIdStr, tableSchemaKey);
            
            // 获取当前传入的表ID列表（来自DataQuestionController）
            String currentTableIdsStr = bufferUtil.getField(userIdStr, tableIdsKey);
            
            // 如果session缓存为空，使用默认逻辑生成
            if (tableInfo == null || tableableSchema == null) {
                log.info("🔍 [数据问答] Session缓存为空，使用默认逻辑生成表信息, sessionId: {}", sessionId);
                
                // 获取当前会话的表选择（已经在上面获取过了）
                List<Long> currentTableIds = null;
                if (currentTableIdsStr != null && !currentTableIdsStr.trim().isEmpty()) {
                    currentTableIds = java.util.Arrays.stream(currentTableIdsStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::valueOf)
                        .collect(java.util.stream.Collectors.toList());
                }
                
                // 生成表信息
                if (currentTableIds != null && !currentTableIds.isEmpty()) {
                    tableInfo = tableInfoService.getSelectedTablesFormattedForDify(dbConfigId, currentTableIds, userId);
                    tableableSchema = tableInfoService.getSelectedTablesFormattedForExecutor(dbConfigId, currentTableIds, userId);
                } else if (tableId != null) {
                    // 兼容模式：如果指定了单个表ID
                    tableInfo = tableInfoService.getStandardTableNameForDify(dbConfigId, tableId, userId);
                    tableableSchema = tableInfoService.getStandardTableNameForExecutor(dbConfigId, tableId, userId);
                } else {
                    // 获取所有启用的表信息
                    tableInfo = tableInfoService.getEnabledTablesFormattedForDify(dbConfigId, userId);
                    tableableSchema = tableInfoService.getEnabledTablesFormattedForExecutor(dbConfigId, userId);
                }
                
                // 存储到session缓存
                if (tableInfo != null && tableableSchema != null) {
                    bufferUtil.setField(userIdStr, tableInfoKey, tableInfo, 24, TimeUnit.HOURS);
                    bufferUtil.setField(userIdStr, tableSchemaKey, tableableSchema, 24, TimeUnit.HOURS);
                    log.info("🔍 [数据问答] 已存储到session缓存, sessionId: {}, tableInfo长度: {}, tableSchema长度: {}", 
                        sessionId, tableInfo.length(), tableableSchema.length());
                }
            } else {
                log.info("🔍 [数据问答] 使用session缓存的表信息, sessionId: {}, tableInfo长度: {}, tableSchema长度: {}", 
                    sessionId, tableInfo.length(), tableableSchema.length());
            }
            
            if (tableInfo == null || tableInfo.trim().isEmpty()) {
                response.setSuccess(false);
                response.setError("未找到表信息");
                response.setDuration(System.currentTimeMillis() - startTime);
                return response;
            }

            
            // 存储到统一缓存供Python使用
            bufferUtil.setField(userIdStr, "current_table_info", tableInfo, 24, TimeUnit.HOURS);
            bufferUtil.setField(userIdStr, "TableSchema_result", tableableSchema, 24, TimeUnit.HOURS);
            // 检查超时
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                response.setSuccess(false);
                response.setError("处理超时，请稍后重试");
                response.setDuration(System.currentTimeMillis() - startTime);
                return response;
            }
            
            // 获取最近3轮的对话历史和上一条助手回复
            List<Map<String, String>> history = chatService.getRecentSessionHistory(sessionId, 3);
            String lastReply = chatService.getLastAssistantReply(sessionId);
            String userIdentifier = "user_" + userId;
            
            // 将历史对话存储到缓存，供Python执行时使用
            if (!history.isEmpty()) {
                StringBuilder historyContext = new StringBuilder();
                for (Map<String, String> item : history) {
                    if ("user".equals(item.get("role"))) {
                        if (historyContext.length() > 0) {
                            historyContext.append("\n");
                        }
                        historyContext.append(item.get("content"));
                    }
                }
                bufferUtil.setField(userIdStr, "history_context", historyContext.toString(), -1, java.util.concurrent.TimeUnit.DAYS);
            }
            
            // 如果用户在会话中切换了表，确保使用最新的表信息
            // tableInfo已经在步骤2中根据当前的tableId或dbConfigId获取了最新的表信息
            
            String difyResponse = difyService.blockingChat(tableInfo, question, history, lastReply, userIdentifier)
                .block(); // 阻塞等待响应
            if (difyResponse == null || difyResponse.trim().isEmpty()) {
                response.setSuccess(false);
                response.setError("Dify服务返回空响应");
                response.setDuration(System.currentTimeMillis() - startTime);
                return response;
            }
            
            // 4. 处理Dify响应
            StringBuilder thinkingContent = new StringBuilder();
            StringBuilder pythonCode = new StringBuilder();
            
            try {
                JsonNode rootNode = objectMapper.readTree(difyResponse);
                
                if (rootNode.has("data") && rootNode.get("data").has("outputs") && rootNode.get("data").get("outputs").has("code")) {
                    String codeContent = rootNode.get("data").get("outputs").get("code").asText();
                    
                    Pattern thinkPattern = Pattern.compile("<think>(.*?)</think>", Pattern.DOTALL);
                    Matcher thinkMatcher = thinkPattern.matcher(codeContent);
                    if (thinkMatcher.find()) {
                        String thinking = thinkMatcher.group(1).trim();
                        thinkingContent.append(thinking);
                        // 设置思考内容到response对象
                        response.setThinking(thinking);
                    } else {
                        log.warn("🔍 [数据问答] 未找到思考内容标签");
                    }

                    // 支持多种Python代码块格式：```python, ```Python, ```py, ```PY
                    Pattern codePattern = Pattern.compile("```[Pp]ython\\s*(.*?)```", Pattern.DOTALL);
                    Matcher codeMatcher = codePattern.matcher(codeContent);
                    if (codeMatcher.find()) {
                        String extractedCode = codeMatcher.group(1).trim();
                        pythonCode.append(extractedCode);

                        // 设置Python代码到response对象
                        response.setPythonCode(extractedCode);
                        bufferUtil.savePythonCode(userIdentifier, extractedCode);
                    } else {
                        log.warn("🔍 [数据问答] 未找到Python代码块");
                    }
                } else {
                    log.error("🔍 [数据问答] Dify响应格式不正确，缺少data.outputs.code字段");
                }
            } catch (Exception e) {
                log.error("🔍 [数据问答] 解析Dify响应失败: {}", e.getMessage(), e);
                response.setSuccess(false);
                response.setError("解析Dify响应失败: " + e.getMessage());
                response.setDuration(System.currentTimeMillis() - startTime);
                return response;
            }

            ChatMessage initialMessage = saveInitialAssistantMessage(sessionId, userId, thinkingContent.toString(), pythonCode.toString());
            response.setMessageId(initialMessage.getId());
            
            // 6. 执行Python代码
            if (pythonCode.length() > 0) {
//                log.info("🔍 [数据问答] 步骤6: 执行Python代码");
                
                // 检查超时
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    log.error("🔍 [数据问答] 处理超时，已耗时: {}ms", System.currentTimeMillis() - startTime);
                    response.setSuccess(false);
                    response.setError("处理超时，请稍后重试");
                    response.setDuration(System.currentTimeMillis() - startTime);
                    return response;
                }
                PythonExecutionResult result = pythonExecutorService.executePythonCodeWithResult(initialMessage.getId(), dbConfigId,userId);
                log.debug("🔍 [数据问答] Python执行结果: {}", result.getData());
                
                // 7. 更新消息并构建响应
                initialMessage.setExecutionStatus(result.isSuccess() ? 1 : 2);
                if (result.isSuccess()) {
                    initialMessage.setExecutionResult(result.getData());
                    
                    // 设置执行结果到response对象
                    String responseContent = result.getData();
                    if (responseContent != null && !responseContent.trim().isEmpty()) {
                        response.setResult(responseContent);
                        
                        // 检测结果类型
                        if (responseContent.contains("查询结果:") && responseContent.contains("[{") && responseContent.contains("}]")) {
                            response.setResultType("table");
                            // 提取统计信息
                            if (responseContent.contains("共返回")) {
                                int startIdx = responseContent.indexOf("共返回");
                                int endIdx = responseContent.indexOf("\n", startIdx);
                                if (endIdx == -1) endIdx = responseContent.length();
                                response.setResultInfo(responseContent.substring(startIdx, endIdx));
                            }
                        } else if (responseContent.matches(".*\\d+.*") && responseContent.length() < 100) {
                            response.setResultType("single");
                        } else {
                            response.setResultType("text");
                        }
                    } else {
                        // 如果执行结果为空，使用思考内容作为响应
                        responseContent = thinkingContent.toString();
                        if (responseContent.trim().isEmpty()) {
                            responseContent = "查询执行完成，但未返回具体数据。";
                        }
                        response.setResult(responseContent);
                        response.setResultType("text");
                    }
                } else {
                    initialMessage.setErrorMessage(result.getErrorMessage());
                    initialMessage.setExecutionResult(result.getErrorMessage());
                    log.error("🔍 [数据问答] Python执行失败: {}", result.getErrorMessage());
                    response.setSuccess(false);
                    response.setError("Python代码执行失败: " + result.getErrorMessage());
                }
                messageMapper.updateById(initialMessage);
            } else {
                log.warn("🔍 [数据问答] 没有Python代码需要执行");
                // 如果没有Python代码，只返回思考内容
                String responseContent = thinkingContent.toString();
                if (responseContent.trim().isEmpty()) {
                    responseContent = "AI正在分析您的问题...";
                }
                response.setResult(responseContent);
                response.setResultType("text");
            }
            
            // 8. 设置处理时间

            // 最终超时检查
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                log.error("🔍 [数据问答] 处理超时，已耗时: {}ms", System.currentTimeMillis() - startTime);
                response.setSuccess(false);
                response.setError("处理超时，请稍后重试");
                response.setDuration(System.currentTimeMillis() - startTime);
                return response;
            }
            
            // 设置处理耗时
            response.setDuration(System.currentTimeMillis() - startTime);
            return response;
            
        } catch (Exception e) {
            log.error("🔍 [数据问答] 处理数据问答失败(同步版本): {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setError("处理数据问答失败: " + e.getMessage());
            response.setDuration(System.currentTimeMillis() - startTime);
            return response;
        }
    }
    
    @Override
    @Transactional
    public DataQuestionResponse processDataQuestionSync(Long sessionId, Long userId, String question, Long dbConfigId, Long tableId, String tableName) {
        // 如果提供了表名但没有表ID，尝试查找表ID
        if (tableId == null && tableName != null && !tableName.trim().isEmpty()) {
            tableId = tableInfoService.getTableIdByName(dbConfigId, tableName);
            if (tableId != null) {
                log.info("🔍 [数据问答] 根据表名 {} 找到表ID: {}", tableName, tableId);
            } else {
                log.warn("🔍 [数据问答] 根据表名 {} 未找到对应的表ID", tableName);
            }
        }
        // 调用原有的方法
        return processDataQuestionSync(sessionId, userId, question, dbConfigId, tableId);
    }
    

}
