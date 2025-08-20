package com.mt.agent.workflow.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("schema_version")
public class SchemaVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long dbConfigId;
    private Integer versionNo;
    private Integer status;
    private Integer tableCount;
    private Integer columnCount;
    private String notes;
    private Long createdAtMs;
    private Long finishedAtMs;
}


