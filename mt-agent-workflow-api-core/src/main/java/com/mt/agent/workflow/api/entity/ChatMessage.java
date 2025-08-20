package com.mt.agent.workflow.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long sessionId;
    private Long userId;
    private String role; // user, assistant, system
    private String content;
    private String contentType; // text, sql, result, error, thinking, python_code
    private Long sqlExecutionId;
    private String difyTraceId;
    private Integer tokensUsed;
    private Long durationMs;
    private Integer status; // 1=成功,2=失败,0=处理中
    private String errorMessage;
    private String metadataJson;
    
    // 新增Dify接口相关字段
    private String thinkingContent; // AI思考过程内容
    private String pythonCode; // 生成的Python代码
    private String executionResult; // 执行结果
    private Integer executionStatus; // 执行状态：1成功,2失败,0执行中
    
    private Long createdAtMs;
}
