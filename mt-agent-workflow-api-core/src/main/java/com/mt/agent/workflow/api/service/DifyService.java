package com.mt.agent.workflow.api.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Dify服务接口
 * 负责与Dify API的交互
 */
public interface DifyService {
    
    /**
     * 流式调用Dify聊天API
     * 
     * @param allTableNames 所有表名和结构信息
     * @param userInput 用户输入的问题
     * @param history 对话历史
     * @param lastReply 上次回复
     * @param user 用户标识
     * @return 流式响应
     */
    Flux<String> streamChat(String allTableNames, String userInput, List<Map<String, String>> history, String lastReply, String user);

    /**
     * 阻塞式调用Dify聊天API
     * 
     * @param allTableNames 所有表名和结构信息
     * @param userInput 用户输入的问题
     * @param history 对话历史
     * @param lastReply 上次回复
     * @param user 用户标识
     * @return 阻塞式响应
     */
    Mono<String> blockingChat(String allTableNames, String userInput, List<Map<String, String>> history, String lastReply, String user);
}
