package com.mt.agent.workflow.api.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DDL解析器
 * 用于解析MySQL的CREATE TABLE语句，提取表结构信息
 */
@Slf4j
public class DdlParser {
    
    @Data
    public static class ColumnInfo {
        private String columnName;
        private String dataType;
        private boolean nullable;
        private String defaultValue;
        private String comment;
        private boolean isPrimaryKey;
        private boolean isAutoIncrement;
    }
    
    @Data
    public static class TableStructure {
        private String tableName;
        private String tableComment;
        private List<ColumnInfo> columns;
        private List<String> primaryKeys;
        private List<String> indexes;
    }
    
    /**
     * 解析CREATE TABLE DDL语句
     */
    public static TableStructure parseCreateTable(String ddl) {
        if (ddl == null || ddl.trim().isEmpty()) {
            return null;
        }
        
        try {
            TableStructure structure = new TableStructure();
            structure.setColumns(new ArrayList<>());
            structure.setPrimaryKeys(new ArrayList<>());
            structure.setIndexes(new ArrayList<>());
            
            // 提取表名
            structure.setTableName(extractTableName(ddl));
            
            // 提取表注释
            structure.setTableComment(extractTableComment(ddl));
            
            // 提取列定义
            List<ColumnInfo> columns = extractColumns(ddl);
            structure.setColumns(columns);
            
            // 提取主键
            structure.setPrimaryKeys(extractPrimaryKeys(ddl));
            
            // 提取索引
            structure.setIndexes(extractIndexes(ddl));
            
            return structure;
            
        } catch (Exception e) {
            log.error("解析DDL失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 提取表名
     */
    private static String extractTableName(String ddl) {
        Pattern pattern = Pattern.compile("CREATE\\s+TABLE\\s+(?:`?([^`\\s(]+)`?|([^\\s(]+))", 
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(ddl);
        if (matcher.find()) {
            return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        }
        return null;
    }
    
    /**
     * 提取表注释
     */
    private static String extractTableComment(String ddl) {
        Pattern pattern = Pattern.compile("COMMENT\\s*=\\s*['\"]([^'\"]*)['\"]", 
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(ddl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * 提取列定义
     */
    private static List<ColumnInfo> extractColumns(String ddl) {
        List<ColumnInfo> columns = new ArrayList<>();

        if (ddl == null || ddl.isEmpty()) {
            return columns;
        }

        // 1) 找到 CREATE TABLE 字段定义括号范围
        int createIdx = indexOfIgnoreCase(ddl, "CREATE TABLE");
        if (createIdx < 0) {
            return columns;
        }
        int parenStart = ddl.indexOf('(', createIdx);
        if (parenStart < 0) {
            return columns;
        }
        int parenEnd = findMatchingParenEnd(ddl, parenStart);
        if (parenEnd < 0) {
            return columns;
        }

        String columnBlock = ddl.substring(parenStart + 1, parenEnd);

        // 2) 安全分割为各字段/约束定义（忽略字符串、反引号与内部括号）
        List<String> defs = splitColumnDefinitionsSafely(columnBlock);
        for (String def : defs) {
            String fieldDef = def.trim();
            if (fieldDef.isEmpty()) continue;
            if (isConstraintDefinition(fieldDef)) continue;

            // 去除尾部逗号
            if (fieldDef.endsWith(",")) {
                fieldDef = fieldDef.substring(0, fieldDef.length() - 1).trim();
            }

            ColumnInfo column = parseColumnDefinition(fieldDef);
            if (column != null) {
                columns.add(column);
            }
        }

        return columns;
    }

    private static ColumnInfo parseColumnDefinition(String fieldDef) {
        try {
            // 保留原始以便判断自增
            String original = fieldDef;

            // 移除反引号
            fieldDef = fieldDef.replace("`", "");

            // 切出 列名 + 数据类型 + 余下部分
            // 数据类型支持: BIGINT, VARCHAR(255), DOUBLE(15,2), DECIMAL(10, 4), INT UNSIGNED 等
            Pattern p = Pattern.compile(
                "^\\s*(\\w+)\\s+([A-Z0-9_]+(?:\\s+(?:UNSIGNED|ZEROFILL))?(?:\\s*\\([^)]*\\))?)\\s*(.*)$",
                Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(fieldDef);
            if (!m.find()) {
                return null;
            }

            String name = m.group(1);
            String type = m.group(2);
            String remaining = m.group(3);

            ColumnInfo info = new ColumnInfo();
            info.setColumnName(name);
            info.setDataType(type.trim());

            // NULL/NOT NULL
            boolean nullable = (indexOfIgnoreCase(remaining, "NOT NULL") < 0);
            info.setNullable(nullable);

            // DEFAULT 值（支持 'x'、"x"、未加引号）
            String defaultValue = extractDefaultValue(remaining);
            info.setDefaultValue(defaultValue);

            // COMMENT 值（支持单引号 '' 转义与双引号）
            String comment = extractComment(remaining);
            info.setComment(comment);

            // 是否自增（就近判断当前定义片段）
            info.setAutoIncrement(indexOfIgnoreCase(original, "AUTO_INCREMENT") >= 0);

            return info;
        } catch (Exception e) {
            log.debug("解析字段定义失败: {}", fieldDef, e);
            return null;
        }
    }

    private static int indexOfIgnoreCase(String src, String needle) {
        return src.toLowerCase().indexOf(needle.toLowerCase());
    }

    private static int findMatchingParenEnd(String ddl, int startIndex) {
        int depth = 0;
        boolean inString = false;
        char stringChar = 0;
        boolean inBacktick = false;

        for (int i = startIndex; i < ddl.length(); i++) {
            char c = ddl.charAt(i);

            if (!inString) {
                if (c == '`') {
                    inBacktick = !inBacktick;
                }
            }

            if (!inBacktick) {
                if (c == '\'' || c == '"') {
                    if (!inString) {
                        inString = true;
                        stringChar = c;
                    } else if (c == stringChar) {
                        // SQL 单引号转义 ''
                        if (c == '\'' && i + 1 < ddl.length() && ddl.charAt(i + 1) == '\'') {
                            i++; // 跳过转义的第二个引号
                        } else {
                            inString = false;
                        }
                    }
                } else if (!inString) {
                    if (c == '(') depth++;
                    else if (c == ')') {
                        depth--;
                        if (depth == 0) return i;
                    }
                }
            }
        }
        return -1;
    }

    private static List<String> splitColumnDefinitionsSafely(String block) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inString = false;
        char stringChar = 0;
        boolean inBacktick = false;

        for (int i = 0; i < block.length(); i++) {
            char c = block.charAt(i);

            if (!inString) {
                if (c == '`') {
                    inBacktick = !inBacktick;
                    current.append(c);
                    continue;
                }
            }

            if (!inBacktick) {
                if (c == '\'' || c == '"') {
                    if (!inString) {
                        inString = true;
                        stringChar = c;
                    } else if (c == stringChar) {
                        if (c == '\'' && i + 1 < block.length() && block.charAt(i + 1) == '\'') {
                            current.append(c);
                            i++;
                        } else {
                            inString = false;
                        }
                    }
                    current.append(c);
                    continue;
                }

                if (!inString) {
                    if (c == '(') depth++;
                    else if (c == ')') depth = Math.max(0, depth - 1);
                }
            }

            if (!inString && !inBacktick && depth == 0 && c == ',') {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) parts.add(current.toString());
        return parts;
    }

    private static boolean isConstraintDefinition(String def) {
        String up = def.trim().toUpperCase();
        return up.startsWith("PRIMARY KEY") ||
               up.startsWith("KEY") ||
               up.startsWith("INDEX") ||
               up.startsWith("UNIQUE") ||
               up.startsWith("FOREIGN KEY") ||
               up.startsWith("CONSTRAINT");
    }

    private static String extractDefaultValue(String remaining) {
        try {
            Pattern pattern = Pattern.compile(
                "DEFAULT\\s+(?:'((?:''|[^'])*)'|\"((?:\\\\\"|[^\"])*)\"|([^\\s,]+))",
                Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(remaining);
            if (matcher.find()) {
                String v1 = matcher.group(1);
                String v2 = matcher.group(2);
                String v3 = matcher.group(3);
                if (v1 != null) return v1.replace("''", "'");
                if (v2 != null) return v2;
                return v3;
            }
        } catch (Exception e) {
            log.debug("提取默认值失败: {}", remaining);
        }
        return null;
    }

    private static String extractComment(String remaining) {
        try {
            Pattern singleQuoted = Pattern.compile("COMMENT\\s+'((?:''|[^'])*)'", Pattern.CASE_INSENSITIVE);
            Pattern doubleQuoted = Pattern.compile("COMMENT\\s+\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
            Matcher m1 = singleQuoted.matcher(remaining);
            if (m1.find()) {
                return m1.group(1).replace("''", "'");
            }
            Matcher m2 = doubleQuoted.matcher(remaining);
            if (m2.find()) {
                return m2.group(1);
            }
        } catch (Exception e) {
            log.debug("提取注释失败: {}", remaining);
        }
        return null;
    }
    
    /**
     * 提取主键
     */
    private static List<String> extractPrimaryKeys(String ddl) {
        List<String> primaryKeys = new ArrayList<>();
        
        Pattern pattern = Pattern.compile(
            "PRIMARY\\s+KEY\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = pattern.matcher(ddl);
        if (matcher.find()) {
            String pkColumns = matcher.group(1);
            String[] columns = pkColumns.split(",");
            for (String column : columns) {
                primaryKeys.add(column.trim().replaceAll("`", ""));
            }
        }
        
        return primaryKeys;
    }
    
    /**
     * 提取索引
     */
    private static List<String> extractIndexes(String ddl) {
        List<String> indexes = new ArrayList<>();
        
        Pattern pattern = Pattern.compile(
            "(?:KEY|INDEX)\\s+`?([^`\\s(]+)`?\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = pattern.matcher(ddl);
        while (matcher.find()) {
            String indexName = matcher.group(1);
            String indexColumns = matcher.group(2);
            indexes.add(indexName + "(" + indexColumns + ")");
        }
        
        return indexes;
    }
    
    /**
     * 格式化表结构为提示词格式
     */
    public static String formatTableStructure(TableStructure structure) {
        if (structure == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("表名: ").append(structure.getTableName()).append("\n");
        
        if (structure.getTableComment() != null && !structure.getTableComment().isEmpty()) {
            sb.append("说明: ").append(structure.getTableComment()).append("\n");
        }
        
        sb.append("字段：\n");
        for (ColumnInfo column : structure.getColumns()) {
            sb.append("  - ").append(column.getColumnName())
              .append(" (").append(column.getDataType()).append(")")
              .append(column.isNullable() ? " 可空" : " 非空");
            
            if (column.isPrimaryKey()) {
                sb.append(" [主键]");
            }
            
            if (column.isAutoIncrement()) {
                sb.append(" [自增]");
            }
            
            if (column.getComment() != null && !column.getComment().isEmpty()) {
                sb.append(" - ").append(column.getComment());
            }
            
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 从DDL直接格式化为提示词格式
     */
    public static String formatDdlToPrompt(String ddl) {
        TableStructure structure = parseCreateTable(ddl);
        return formatTableStructure(structure);
    }
    
    /**
     * 从DDL中解析字段信息，返回适合前端展示的Map列表
     */
    public static List<java.util.Map<String, Object>> parseColumnsFromDdl(String ddl) {
        List<java.util.Map<String, Object>> columns = new ArrayList<>();
        
        try {
            TableStructure structure = parseCreateTable(ddl);
            if (structure != null && structure.getColumns() != null) {
                for (ColumnInfo column : structure.getColumns()) {
                    java.util.Map<String, Object> columnMap = new java.util.HashMap<>();
                    columnMap.put("name", column.getColumnName());
                    columnMap.put("type", column.getDataType());
                    columnMap.put("nullable", column.isNullable());
                    columnMap.put("defaultValue", column.getDefaultValue());
                    columnMap.put("comment", column.getComment() != null ? column.getComment() : "");
                    columnMap.put("isPrimaryKey", column.isPrimaryKey());
                    columnMap.put("isAutoIncrement", column.isAutoIncrement());
                    columns.add(columnMap);
                }
            }
        } catch (Exception e) {
            log.error("解析DDL字段信息失败: {}", e.getMessage(), e);
        }
        
        return columns;
    }
    
    /**
     * 更新DDL中指定字段的注释
     */
    public static String updateColumnComment(String ddl, String columnName, String newComment) {
        if (ddl == null || columnName == null) {
            return ddl;
        }
        
        try {
            // 简化实现：查找字段定义行并更新注释
            String[] lines = ddl.split("\n");
            StringBuilder updatedDdl = new StringBuilder();
            
            for (String line : lines) {
                String trimmedLine = line.trim();
                
                // 检查是否是目标字段的定义行
                if (isColumnDefinitionLine(trimmedLine, columnName)) {
                    // 更新字段注释
                    String updatedLine = updateColumnCommentInLine(line, newComment);
                    updatedDdl.append(updatedLine);
                } else {
                    updatedDdl.append(line);
                }
                updatedDdl.append("\n");
            }
            
            return updatedDdl.toString();
            
        } catch (Exception e) {
            log.error("更新字段注释失败: {}", e.getMessage(), e);
            return ddl;
        }
    }
    
    /**
     * 检查是否是指定字段的定义行
     */
    private static boolean isColumnDefinitionLine(String line, String columnName) {
        // 简化的字段匹配逻辑
        String normalizedLine = line.toLowerCase().trim();
        String normalizedColumnName = columnName.toLowerCase();
        
        // 匹配 `column_name` 或 column_name 开头的行
        return normalizedLine.startsWith("`" + normalizedColumnName + "`") ||
               normalizedLine.startsWith(normalizedColumnName + " ");
    }
    
    /**
     * 在字段定义行中更新注释
     */
    private static String updateColumnCommentInLine(String line, String newComment) {
        // 简化实现：替换或添加COMMENT部分
        String commentPattern = "COMMENT\\s*'[^']*'";
        String newCommentPart = "COMMENT '" + newComment.replace("'", "\\'") + "'";
        
        if (line.matches(".*COMMENT\\s*'.*'.*")) {
            // 替换现有注释
            return line.replaceFirst(commentPattern, newCommentPart);
        } else {
            // 添加新注释（在行末添加，去掉可能的逗号和空格后添加）
            String trimmedLine = line.trim();
            if (trimmedLine.endsWith(",")) {
                return trimmedLine.substring(0, trimmedLine.length() - 1) + " " + newCommentPart + ",";
            } else {
                return trimmedLine + " " + newCommentPart;
            }
        }
    }
}
