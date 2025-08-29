package com.mt.agent.workflow.api.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 表名格式化工具类
 * 用于生成标准格式的all_table_name，供大模型接口使用
 */
@Slf4j
@Component
public class TableNameFormatter {

    /**
     * 执行器生成SQL的tableSchema
     * 生成标准格式的all_table_name
     * 格式：表名，包含以下字段：字段1备注、字段2备注...
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

        // 1. 表名和描述
        if (tableComment != null && !tableComment.trim().isEmpty()) {
            result.append("\"").append(tableComment).append("\"");
        } else {
            result.append("\"").append(tableName).append("\"");
        }

        // 2. 字段信息
        result.append("，包含以下字段：");

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
     * 格式：字段1备注、字段2备注...
     *
     * @param tableDdl 表的DDL语句
     * @return 格式化后的字段信息字符串
     */
    private String parseFieldsFromDdl(String tableDdl) {
        if (tableDdl == null || tableDdl.trim().isEmpty()) {
            return null;
        }

        try {
            // 使用DdlParser统一解析DDL
            DdlParser.TableStructure structure = DdlParser.parseCreateTable(tableDdl);
            if (structure == null || structure.getColumns() == null || structure.getColumns().isEmpty()) {
                return null;
            }

            StringBuilder fieldsInfo = new StringBuilder();
            boolean hasFields = false;

            for (DdlParser.ColumnInfo column : structure.getColumns()) {
                if (hasFields) {
                    fieldsInfo.append("、");
                }

                // 优先使用字段注释，如果没有注释则使用字段名
                String fieldDescription = column.getComment() != null && !column.getComment().trim().isEmpty()
                        ? column.getComment().trim()
                        : column.getColumnName();

                fieldsInfo.append(fieldDescription);
                hasFields = true;
            }

            return hasFields ? fieldsInfo.toString() : null;

        } catch (Exception e) {
            log.error("解析DDL字段信息失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 生成执行器生成SQL的tableSchema格式
     * 格式：SQL数据库的表名是```表名```，表描述为```表备注```
     * 并且字段按照逗号分隔的格式"字段名,数据类型,字段描述"列举如下：
     *
     * @param tableName 表名
     * @param tableComment 表注释
     * @param tableDdl 表的DDL语句
     * @return 格式化后的tableSchema字符串
     */
    public String formatTableSchemaForExecutor(String tableName, String tableComment, String tableDdl) {
        if (tableName == null || tableName.trim().isEmpty()) {
            log.warn("表名为空，无法格式化");
            return "";
        }
        StringBuilder result = new StringBuilder();

        // 1. 表基本信息
        result.append("SQL数据库的表名是```").append(tableName).append("```，");

        if (tableComment != null && !tableComment.trim().isEmpty()) {
            result.append("表描述为```").append(tableComment).append("```");
        } else {
            result.append("表描述为```数据表```");
        }

        result.append("\n");

        // 2. 字段信息
        result.append("并且字段按照逗号分隔的格式\"字段名,数据类型,字段描述\"列举如下：\n");

        // 解析DDL获取字段信息
        String fieldsInfo = parseFieldsForExecutor(tableDdl);
        if (fieldsInfo != null && !fieldsInfo.trim().isEmpty()) {
            result.append(fieldsInfo);
        } else {
            // 如果无法解析DDL，提供默认信息
            result.append("无法解析表结构，请检查DDL格式");
        }

        return result.toString();
    }

    /**
     * 从DDL语句中解析字段信息（执行器格式）
     * 格式：字段名,数据类型,字段描述,
     *
     * @param tableDdl 表的DDL语句
     * @return 格式化后的字段信息字符串
     */
    private String parseFieldsForExecutor(String tableDdl) {
        if (tableDdl == null || tableDdl.trim().isEmpty()) {
            return null;
        }

        try {
            // 使用DdlParser统一解析DDL
            DdlParser.TableStructure structure = DdlParser.parseCreateTable(tableDdl);
            if (structure == null || structure.getColumns() == null || structure.getColumns().isEmpty()) {
                return null;
            }

            StringBuilder fieldsInfo = new StringBuilder();
            boolean hasFields = false;

            for (DdlParser.ColumnInfo column : structure.getColumns()) {
                if (hasFields) {
                    fieldsInfo.append("\n");
                }

                // 格式：字段名,数据类型,字段描述,
                fieldsInfo.append(column.getColumnName()).append(",")
                        .append(column.getDataType()).append(",")
                        .append(column.getComment() != null && !column.getComment().trim().isEmpty() 
                            ? column.getComment().trim() 
                            : column.getColumnName()) // 使用字段名而不是"字段描述"
                        .append(",");

                hasFields = true;
            }

            return hasFields ? fieldsInfo.toString() : null;

        } catch (Exception e) {
            log.error("解析DDL字段信息失败(执行器格式): {}", e.getMessage(), e);
            return null;
        }
    }
}
