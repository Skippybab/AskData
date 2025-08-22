package com.mt.agent.workflow.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sql_guard_policy")
public class SqlGuardPolicy {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long dbConfigId;
    private Integer readOnly;
    private String denyKeywords;
    private String tableBlacklist;
    private String tableWhitelist;
    private Long maxScanRows;
    private Integer maxTimeoutMs;
    private Integer status;
    private Long createdAtMs;
    private Long updatedAtMs;
}


