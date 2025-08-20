package com.mt.agent.workflow.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mt.agent.workflow.api.entity.DbConfig;
import com.mt.agent.workflow.api.entity.TableInfo;
import com.mt.agent.workflow.api.infra.DbConnectionPoolManager;
import com.mt.agent.workflow.api.infra.ExternalDbExecutor;
import com.mt.agent.workflow.api.mapper.DbConfigMapper;
import com.mt.agent.workflow.api.mapper.TableInfoMapper;
import com.mt.agent.workflow.api.service.SchemaContextService;
import com.mt.agent.workflow.api.service.TablePermissionService;
import com.mt.agent.workflow.api.util.CryptoKeyProvider;
import com.mt.agent.workflow.api.util.DdlParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SchemaContextServiceImpl implements SchemaContextService {

    @Autowired
    private DbConfigMapper dbConfigMapper;
    @Autowired
    private TableInfoMapper tableInfoMapper;
    @Autowired
    private TablePermissionService tablePermissionService;

    @Override
    public String buildPromptContext(Long dbConfigId) {
        return buildPromptContext(dbConfigId, null);
    }
    
    @Override
    public String buildPromptContext(Long dbConfigId, Long userId) {
        try {
            // 获取启用的表信息
            List<TableInfo> enabledTables = tableInfoMapper.selectList(
                new LambdaQueryWrapper<TableInfo>()
                    .eq(TableInfo::getDbConfigId, dbConfigId)
                    .eq(TableInfo::getEnabled, 1)
                    .orderByAsc(TableInfo::getTableName)
                    .last("limit 20") // 限制表数量
            );

            if (enabledTables.isEmpty()) {
                return "当前没有可用的数据表，请先同步数据库结构并启用表。";
            }

            // 如果指定了用户ID，进行权限过滤
            if (userId != null) {
                List<String> accessibleTables = tablePermissionService.getUserAccessibleTables(userId, dbConfigId);
                enabledTables = enabledTables.stream()
                    .filter(table -> accessibleTables.contains(table.getTableName()))
                    .collect(Collectors.toList());
                
                if (enabledTables.isEmpty()) {
                    return "您没有访问任何表的权限，请联系管理员授权。";
                }
            }

            StringBuilder context = new StringBuilder();
            context.append("数据库结构信息：\n\n");

            for (TableInfo table : enabledTables) {
                // 使用DDL解析器解析表结构
                String tableStructure = DdlParser.formatDdlToPrompt(table.getTableDdl());
                if (tableStructure != null && !tableStructure.trim().isEmpty()) {
                    context.append(tableStructure).append("\n");
                } else {
                    // 如果解析失败，使用基本信息
                    context.append("表名: ").append(table.getTableName()).append("\n");
                    if (table.getTableComment() != null && !table.getTableComment().isEmpty()) {
                        context.append("说明: ").append(table.getTableComment()).append("\n");
                    }
                    context.append("字段：\n");
                    context.append("  - 字段信息解析失败，请检查DDL格式\n");
                    context.append("\n");
                }
            }

            context.append("请根据以上数据库结构，生成准确的SQL查询语句。");
            return context.toString();

        } catch (Exception e) {
            log.error("构建schema上下文失败: {}", e.getMessage(), e);
            return "构建数据库上下文信息失败: " + e.getMessage();
        }
    }

    @Override
    public String buildSchemaDigest(Long dbConfigId, int maxTables, int maxColumns) {
        return buildSchemaDigest(dbConfigId, maxTables, maxColumns, null);
    }
    
    @Override
    public String buildSchemaDigest(Long dbConfigId, int maxTables, int maxColumns, Long userId) {
        try {
            List<TableInfo> tables = tableInfoMapper.selectList(
                new LambdaQueryWrapper<TableInfo>()
                    .eq(TableInfo::getDbConfigId, dbConfigId)
                    .eq(TableInfo::getEnabled, 1)
                    .orderByAsc(TableInfo::getTableName)
                    .last("limit " + maxTables)
            );

            // 如果指定了用户ID，进行权限过滤
            if (userId != null) {
                List<String> accessibleTables = tablePermissionService.getUserAccessibleTables(userId, dbConfigId);
                tables = tables.stream()
                    .filter(table -> accessibleTables.contains(table.getTableName()))
                    .collect(Collectors.toList());
            }

            return tables.stream()
                .map(table -> table.getTableName() + 
                    (table.getTableComment() != null ? "(" + table.getTableComment() + ")" : ""))
                .collect(Collectors.joining(", "));

        } catch (Exception e) {
            log.error("构建schema摘要失败: {}", e.getMessage(), e);
            return "Schema摘要不可用";
        }
    }

    @Override
    public String getTableSample(Long dbConfigId, String tableName, int maxRows) {
        try {
            DbConfig config = dbConfigMapper.selectById(dbConfigId);
            if (config == null) return "{}";

            DataSource ds = DbConnectionPoolManager.getOrCreate(dbConfigId, config, CryptoKeyProvider.getMasterKey());
            
            String sql = "SELECT * FROM " + tableName + " LIMIT " + maxRows;
            ExternalDbExecutor.ExecOptions options = new ExternalDbExecutor.ExecOptions();
            options.maxRows = maxRows;
            options.queryTimeoutSeconds = 10;

            ExternalDbExecutor.QueryResult result = ExternalDbExecutor.query(ds, sql, options);
            
            // 简单转换为JSON字符串
            StringBuilder json = new StringBuilder("{\"rows\":[");
            for (int i = 0; i < result.rows.size(); i++) {
                if (i > 0) json.append(",");
                json.append("{");
                Map<String, Object> row = result.rows.get(i);
                int colIndex = 0;
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    if (colIndex > 0) json.append(",");
                    json.append("\"").append(entry.getKey()).append("\":\"")
                        .append(entry.getValue() != null ? entry.getValue().toString() : "").append("\"");
                    colIndex++;
                }
                json.append("}");
            }
            json.append("],\"count\":").append(result.rowCount).append("}");

            return json.toString();

        } catch (Exception e) {
            log.error("获取表样例数据失败: {}", e.getMessage(), e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 检查用户是否有表的访问权限
     */
    public boolean hasTableAccess(Long userId, Long dbConfigId, String tableName) {
        return tablePermissionService.hasQueryPermission(userId, dbConfigId, tableName);
    }
    
    /**
     * 为用户授权表访问权限
     */
    public boolean grantTableAccess(Long userId, Long dbConfigId, String tableName) {
        return tablePermissionService.grantTablePermission(userId, dbConfigId, tableName, 1);
    }
}
