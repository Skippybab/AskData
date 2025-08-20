package com.mt.agent.workflow.api.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 聊天编排服务
 * 处理数据问答的完整流程：NL2SQL -> 执行 -> Dify解释 -> 流式返回
 */
public interface ChatOrchestratorService {
    
    /**
     * 处理用户消息，执行完整的数据问答流程
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param question 用户问题
     * @param dbConfigId 数据库配置ID
     * @param emitter SSE发射器
     */
    void processDataQuestion(Long sessionId, Long userId, String question, Long dbConfigId, Long tableId, SseEmitter emitter);
    
    /**
     * 处理用户消息，执行完整的数据问答流程（同步版本）
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param question 用户问题
     * @param dbConfigId 数据库配置ID
     * @param tableId 表ID
     * @return SSE格式的响应字符串
     */
    String processDataQuestionSync(Long sessionId, Long userId, String question, Long dbConfigId, Long tableId);
    
    /**
     * 流式事件类型
     */
    enum EventType {
        SQL("sql"),              // 生成的SQL
        RESULT("result"),        // 查询结果
        LLM_TOKEN("llm_token"),  // LLM流式token
        ERROR("error"),          // 错误信息
        DONE("done");            // 完成事件
        
        public final String value;
        
        EventType(String value) {
            this.value = value;
        }
    }
}
