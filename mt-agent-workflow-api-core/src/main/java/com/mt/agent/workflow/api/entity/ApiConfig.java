package com.mt.agent.workflow.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * API配置实体类
 * 用于管理对外暴露的数据问答API接口
 */
@Data
@TableName("api_config")
public class ApiConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * API名称
     */
    private String apiName;
    
    /**
     * API路径（唯一）
     */
    private String apiPath;
    
    /**
     * API密钥（用于鉴权）
     */
    private String apiKey;
    
    /**
     * 关联的数据库配置ID
     */
    private Long dbConfigId;
    
    /**
     * 关联的数据表ID
     */
    private Long tableId;
    
    /**
     * API描述
     */
    private String description;
    
    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;
    
    /**
     * 调用次数
     */
    private Long callCount;
    
    /**
     * 最后调用时间
     */
    private Date lastCallTime;
    
    /**
     * 创建用户ID
     */
    private Long userId;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
    
    /**
     * 请求限制（每分钟最大请求数）
     */
    private Integer rateLimit;
    
    /**
     * 超时时间（秒）
     */
    private Integer timeout;
}