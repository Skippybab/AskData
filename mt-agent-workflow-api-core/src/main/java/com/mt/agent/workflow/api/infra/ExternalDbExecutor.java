package com.mt.agent.workflow.api.infra;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternalDbExecutor {
    public static class ExecOptions {
        public int queryTimeoutSeconds = 30;
        public int maxRows = 1000;
    }

    public static class QueryResult {
        public List<Map<String, Object>> rows = new ArrayList<>();
        public long rowCount;
        public long durationMs;
    }

    public static QueryResult query(DataSource ds, String sql, ExecOptions opt) {
        long start = System.currentTimeMillis();
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            conn.setReadOnly(true);
            stmt.setQueryTimeout(opt.queryTimeoutSeconds);
            stmt.setMaxRows(opt.maxRows);
            boolean has = stmt.execute(sql);
            QueryResult qr = new QueryResult();
            if (has) {
                try (ResultSet rs = stmt.getResultSet()) {
                    int colCount = rs.getMetaData().getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= colCount; i++) {
                            String label = rs.getMetaData().getColumnLabel(i);
                            row.put(label, rs.getObject(i));
                        }
                        qr.rows.add(row);
                    }
                    qr.rowCount = qr.rows.size();
                }
            }
            qr.durationMs = System.currentTimeMillis() - start;
            return qr;
        } catch (Exception e) {
            throw new RuntimeException("外库查询失败: " + e.getMessage(), e);
        }
    }
}


