package com.mt.agent.enums.consensus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DatasourceFields {
    DATASOURCE_INDEX("dataSourceIndex", "数据源索引", String.class),
    DATASOURCE_NAME("dataSourceName", "数据源名称", String.class),
    DATASOURCE_DESC("dataSourceDesc", "数据源描述", String.class),
    DB_URL("dbUrl", "数据库连接地址", String.class),
    TABLE_NAME("tableName", "表名", String.class),
    CREATE_SQL("createSQL", "创建SQL", String.class);

    private final String fieldName;
    private final String name;
    private final Class<?> clazz;

    public static DatasourceFields getByFieldName(String fieldName) {
        for (DatasourceFields field : DatasourceFields.values()) {
            if (field.getFieldName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

}
