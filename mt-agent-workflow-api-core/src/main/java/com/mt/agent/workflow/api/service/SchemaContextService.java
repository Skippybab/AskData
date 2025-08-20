package com.mt.agent.workflow.api.service;

/**
 * Schema上下文服务
 * 为NL2SQL构建数据库schema上下文信息
 */
public interface SchemaContextService {
    
    /**
     * 构建NL2SQL的Prompt上下文
     * @param dbConfigId 数据库配置ID
     * @return 格式化的schema上下文字符串
     */
    String buildPromptContext(Long dbConfigId);
    
    /**
     * 构建NL2SQL的Prompt上下文（带用户权限控制）
     * @param dbConfigId 数据库配置ID
     * @param userId 用户ID
     * @return 格式化的schema上下文字符串
     */
    String buildPromptContext(Long dbConfigId, Long userId);
    
    /**
     * 获取允许使用的表和列的摘要信息
     * @param dbConfigId 数据库配置ID
     * @param maxTables 最大表数量限制
     * @param maxColumns 最大列数量限制
     * @return schema摘要信息
     */
    String buildSchemaDigest(Long dbConfigId, int maxTables, int maxColumns);
    
    /**
     * 获取允许使用的表和列的摘要信息（带用户权限控制）
     * @param dbConfigId 数据库配置ID
     * @param maxTables 最大表数量限制
     * @param maxColumns 最大列数量限制
     * @param userId 用户ID
     * @return schema摘要信息
     */
    String buildSchemaDigest(Long dbConfigId, int maxTables, int maxColumns, Long userId);
    
    /**
     * 获取指定表的样例数据
     * @param dbConfigId 数据库配置ID
     * @param tableName 表名
     * @param maxRows 最大行数
     * @return 样例数据JSON
     */
    String getTableSample(Long dbConfigId, String tableName, int maxRows);
}
