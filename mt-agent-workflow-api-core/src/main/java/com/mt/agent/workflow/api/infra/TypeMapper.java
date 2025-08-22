package com.mt.agent.workflow.api.infra;

public class TypeMapper {
	public static String toNormType(String mysqlDataType) {
		if (mysqlDataType == null) return "text";
		String t = mysqlDataType.toLowerCase();
		if (t.contains("int")) return "int";
		if (t.equals("decimal") || t.equals("numeric") || t.equals("double") || t.equals("float")) return "decimal";
		if (t.equals("date") || t.equals("datetime") || t.equals("timestamp") || t.equals("time") || t.equals("year")) return "date";
		if (t.equals("bool") || t.equals("boolean") || t.equals("tinyint")) return "boolean";
		return "text";
	}
}
