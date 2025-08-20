package com.mt.agent.workflow.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 表权限控制实体
 */
@Data
@TableName("table_permission")
public class TablePermission {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 数据库配置ID
     */
    private Long dbConfigId;
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 权限类型：1-查询，2-修改，3-删除
     */
    private Integer permissionType;
    
    /**
     * 是否启用：0-禁用，1-启用
     */
    private Integer enabled;
    
    /**
     * 创建时间
     */
    private Date createdAt;
    
    /**
     * 更新时间
     */
    private Date updatedAt;
}
