package com.mt.agent.workflow.api.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mt.agent.workflow.api.entity.TableInfo;
import com.mt.agent.workflow.api.mapper.TableInfoMapper;
import com.mt.agent.workflow.api.util.BufferUtil;
import com.mt.agent.workflow.api.util.DdlParser;
import com.mt.agent.workflow.api.util.TableNameFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TableInfoService {

    private static final Logger log = LoggerFactory.getLogger(TableInfoService.class);

    @Autowired
    private TableInfoMapper tableInfoMapper;
    
    @Autowired
    private TableNameFormatter tableNameFormatter;

    @Autowired
    private BufferUtil bufferUtil;

    /**
     * 根据数据库配置ID获取所有启用的表的DDL信息，格式化为字符串。
     * @param dbConfigId 数据库配置ID
     * @return 格式化后的DDL字符串
     */
    public String getEnabledTablesDdl(Long dbConfigId) {
        return getEnabledTablesDdl(dbConfigId, null);
    }
    
    /**
     * 根据数据库配置ID和用户ID获取有权限的表的DDL信息，格式化为字符串。
     * @param dbConfigId 数据库配置ID
     * @param userId 用户ID
     * @return 格式化后的DDL字符串
     */
    public String getEnabledTablesDdl(Long dbConfigId, Long userId) {
//        log.info("🔍 [TableInfoService] 开始查询启用的表信息, dbConfigId: {}, userId: {}", dbConfigId, userId);
        
        QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("db_config_id", dbConfigId);
        queryWrapper.eq("enabled", 1);
        
        List<TableInfo> tableInfos = tableInfoMapper.selectList(queryWrapper);
        log.info("🔍 [TableInfoService] 查询到启用表数量: {}", tableInfos.size());
        
        // 如果没有找到启用的表，查询所有表信息进行调试
        if (tableInfos.isEmpty()) {
            log.warn("🔍 [TableInfoService] 没有找到启用的表，查询所有表信息进行调试");
            QueryWrapper<TableInfo> allTablesQuery = new QueryWrapper<>();
            allTablesQuery.eq("db_config_id", dbConfigId);
            List<TableInfo> allTables = tableInfoMapper.selectList(allTablesQuery);
            log.info("🔍 [TableInfoService] 该数据库配置下的所有表数量: {}", allTables.size());
            
            for (TableInfo table : allTables) {
                log.info("🔍 [TableInfoService] 表信息: id={}, name={}, enabled={}, ddl长度={}", 
                    table.getId(), 
                    table.getTableName(), 
                    table.getEnabled(),
                    table.getTableDdl() != null ? table.getTableDdl().length() : 0);
            }
        } else {
            log.info("🔍 [TableInfoService] 找到的启用表:");
            for (TableInfo table : tableInfos) {
                log.info("🔍 [TableInfoService] 启用表: id={}, name={}, ddl长度={}", 
                    table.getId(), 
                    table.getTableName(),
                    table.getTableDdl() != null ? table.getTableDdl().length() : 0);
            }
        }
        
        // 移除权限控制，所有表都有权限
        log.info("🔍 [TableInfoService] 跳过权限检查，所有表都有权限");
        
        String result = tableInfos.stream()
                .map(TableInfo::getTableDdl)
                .collect(Collectors.joining("\n\n"));
        
        log.info("🔍 [TableInfoService] 最终返回的DDL字符串长度: {}", result != null ? result.length() : 0);
        
        return result;
    }
    
    /**
     * 根据数据库配置ID和用户ID获取有权限的表的格式化信息，用于Dify接口的all_table_name参数
     * 使用TableNameFormatter格式化，确保符合Dify接口的格式要求
     * @param dbConfigId 数据库配置ID
     * @param userId 用户ID
     * @return 格式化后的表信息字符串
     */
    public String getEnabledTablesFormattedForDify(Long dbConfigId, Long userId) {
//        log.info("🔍 [TableInfoService] 开始查询启用的表信息用于Dify格式化, dbConfigId: {}, userId: {}", dbConfigId, userId);
        
        QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("db_config_id", dbConfigId);
        queryWrapper.eq("enabled", 1);
        
        List<TableInfo> tableInfos = tableInfoMapper.selectList(queryWrapper);
        log.info("🔍 [TableInfoService] 查询到启用表数量: {}", tableInfos.size());
        
        // 移除权限控制，所有表都有权限
        log.info("🔍 [TableInfoService] 跳过权限检查，所有表都有权限");
        
        // 使用TableNameFormatter格式化每个表的信息，确保符合Dify接口格式要求
        StringBuilder result = new StringBuilder();
        for (TableInfo tableInfo : tableInfos) {
            String formattedTableInfo = tableNameFormatter.formatTableNameForDify(
                tableInfo.getTableName(),
                tableInfo.getTableComment(),
                tableInfo.getTableDdl()
            );
            // 生成TableSchema
            String formattedTableSchema = tableNameFormatter.formatTableSchemaForExecutor(
                tableInfo.getTableName(),
                tableInfo.getTableComment(),
                tableInfo.getTableDdl()
            );
            bufferUtil.setField(userId.toString(), "TableSchema_result", formattedTableSchema, -1, TimeUnit.DAYS);
            
            if (formattedTableInfo != null && !formattedTableInfo.trim().isEmpty()) {
                if (result.length() > 0) {
                    result.append("\n\n");
                }
                result.append(formattedTableInfo);
            }
        }
        
        String finalResult = result.toString();
        log.info("🔍 [TableInfoService] 最终返回的格式化表信息长度: {}", finalResult != null ? finalResult.length() : 0);
        
        return finalResult;
    }
    
    /**
     * 获取格式化的表结构信息（用于提示词）
     * @param dbConfigId 数据库配置ID
     * @param userId 用户ID
     * @return 格式化的表结构信息
     */
    public String getFormattedTableStructures(Long dbConfigId, Long userId) {
        QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("db_config_id", dbConfigId);
        queryWrapper.eq("enabled", 1);
        
        List<TableInfo> tableInfos = tableInfoMapper.selectList(queryWrapper);
        
        // 移除权限控制，所有表都有权限
//        log.info("🔍 [TableInfoService] getFormattedTableStructures: 跳过权限检查，所有表都有权限");
        
        StringBuilder result = new StringBuilder();
        for (TableInfo tableInfo : tableInfos) {
            String formattedStructure = DdlParser.formatDdlToPrompt(tableInfo.getTableDdl());
            if (formattedStructure != null && !formattedStructure.trim().isEmpty()) {
                result.append(formattedStructure).append("\n\n");
            }
        }
        
        return result.toString();
    }
    
    /**
     * 获取单个表的格式化结构信息（用于提示词）
     * @param dbConfigId 数据库配置ID
     * @param tableId 表ID
     * @param userId 用户ID
     * @return 格式化的表结构信息
     */
    public String getFormattedTableStructure(Long dbConfigId, Long tableId, Long userId) {
        QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("db_config_id", dbConfigId);
        queryWrapper.eq("id", tableId);
        queryWrapper.eq("enabled", 1);
        
        TableInfo tableInfo = tableInfoMapper.selectOne(queryWrapper);
        
        if (tableInfo == null) {
            return null;
        }
        
        // 移除权限控制，所有表都有权限
        log.info("🔍 [TableInfoService] getFormattedTableStructure: 跳过权限检查，所有表都有权限");
        
        return DdlParser.formatDdlToPrompt(tableInfo.getTableDdl());
    }
    
    /**
     * 获取单个表的标准化格式信息（用于Dify接口的all_table_name参数）
     * @param dbConfigId 数据库配置ID
     * @param tableId 表ID
     * @param userId 用户ID
     * @return 标准格式的表信息
     */
    public String getStandardTableNameFormat(Long dbConfigId, Long tableId, Long userId) {
        log.info("🔍 [TableInfoService] 开始查询单个表信息, dbConfigId: {}, tableId: {}, userId: {}", dbConfigId, tableId, userId);
        
        QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("db_config_id", dbConfigId);
        queryWrapper.eq("id", tableId);
        queryWrapper.eq("enabled", 1);
        
        TableInfo tableInfo = tableInfoMapper.selectOne(queryWrapper);
        
        if (tableInfo == null) {
            log.warn("🔍 [TableInfoService] 未找到指定的表信息, dbConfigId: {}, tableId: {}", dbConfigId, tableId);
            
            // 查询该数据库下的所有表进行调试
            QueryWrapper<TableInfo> allTablesQuery = new QueryWrapper<>();
            allTablesQuery.eq("db_config_id", dbConfigId);
            List<TableInfo> allTables = tableInfoMapper.selectList(allTablesQuery);
            log.info("🔍 [TableInfoService] 该数据库配置下的所有表数量: {}", allTables.size());
            
            for (TableInfo table : allTables) {
                log.info("🔍 [TableInfoService] 表信息: id={}, name={}, enabled={}", 
                    table.getId(), 
                    table.getTableName(), 
                    table.getEnabled());
            }
            
            return null;
        }
        
        log.info("🔍 [TableInfoService] 找到表信息: id={}, name={}, enabled={}, ddl长度={}", 
            tableInfo.getId(), 
            tableInfo.getTableName(), 
            tableInfo.getEnabled(),
            tableInfo.getTableDdl() != null ? tableInfo.getTableDdl().length() : 0);
        
        // 移除权限控制，所有表都有权限
        log.info("🔍 [TableInfoService] getStandardTableNameFormat: 跳过权限检查，所有表都有权限");
        
        String result = tableNameFormatter.formatTableNameForDify(
            tableInfo.getTableName(),
            tableInfo.getTableComment(),
            tableInfo.getTableDdl()
        );
        
        log.info("🔍 [TableInfoService] 格式化后的表信息长度: {}", result != null ? result.length() : 0);
        
        return result;
    }
    
    /**
     * 获取表字段信息
     * @param dbConfigId 数据库配置ID
     * @param tableId 表ID
     * @return 字段信息列表
     */
    public List<java.util.Map<String, Object>> getTableColumns(Long dbConfigId, Long tableId) {
        log.info("🔍 [TableInfoService] 获取表字段信息, dbConfigId: {}, tableId: {}", dbConfigId, tableId);
        
        QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("db_config_id", dbConfigId);
        queryWrapper.eq("id", tableId);
        queryWrapper.eq("enabled", 1);
        
        TableInfo tableInfo = tableInfoMapper.selectOne(queryWrapper);
        
        if (tableInfo == null) {
            log.warn("🔍 [TableInfoService] 未找到表信息");
            return new java.util.ArrayList<>();
        }
        
        // 解析DDL获取字段信息
        return DdlParser.parseColumnsFromDdl(tableInfo.getTableDdl());
    }
    
    /**
     * 更新字段备注
     * @param dbConfigId 数据库配置ID
     * @param tableId 表ID
     * @param columnName 字段名
     * @param comment 备注信息
     * @return 是否成功
     */
    public boolean updateColumnComment(Long dbConfigId, Long tableId, String columnName, String comment) {
        log.info("🔍 [TableInfoService] 更新字段备注, dbConfigId: {}, tableId: {}, columnName: {}", 
                dbConfigId, tableId, columnName);
        
        // 这里应该更新DDL中的字段注释
        // 由于这是一个复杂的DDL修改操作，暂时返回成功
        // 实际实现中需要解析DDL，修改指定字段的注释，然后更新数据库
        
        try {
            QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("db_config_id", dbConfigId);
            queryWrapper.eq("id", tableId);
            
            TableInfo tableInfo = tableInfoMapper.selectOne(queryWrapper);
            if (tableInfo != null) {
                // 更新DDL中的字段注释（简化实现）
                String updatedDdl = DdlParser.updateColumnComment(tableInfo.getTableDdl(), columnName, comment);
                if (updatedDdl != null) {
                    tableInfo.setTableDdl(updatedDdl);
                    tableInfoMapper.updateById(tableInfo);
                    log.info("🔍 [TableInfoService] 字段备注更新成功");
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("🔍 [TableInfoService] 更新字段备注失败: {}", e.getMessage(), e);
        }
        
        return false;
    }
    
    /**
     * 根据表ID获取表名
     * @param tableId 表ID
     * @return 表名
     */
    public String getTableNameById(Long tableId) {
        TableInfo tableInfo = tableInfoMapper.selectById(tableId);
        return tableInfo != null ? tableInfo.getTableName() : null;
    }
    
    /**
     * 批量启用数据库下的所有表
     * 解决用户需要手动开启数据库表权限的问题
     * @param dbConfigId 数据库配置ID
     * @return 启用的表数量
     */
    public int enableAllTables(Long dbConfigId) {
        log.info("🔍 [TableInfoService] 开始批量启用所有表, dbConfigId: {}", dbConfigId);
        
        // 查询所有禁用的表
        QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("db_config_id", dbConfigId);
        queryWrapper.eq("enabled", 0);
        
        List<TableInfo> disabledTables = tableInfoMapper.selectList(queryWrapper);
        
        if (disabledTables.isEmpty()) {
            log.info("🔍 [TableInfoService] 没有禁用的表，无需启用");
            return 0;
        }
        
        log.info("🔍 [TableInfoService] 发现 {} 个禁用的表，正在启用", disabledTables.size());
        
        int enabledCount = 0;
        for (TableInfo tableInfo : disabledTables) {
            try {
                tableInfo.setEnabled(1);
                int result = tableInfoMapper.updateById(tableInfo);
                if (result > 0) {
                    enabledCount++;
                    log.debug("🔍 [TableInfoService] 成功启用表: {}", tableInfo.getTableName());
                }
            } catch (Exception e) {
                log.error("🔍 [TableInfoService] 启用表失败: {}, 错误: {}", tableInfo.getTableName(), e.getMessage());
            }
        }
        
        log.info("🔍 [TableInfoService] 批量启用完成，成功启用 {} 个表", enabledCount);
        return enabledCount;
    }
}
