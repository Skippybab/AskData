package com.mt.agent.workflow.api.dto;

import lombok.Data;

/**
 * 数据问答请求DTO
 */
@Data
public class DataQuestionRequest {
    
    /**
     * 会话ID
     */
    private Long sessionId;
    
    /**
     * 用户问题
     */
    private String question;
    
    /**
     * 数据库配置ID
     */
    private Long dbConfigId;
    
    /**
     * 表ID（可选）
     */
    private Long tableId;
    
    /**
     * 表名（可选，当没有tableId时使用）
     */
    private String tableName;
    
    /**
     * 是否需要流式响应
     */
    private Boolean streaming = false;
    
    /**
     * 超时时间（秒）
     */
    private Integer timeout = 240;
}