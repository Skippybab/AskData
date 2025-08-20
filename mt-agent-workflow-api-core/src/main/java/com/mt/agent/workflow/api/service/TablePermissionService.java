package com.mt.agent.workflow.api.service;

import java.util.List;

/**
 * 表权限服务接口
 */
public interface TablePermissionService {
    
    /**
     * 检查用户是否有表的查询权限
     * @param userId 用户ID
     * @param dbConfigId 数据库配置ID
     * @param tableName 表名
     * @return 是否有权限
     */
    boolean hasQueryPermission(Long userId, Long dbConfigId, String tableName);
    
    /**
     * 获取用户有权限的表列表
     * @param userId 用户ID
     * @param dbConfigId 数据库配置ID
     * @return 有权限的表名列表
     */
    List<String> getUserAccessibleTables(Long userId, Long dbConfigId);
    
    /**
     * 为用户授权表访问权限
     * @param userId 用户ID
     * @param dbConfigId 数据库配置ID
     * @param tableName 表名
     * @param permissionType 权限类型
     * @return 是否成功
     */
    boolean grantTablePermission(Long userId, Long dbConfigId, String tableName, Integer permissionType);
    
    /**
     * 撤销用户的表访问权限
     * @param userId 用户ID
     * @param dbConfigId 数据库配置ID
     * @param tableName 表名
     * @return 是否成功
     */
    boolean revokeTablePermission(Long userId, Long dbConfigId, String tableName);
}
