package com.mt.agent.workflow.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mt.agent.workflow.api.entity.DbConfig;
import com.mt.agent.workflow.api.entity.TableInfo;
import com.mt.agent.workflow.api.infra.DbConnectionPoolManager;
import com.mt.agent.workflow.api.infra.ExternalDbExecutor;
import com.mt.agent.workflow.api.mapper.DbConfigMapper;
import com.mt.agent.workflow.api.mapper.TableInfoMapper;
import com.mt.agent.workflow.api.service.SchemaContextService;
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

            // 移除权限控制，所有表都有权限
            log.info("🔍 [SchemaContextService] 跳过权限检查，所有表都有权限");

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

            // 根据项目需求，用户登录后无需权限控制，跳过权限过滤
            log.info("🔍 [SchemaContextService] 跳过权限过滤，所有已启用的表都可访问");

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
     * 根据项目需求，用户登录后无需权限控制，默认返回true
     */
    public boolean hasTableAccess(Long userId, Long dbConfigId, String tableName) {
        log.debug("🔍 [SchemaContextService] 权限检查跳过，默认返回有权限");
        return true;
    }
    
    /**
     * 为用户授权表访问权限
     * 根据项目需求，用户登录后无需权限控制，默认返回true
     */
    public boolean grantTableAccess(Long userId, Long dbConfigId, String tableName) {
        log.debug("🔍 [SchemaContextService] 权限授予跳过，默认返回成功");
        return true;
    }

    @Override
    public String getTableSchema(Long dbConfigId, String tableName) {
        try {
            log.info("🔍 [SchemaContextService] 获取表结构: dbConfigId={}, tableName={}", dbConfigId, tableName);
            
            // 如果tableName为空，返回所有表的结构
            if (tableName == null || tableName.trim().isEmpty()) {
                return buildPromptContext(dbConfigId);
            }
            
            // 查找指定表的信息
            TableInfo tableInfo = tableInfoMapper.selectOne(
                new LambdaQueryWrapper<TableInfo>()
                    .eq(TableInfo::getDbConfigId, dbConfigId)
                    .eq(TableInfo::getTableName, tableName)
                    .eq(TableInfo::getEnabled, 1)
            );
            
            if (tableInfo == null) {
                log.warn("🔍 [SchemaContextService] 未找到表信息: {}", tableName);
                // 尝试查找类似的表名
                List<TableInfo> allTables = tableInfoMapper.selectList(
                    new LambdaQueryWrapper<TableInfo>()
                        .eq(TableInfo::getDbConfigId, dbConfigId)
                        .eq(TableInfo::getEnabled, 1)
                        .orderByAsc(TableInfo::getTableName)
                        .last("limit 10")
                );
                
                StringBuilder availableTables = new StringBuilder("可用的表包括：\n");
                for (TableInfo table : allTables) {
                    availableTables.append("- ").append(table.getTableName());
                    if (table.getTableComment() != null && !table.getTableComment().isEmpty()) {
                        availableTables.append(" (").append(table.getTableComment()).append(")");
                    }
                    availableTables.append("\n");
                }
                return availableTables.toString();
            }
            
            // 使用DDL解析器解析表结构
            String tableStructure = DdlParser.formatDdlToPrompt(tableInfo.getTableDdl());
            if (tableStructure != null && !tableStructure.trim().isEmpty()) {
                log.info("🔍 [SchemaContextService] 成功解析表结构");
                return tableStructure;
            } else {
                // 如果DDL解析失败，使用基本信息
                log.warn("🔍 [SchemaContextService] DDL解析失败，使用基本信息");
                StringBuilder basicInfo = new StringBuilder();
                basicInfo.append("表名: ").append(tableInfo.getTableName()).append("\n");
                if (tableInfo.getTableComment() != null && !tableInfo.getTableComment().isEmpty()) {
                    basicInfo.append("说明: ").append(tableInfo.getTableComment()).append("\n");
                }
                basicInfo.append("字段信息解析失败，请检查DDL格式\n");
                return basicInfo.toString();
            }
            
        } catch (Exception e) {
            log.error("🔍 [SchemaContextService] 获取表结构失败: {}", e.getMessage(), e);
            return String.format("获取表结构失败: %s\n建议检查数据库连接和表名是否正确", e.getMessage());
        }
    }
}
