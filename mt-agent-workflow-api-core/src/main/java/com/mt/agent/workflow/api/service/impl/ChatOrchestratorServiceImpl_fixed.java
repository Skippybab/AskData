    @Override
    @Transactional
    public String processDataQuestionSync(Long sessionId, Long userId, String question, Long dbConfigId, Long tableId) {
        log.info("🔍 [数据问答] 开始处理数据问答(同步版本), sessionId: {}, userId: {}, dbConfigId: {}, tableId: {}", sessionId, userId, dbConfigId, tableId);
        log.info("🔍 [数据问答] 用户问题: {}", question);
        
        // 设置整体超时时间（4分钟，比前端超时时间短）
        long startTime = System.currentTimeMillis();
        long timeoutMs = 4 * 60 * 1000; // 4分钟
        
        DataQuestionResponse response = DataQuestionResponse.success(sessionId, null);
        
        try {
            // 1. 保存用户消息
            log.info("🔍 [数据问答] 步骤1: 保存用户消息");
            ChatMessage userMessage = saveUserMessage(sessionId, userId, question);
            log.info("🔍 [数据问答] 用户消息保存成功, messageId: {}", userMessage.getId());
            
            // 2. 获取表信息
            log.info("🔍 [数据问答] 步骤2: 获取表信息");
            String tableInfo;
            if (tableId != null) {
                // 如果指定了表ID，获取单个表的信息
                tableInfo = tableInfoService.getStandardTableNameFormat(dbConfigId, tableId, userId);
            } else {
                // 如果没有指定表ID，获取所有启用的表信息
                tableInfo = tableInfoService.getEnabledTablesDdl(dbConfigId, userId);
            }
            
            if (tableInfo == null || tableInfo.trim().isEmpty()) {
                log.error("🔍 [数据问答] 获取表信息失败: 表信息为空");
                response.setSuccess(false);
                response.setError("未找到表信息");
                response.setDuration(System.currentTimeMillis() - startTime);
                return objectMapper.writeValueAsString(response);
            }
            log.info("🔍 [数据问答] 表信息获取成功, 长度: {}", tableInfo.length());
            log.debug("🔍 [数据问答] 表信息内容: {}", tableInfo.substring(0, Math.min(200, tableInfo.length())) + "...");
            
            // 3. 调用Dify服务
            log.info("🔍 [数据问答] 步骤3: 调用Dify服务");
            
            // 检查超时
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                log.error("🔍 [数据问答] 处理超时，已耗时: {}ms", System.currentTimeMillis() - startTime);
                response.setSuccess(false);
                response.setError("处理超时，请稍后重试");
                response.setDuration(System.currentTimeMillis() - startTime);
                return objectMapper.writeValueAsString(response);
            }
            
            List<Map<String, String>> history = new ArrayList<>();
            String lastReply = null;
            String userIdentifier = "user_" + userId;
            
            log.info("🔍 [数据问答] Dify调用参数: userIdentifier={}, historySize={}, lastReply={}", userIdentifier, history.size(), lastReply);
            String difyResponse = difyService.blockingChat(tableInfo, question, history, lastReply, userIdentifier)
                .block(); // 阻塞等待响应
            if (difyResponse == null || difyResponse.trim().isEmpty()) {
                log.error("🔍 [数据问答] Dify服务返回空响应");
                response.setSuccess(false);
                response.setError("Dify服务返回空响应");
                response.setDuration(System.currentTimeMillis() - startTime);
                return objectMapper.writeValueAsString(response);
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
                        // 设置思考内容到response对象
                        response.setThinking(thinking);
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
                        // 设置Python代码到response对象
                        response.setPythonCode(extractedCode);
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
                return objectMapper.writeValueAsString(response);
            }
            
            // 5. 保存初始助手消息
            log.info("🔍 [数据问答] 步骤5: 保存初始助手消息");
            log.info("🔍 [数据问答] 思考内容长度: {}, Python代码长度: {}", thinkingContent.length(), pythonCode.length());
            ChatMessage initialMessage = saveInitialAssistantMessage(sessionId, userId, thinkingContent.toString(), pythonCode.toString());
            log.info("🔍 [数据问答] 初始助手消息保存成功, messageId: {}", initialMessage.getId());
            response.setMessageId(initialMessage.getId());
            
            // 6. 执行Python代码
            if (pythonCode.length() > 0) {
                log.info("🔍 [数据问答] 步骤6: 执行Python代码");
                
                // 检查超时
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    log.error("🔍 [数据问答] 处理超时，已耗时: {}ms", System.currentTimeMillis() - startTime);
                    response.setSuccess(false);
                    response.setError("处理超时，请稍后重试");
                    response.setDuration(System.currentTimeMillis() - startTime);
                    return objectMapper.writeValueAsString(response);
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
                log.info("🔍 [数据问答] 消息更新完成");
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
            log.info("🔍 [数据问答] 步骤8: 设置处理时间");
            
            // 最终超时检查
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                log.error("🔍 [数据问答] 处理超时，已耗时: {}ms", System.currentTimeMillis() - startTime);
                response.setSuccess(false);
                response.setError("处理超时，请稍后重试");
                response.setDuration(System.currentTimeMillis() - startTime);
                return objectMapper.writeValueAsString(response);
            }
            
            // 设置处理耗时
            response.setDuration(System.currentTimeMillis() - startTime);
            
            log.info("🔍 [数据问答] 数据问答处理完成(同步版本), sessionId: {}, 总耗时: {}ms", 
                    sessionId, response.getDuration());
            
            // 返回JSON格式的响应
            String jsonResponse = objectMapper.writeValueAsString(response);
            log.debug("🔍 [数据问答] 最终响应: {}", jsonResponse);
            return jsonResponse;
            
        } catch (Exception e) {
            log.error("🔍 [数据问答] 处理数据问答失败(同步版本): {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setError("处理数据问答失败: " + e.getMessage());
            response.setDuration(System.currentTimeMillis() - startTime);
            try {
                return objectMapper.writeValueAsString(response);
            } catch (Exception jsonEx) {
                log.error("序列化错误响应失败: {}", jsonEx.getMessage());
                return "{\"success\":false,\"error\":\"处理失败\"}";
            }
        }
    }