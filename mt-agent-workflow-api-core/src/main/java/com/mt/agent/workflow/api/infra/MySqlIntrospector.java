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
	public List<ColumnMeta> listColumns(DataSource ds, String databaseName, String tableName) {
		String sql = "select ordinal_position, column_name, data_type, is_nullable, column_default, ifnull(column_comment,'') as column_comment from information_schema.columns where table_schema = ? and table_name = ? order by ordinal_position";
		List<ColumnMeta> list = new ArrayList<>();
		try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, databaseName);
			ps.setString(2, tableName);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					ColumnMeta m = new ColumnMeta();
					m.ordinal = rs.getInt("ordinal_position");
					m.columnName = rs.getString("column_name");
					m.dbDataType = rs.getString("data_type");
					m.normType = TypeMapper.toNormType(m.dbDataType);
					String isNullable = rs.getString("is_nullable");
					m.isNullable = ("YES".equalsIgnoreCase(isNullable) ? 1 : 0);
					m.columnDefault = rs.getString("column_default");
					m.columnComment = rs.getString("column_comment");
					list.add(m);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("列元数据探查失败: " + e.getMessage(), e);
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
