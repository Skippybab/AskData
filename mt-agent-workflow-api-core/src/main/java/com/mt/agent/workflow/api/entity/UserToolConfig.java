package com.mt.agent.workflow.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_tool_config")
public class UserToolConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long userId;
    private String toolType; // sql_query, kb_qa, api_call等
    private String toolName;
    private String configJson;
    private Integer enabled; // 1=启用, 0=禁用
    private Long createdAtMs;
    private Long updatedAtMs;
}
