package com.mt.agent.workflow.api.util;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

/**
 * 测试DdlParser的数据类型清理功能
 */
public class DdlParserDataTypeTest {
    
    @Test
    public void testDataTypeCleaningInColumnParsing() {
        String testDdl = """
            CREATE TABLE `test_table` (
              `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
              `name` varchar(255) NOT NULL COMMENT '姓名',
              `age` int(11) unsigned DEFAULT NULL COMMENT '年龄',
              `salary` decimal(10,2) DEFAULT NULL COMMENT '薪资',
              `height` double(5,2) DEFAULT NULL COMMENT '身高',
              `status` tinyint(1) DEFAULT '1' COMMENT '状态',
              `description` text COMMENT '描述',
              `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试表';
            """;
        
        System.out.println("=== 测试DDL解析中的数据类型清理 ===");
        
        // 使用DdlParser解析DDL
        List<Map<String, Object>> columns = DdlParser.parseColumnsFromDdl(testDdl);
        
        System.out.println("解析结果:");
        for (Map<String, Object> column : columns) {
            String name = (String) column.get("name");
            String type = (String) column.get("type");
            String comment = (String) column.get("comment");
            
            System.out.println(String.format("字段: %-15s 类型: %-10s 注释: %s", 
                name, type, comment));
        }
        
        // 验证类型是否正确清理
        assert columns.size() > 0 : "应该解析出字段";
        
        // 检查具体的类型清理结果
        for (Map<String, Object> column : columns) {
            String type = (String) column.get("type");
            String name = (String) column.get("name");
            
            switch (name) {
                case "id":
                    assert "BIGINT".equals(type) : "id字段类型应该是BIGINT，实际是: " + type;
                    break;
                case "name":
                    assert "VARCHAR".equals(type) : "name字段类型应该是VARCHAR，实际是: " + type;
                    break;
                case "age":
                    assert "INT".equals(type) : "age字段类型应该是INT，实际是: " + type;
                    break;
                case "salary":
                    assert "DECIMAL".equals(type) : "salary字段类型应该是DECIMAL，实际是: " + type;
                    break;
                case "height":
                    assert "DOUBLE".equals(type) : "height字段类型应该是DOUBLE，实际是: " + type;
                    break;
                case "status":
                    assert "TINYINT".equals(type) : "status字段类型应该是TINYINT，实际是: " + type;
                    break;
                case "description":
                    assert "TEXT".equals(type) : "description字段类型应该是TEXT，实际是: " + type;
                    break;
                case "created_at":
                    assert "DATETIME".equals(type) : "created_at字段类型应该是DATETIME，实际是: " + type;
                    break;
            }
        }
        
        System.out.println("✅ 所有数据类型清理测试通过!");
    }
    
    @Test
    public void testDataTypeCleaningDirect() {
        System.out.println("\n=== 测试直接数据类型清理功能 ===");
        DdlParser.testCleanDataType();
    }
}
