package com.mt.agent.workflow.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("query_result_cache")
public class QueryResultCache {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long dbConfigId;
    private Long schemaVersionId;
    private String sqlHash;
    private String previewJson; // 结果预览JSON(前N行+统计)
    private String resultPath; // 完整结果文件路径
    private Long rowCount; // 总行数
    private String columnHeadersJson; // 列头信息JSON
    private Long fileSizeBytes; // 完整结果文件大小
    private Long expiredAtMs;
    private Long createdAtMs;
}


