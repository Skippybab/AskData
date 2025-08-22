package com.mt.agent.workflow.api.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表名格式化工具类
 * 用于生成标准格式的all_table_name，供Dify接口使用
 */
@Slf4j
@Component
public class TableNameFormatter {

    /**
     * 生成标准格式的all_table_name
     * 完全按照PromptParam.java中的CAICT_TABLE_1格式生成
     * 
     * @param tableName 表名
     * @param tableComment 表注释
     * @param tableDdl 表的DDL语句
     * @return 格式化后的all_table_name字符串
     */
    public String formatTableNameForDify(String tableName, String tableComment, String tableDdl) {
        if (tableName == null || tableName.trim().isEmpty()) {
            log.warn("表名为空，无法格式化");
            return "";
        }

        StringBuilder result = new StringBuilder();
        
        // 1. 表基本信息 - 完全按照PromptParam.java中的格式
        result.append("SQL数据库的表名是```").append(tableName).append("```，");
        
        if (tableComment != null && !tableComment.trim().isEmpty()) {
            result.append("表描述为```").append(tableComment).append("```");
        } else {
            result.append("表描述为```数据表```");
        }
        
        result.append("\n");
        
        // 2. 字段信息 - 完全按照PromptParam.java中的格式
        result.append("并且字段按照逗号分隔的格式\"字段名,数据类型,字段描述\"列举如下：\n");
        
        // 解析DDL获取字段信息
        String fieldsInfo = parseFieldsFromDdl(tableDdl);
        if (fieldsInfo != null && !fieldsInfo.trim().isEmpty()) {
            result.append(fieldsInfo);
        } else {
            // 如果无法解析DDL，提供默认信息
            result.append("无法解析表结构，请检查DDL格式");
        }
        
        return result.toString();
    }

    /**
     * 从DDL语句中解析字段信息
     * 完全按照PromptParam.java中的字段格式解析
     * 
     * @param tableDdl 表的DDL语句
     * @return 格式化后的字段信息字符串
     */
    private String parseFieldsFromDdl(String tableDdl) {
        if (tableDdl == null || tableDdl.trim().isEmpty()) {
            return null;
        }

        try {
            StringBuilder fieldsInfo = new StringBuilder();
            
            // 更精确的字段匹配正则表达式
            // 匹配格式：`字段名` 数据类型 [约束] [COMMENT '注释']
            Pattern fieldPattern = Pattern.compile(
                "\\s*`([a-zA-Z_][a-zA-Z0-9_]*)`\\s+([a-zA-Z]+(?:\\([^)]*\\))?)\\s*(?:[^,]*?COMMENT\\s*'([^']*)')?[^,]*?(?:,|\\s*$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
            );
            
            Matcher matcher = fieldPattern.matcher(tableDdl);
            boolean hasFields = false;
            
            while (matcher.find()) {
                String fieldName = matcher.group(1);
                String dataType = matcher.group(2);
                String comment = matcher.group(3);
                
                if (fieldName != null && !fieldName.trim().isEmpty()) {
                    if (hasFields) {
                        fieldsInfo.append(",\n");
                    }
                    
                    // 完全按照PromptParam.java中的格式：字段名,数据类型,字段描述
                    fieldsInfo.append(fieldName).append(",")
                             .append(dataType).append(",")
                             .append(comment != null ? comment.trim() : "字段描述");
                    
                    hasFields = true;
                    
                    log.debug("解析字段: {} -> {}, {}, {}", fieldName, dataType, comment, "字段描述");
                }
            }
            
            // 处理最后一个字段（没有逗号结尾）
            Pattern lastFieldPattern = Pattern.compile(
                "\\s*`?([a-zA-Z_][a-zA-Z0-9_]*)`?\\s+([a-zA-Z]+(?:\\([^)]*\\))?)\\s*(?:[^,]*?COMMENT\\s*'([^']*)')?[^,]*?(?:,|\\s*$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
            );
            
            Matcher lastMatcher = lastFieldPattern.matcher(tableDdl);
            while (lastMatcher.find()) {
                String fieldName = lastMatcher.group(1);
                String dataType = lastMatcher.group(2);
                String comment = lastMatcher.group(3);
                
                if (fieldName != null && !fieldName.trim().isEmpty()) {
                    // 检查是否已经包含这个字段
                    if (!fieldsInfo.toString().contains(fieldName + ",")) {
                        if (hasFields) {
                            fieldsInfo.append(",\n");
                        }
                        
                        // 完全按照PromptParam.java中的格式：字段名,数据类型,字段描述
                        fieldsInfo.append(fieldName).append(",")
                                 .append(dataType).append(",")
                                 .append(comment != null ? comment.trim() : "字段描述");
                        
                        hasFields = true;
                    }
                }
            }
            
            return hasFields ? fieldsInfo.toString() : null;
            
        } catch (Exception e) {
            log.error("解析DDL字段信息失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 生成简化的表名格式（用于调试或备用）
     * 
     * @param tableName 表名
     * @param tableComment 表注释
     * @return 简化的表名格式
     */
    public String formatSimpleTableName(String tableName, String tableComment) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        result.append("表名: ").append(tableName).append("\n");
        
        if (tableComment != null && !tableComment.trim().isEmpty()) {
            result.append("说明: ").append(tableComment).append("\n");
        } else {
            result.append("说明: 数据表\n");
        }
        
        result.append("字段：\n");
        result.append("  - 字段信息需要从DDL中解析");
        
        return result.toString();
    }
}
