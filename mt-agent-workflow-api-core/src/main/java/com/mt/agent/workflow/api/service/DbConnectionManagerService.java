package com.mt.agent.workflow.api.service;

import com.mt.agent.workflow.api.entity.DbConfig;
import java.util.List;

/**
 * 数据库连接池管理服务
 * 负责自动管理数据库连接池的创建、维护和清理
 */
public interface DbConnectionManagerService {
    
    /**
     * 初始化所有启用的数据库连接
     */
    void initializeEnabledConnections();
    
    /**
     * 为指定的数据库配置创建连接池
     */
    boolean createConnectionPool(DbConfig config);
    
    /**
     * 关闭指定数据库配置的连接池
     */
    void closeConnectionPool(Long dbConfigId);
    
    /**
     * 获取数据库连接
     */
    java.sql.Connection getConnection(Long dbConfigId);
    
    /**
     * 测试数据库连接
     */
    boolean testConnection(DbConfig config);
    
    /**
     * 获取所有活跃的连接池信息
     */
    List<String> getActiveConnectionPools();
}
