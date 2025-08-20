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

@Service
public class TableInfoService {

    @Autowired
    private TableInfoMapper tableInfoMapper;
    
    @Autowired
    private TablePermissionService tablePermissionService;
    
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
        QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("db_config_id", dbConfigId);
        queryWrapper.eq("enabled", 1);
        
        List<TableInfo> tableInfos = tableInfoMapper.selectList(queryWrapper);
        
        // 如果指定了用户ID，进行权限过滤
        if (userId != null) {
            List<String> accessibleTables = tablePermissionService.getUserAccessibleTables(userId, dbConfigId);
            tableInfos = tableInfos.stream()
                    .filter(table -> accessibleTables.contains(table.getTableName()))
                    .collect(Collectors.toList());
        }
        
        return tableInfos.stream()
                .map(TableInfo::getTableDdl)
                .collect(Collectors.joining("\n\n"));
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
        
        // 暂时移除权限控制，实现最小闭环
        // 如果指定了用户ID，进行权限过滤
        // if (userId != null) {
        //     List<String> accessibleTables = tablePermissionService.getUserAccessibleTables(userId, dbConfigId);
        //     tableInfos = tableInfos.stream()
        //             .filter(table -> accessibleTables.contains(table.getTableName()))
        //             .collect(Collectors.toList());
        // }
        
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
        
        // 暂时移除权限控制，实现最小闭环
        // if (userId != null) {
        //     List<String> accessibleTables = tablePermissionService.getUserAccessibleTables(userId, dbConfigId);
        //     if (!accessibleTables.contains(tableInfo.getTableName())) {
        //         return null;
        //     }
        // }
        
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
        QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("db_config_id", dbConfigId);
        queryWrapper.eq("id", tableId);
        queryWrapper.eq("enabled", 1);
        
        TableInfo tableInfo = tableInfoMapper.selectOne(queryWrapper);
        
        if (tableInfo == null) {
            return null;
        }
        
        // 暂时移除权限控制，实现最小闭环
        // if (userId != null) {
        //     List<String> accessibleTables = tablePermissionService.getUserAccessibleTables(userId, dbConfigId);
        //     if (!accessibleTables.contains(tableInfo.getTableName())) {
        //         return null;
        //     }
        // }
        
        return tableNameFormatter.formatTableNameForDify(
            tableInfo.getTableName(),
            tableInfo.getTableComment(),
            tableInfo.getTableDdl()
        );
    }
}
