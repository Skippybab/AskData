package com.mt.agent.workflow.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("db_config_acl")
public class DbConfigAcl {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long dbConfigId;
    private Integer subjectType;
    private Long subjectId;
    private Integer permUse;
    private Integer permManage;
}


