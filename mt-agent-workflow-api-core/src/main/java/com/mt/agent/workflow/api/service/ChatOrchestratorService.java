package com.mt.agent.workflow.api.service;

import com.mt.agent.workflow.api.dto.DataQuestionResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 聊天编排服务
 * 处理数据问答的完整流程：NL2SQL -> 执行 -> Dify解释 -> 流式返回
 */
public interface ChatOrchestratorService {
    
    /**
     * 处理用户消息，执行完整的数据问答流程（同步版本）
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param question 用户问题
     * @param dbConfigId 数据库配置ID
     * @param tableId 表ID
     * @return 数据问答响应对象
     */
    DataQuestionResponse processDataQuestionSync(Long sessionId, Long userId, String question, Long dbConfigId, Long tableId);
}
