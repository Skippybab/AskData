package com.mt.agent.workflow.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据问答响应DTO
 * 用于替代SSE方式，提供简单的JSON响应格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQuestionResponse {
    
    /**
     * 请求是否成功
     */
    private boolean success;
    
    /**
     * 会话ID
     */
    private Long sessionId;
    
    /**
     * 消息ID
     */
    private Long messageId;
    
    /**
     * AI思考过程
     */
    private String thinking;
    
    /**
     * 生成的Python代码
     */
    private String pythonCode;
    
    /**
     * SQL查询语句
     */
    private String sql;
    
    /**
     * 查询结果数据（JSON字符串或原始文本）
     */
    private String result;
    
    /**
     * 结果类型：table, single, text
     */
    private String resultType;
    
    /**
     * 结果统计信息
     */
    private String resultInfo;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 响应时间戳
     */
    private long timestamp;
    
    /**
     * 处理耗时（毫秒）
     */
    private long duration;
    
    /**
     * 创建成功响应
     */
    public static DataQuestionResponse success(Long sessionId, Long messageId) {
        return DataQuestionResponse.builder()
                .success(true)
                .sessionId(sessionId)
                .messageId(messageId)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建错误响应
     */
    public static DataQuestionResponse error(String error) {
        return DataQuestionResponse.builder()
                .success(false)
                .error(error)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
