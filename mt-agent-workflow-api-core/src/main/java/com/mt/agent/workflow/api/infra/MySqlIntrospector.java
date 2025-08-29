package com.mt.agent.workflow.api.infra;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class MySqlIntrospector implements ExternalDbIntrospector {
	@Override
	public List<TableMeta> listTables(DataSource ds, String databaseName) {
		String sql = "select table_schema, table_name, table_type, table_rows, engine, ifnull(table_comment,'') as table_comment from information_schema.tables where table_schema = ? order by table_name";
		List<TableMeta> list = new ArrayList<>();
		try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, databaseName);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					TableMeta t = new TableMeta();
					t.schemaName = rs.getString("table_schema");
					t.tableName = rs.getString("table_name");
					t.tableType = rs.getString("table_type");
					long rows = 0;
					try { rows = rs.getLong("table_rows"); } catch (Exception ignored) {}
					t.rowsEstimate = rows;
					t.engine = rs.getString("engine");
					t.tableComment = rs.getString("table_comment");
					list.add(t);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("表元数据探查失败: " + e.getMessage(), e);
		}
		return list;
	}

	@Override
	public String getTableDdl(DataSource ds, String tableName) {
		String sql = "SHOW CREATE TABLE " + tableName;
		try (Connection c = ds.getConnection(); 
			 PreparedStatement ps = c.prepareStatement(sql);
			 ResultSet rs = ps.executeQuery()) {
			if (rs.next()) {
				return rs.getString("Create Table");
			}
		} catch (Exception e) {
			throw new RuntimeException("获取表DDL失败: " + e.getMessage(), e);
		}
		return null;
	}
}
