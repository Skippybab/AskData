package com.mt.agent.workflow.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("table_info")
public class TableInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dbConfigId;
    private String tableName;
    private String tableComment;
    private String tableDdl;
    private Integer enabled;
}
