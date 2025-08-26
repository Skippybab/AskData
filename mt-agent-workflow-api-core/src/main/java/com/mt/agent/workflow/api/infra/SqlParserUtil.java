package com.mt.agent.workflow.api.infra;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqlParserUtil {
    private static final Set<String> DML_DDL = new HashSet<>(Arrays.asList(
            "INSERT", "UPDATE", "DELETE", "MERGE", "REPLACE",
            "DROP", "ALTER", "TRUNCATE", "CREATE", "RENAME", "GRANT", "REVOKE"
    ));

    public static void assertReadOnly(String sql) {
        String s = stripComments(sql).trim().toUpperCase(Locale.ROOT);
        for (String kw : DML_DDL) {
            if (s.contains(kw + " ") || s.startsWith(kw)) {
                throw new IllegalArgumentException("仅允许只读查询，检测到关键字: " + kw);
            }
        }
    }

    public static String ensureLimit(String sql, int maxRows) {
        log.info("🔍 [SQL解析] 开始处理LIMIT，原始SQL: {}", sql);
        log.info("🔍 [SQL解析] 最大行数限制: {}", maxRows);
        
        String s = sql.trim();
        String upper = s.toUpperCase(Locale.ROOT);
        
        log.info("🔍 [SQL解析] 处理后的SQL: {}", s);
        log.info("🔍 [SQL解析] 大写SQL: {}", upper);
        
        if (upper.contains(" LIMIT ")) {
            return s;
        }
        
        String result = s + " LIMIT " + Math.max(1, maxRows);
        log.info("🔍 [SQL解析] 添加LIMIT子句，最终SQL: {}", result);
        
        return result;
    }

    private static String stripComments(String sql) {
        return sql.replaceAll("/\\*.*?\\*/", " ").replaceAll("--.*?(\\r?\\n)", " ");
    }
}


