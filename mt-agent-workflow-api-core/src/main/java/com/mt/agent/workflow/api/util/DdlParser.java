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
        
        // 提取列定义部分
        Pattern columnPattern = Pattern.compile(
            "`?([^`\\s]+)`?\\s+([^\\s,]+)(?:\\s*\\([^)]*\\))?\\s*" +
            "(?:CHARACTER\\s+SET\\s+[^\\s]+)?\\s*" +
            "(?:COLLATE\\s+[^\\s]+)?\\s*" +
            "(NOT\\s+NULL|NULL)?\\s*" +
            "(?:DEFAULT\\s+([^\\s,]+))?\\s*" +
            "(?:AUTO_INCREMENT)?\\s*" +
            "(?:COMMENT\\s+['\"]([^'\"]*)['\"])?\\s*" +
            "(?:PRIMARY\\s+KEY)?\\s*[,)]",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = columnPattern.matcher(ddl);
        while (matcher.find()) {
            ColumnInfo column = new ColumnInfo();
            column.setColumnName(matcher.group(1));
            column.setDataType(matcher.group(2));
            column.setNullable(!"NOT NULL".equalsIgnoreCase(matcher.group(3)));
            column.setDefaultValue(matcher.group(4));
            column.setComment(matcher.group(5));
            column.setAutoIncrement(ddl.contains("AUTO_INCREMENT"));
            
            columns.add(column);
        }
        
        return columns;
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
}
