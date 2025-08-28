package com.mt.agent.workflow.api.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mt.agent.workflow.api.entity.TableInfo;
import com.mt.agent.workflow.api.mapper.TableInfoMapper;
import com.mt.agent.workflow.api.util.DdlParser;
import com.mt.agent.workflow.api.util.TableNameFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
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

        String result = tableInfos.stream()
                .map(TableInfo::getTableDdl)
                .collect(Collectors.joining("\n\n"));
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
        QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("db_config_id", dbConfigId);
        queryWrapper.eq("enabled", 1);
        
        List<TableInfo> tableInfos = tableInfoMapper.selectList(queryWrapper);

        // 使用TableNameFormatter格式化每个表的信息，确保符合Dify接口格式要求
        StringBuilder result = new StringBuilder();
        for (TableInfo tableInfo : tableInfos) {
            String formattedTableInfo = tableNameFormatter.formatTableNameForDify(
                tableInfo.getTableName(),
                tableInfo.getTableComment(),
                tableInfo.getTableDdl()
            );
            if (formattedTableInfo != null && !formattedTableInfo.trim().isEmpty()) {
                if (result.length() > 0) {
                    result.append("\n\n");
                }
                result.append(formattedTableInfo);
            }
        }
        
        String finalResult = result.toString();
        return finalResult;
    }
    
    /**
     * 获取格式化的表结构信息（用于提示词）
     * @param dbConfigId 数据库配置ID
     * @param userId 用户ID
     * @return 格式化的表结构信息
     */
    public String getEnabledTablesFormattedForExecutor(Long dbConfigId, Long userId) {
        QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("db_config_id", dbConfigId);
        queryWrapper.eq("enabled", 1);

        List<TableInfo> tableInfos = tableInfoMapper.selectList(queryWrapper);
        // 生成TableSchema
        StringBuilder result = new StringBuilder();
        for (TableInfo tableInfo : tableInfos) {
            String formattedTableInfo = tableNameFormatter.formatTableSchemaForExecutor(
                    tableInfo.getTableName(),
                    tableInfo.getTableComment(),
                    tableInfo.getTableDdl()
            );
            if (formattedTableInfo != null && !formattedTableInfo.trim().isEmpty()) {
                if (result.length() > 0) {
                    result.append("\n\n");
                }
                result.append(formattedTableInfo);
            }
        }
        String finalResult = result.toString();
        return finalResult;
    }
    
    /**
     * 根据指定的表ID列表获取格式化的表信息（用于Dify接口）
     * @param dbConfigId 数据库配置ID
     * @param tableIds 表ID列表
     * @param userId 用户ID
     * @return 格式化后的表信息字符串
     */
    public String getSelectedTablesFormattedForDify(Long dbConfigId, List<Long> tableIds, Long userId) {
        if (tableIds == null || tableIds.isEmpty()) {
            log.warn("🔍 [TableInfoService] 表ID列表为空，返回空字符串");
            return "";
        }
        
        QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("db_config_id", dbConfigId);
        queryWrapper.in("id", tableIds);
        queryWrapper.eq("enabled", 1);
        
        List<TableInfo> tableInfos = tableInfoMapper.selectList(queryWrapper);
        log.info("🔍 [TableInfoService] 查询到 {} 个指定表信息", tableInfos.size());

        // 使用TableNameFormatter格式化每个表的信息，确保符合Dify接口格式要求
        StringBuilder result = new StringBuilder();
        for (TableInfo tableInfo : tableInfos) {
            String formattedTableInfo = tableNameFormatter.formatTableNameForDify(
                tableInfo.getTableName(),
                tableInfo.getTableComment(),
                tableInfo.getTableDdl()
            );
            if (formattedTableInfo != null && !formattedTableInfo.trim().isEmpty()) {
                if (result.length() > 0) {
                    result.append("\n\n");
                }
                result.append(formattedTableInfo);
            }
        }
        
        String finalResult = result.toString();
        log.info("🔍 [TableInfoService] 生成的Dify格式表信息长度: {}", finalResult.length());
        return finalResult;
    }
    
    /**
     * 根据指定的表ID列表获取格式化的表结构信息（用于执行器）
     * @param dbConfigId 数据库配置ID
     * @param tableIds 表ID列表
     * @param userId 用户ID
     * @return 格式化的表结构信息
     */
    public String getSelectedTablesFormattedForExecutor(Long dbConfigId, List<Long> tableIds, Long userId) {
        if (tableIds == null || tableIds.isEmpty()) {
            log.warn("🔍 [TableInfoService] 表ID列表为空，返回空字符串");
            return "";
        }
        
        QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("db_config_id", dbConfigId);
        queryWrapper.in("id", tableIds);
        queryWrapper.eq("enabled", 1);

        List<TableInfo> tableInfos = tableInfoMapper.selectList(queryWrapper);
        log.info("🔍 [TableInfoService] 查询到 {} 个指定表信息", tableInfos.size());
        
        // 生成TableSchema
        StringBuilder result = new StringBuilder();
        for (TableInfo tableInfo : tableInfos) {
            String formattedTableInfo = tableNameFormatter.formatTableSchemaForExecutor(
                    tableInfo.getTableName(),
                    tableInfo.getTableComment(),
                    tableInfo.getTableDdl()
            );
            if (formattedTableInfo != null && !formattedTableInfo.trim().isEmpty()) {
                if (result.length() > 0) {
                    result.append("\n\n");
                }
                result.append(formattedTableInfo);
            }
        }
        String finalResult = result.toString();
        log.info("🔍 [TableInfoService] 生成的执行器格式表信息长度: {}", finalResult.length());
        return finalResult;
    }
    
    /**
     * 根据表名获取表ID
     * @param dbConfigId 数据库配置ID
     * @param tableName 表名
     * @return 表ID，如果未找到则返回null
     */
    public Long getTableIdByName(Long dbConfigId, String tableName) {
        log.info("🔍 [TableInfoService] 根据表名查询表ID, dbConfigId: {}, tableName: {}", dbConfigId, tableName);
        
        QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("db_config_id", dbConfigId);
        queryWrapper.eq("table_name", tableName);
        queryWrapper.eq("enabled", 1);
        
        TableInfo tableInfo = tableInfoMapper.selectOne(queryWrapper);
        
        if (tableInfo == null) {
            log.warn("🔍 [TableInfoService] 未找到指定的表, dbConfigId: {}, tableName: {}", dbConfigId, tableName);
            return null;
        }
        
        log.info("🔍 [TableInfoService] 找到表ID: {}, name: {}", tableInfo.getId(), tableInfo.getTableName());
        return tableInfo.getId();
    }
    
    /**
     * 根据表名获取单个表的标准化格式信息（用于Dify接口的all_table_name参数）
     * @param dbConfigId 数据库配置ID
     * @param tableName 表名
     * @param userId 用户ID
     * @return 标准格式的表信息
     */
    public String getStandardTableNameFormatByName(Long dbConfigId, String tableName, Long userId) {
        log.info("🔍 [TableInfoService] 根据表名查询单个表信息, dbConfigId: {}, tableName: {}, userId: {}", dbConfigId, tableName, userId);
        
        QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("db_config_id", dbConfigId);
        queryWrapper.eq("table_name", tableName);
        queryWrapper.eq("enabled", 1);
        
        TableInfo tableInfo = tableInfoMapper.selectOne(queryWrapper);
        
        if (tableInfo == null) {
            log.warn("🔍 [TableInfoService] 未找到指定的表信息, dbConfigId: {}, tableName: {}", dbConfigId, tableName);
            return null;
        }
        
        log.info("🔍 [TableInfoService] 找到表信息: id={}, name={}, enabled={}, ddl长度={}", 
            tableInfo.getId(), 
            tableInfo.getTableName(), 
            tableInfo.getEnabled(),
            tableInfo.getTableDdl() != null ? tableInfo.getTableDdl().length() : 0);
        
        String result = tableNameFormatter.formatTableNameForDify(
            tableInfo.getTableName(),
            tableInfo.getTableComment(),
            tableInfo.getTableDdl()
        );
        return result;
    }
    
    /**
     * 获取单个表的标准化格式信息（用于Dify接口的all_table_name参数）
     * @param dbConfigId 数据库配置ID
     * @param tableId 表ID
     * @param userId 用户ID
     * @return 标准格式的表信息
     */
    public String getStandardTableNameForDify(Long dbConfigId, Long tableId, Long userId) {
        QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("db_config_id", dbConfigId);
        queryWrapper.eq("id", tableId);
        queryWrapper.eq("enabled", 1);
        
        TableInfo tableInfo = tableInfoMapper.selectOne(queryWrapper);
        
        if (tableInfo == null) {
            log.warn("🔍 [TableInfoService] 未找到指定的表信息, dbConfigId: {}, tableId: {}", dbConfigId, tableId);
            return null;
        }
        
        String result = tableNameFormatter.formatTableNameForDify(
            tableInfo.getTableName(),
            tableInfo.getTableComment(),
            tableInfo.getTableDdl()
        );
        
        return result;
    }

    /**
     * 获取单个表的标准化格式信息（用于Gen_Sql接口的tableSchema参数）
     * @param dbConfigId 数据库配置ID
     * @param tableId 表ID
     * @param userId 用户ID
     * @return 标准格式的表信息
     */
    public String getStandardTableNameForExecutor(Long dbConfigId, Long tableId, Long userId){
        QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("db_config_id", dbConfigId);
        queryWrapper.eq("id", tableId);
        queryWrapper.eq("enabled", 1);

        TableInfo tableInfo = tableInfoMapper.selectOne(queryWrapper);

        if (tableInfo == null) {
            log.warn("🔍 [TableInfoService] 未找到指定的表信息, dbConfigId: {}, tableId: {}", dbConfigId, tableId);
            return null;
        }

        String result = tableNameFormatter.formatTableSchemaForExecutor(
                tableInfo.getTableName(),
                tableInfo.getTableComment(),
                tableInfo.getTableDdl()
        );
        return result;
    }
    
    /**
     * 获取表字段信息
     * @param dbConfigId 数据库配置ID
     * @param tableId 表ID
     * @return 字段信息列表
     */
    public List<java.util.Map<String, Object>> getTableColumns(Long dbConfigId, Long tableId) {
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
        try {
            QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("db_config_id", dbConfigId);
            queryWrapper.eq("id", tableId);
            
            TableInfo tableInfo = tableInfoMapper.selectOne(queryWrapper);
            if (tableInfo != null) {
                // 更新DDL中的字段注释
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
