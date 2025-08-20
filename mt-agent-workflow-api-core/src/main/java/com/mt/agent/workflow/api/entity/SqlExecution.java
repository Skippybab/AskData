package com.mt.agent.workflow.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sql_execution")
public class SqlExecution {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long dbConfigId;
    private Long schemaVersionId;
    private String tablesUsed;
    private String sqlText;
    private Integer status;
    private Long durationMs;
    private Long affectedRows;
    private String errorMsg;
    private String resultCacheKey;
    private Long createdAtMs;
    private Long finishedAtMs;
}


