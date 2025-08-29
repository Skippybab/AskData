package com.mt.agent.workflow.api.infra;

import javax.sql.DataSource;
import java.util.List;

public interface ExternalDbIntrospector {
	List<TableMeta> listTables(DataSource ds, String databaseName);
	String getTableDdl(DataSource ds, String tableName);

	class TableMeta {
		public String schemaName;
		public String tableName;
		public String tableType;
		public Long rowsEstimate;
		public String engine;
		public String tableComment;
	}
}
