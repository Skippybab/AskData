package com.mt.agent.workflow.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mt.agent.workflow.api.entity.TableInfo;
import com.mt.agent.workflow.api.entity.SchemaVersion;
import com.mt.agent.workflow.api.mapper.TableInfoMapper;
import com.mt.agent.workflow.api.mapper.SchemaVersionMapper;
import com.mt.agent.workflow.api.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;
import com.mt.agent.workflow.api.entity.DbConfig;
import com.mt.agent.workflow.api.mapper.DbConfigMapper;
import com.mt.agent.workflow.api.util.PasswordCipherService;
import com.mt.agent.workflow.api.util.CryptoKeyProvider;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Slf4j
@RestController
@RequestMapping("/api/db/schema")
@CrossOrigin
public class SchemaController {

    @Autowired
    private TableInfoMapper tableInfoMapper;
    @Autowired
    private SchemaVersionMapper schemaVersionMapper;
    @Autowired
    private DbConfigMapper dbConfigMapper;

    @GetMapping("/{dbConfigId}/tables")
    public Result<List<TableInfo>> listTables(@PathVariable Long dbConfigId) {
        LambdaQueryWrapper<TableInfo> qw = new LambdaQueryWrapper<TableInfo>()
                .eq(TableInfo::getDbConfigId, dbConfigId);
        List<TableInfo> tables = tableInfoMapper.selectList(qw);
        
        // 添加调试日志
        log.info("查询数据库 {} 的表信息，共找到 {} 个表", dbConfigId, tables.size());
        for (TableInfo table : tables) {
            log.info("表: {}, enabled: {}, DDL长度: {}", 
                    table.getTableName(), 
                    table.getEnabled(), 
                    table.getTableDdl() != null ? table.getTableDdl().length() : 0);
        }
        
        return Result.success(tables);
    }

    @GetMapping("/{dbConfigId}/tables/enabled")
    public Result<List<TableInfo>> listEnabledTables(@PathVariable Long dbConfigId) {
        LambdaQueryWrapper<TableInfo> qw = new LambdaQueryWrapper<TableInfo>()
                .eq(TableInfo::getDbConfigId, dbConfigId)
                .eq(TableInfo::getEnabled, 1);
        List<TableInfo> tables = tableInfoMapper.selectList(qw);
        
        return Result.success(tables);
    }

    @PutMapping("/{dbConfigId}/tables/{tableId}/enabled")
    public Result<String> setTableEnabled(@PathVariable Long dbConfigId, @PathVariable Long tableId,
                                         @RequestBody Map<String, Object> request) {
        try {
            Boolean enabled = (Boolean) request.get("enabled");
            if (enabled == null) {
                return Result.error("enabled参数不能为空");
            }

            LambdaUpdateWrapper<TableInfo> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(TableInfo::getId, tableId)
                    .eq(TableInfo::getDbConfigId, dbConfigId)
                    .set(TableInfo::getEnabled, enabled ? 1 : 0);

            int updatedRows = tableInfoMapper.update(null, updateWrapper);
            if (updatedRows > 0) {
                return Result.success("设置成功");
            } else {
                return Result.error("表不存在");
            }
        } catch (Exception e) {
            return Result.error("设置失败: " + e.getMessage());
        }
    }

    @PutMapping("/{dbConfigId}/tables/enabled")
    public Result<String> setTablesEnabled(@PathVariable Long dbConfigId,
                                          @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> tableIdsInt = (List<Integer>) request.get("tableIds");
            List<Long> tableIds = tableIdsInt.stream().map(Long::valueOf).collect(Collectors.toList());
            
            Boolean enabled = (Boolean) request.get("enabled");

            if (tableIds == null || tableIds.isEmpty()) {
                return Result.error("tableIds参数不能为空");
            }
            if (enabled == null) {
                return Result.error("enabled参数不能为空");
            }

            LambdaUpdateWrapper<TableInfo> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(TableInfo::getId, tableIds)
                    .eq(TableInfo::getDbConfigId, dbConfigId)
                    .set(TableInfo::getEnabled, enabled ? 1 : 0);

            int result = tableInfoMapper.update(null, updateWrapper);
            return Result.success("批量设置成功，影响 " + result + " 个表");
        } catch (Exception e) {
            return Result.error("批量设置失败: " + e.getMessage());
        }
    }
    
    // Note: The endpoints for status and columns are removed for now as they depended on the old entities.
    // They need to be re-implemented if the functionality is required.
    
    /**
     * 获取表的字段信息
     */
    @GetMapping("/{dbConfigId}/tables/{tableId}/columns")
    public Result<List<Map<String, Object>>> listColumns(@PathVariable Long dbConfigId, @PathVariable Long tableId) {
        try {
            log.info("开始获取字段信息 - dbConfigId: {}, tableId: {}", dbConfigId, tableId);
            
            // 从table_info表中获取表的DDL信息
            TableInfo tableInfo = tableInfoMapper.selectById(tableId);
            if (tableInfo == null || !tableInfo.getDbConfigId().equals(dbConfigId)) {
                log.error("表信息不存在 - tableId: {}, dbConfigId: {}", tableId, dbConfigId);
                return Result.error("表信息不存在");
            }
            
            log.info("找到表信息 - 表名: {}, DDL长度: {}", 
                    tableInfo.getTableName(), 
                    tableInfo.getTableDdl() != null ? tableInfo.getTableDdl().length() : 0);
            
            String ddl = tableInfo.getTableDdl();
            List<Map<String, Object>> columns = new ArrayList<>();
            
            if (ddl != null && !ddl.trim().isEmpty()) {
                // 尝试从DDL解析字段信息
                columns = parseColumnsFromDdl(ddl);
                log.info("从DDL解析字段信息，共解析出 {} 个字段", columns.size());
                
                // 输出前几个字段的详细信息用于调试
                for (int i = 0; i < Math.min(3, columns.size()); i++) {
                    Map<String, Object> col = columns.get(i);
                    log.debug("字段 {}: {}", i + 1, col);
                }
            } else {
                log.warn("表的DDL信息为空 - 表名: {}", tableInfo.getTableName());
            }
            
            // 如果DDL解析失败或字段数量为0，尝试从数据库直接查询
            if (columns.isEmpty()) {
                log.warn("DDL解析失败或字段为空，尝试从数据库直接查询字段信息");
                columns = queryColumnsFromDatabase(dbConfigId, tableInfo.getTableName());
                log.info("从数据库查询字段信息，共查询到 {} 个字段", columns.size());
            }
            
            log.info("最终返回字段数量: {}", columns.size());
            return Result.success(columns);
        } catch (Exception e) {
            log.error("获取字段信息失败: {}", e.getMessage(), e);
            return Result.error("获取字段信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 从DDL中解析字段信息
     */
    private List<Map<String, Object>> parseColumnsFromDdl(String ddl) {
        List<Map<String, Object>> columns = new ArrayList<>();
        
        try {
            log.debug("开始解析DDL: {}", ddl.substring(0, Math.min(200, ddl.length())) + "...");
            
            // 使用正则表达式提取字段定义
            String ddlUpper = ddl.toUpperCase();
            
            // 找到CREATE TABLE语句的字段定义部分
            int startIndex = ddlUpper.indexOf("(");
            if (startIndex == -1) {
                log.warn("DDL中未找到字段定义开始标记");
                return columns;
            }
            
            // 找到字段定义结束位置（最后一个字段后的逗号到PRIMARY KEY/KEY/INDEX之前）
            int endIndex = findColumnDefinitionEnd(ddl, startIndex);
            if (endIndex == -1) {
                log.warn("DDL中未找到字段定义结束位置");
                return columns;
            }
            
            // 提取字段定义部分
            String columnDefinitions = ddl.substring(startIndex + 1, endIndex);
            log.debug("字段定义部分: {}", columnDefinitions);
            
            // 按逗号安全分割字段定义（忽略括号与字符串中的逗号）
            List<String> fieldDefs = splitColumnDefinitionsSafely(columnDefinitions);
            
            for (String fieldDef : fieldDefs) {
                fieldDef = fieldDef.trim();
                if (fieldDef.isEmpty()) continue;
                
                // 跳过约束定义
                if (isConstraintDefinition(fieldDef)) {
                    continue;
                }
                
                Map<String, Object> column = parseColumnDefinition(fieldDef);
                if (column != null) {
                    columns.add(column);
                    log.debug("解析字段: {}", column.get("columnName"));
                }
            }
            
            log.info("DDL解析完成，共解析出 {} 个字段", columns.size());
            
        } catch (Exception e) {
            log.error("解析DDL失败: {}", e.getMessage(), e);
        }
        
        return columns;
    }
    
    /**
     * 查找字段定义结束位置
     */
    private int findColumnDefinitionEnd(String ddl, int startIndex) {
        int parenCount = 0;
        boolean inString = false;
        char stringChar = 0;
        
        for (int i = startIndex; i < ddl.length(); i++) {
            char c = ddl.charAt(i);
            
            // 处理字符串
            if ((c == '\'' || c == '"') && (i == 0 || ddl.charAt(i-1) != '\\')) {
                if (!inString) {
                    inString = true;
                    stringChar = c;
                } else if (c == stringChar) {
                    inString = false;
                }
                continue;
            }
            
            if (inString) continue;
            
            // 处理括号
            if (c == '(') {
                parenCount++;
            } else if (c == ')') {
                parenCount--;
                if (parenCount == 0) {
                    // 找到与 CREATE TABLE 的 '(' 对应的 ')'，即字段与约束定义整体的结束
                    return i;
                }
            }
        }
        
        return -1;
    }
    
    /**
     * 判断是否为约束定义
     */
    private boolean isConstraintDefinition(String fieldDef) {
        String upper = fieldDef.toUpperCase();
        return upper.startsWith("PRIMARY KEY") || 
               upper.startsWith("KEY") || 
               upper.startsWith("INDEX") || 
               upper.startsWith("UNIQUE") ||
               upper.startsWith("FOREIGN KEY") ||
               upper.startsWith("CONSTRAINT");
    }
    
    /**
     * 解析字段定义
     */
    private Map<String, Object> parseColumnDefinition(String fieldDef) {
        try {
            // 移除反引号
            fieldDef = fieldDef.replaceAll("`", "");
            
            // 使用正则表达式解析字段定义
            // 格式: column_name data_type[(..)] [UNSIGNED|ZEROFILL] ...
            // 兼容如: double(15,2)、decimal(10, 4)、bigint、varchar(255)
            String pattern = "^\\s*(\\w+)\\s+([A-Z0-9_]+(?:\\s+(?:UNSIGNED|ZEROFILL))?(?:\\s*\\([^)]*\\))?)\\s*(.*)$";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(fieldDef);
            
            if (!m.find()) {
                log.debug("无法解析字段定义: {}", fieldDef);
                return null;
            }
            
            String columnName = m.group(1);
            String dataType = m.group(2);
            String remaining = m.group(3);
            
            Map<String, Object> column = new HashMap<>();
            column.put("columnName", columnName);
            column.put("dbDataType", dataType.trim());
            
            // 解析NOT NULL
            boolean isNullable = !remaining.toUpperCase().contains("NOT NULL");
            column.put("isNullable", isNullable ? 1 : 0);
            
            // 解析DEFAULT值
            String defaultValue = extractDefaultValue(remaining);
            column.put("columnDefault", defaultValue);
            
            // 解析COMMENT
            String comment = extractComment(remaining);
            column.put("columnComment", comment);
            
            return column;
            
        } catch (Exception e) {
            log.warn("解析字段定义失败: {}", fieldDef, e);
            return null;
        }
    }
    
    /**
     * 提取默认值
     */
    private String extractDefaultValue(String remaining) {
        try {
            // 支持 DEFAULT 'x', DEFAULT "x", DEFAULT x
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "DEFAULT\\s+(?:'((?:''|[^'])*)'|\"((?:\\\\\"|[^\"])*)\"|([^\\s,]+))",
                java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(remaining);
            if (matcher.find()) {
                String v1 = matcher.group(1);
                String v2 = matcher.group(2);
                String v3 = matcher.group(3);
                if (v1 != null) {
                    return v1.replace("''", "'");
                }
                if (v2 != null) {
                    return v2;
                }
                return v3;
            }
        } catch (Exception e) {
            log.debug("提取默认值失败: {}", remaining);
        }
        return null;
    }
    
    /**
     * 提取注释
     */
    private String extractComment(String remaining) {
        try {
            // COMMENT '...'(支持内部以 '' 转义的单引号) 或 COMMENT "..."
            java.util.regex.Pattern singleQuoted = java.util.regex.Pattern.compile(
                "COMMENT\\s+'((?:''|[^'])*)'", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Pattern doubleQuoted = java.util.regex.Pattern.compile(
                "COMMENT\\s+\"([^\"]*)\"", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m1 = singleQuoted.matcher(remaining);
            if (m1.find()) {
                String content = m1.group(1);
                return content.replace("''", "'");
            }
            java.util.regex.Matcher m2 = doubleQuoted.matcher(remaining);
            if (m2.find()) {
                return m2.group(1);
            }
        } catch (Exception e) {
            log.debug("提取注释失败: {}", remaining);
        }
        return null;
    }

    /**
     * 安全分割字段定义：仅在顶层（括号深度为0）且不在字符串/反引号中时按逗号分割
     */
    private List<String> splitColumnDefinitionsSafely(String columnDefinitions) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        boolean inString = false;
        char stringChar = 0;
        boolean inBacktick = false;

        for (int i = 0; i < columnDefinitions.length(); i++) {
            char c = columnDefinitions.charAt(i);

            // 处理反引号包裹（表/列/索引名）
            if (!inString && c == '`') {
                inBacktick = !inBacktick;
                current.append(c);
                continue;
            }

            if (!inBacktick) {
                // 处理字符串字面量（支持单/双引号）
                if ((c == '\'' || c == '"')) {
                    if (!inString) {
                        inString = true;
                        stringChar = c;
                    } else if (c == stringChar) {
                        // 处理 SQL 中单引号转义 ''
                        if (c == '\'' && i + 1 < columnDefinitions.length() && columnDefinitions.charAt(i + 1) == '\'') {
                            current.append(c); // 保留一个引号，跳过下一个
                            i++; // 跳过转义的第二个引号
                        } else {
                            inString = false;
                        }
                    }
                    current.append(c);
                    continue;
                }

                // 处理括号深度
                if (!inString) {
                    if (c == '(') parenDepth++;
                    else if (c == ')') parenDepth = Math.max(0, parenDepth - 1);
                }
            }

            // 在顶层、非字符串、非反引号状态下的逗号才作为分隔符
            if (!inString && !inBacktick && parenDepth == 0 && c == ',') {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            parts.add(current.toString());
        }

        return parts;
    }
    
    /**
     * 从数据库直接查询字段信息
     */
    private List<Map<String, Object>> queryColumnsFromDatabase(Long dbConfigId, String tableName) {
        List<Map<String, Object>> columns = new ArrayList<>();
        
        try {
            // 获取数据库配置
            DbConfig dbConfig = dbConfigMapper.selectById(dbConfigId);
            if (dbConfig == null) {
                log.error("数据库配置不存在: {}", dbConfigId);
                return columns;
            }
            
            // 构建JDBC URL
            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?characterEncoding=utf-8&serverTimezone=Asia/Shanghai",
                    dbConfig.getHost(), dbConfig.getPort(), dbConfig.getDatabaseName());
            
            // 解密密码
            String password;
            try {
                password = PasswordCipherService.decryptToStringFromString(
                    CryptoKeyProvider.getMasterKey(), dbConfig.getPasswordCipher());
            } catch (Exception e) {
                log.error("密码解密失败: {}", e.getMessage());
                return columns;
            }
            
            // 查询字段信息
            String sql = "SELECT ordinal_position, column_name, data_type, is_nullable, column_default, " +
                        "IFNULL(column_comment, '') as column_comment " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = ? AND table_name = ? " +
                        "ORDER BY ordinal_position";
            
            try (Connection conn = java.sql.DriverManager.getConnection(jdbcUrl, dbConfig.getUsername(), password);
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                
                ps.setString(1, dbConfig.getDatabaseName());
                ps.setString(2, tableName);
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> column = new HashMap<>();
                        column.put("columnName", rs.getString("column_name"));
                        column.put("dbDataType", rs.getString("data_type"));
                        column.put("isNullable", "YES".equalsIgnoreCase(rs.getString("is_nullable")) ? 1 : 0);
                        column.put("columnDefault", rs.getString("column_default"));
                        column.put("columnComment", rs.getString("column_comment"));
                        columns.add(column);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("从数据库查询字段信息失败: {}", e.getMessage(), e);
        }
        
        return columns;
    }
    
    /**
     * 获取数据库同步状态
     */
    @GetMapping("/{dbConfigId}/status")
    public Result<Map<String, Object>> getSchemaStatus(@PathVariable Long dbConfigId) {
        try {
            // 获取最新的同步版本
            LambdaQueryWrapper<SchemaVersion> versionQw = new LambdaQueryWrapper<SchemaVersion>()
                    .eq(SchemaVersion::getDbConfigId, dbConfigId)
                    .orderByDesc(SchemaVersion::getVersionNo)
                    .last("LIMIT 1");
            
            SchemaVersion latestVersion = schemaVersionMapper.selectOne(versionQw);
            
            Map<String, Object> result = new HashMap<>();
            if (latestVersion != null) {
                result.put("versionNo", latestVersion.getVersionNo());
                result.put("status", latestVersion.getStatus());
                result.put("createdAtMs", latestVersion.getCreatedAtMs());
                result.put("tableCount", latestVersion.getTableCount());
            } else {
                result.put("versionNo", 0);
                result.put("status", 0); // 0表示未同步
                result.put("createdAtMs", 0L);
                result.put("tableCount", 0);
            }
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取同步状态失败: " + e.getMessage());
        }
    }
}