package com.mt.agent.workflow.api.util;

import com.mt.agent.workflow.api.ai.service.AiService;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import com.mt.agent.workflow.api.ai.enums.AliModelType;
import com.mt.agent.workflow.api.ai.model.RequestConfig;
import com.mt.agent.workflow.api.service.TableInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mt.agent.workflow.api.util.ExePrompt.GEN_SQL;


/**
 * AI SQL查询工具类
 * 用于将自然语言转换为SQL并执行查询
 */
@Slf4j
@Component
public class AISQLQueryUtil {
    private static final boolean ENABLE_NORMALIZED_SQL = false;

    @Autowired
    private AiService aiService;

    @Autowired
    private BufferUtil bufferUtil;

    /**
     * SQL关键字集合，这些关键字不应该被反引号包裹
     */
    private static final Set<String> SQL_KEYWORDS = new HashSet<String>() {
        {
            add("DISTINCT");
            add("COUNT");
            add("SUM");
            add("AVG");
            add("MAX");
            add("MIN");
            add("SELECT");
            add("FROM");
            add("WHERE");
            add("ORDER");
            add("BY");
            add("GROUP");
            add("HAVING");
            add("LIMIT");
            add("OFFSET");
            add("JOIN");
            add("INNER");
            add("LEFT");
            add("RIGHT");
            add("FULL");
            add("OUTER");
            add("ON");
            add("AS");
            add("AND");
            add("OR");
            add("NOT");
            add("NULL");
            add("IS");
            add("IN");
            add("LIKE");
            add("BETWEEN");
            add("EXISTS");
            add("ALL");
            add("ANY");
            add("SOME");
            add("UNION");
            add("INTERSECT");
            add("EXCEPT");
            add("CASE");
            add("WHEN");
            add("THEN");
            add("ELSE");
            add("END");
            add("IF");
            add("IFNULL");
            add("ISNULL");
            add("COALESCE");
            add("NULLIF");
            add("CAST");
            add("CONVERT");
            add("SUBSTRING");
            add("CONCAT");
            add("LENGTH");
            add("TRIM");
            add("UPPER");
            add("LOWER");
            add("REPLACE");
            add("DATE");
            add("TIME");
            add("TIMESTAMP");
            add("YEAR");
            add("MONTH");
            add("DAY");
            add("HOUR");
            add("MINUTE");
            add("SECOND");
            add("NOW");
            add("CURDATE");
            add("CURTIME");
            add("DATEADD");
            add("DATEDIFF");
            add("DATEPART");
            add("FORMAT");
            add("ROUND");
            add("FLOOR");
            add("CEIL");
            add("ABS");
            add("SIGN");
            add("SQRT");
            add("POWER");
            add("LOG");
            add("EXP");
            add("SIN");
            add("COS");
            add("TAN");
            add("ASIN");
            add("ACOS");
            add("ATAN");
        }
    };

    /**
     * 需要特殊处理的数据库字段名集合
     * 这些字段名在AS前后需要用反引号包裹
     */
    private static final Set<String> TARGET_FIELD_NAMES = new HashSet<String>() {
        {
            add("id");
            add("district_code");
            add("city");
            add("region");
            add("varchar");
            add("registered_time");
            add("registered_capital");
            add("industry_code");
            add("market_experience");
            add("year");
            add("employee_num");
            add("total_revenue");
            add("main_revenue");
            add("main_revenue_ratio");
            add("preside_international_std");
            add("preside_industry_std");
            add("participate_std");
            add("valid_patents");
            add("sales_expenses");
            add("management_expenses");
            add("main_business_cost");
            add("gross_margin_pct");
            add("revenue_per_capita");
            add("export_amount");
            add("rd_expense_total");
            add("rd_revenue_ratio");
            add("revenue_growth_rate");
            add("net_profit");
            add("profit_growth_rate");
            add("employee_num_growth_rate");
            add("main_revenue_growth_rate");
            add("net_profit_margin");
            add("gross_margin_pct_growth_rate");
            add("net_profit_margin_growth_rate");
        }
    };

    /**
     * 检查是否为SQL关键字
     */
    private boolean isSqlKeyword(String word) {
        return SQL_KEYWORDS.contains(word.toUpperCase());
    }

    /**
     * 检查是否为目标字段名（需要特殊处理的字段）
     */
    private boolean isTargetFieldName(String word) {
        return TARGET_FIELD_NAMES.contains(word.toLowerCase());
    }

    /**
     * 清理字段名，去除所有类型的引号包裹
     * 
     * @param fieldName 原始字段名
     * @return 清理后的字段名
     */
    private String cleanFieldName(String fieldName) {
        if (StringUtils.isBlank(fieldName)) {
            return fieldName;
        }

        fieldName = fieldName.trim();

        // 去除单引号包裹
        if (fieldName.startsWith("'") && fieldName.endsWith("'") && fieldName.length() > 2) {
            fieldName = fieldName.substring(1, fieldName.length() - 1);
        }
        // 去除双引号包裹
        else if (fieldName.startsWith("\"") && fieldName.endsWith("\"") && fieldName.length() > 2) {
            fieldName = fieldName.substring(1, fieldName.length() - 1);
        }
        // 去除反引号包裹
        else if (fieldName.startsWith("`") && fieldName.endsWith("`") && fieldName.length() > 2) {
            fieldName = fieldName.substring(1, fieldName.length() - 1);
        }

        return fieldName;
    }

    /**
     * 规范化别名：统一用反引号包裹
     * 处理AS后面的别名字段
     * 
     * @param alias 原始别名
     * @return 规范化后的别名
     */
    private String normalizeAlias(String alias) {
        if (StringUtils.isBlank(alias)) {
            return alias;
        }

        alias = alias.trim();

        // 去除末尾可能的分号、逗号、小数点、括号等符号
        alias = alias.replaceAll("[;,.(\\[\\])]$", "");

        // 清理现有的引号包裹
        String cleanedAlias = cleanFieldName(alias);

        // 统一用反引号包裹
        return "`" + cleanedAlias + "`";
    }

    /**
     * 智能处理字段名中包含的目标字段
     * 
     * @param fieldExpression 字段表达式（可能包含函数调用、表别名等）
     * @return 处理后的字段表达式
     */
    private String smartProcessFieldExpression(String fieldExpression) {
        if (StringUtils.isBlank(fieldExpression)) {
            return fieldExpression;
        }

        fieldExpression = fieldExpression.trim();

        // 如果是函数调用，需要特殊处理
        if (fieldExpression.contains("(") && fieldExpression.contains(")")) {
            return processComplexFieldExpression(fieldExpression);
        }

        // 处理表别名.字段名格式
        if (fieldExpression.contains(".") && !fieldExpression.startsWith(".") && !fieldExpression.endsWith(".")) {
            return processTableFieldExpression(fieldExpression);
        }

        // 处理简单字段名
        return processSimpleFieldExpression(fieldExpression);
    }

    /**
     * 处理复杂字段表达式（包含函数调用等）
     */
    private String processComplexFieldExpression(String fieldExpression) {
        // 检查是否为窗口函数 (包含 OVER 关键字)
        if (fieldExpression.toUpperCase().contains(" OVER ")) {
            return processWindowFunction(fieldExpression);
        }

        // 对于包含函数的复杂表达式，只处理函数参数中的目标字段
        Pattern functionPattern = Pattern.compile("(\\w+)\\s*\\(([^)]+)\\)");
        Matcher matcher = functionPattern.matcher(fieldExpression);

        if (matcher.find()) {
            String functionName = matcher.group(1);
            String parameter = matcher.group(2).trim();

            // 递归处理函数参数
            String processedParameter = smartProcessFieldParameter(parameter);

            return functionName + "(" + processedParameter + ")";
        }

        return fieldExpression;
    }

    /**
     * 处理窗口函数：LAG(...) OVER (...)
     */
    private String processWindowFunction(String fieldExpression) {
        log.debug("【窗口函数处理】开始处理: {}", fieldExpression);

        // 使用更精确的正则表达式匹配窗口函数，确保括号匹配正确
        // 匹配格式：function_name(params) OVER (over_clause)
        Pattern windowPattern = Pattern.compile(
                "(\\w+)\\s*\\(([^)]+)\\)\\s+(OVER)\\s*\\((.+)\\)$",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = windowPattern.matcher(fieldExpression);

        if (matcher.find()) {
            String functionName = matcher.group(1);
            String functionParams = matcher.group(2).trim();
            String overKeyword = matcher.group(3);
            String overClause = matcher.group(4).trim();

            log.debug("【窗口函数处理】函数名: {}, 参数: {}, OVER子句: {}", functionName, functionParams, overClause);

            // 处理函数参数，保持数字不变
            String processedFunctionParams = processWindowFunctionParams(functionParams);

            // 处理OVER子句 - 直接在这里处理，避免与全局ORDER BY处理冲突
            String processedOverClause = processOverClauseDirectly(overClause);

            String result = functionName + "(" + processedFunctionParams + ") " + overKeyword + " ("
                    + processedOverClause + ")";
            log.debug("【窗口函数处理】处理结果: {}", result);
            return result;
        }

        log.debug("【窗口函数处理】未匹配到窗口函数，回退到普通函数处理");

        // 如果不匹配窗口函数模式，回退到普通函数处理
        Pattern functionPattern = Pattern.compile("(\\w+)\\s*\\(([^)]+)\\)");
        Matcher functionMatcher = functionPattern.matcher(fieldExpression);

        if (functionMatcher.find()) {
            String functionName = functionMatcher.group(1);
            String parameter = functionMatcher.group(2).trim();

            // 递归处理函数参数
            String processedParameter = smartProcessFieldParameter(parameter);

            return functionName + "(" + processedParameter + ")";
        }

        return fieldExpression;
    }

    /**
     * 专门处理窗口函数参数，确保数字不被错误处理
     */
    private String processWindowFunctionParams(String params) {
        if (StringUtils.isBlank(params) || params.equals("*")) {
            return params;
        }

        // 处理多个参数的情况，用逗号分隔
        if (params.contains(",")) {
            String[] paramArray = params.split(",");
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < paramArray.length; i++) {
                String param = paramArray[i].trim();

                // 检查是否为纯数字
                if (param.matches("\\d+")) {
                    result.append(param);
                } else {
                    // 处理字段名
                    String cleanedParam = cleanFieldName(param);
                    if (isTargetFieldName(cleanedParam)) {
                        result.append("`" + cleanedParam + "`");
                    } else {
                        result.append(cleanedParam);
                    }
                }

                if (i < paramArray.length - 1) {
                    result.append(", ");
                }
            }
            return result.toString();
        }

        // 处理单个参数
        if (params.matches("\\d+")) {
            return params;
        }

        // 处理单个字段名
        String cleanedParam = cleanFieldName(params);
        if (isTargetFieldName(cleanedParam)) {
            return "`" + cleanedParam + "`";
        }

        return cleanedParam;
    }

    /**
     * 直接处理OVER子句，避免与全局ORDER BY处理冲突
     */
    private String processOverClauseDirectly(String overClause) {
        if (StringUtils.isBlank(overClause)) {
            return overClause;
        }

        String result = overClause;

        // 处理 PARTITION BY 子句
        if (overClause.toUpperCase().contains("PARTITION BY")) {
            Pattern partitionPattern = Pattern.compile("(PARTITION\\s+BY\\s+)([a-zA-Z_][a-zA-Z0-9_]*)",
                    Pattern.CASE_INSENSITIVE);
            Matcher partitionMatcher = partitionPattern.matcher(result);

            if (partitionMatcher.find()) {
                String partitionBy = partitionMatcher.group(1);
                String field = partitionMatcher.group(2).trim();

                // 清理字段名，去除现有的引号
                String cleanedField = cleanFieldName(field);

                // 处理字段名 - 检查是否是目标字段名
                if (isTargetFieldName(cleanedField)) {
                    String processedField = "`" + cleanedField + "`";
                    result = partitionMatcher.replaceFirst(partitionBy + processedField);
                }
            }
        }

        // 处理 ORDER BY 子句 - 使用更精确的匹配，确保不会匹配到其他地方
        if (result.toUpperCase().contains("ORDER BY")) {
            Pattern orderByPattern = Pattern.compile("(ORDER\\s+BY\\s+)([a-zA-Z_][a-zA-Z0-9_]*)(?=\\s*\\)|$)",
                    Pattern.CASE_INSENSITIVE);
            Matcher orderByMatcher = orderByPattern.matcher(result);

            if (orderByMatcher.find()) {
                String orderBy = orderByMatcher.group(1);
                String field = orderByMatcher.group(2).trim();

                // 清理字段名，去除现有的引号
                String cleanedField = cleanFieldName(field);

                // 处理字段名 - 检查是否是目标字段名
                if (isTargetFieldName(cleanedField)) {
                    String processedField = "`" + cleanedField + "`";
                    result = orderByMatcher.replaceFirst(orderBy + processedField);
                }
            }
        }

        return result;
    }

    /**
     * 检查指定位置的ORDER BY是否在窗口函数内部
     */
    private boolean isOrderByInWindowFunction(String sql, int orderByPosition) {
        // 查找ORDER BY之前最近的OVER关键字
        String beforeOrderBy = sql.substring(0, orderByPosition);
        int overPos = beforeOrderBy.lastIndexOf(" OVER ");

        if (overPos == -1) {
            return false; // 没有OVER，不在窗口函数内
        }

        // 检查OVER之后、ORDER BY之前是否有开括号但没有闭括号
        String betweenOverAndOrderBy = sql.substring(overPos + 6, orderByPosition);
        int openParens = 0;

        for (char c : betweenOverAndOrderBy.toCharArray()) {
            if (c == '(') {
                openParens++;
            } else if (c == ')') {
                openParens--;
            }
        }

        // 如果开括号多于闭括号，说明ORDER BY在OVER子句的括号内
        return openParens > 0;
    }

    /**
     * 处理OVER子句
     */
    private String processOverClause(String overClause) {
        if (StringUtils.isBlank(overClause)) {
            return overClause;
        }

        // 处理 PARTITION BY 子句
        if (overClause.toUpperCase().contains("PARTITION BY")) {
            overClause = processPartitionByClause(overClause);
        }

        // 处理 ORDER BY 子句
        if (overClause.toUpperCase().contains("ORDER BY")) {
            overClause = processOrderByInOverClause(overClause);
        }

        return overClause;
    }

    /**
     * 处理PARTITION BY子句
     */
    private String processPartitionByClause(String clause) {
        Pattern partitionPattern = Pattern.compile("(PARTITION\\s+BY\\s+)([^\\s]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = partitionPattern.matcher(clause);

        if (matcher.find()) {
            String partitionBy = matcher.group(1);
            String field = matcher.group(2).trim();

            // 清理字段名，去除现有的引号
            String cleanedField = cleanFieldName(field);

            // 处理字段名 - 检查是否是目标字段名
            if (isTargetFieldName(cleanedField)) {
                field = "`" + cleanedField + "`";
            } else {
                field = cleanedField;
            }

            return clause.replace(matcher.group(0), partitionBy + field);
        }

        return clause;
    }

    /**
     * 处理OVER子句中的ORDER BY
     */
    private String processOrderByInOverClause(String clause) {
        Pattern orderByPattern = Pattern.compile("(ORDER\\s+BY\\s+)([^\\s)]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = orderByPattern.matcher(clause);

        if (matcher.find()) {
            String orderBy = matcher.group(1);
            String field = matcher.group(2).trim();

            // 清理字段名，去除现有的引号
            String cleanedField = cleanFieldName(field);

            // 处理字段名 - 检查是否是目标字段名
            if (isTargetFieldName(cleanedField)) {
                field = "`" + cleanedField + "`";
            } else {
                field = cleanedField;
            }

            return clause.replace(matcher.group(0), orderBy + field);
        }

        return clause;
    }

    /**
     * 处理函数参数中的字段名
     */
    private String smartProcessFieldParameter(String parameter) {
        if (StringUtils.isBlank(parameter) || parameter.equals("*")) {
            return parameter;
        }

        // 处理 DISTINCT field_name 格式
        if (parameter.toUpperCase().startsWith("DISTINCT ")) {
            String[] parts = parameter.split("\\s+", 2);
            if (parts.length == 2) {
                String distinctKeyword = parts[0];
                String fieldName = parts[1].trim();
                String processedFieldName = processSimpleFieldExpression(fieldName);
                return distinctKeyword + " " + processedFieldName;
            }
        }

        // 处理多个字段的情况，用逗号分隔
        if (parameter.contains(",")) {
            String[] fields = parameter.split(",");
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < fields.length; i++) {
                String fieldName = fields[i].trim();

                // 检查是否为数字，如果是数字则不处理
                if (fieldName.matches("\\d+")) {
                    result.append(fieldName);
                } else {
                    result.append(processSimpleFieldExpression(fieldName));
                }

                if (i < fields.length - 1) {
                    result.append(", ");
                }
            }
            return result.toString();
        }

        // 检查是否为数字，如果是数字则不处理
        if (parameter.matches("\\d+")) {
            return parameter;
        }

        // 处理单个字段名
        return processSimpleFieldExpression(parameter);
    }

    /**
     * 处理表别名.字段名格式
     */
    private String processTableFieldExpression(String fieldExpression) {
        String[] parts = fieldExpression.split("\\.", 2);
        if (parts.length == 2) {
            String tableAlias = parts[0].trim();
            String columnName = parts[1].trim();

            // 清理字段名
            String cleanedTableAlias = cleanFieldName(tableAlias);
            String cleanedColumnName = cleanFieldName(columnName);

            // 检查字段名是否需要处理
            if (isTargetFieldName(cleanedColumnName)) {
                cleanedColumnName = "`" + cleanedColumnName + "`";
            }

            // 表别名通常不需要反引号，除非它是目标字段名
            if (isTargetFieldName(cleanedTableAlias)) {
                cleanedTableAlias = "`" + cleanedTableAlias + "`";
            }

            return cleanedTableAlias + "." + cleanedColumnName;
        }

        return fieldExpression;
    }

    /**
     * 处理简单字段表达式
     */
    private String processSimpleFieldExpression(String fieldExpression) {
        if (StringUtils.isBlank(fieldExpression) || fieldExpression.equals("*")) {
            return fieldExpression;
        }

        // 去除末尾的特殊符号
        fieldExpression = fieldExpression.replaceAll("[;,.(\\[\\])]$", "");

        // 清理现有的引号包裹
        String cleanedField = cleanFieldName(fieldExpression);

        // 检查是否为SQL关键字或数字
        if (isSqlKeyword(cleanedField) || cleanedField.matches("\\d+")) {
            return cleanedField;
        }

        // 检查是否为目标字段名
        if (isTargetFieldName(cleanedField)) {
            return "`" + cleanedField + "`";
        }

        // 不是目标字段名，保持原样（不加反引号）
        return cleanedField;
    }

    /**
     * 修复错误的字段名单引号包裹，将字段名的单引号转换为反引号
     * 
     * @param sql 原始SQL语句
     * @return 修复后的SQL语句
     */
    private String fixFieldNameQuotes(String sql) {
        log.info("【修复字段名单引号】开始处理...");

        // 1. 处理 WHERE 子句中的字段名单引号（排除纯数字）
        // 模式：'field_name' = value, 'field_name' LIKE value, 'field_name' IN (...), etc.
        sql = sql.replaceAll(
                "'([a-zA-Z_][a-zA-Z0-9_]*)'\\s*(=|!=|<>|<|>|<=|>=|LIKE|IN|BETWEEN|NOT\\s+IN|NOT\\s+LIKE)\\s*",
                "`$1` $2 ");

        // 2. 处理 ORDER BY 子句中的字段名单引号（排除纯数字）
        // 模式：ORDER BY 'field_name' ASC/DESC
        sql = sql.replaceAll("(?i)(ORDER\\s+BY\\s+)'([a-zA-Z_][a-zA-Z0-9_]*)'(\\s+(ASC|DESC))?", "$1`$2`$3");

        // 3. 处理 GROUP BY 子句中的字段名单引号（排除纯数字）
        // 模式：GROUP BY 'field_name'
        sql = sql.replaceAll("(?i)(GROUP\\s+BY\\s+)'([a-zA-Z_][a-zA-Z0-9_]*)'", "$1`$2`");

        // 4. 处理 SELECT 子句中的字段名单引号（支持多行，排除纯数字）
        // 使用循环处理，确保所有单引号字段名都被正确转换
        // 模式：'field_name' AS alias（在SELECT子句中）
        sql = sql.replaceAll("(?is)'([a-zA-Z_][a-zA-Z0-9_]*)'(\\s+AS\\s+)", "`$1`$2");

        // 5. 处理 FROM 子句后的字段名单引号（但要小心表名，排除纯数字）
        // 模式：FROM table_name WHERE 'field_name' = value
        sql = sql.replaceAll("(?i)(FROM\\s+\\w+\\s+WHERE\\s+)'([a-zA-Z_][a-zA-Z0-9_]*)'", "$1`$2`");

        // 6. 处理SELECT子句中剩余的单引号字段名（排除纯数字）
        // 处理所有在SELECT和FROM之间的单引号字段名
        Pattern selectPattern = Pattern.compile("(?is)(SELECT\\s+)(.*?)(\\s+FROM)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher selectMatcher = selectPattern.matcher(sql);
        if (selectMatcher.find()) {
            String selectClause = selectMatcher.group(2);
            // 在SELECT子句中将所有剩余的单引号字段名转换为反引号
            selectClause = selectClause.replaceAll("'([a-zA-Z_][a-zA-Z0-9_]*)'", "`$1`");
            sql = sql.replace(selectMatcher.group(0), selectMatcher.group(1) + selectClause + selectMatcher.group(3));
        }

        // 7. 处理 AND/OR 后的字段名单引号（排除纯数字）
        // 模式：AND 'field_name' = value, OR 'field_name' LIKE value
        sql = sql.replaceAll(
                "(?i)(AND|OR)\\s+'([a-zA-Z_][a-zA-Z0-9_]*)'\\s*(=|!=|<>|<|>|<=|>=|LIKE|IN|BETWEEN|NOT\\s+IN|NOT\\s+LIKE)\\s*",
                "$1 `$2` $3 ");

        log.info("【修复字段名单引号】处理完成");
        return sql;
    }

    /**
     * 从包含说明文字的文本中提取真正的SQL语句
     * 
     * @param rawInput 原始输入文本，可能包含大量说明和解释
     * @return 提取出的纯SQL语句
     */
    private String extractActualSQL(String rawInput) {
        if (StringUtils.isBlank(rawInput)) {
            return rawInput;
        }

        log.info("【SQL提取】开始从原始输入中提取SQL语句...");
        log.debug("【SQL提取】原始输入长度: {} 字符", rawInput.length());

        String processedInput = rawInput;

        // 1. 去除markdown代码块标记
        processedInput = processedInput.replaceAll("```sql\\s*", "");
        processedInput = processedInput.replaceAll("```SQL\\s*", "");
        processedInput = processedInput.replaceAll("```\\s*", "");

        // 2. 按行分割，查找包含SELECT的行作为SQL开始
        String[] lines = processedInput.split("\\r?\\n");
        int sqlStartIndex = -1;
        int sqlEndIndex = -1;

        // 查找SQL开始位置
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.toUpperCase().startsWith("SELECT")) {
                sqlStartIndex = i;
                break;
            }
        }

        if (sqlStartIndex == -1) {
            log.warn("【SQL提取】未找到SELECT语句，返回原始输入");
            return rawInput;
        }

        // 查找SQL结束位置
        // 从SELECT开始往后找，直到遇到说明文字或空行后的说明文字
        for (int i = sqlStartIndex; i < lines.length; i++) {
            String line = lines[i].trim();

            // 如果是空行，继续查看下一行
            if (line.isEmpty()) {
                continue;
            }

            // 检查是否是SQL结束的标志
            if (isSQLEndIndicator(line)) {
                sqlEndIndex = i - 1;
                break;
            }

            // 如果遇到SQL语句结束符号（分号），且下一行不是SQL语句，则结束
            if (line.endsWith(";")) {
                sqlEndIndex = i;
                // 检查后面是否还有SQL语句
                boolean hasMoreSQL = false;
                for (int j = i + 1; j < lines.length; j++) {
                    String nextLine = lines[j].trim();
                    if (!nextLine.isEmpty() && isPartOfSQL(nextLine) && !isSQLEndIndicator(nextLine)) {
                        hasMoreSQL = true;
                        break;
                    }
                    if (!nextLine.isEmpty() && isSQLEndIndicator(nextLine)) {
                        break;
                    }
                }
                if (!hasMoreSQL) {
                    break;
                }
            }
        }

        // 如果没有找到结束位置，取到最后一行
        if (sqlEndIndex == -1) {
            sqlEndIndex = lines.length - 1;
        }

        // 提取SQL语句
        StringBuilder sqlBuilder = new StringBuilder();
        for (int i = sqlStartIndex; i <= sqlEndIndex; i++) {
            if (i < lines.length) {
                String line = lines[i];
                // 跳过明显的说明行
                if (!isSQLEndIndicator(line.trim())) {
                    sqlBuilder.append(line).append("\n");
                }
            }
        }

        String extractedSQL = sqlBuilder.toString().trim();

        // 3. 最后的清理工作
        extractedSQL = cleanupExtractedSQL(extractedSQL);

        log.info("【SQL提取】成功提取SQL语句，长度: {} 字符", extractedSQL.length());
        log.debug("【SQL提取】提取的SQL: \n{}", extractedSQL);

        return extractedSQL;
    }

    /**
     * 判断是否是SQL结束的指示符
     */
    private boolean isSQLEndIndicator(String line) {
        if (StringUtils.isBlank(line)) {
            return false;
        }

        String upperLine = line.toUpperCase();

        // 常见的说明文字开头
        String[] indicators = {
                "关键点说明", "关键点：", "说明：", "注意：", "备注：", "解释：",
                "KEY POINTS", "EXPLANATION", "NOTES", "DESCRIPTION",
                "1.", "2.", "3.", "4.", "5.", "6.", "7.", "8.", "9.",
                "•", "·", "-", "*",
                "这个查询", "该SQL", "此语句", "查询语句",
                "SQL执行", "数据库查询", "查询结果"
        };

        for (String indicator : indicators) {
            if (upperLine.startsWith(indicator.toUpperCase()) ||
                    line.startsWith(indicator)) {
                return true;
            }
        }

        // 检查是否是纯数字开头的说明行（如 "1. 使用xx字段"）
        if (line.matches("^\\d+\\.\\s+.*")) {
            return true;
        }

        return false;
    }

    /**
     * 判断是否是SQL语句的一部分
     */
    private boolean isPartOfSQL(String line) {
        if (StringUtils.isBlank(line)) {
            return false;
        }

        String upperLine = line.toUpperCase().trim();

        // SQL关键字
        String[] sqlKeywords = {
                "SELECT", "FROM", "WHERE", "ORDER BY", "GROUP BY",
                "HAVING", "LIMIT", "OFFSET", "JOIN", "INNER JOIN",
                "LEFT JOIN", "RIGHT JOIN", "UNION", "AND", "OR"
        };

        for (String keyword : sqlKeywords) {
            if (upperLine.startsWith(keyword)) {
                return true;
            }
        }

        // 检查是否包含SQL操作符
        if (upperLine.contains(" = ") || upperLine.contains(" LIKE ") ||
                upperLine.contains(" IN ") || upperLine.contains(" BETWEEN ") ||
                upperLine.contains(">=") || upperLine.contains("<=") ||
                upperLine.contains("!=") || upperLine.contains("<>")) {
            return true;
        }

        // 检查是否是字段名或表名（通常包含字母数字下划线）
        if (line.matches("^\\s*[a-zA-Z_][a-zA-Z0-9_]*\\s*[,;]?\\s*$")) {
            return true;
        }

        return false;
    }

    /**
     * 清理提取出的SQL语句
     */
    private String cleanupExtractedSQL(String sql) {
        if (StringUtils.isBlank(sql)) {
            return sql;
        }

        // 去除多余的空行
        sql = sql.replaceAll("\\n\\s*\\n\\s*\\n", "\n\n");

        // 去除行首行尾的空白字符，但保留SQL格式
        String[] lines = sql.split("\\r?\\n");
        StringBuilder cleanedSQL = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                cleanedSQL.append(trimmedLine).append("\n");
            }
        }

        // 返回清理后的SQL，不自动添加分号
        String result = cleanedSQL.toString().trim();

        return result;
    }

    /**
     * 检测SQL是否包含嵌套查询（子查询）
     * 
     * @param sql SQL语句
     * @return true表示包含嵌套查询
     */
    private boolean hasNestedQuery(String sql) {
        if (StringUtils.isBlank(sql)) {
            return false;
        }

        String upperSql = sql.toUpperCase();

        // 简单检测：查找SELECT关键字的数量
        // 如果有多个SELECT，很可能是嵌套查询
        int selectCount = 0;
        int index = 0;
        while ((index = upperSql.indexOf("SELECT", index)) != -1) {
            selectCount++;
            index += 6; // "SELECT"的长度
        }

        // 如果有多个SELECT，进一步验证是否真的是嵌套查询
        if (selectCount > 1) {
            // 检查是否有子查询的典型模式，使用正则表达式支持空白字符（包括换行符）

            // 1. WHERE子句中的子查询：WHERE field IN (SELECT ...)
            if (upperSql.matches("(?s).*\\bIN\\s*\\(\\s*SELECT.*") || upperSql.contains("IN(SELECT")) {
                return true;
            }

            // 2. FROM子句中的子查询：FROM (SELECT ...) - 支持换行符
            if (upperSql.matches("(?s).*\\bFROM\\s*\\(\\s*SELECT.*") || upperSql.contains("FROM(SELECT")) {
                return true;
            }

            // 3. SELECT子句中的子查询：SELECT (SELECT ...)
            if (upperSql.matches("(?s).*\\bSELECT\\s*\\(\\s*SELECT.*") || upperSql.contains("SELECT(SELECT")) {
                return true;
            }

            // 4. 比较操作中的子查询：= (SELECT ...), > (SELECT ...), 等
            if (upperSql.matches("(?s).*[=<>!]+\\s*\\(\\s*SELECT.*")) {
                return true;
            }

            // 5. EXISTS子查询：WHERE EXISTS (SELECT ...)
            if (upperSql.matches("(?s).*\\bEXISTS\\s*\\(\\s*SELECT.*") || upperSql.contains("EXISTS(SELECT")) {
                return true;
            }
        }

        return false;
    }

    /**
     * SQL标注化处理，用于降低SQL执行异常率
     * 
     * @param sql 原始SQL语句
     * @return 处理后的SQL语句
     */
    private String normalizeSql(String sql) {
        if (StringUtils.isBlank(sql)) {
            return sql;
        }

        log.info("【SQL标注化处理】原始SQL: \n{}", sql);

        // 检测是否包含嵌套查询
        boolean hasNested = hasNestedQuery(sql);
        if (hasNested) {
            log.info("【SQL标注化处理】检测到嵌套查询，采用简化处理模式");
        }

        String processedSql = sql;

        // 1. 处理错误的字段名单引号包裹：将字段名的单引号转换为反引号（对嵌套查询相对安全）
        processedSql = fixFieldNameQuotes(processedSql);

        // 2. 处理整数值格式：更精确地去除 '整数' 和 `整数` 格式中的引号
        processedSql = normalizeNumericValues(processedSql);

        // 3. 对于嵌套查询，跳过复杂的字段名标注化处理，避免破坏查询结构
        if (!hasNested) {
            // 只有非嵌套查询才进行完整的字段名标注化
            processedSql = normalizeFieldsAndAliases(processedSql);
            log.info("【SQL标注化处理】已完成完整的字段名标注化处理");
        } else {
            log.info("【SQL标注化处理】跳过字段名标注化处理，保持嵌套查询结构完整");
        }

        log.info("【SQL标注化处理】处理后SQL: \n{}", processedSql);

        return processedSql;
    }

    /**
     * 标准化数字值：更精确地处理引号包裹的数字
     * 
     * @param sql SQL语句
     * @return 处理后的SQL语句
     */
    private String normalizeNumericValues(String sql) {
        log.info("【数字值标准化】开始处理...");

        String processedSql = sql;

        // 1. 处理 IN 子句中的数字：IN ('123', '456') -> IN (123, 456)
        // 使用 Pattern 和 Matcher 来精确处理 IN 子句
        Pattern inPattern = Pattern.compile("(?i)(IN\\s*\\()([^)]*?)(\\))", Pattern.CASE_INSENSITIVE);
        Matcher inMatcher = inPattern.matcher(processedSql);

        StringBuffer sb = new StringBuffer();
        while (inMatcher.find()) {
            String inPrefix = inMatcher.group(1); // "IN ("
            String inContent = inMatcher.group(2); // 内容
            String inSuffix = inMatcher.group(3); // ")"

            // 在 IN 子句内部去除数字的单引号，但保留反引号（反引号通常用于字段名）
            inContent = inContent.replaceAll("'(\\d+)'", "$1");
            // 注意：不要处理反引号包裹的数字，因为在SQL中反引号主要用于字段名

            inMatcher.appendReplacement(sb, inPrefix + inContent + inSuffix);
        }
        inMatcher.appendTail(sb);
        processedSql = sb.toString();

        // 2. 处理其他位置的数字引号：= '123' -> = 123, > '456' -> > 456 等
        // 但要避免影响已处理的 IN 子句，并且不要处理反引号包裹的内容
        processedSql = processedSql.replaceAll("([=<>!]+\\s*)'(\\d+)'", "$1$2");
        // 移除对反引号包裹数字的处理，避免破坏字段名
        // processedSql = processedSql.replaceAll("([=<>!]+\\s*)`(\\d+)`", "$1$2");

        // 3. 处理比较操作符周围的数字：BETWEEN '1' AND '100' -> BETWEEN 1 AND 100
        processedSql = processedSql.replaceAll("(?i)(BETWEEN\\s+)'(\\d+)'(\\s+AND\\s+)'(\\d+)'", "$1$2$3$4");
        // 移除对反引号包裹数字的处理，避免破坏字段名
        // processedSql =
        // processedSql.replaceAll("(?i)(BETWEEN\\s+)`(\\d+)`(\\s+AND\\s+)`(\\d+)`",
        // "$1$2$3$4");

        log.info("【数字值标准化】处理完成");
        return processedSql;
    }

    /**
     * 标准化字段名和别名：处理字段名的反引号包裹
     * 完全重写以避免全局替换导致的跨子句污染和位置重叠问题
     */
    private String normalizeFieldsAndAliases(String sql) {
        log.info("【SQL标注化处理】开始完整的字段名标注化处理");

        String processedSql = sql;

        // 1. 处理SELECT子句中的字段名 - 使用精确替换避免影响其他部分
        Pattern selectPattern = Pattern.compile("(?i)(SELECT\\s+)(.+?)(\\s+FROM)", Pattern.DOTALL);
        Matcher selectMatcher = selectPattern.matcher(processedSql);

        if (selectMatcher.find()) {
            String selectKeyword = selectMatcher.group(1);
            String selectClause = selectMatcher.group(2);
            String fromKeyword = selectMatcher.group(3);

            String processedSelectClause = processSelectFields(selectClause);

            // 使用StringBuffer进行替换，避免位置错乱
            StringBuffer sb = new StringBuffer();
            selectMatcher.reset();
            while (selectMatcher.find()) {
                selectMatcher.appendReplacement(sb, selectKeyword + processedSelectClause + fromKeyword);
            }
            selectMatcher.appendTail(sb);
            processedSql = sb.toString();
        }

        // 2. 处理WHERE子句中的字段名
        Pattern wherePattern = Pattern.compile(
                "(?i)(WHERE\\s+)(.+?)(?=(\\s+ORDER\\s+BY|\\s+GROUP\\s+BY|\\s+HAVING|\\s+LIMIT|;|$))", Pattern.DOTALL);
        Matcher whereMatcher = wherePattern.matcher(processedSql);

        if (whereMatcher.find()) {
            String whereKeyword = whereMatcher.group(1);
            String whereClause = whereMatcher.group(2);

            // 去除可能包含的分号
            whereClause = whereClause.replaceAll(";\\s*$", "");
            String processedWhereClause = processConditionFields(whereClause);

            // 使用StringBuffer进行替换，避免位置错乱
            StringBuffer sb = new StringBuffer();
            whereMatcher.reset();
            while (whereMatcher.find()) {
                whereMatcher.appendReplacement(sb, whereKeyword + processedWhereClause);
            }
            whereMatcher.appendTail(sb);
            processedSql = sb.toString();
        }

        // 3. 处理ORDER BY子句中的字段名 - 确保不匹配窗口函数内部的ORDER BY
        Pattern orderByPattern = Pattern.compile("(?i)\\bORDER\\s+BY\\s+(.+?)(?=\\s+LIMIT|;|$)", Pattern.DOTALL);
        Matcher orderByMatcher = orderByPattern.matcher(processedSql);

        if (orderByMatcher.find()) {
            String orderByClause = orderByMatcher.group(1);

            // 检查ORDER BY是否在窗口函数内部，如果是则跳过处理
            if (!isOrderByInWindowFunction(processedSql, orderByMatcher.start())) {
                // 去除可能包含的分号
                orderByClause = orderByClause.replaceAll(";\\s*$", "");
                String processedOrderByClause = processOrderByFields(orderByClause);

                // 使用StringBuffer进行替换，避免位置错乱
                StringBuffer sb = new StringBuffer();
                orderByMatcher.reset();
                while (orderByMatcher.find()) {
                    if (!isOrderByInWindowFunction(processedSql, orderByMatcher.start())) {
                        orderByMatcher.appendReplacement(sb, "ORDER BY " + processedOrderByClause);
                    } else {
                        orderByMatcher.appendReplacement(sb, orderByMatcher.group(0));
                    }
                }
                orderByMatcher.appendTail(sb);
                processedSql = sb.toString();
            }
        }

        // 4. 处理GROUP BY子句中的字段名
        Pattern groupByPattern = Pattern.compile(
                "(?i)(GROUP\\s+BY\\s+)(.+?)(?=(\\s+HAVING|\\s+ORDER\\s+BY|\\s+LIMIT|;|$))",
                Pattern.DOTALL);
        Matcher groupByMatcher = groupByPattern.matcher(processedSql);

        if (groupByMatcher.find()) {
            String groupByKeyword = groupByMatcher.group(1);
            String groupByClause = groupByMatcher.group(2);

            // 去除可能包含的分号
            groupByClause = groupByClause.replaceAll(";\\s*$", "");
            String processedGroupByClause = processGroupByFields(groupByClause);

            // 使用StringBuffer进行替换，避免位置错乱
            StringBuffer sb = new StringBuffer();
            groupByMatcher.reset();
            while (groupByMatcher.find()) {
                groupByMatcher.appendReplacement(sb, groupByKeyword + processedGroupByClause);
            }
            groupByMatcher.appendTail(sb);
            processedSql = sb.toString();
        }

        log.info("【SQL标注化处理】已完成完整的字段名标注化处理");
        return processedSql;
    }

    /**
     * 处理SELECT子句中的字段（重新优化的版本）
     * 根据用户要求对目标字段名进行规范化处理
     */
    private String processSelectFields(String selectClause) {
        log.debug("【SELECT字段处理】开始处理SELECT子句: {}", selectClause);

        // 使用智能分割，考虑括号嵌套
        List<String> fields = smartSplitFields(selectClause);
        StringBuilder processedFields = new StringBuilder();

        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i).trim();

            log.debug("【SELECT字段处理】处理第{}个字段: {}", i + 1, field);

            // 检查是否包含别名（支持大小写）
            if (field.matches("(?i).*\\s+AS\\s+.*")) {
                // 处理带别名的字段
                field = processFieldWithAlias(field);
                log.debug("【SELECT字段处理】AS别名处理结果: {}", field);
            } else {
                // 处理不带别名的字段（可能是函数调用、普通字段名等）
                field = smartProcessFieldExpression(field);
                log.debug("【SELECT字段处理】字段表达式处理结果: {}", field);
            }

            processedFields.append(field);
            if (i < fields.size() - 1) {
                processedFields.append(", ");
            }
        }

        String result = processedFields.toString();
        log.debug("【SELECT字段处理】最终处理结果: {}", result);
        return result;
    }

    /**
     * 智能分割字段，考虑括号嵌套
     * 确保不会在函数调用中间分割字段
     */
    private List<String> smartSplitFields(String selectClause) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        int parenthesesLevel = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;

        for (int i = 0; i < selectClause.length(); i++) {
            char c = selectClause.charAt(i);

            // 处理引号状态
            if (c == '\'' && !inDoubleQuote && !inBacktick) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote && !inBacktick) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
            }

            // 如果在引号内，直接添加字符
            if (inSingleQuote || inDoubleQuote || inBacktick) {
                currentField.append(c);
                continue;
            }

            // 处理括号嵌套
            if (c == '(') {
                parenthesesLevel++;
                currentField.append(c);
            } else if (c == ')') {
                parenthesesLevel--;
                currentField.append(c);
            } else if (c == ',' && parenthesesLevel == 0) {
                // 只有在括号嵌套为0时才分割字段
                if (currentField.length() > 0) {
                    fields.add(currentField.toString().trim());
                    currentField = new StringBuilder();
                }
            } else {
                currentField.append(c);
            }
        }

        // 添加最后一个字段
        if (currentField.length() > 0) {
            fields.add(currentField.toString().trim());
        }

        log.debug("【智能字段分割】分割结果: {}", fields);
        return fields;
    }

    /**
     * 处理函数调用中的字段名
     */
    private String processFunctionCall(String field) {
        // 处理如 COUNT(field_name), SUM(field_name), COUNT(DISTINCT field_name) 等函数调用
        Pattern functionPattern = Pattern.compile("(\\w+)\\s*\\(([^)]+)\\)");
        Matcher matcher = functionPattern.matcher(field);

        if (matcher.find()) {
            String functionName = matcher.group(1);
            String parameter = matcher.group(2).trim();

            // 处理函数参数
            String processedParameter = processFunctionParameter(parameter);

            return functionName + "(" + processedParameter + ")";
        }

        return field;
    }

    /**
     * 处理函数参数，支持 DISTINCT、多个字段等复杂情况
     */
    private String processFunctionParameter(String parameter) {
        // 跳过 * 和已经用引号包裹的参数
        if (parameter.equals("*") ||
                (parameter.startsWith("`") && parameter.endsWith("`")) ||
                (parameter.startsWith("'") && parameter.endsWith("'"))) {
            return parameter;
        }

        // 处理 DISTINCT field_name 格式
        if (parameter.toUpperCase().startsWith("DISTINCT ")) {
            String[] parts = parameter.split("\\s+", 2);
            if (parts.length == 2) {
                String distinctKeyword = parts[0]; // DISTINCT
                String fieldName = parts[1].trim();

                // 处理字段名
                String processedFieldName = processFieldName(fieldName);
                return distinctKeyword + " " + processedFieldName;
            }
        }

        // 处理多个字段的情况，用逗号分隔
        if (parameter.contains(",")) {
            String[] fields = parameter.split(",");
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < fields.length; i++) {
                String fieldName = fields[i].trim();
                result.append(processFieldName(fieldName));
                if (i < fields.length - 1) {
                    result.append(", ");
                }
            }
            return result.toString();
        }

        // 处理单个字段名
        return processFieldName(parameter);
    }

    /**
     * 处理单个字段名，检查是否为SQL关键字，正确处理"表别名.字段名"格式
     */
    private String processFieldName(String fieldName) {
        fieldName = fieldName.trim();

        // 检查是否为SQL关键字
        if (isSqlKeyword(fieldName)) {
            return fieldName; // 保持SQL关键字不变
        }

        // 检查是否为数字字面量
        if (fieldName.matches("\\d+")) {
            return fieldName; // 保持数字字面量不变
        }

        // 检查是否已经用反引号包裹
        if (fieldName.startsWith("`") && fieldName.endsWith("`")) {
            return fieldName; // 已经包裹，不需要重复处理
        }

        // 检查是否为"表别名.字段名"格式
        if (fieldName.contains(".") && !fieldName.startsWith(".") && !fieldName.endsWith(".")) {
            // 分割表别名和字段名
            String[] parts = fieldName.split("\\.", 2); // 只分割第一个点，防止字段名本身包含点
            if (parts.length == 2) {
                String tableAlias = parts[0].trim();
                String columnName = parts[1].trim();

                // 分别处理表别名和字段名，避免对已经包裹的部分重复处理
                if (!tableAlias.startsWith("`") && !tableAlias.endsWith("`") &&
                        !isSqlKeyword(tableAlias) && !tableAlias.matches("\\d+")) {
                    tableAlias = "`" + tableAlias + "`";
                }

                if (!columnName.startsWith("`") && !columnName.endsWith("`") &&
                        !isSqlKeyword(columnName) && !columnName.matches("\\d+") && !columnName.equals("*")) {
                    columnName = "`" + columnName + "`";
                }

                return tableAlias + "." + columnName;
            }
        }

        // 检查是否为星号通配符
        if (fieldName.equals("*")) {
            return fieldName; // 保持星号不变
        }

        // 为普通字段名添加反引号
        return "`" + fieldName + "`";
    }

    /**
     * 处理带别名的字段（重新优化的版本）
     * 根据用户要求进行AS前后字段的规范化处理
     */
    private String processFieldWithAlias(String field) {
        log.debug("【AS别名处理】开始处理字段: {}", field);

        // 使用正则表达式分割，支持大小写不敏感的AS
        String[] parts = field.split("(?i)\\s+AS\\s+", 2);
        if (parts.length == 2) {
            String fieldExpression = parts[0].trim();
            String alias = parts[1].trim();

            log.debug("【AS别名处理】字段表达式: {}, 别名: {}", fieldExpression, alias);

            // 1. 处理AS前面的字段表达式（可能包含函数调用、表别名等）
            String processedFieldExpression = smartProcessFieldExpression(fieldExpression);

            // 2. 处理AS后面的别名：统一用反引号包裹
            String processedAlias = normalizeAlias(alias);

            String result = processedFieldExpression + " AS " + processedAlias;
            log.debug("【AS别名处理】处理结果: {}", result);

            return result;
        }

        log.debug("【AS别名处理】未找到AS关键字，返回原字段: {}", field);
        return field;
    }

    /**
     * 处理简单字段名
     */
    private String processSimpleField(String field) {
        return processFieldName(field);
    }

    /**
     * 处理条件字段，支持"表别名.字段名"格式，但排除纯数字和IN子句中的数字
     */
    private String processConditionFields(String whereClause) {
        log.debug("【WHERE子句处理】开始处理: {}", whereClause);

        // 首先完全保护所有IN子句，包括字段名和值
        Map<String, String> inClauseMap = new HashMap<>();
        int inClauseCounter = 0;

        // 匹配完整的IN子句，包括字段名和值
        Pattern inPattern = Pattern.compile(
                "(\\b[a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)?)\\s+(IN)\\s*\\(([^)]+)\\)",
                Pattern.CASE_INSENSITIVE);
        Matcher inMatcher = inPattern.matcher(whereClause);

        StringBuffer tempBuffer = new StringBuffer();
        while (inMatcher.find()) {
            String fieldName = inMatcher.group(1).trim();
            String inKeyword = inMatcher.group(2);
            String inContent = inMatcher.group(3).trim();

            log.debug("【WHERE子句处理】发现IN子句 - 字段: {}, 内容: {}", fieldName, inContent);

            // 处理字段名
            String processedFieldName = fieldName;
            String cleanedFieldName = cleanFieldName(fieldName);
            if (isTargetFieldName(cleanedFieldName)) {
                processedFieldName = "`" + cleanedFieldName + "`";
            }

            // 保存处理后的完整IN子句，内容部分不做任何修改
            String processedInClause = processedFieldName + " " + inKeyword + " (" + inContent + ")";
            String placeholder = "__IN_CLAUSE_" + inClauseCounter + "__";
            inClauseMap.put(placeholder, processedInClause);
            inClauseCounter++;

            log.debug("【WHERE子句处理】IN子句处理结果: {}", processedInClause);

            inMatcher.appendReplacement(tempBuffer, placeholder);
        }
        inMatcher.appendTail(tempBuffer);

        String processedWhereClause = tempBuffer.toString();
        log.debug("【WHERE子句处理】IN子句保护后: {}", processedWhereClause);

        // 处理剩余的字段名（非IN子句中的）
        Pattern fieldPattern = Pattern.compile(
                "(\\b[a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)?)\\s*([=<>!]+|LIKE|BETWEEN|IS)\\s*");
        Matcher matcher = fieldPattern.matcher(processedWhereClause);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String operator = matcher.group(2);

            log.debug("【WHERE子句处理】处理字段: {}, 操作符: {}", fieldName, operator);

            // 清理字段名
            String cleanedFieldName = cleanFieldName(fieldName);

            // 只处理目标字段名
            if (isTargetFieldName(cleanedFieldName)) {
                fieldName = "`" + cleanedFieldName + "`";
            } else {
                fieldName = cleanedFieldName;
            }

            matcher.appendReplacement(result, fieldName + " " + operator + " ");
        }
        matcher.appendTail(result);

        processedWhereClause = result.toString();
        log.debug("【WHERE子句处理】字段处理后: {}", processedWhereClause);

        // 恢复IN子句
        for (Map.Entry<String, String> entry : inClauseMap.entrySet()) {
            processedWhereClause = processedWhereClause.replace(entry.getKey(), entry.getValue());
        }

        log.debug("【WHERE子句处理】最终结果: {}", processedWhereClause);
        return processedWhereClause;
    }

    /**
     * 处理IN子句中的字段名部分（这个方法现在不需要了，因为我们在上面的方法中直接处理）
     */
    private String processInFieldName(String fieldPart) {
        // 这个方法现在不需要了，保留是为了兼容性
        return fieldPart;
    }

    /**
     * 处理ORDER BY字段，排除纯数字
     */
    private String processOrderByFields(String orderByClause) {
        String[] fields = orderByClause.split(",");
        StringBuilder processedFields = new StringBuilder();

        for (int i = 0; i < fields.length; i++) {
            String field = fields[i].trim();

            // 处理 field ASC/DESC 格式
            String[] parts = field.split("\\s+");
            if (parts.length > 0) {
                String fieldName = parts[0];

                // 只处理有效的字段名（不是纯数字）
                if (!fieldName.matches("\\d+") && !fieldName.isEmpty()) {
                    fieldName = processFieldName(fieldName);
                }

                processedFields.append(fieldName);

                // 保留ASC/DESC等关键词
                for (int j = 1; j < parts.length; j++) {
                    processedFields.append(" ").append(parts[j]);
                }
            }

            if (i < fields.length - 1) {
                processedFields.append(", ");
            }
        }

        return processedFields.toString();
    }

    /**
     * 处理GROUP BY字段，排除纯数字
     */
    private String processGroupByFields(String groupByClause) {
        String[] fields = groupByClause.split(",");
        StringBuilder processedFields = new StringBuilder();

        for (int i = 0; i < fields.length; i++) {
            String field = fields[i].trim();

            // 只处理有效的字段名（不是纯数字）
            if (!field.matches("\\d+") && !field.isEmpty()) {
                field = processFieldName(field);
            }

            processedFields.append(field);

            if (i < fields.length - 1) {
                processedFields.append(", ");
            }
        }

        return processedFields.toString();
    }

    public String genSQLCAICT(String queryText, String tableName, String pythonCode, String diagHistory,
                         String question,String tables) {
        // 构建完整的提示词
        String fullPrompt = GEN_SQL.replace("{{py_codes}}", pythonCode);
        fullPrompt = fullPrompt.replace("{{tableSchema}}", tables != null ? tables : "");
        fullPrompt = fullPrompt.replace("{{query_text}}", queryText != null ? queryText : "");
        fullPrompt = fullPrompt.replace("{{table_name}}", tableName != null ? tableName : "");
        fullPrompt = fullPrompt.replace("{{diag_history}}", diagHistory != null ? diagHistory : "");
        fullPrompt = fullPrompt.replace("{{question}}", question != null ? question : "");

        log.info("【定制任务执行】查询单行业数据SQL生成提示词: \n{}", fullPrompt);
        RequestConfig config = RequestConfig.defaultConfig();
        config.setTopP(0.01);
        config.setTemperature(0.0f);
        config.setAliModelType(AliModelType.QWEN_3_32B_INSTRUCT);
        String sql = aiService.chat(fullPrompt, config);
        log.info("【定制任务执行】查询单行业数据SQL生成结果: \n{}", sql);
        sql = sql.replace("```sql", "").replace("```SQL", "").replace("```", "").trim();

        // 验证SQL语句
        if (StringUtils.isBlank(sql)) {
            throw new RuntimeException("AI生成的SQL语句为空");
        }

        // 简单的SQL注入防护
        if (sql.toLowerCase().contains("drop") ||
                sql.toLowerCase().contains("delete") ||
                sql.toLowerCase().contains("update") ||
                sql.toLowerCase().contains("insert")) {
            throw new RuntimeException("不允许执行修改数据的SQL语句");
        }

        return sql;
    }




    /**
     * 执行SQL语句并返回结果
     * 
     * @param validatedSql 经过校验的SQL语句（可能包含说明文字）
     * @return 查询结果
     */
    public List<Map<String, Object>> executeSQL(String validatedSql) {
        // 1. 首先从可能包含说明文字的输入中提取真正的SQL语句
        String extractedSQL = extractActualSQL(validatedSql);

        // 2. 判断SQL类型
        if (extractedSQL.trim().toLowerCase().startsWith("select")) {
            // 3. 在执行前进行SQL标注化处理，降低异常率
            String finalSQL = extractedSQL;
            if (ENABLE_NORMALIZED_SQL) {
                finalSQL = normalizeSql(extractedSQL);
                log.info("【SQL执行】使用规范化处理后的SQL: \n{}", finalSQL);
            }

            // 4. 执行标注化处理后的查询语句
            return SqlRunner.db().selectList(finalSQL);
        } else {
            throw new RuntimeException("只支持SELECT查询语句");
        }
    }


}
