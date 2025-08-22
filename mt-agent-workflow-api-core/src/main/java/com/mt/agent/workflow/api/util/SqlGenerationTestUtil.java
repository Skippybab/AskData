package com.mt.agent.workflow.api.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SQL生成测试工具类
 * 用于验证AI SQL生成功能的完整性
 */
@Slf4j
@Component
public class SqlGenerationTestUtil {

    /**
     * 测试SQL生成和执行流程
     */
    public static void testSqlGenerationFlow() {
        log.info("=== SQL生成和执行流程测试开始 ===");
        
        // 测试用例1：简单查询
        testCase1();
        
        // 测试用例2：带条件查询
        testCase2();
        
        // 测试用例3：聚合查询
        testCase3();
        
        log.info("=== SQL生成和执行流程测试完成 ===");
    }
    
    private static void testCase1() {
        log.info("测试用例1：简单查询 - 查询所有数据");
        String queryText = "查询所有数据";
        String tableName = "api_config";
        String expectedPattern = "SELECT.*FROM.*api_config";
        
        log.info("查询文本: {}", queryText);
        log.info("目标表名: {}", tableName);
        log.info("期望的SQL模式: {}", expectedPattern);
        
        // 实际的测试需要在运行时通过依赖注入的服务来执行
        log.info("提示：该测试需要在Spring容器中运行，通过实际的AISQLQueryService来验证");
    }
    
    private static void testCase2() {
        log.info("测试用例2：带条件查询 - 查询启用状态的配置");
        String queryText = "查询状态为启用的API配置";
        String tableName = "api_config";
        String expectedPattern = "SELECT.*FROM.*api_config.*WHERE.*status.*=.*1";
        
        log.info("查询文本: {}", queryText);
        log.info("目标表名: {}", tableName);
        log.info("期望的SQL模式: {}", expectedPattern);
    }
    
    private static void testCase3() {
        log.info("测试用例3：聚合查询 - 统计API调用次数");
        String queryText = "统计各个API的总调用次数";
        String tableName = "api_config";
        String expectedPattern = "SELECT.*api_name.*SUM.*call_count.*FROM.*api_config.*GROUP BY";
        
        log.info("查询文本: {}", queryText);
        log.info("目标表名: {}", tableName);
        log.info("期望的SQL模式: {}", expectedPattern);
    }
    
    /**
     * 生成Python代码测试案例
     */
    public static String generateTestPythonCode() {
        return """
            # 测试Python代码：查询API配置数据并进行分析
            def analyze_api_data():
                # 1. 生成SQL查询
                sql = gen_sql("查询所有启用的API配置", "api_config")
                print(f"生成的SQL: {sql}")
                
                # 2. 执行SQL
                result = exec_sql(sql)
                print(f"查询结果数量: {len(result) if result else 0}")
                
                # 3. 分析结果
                if result:
                    active_count = len(result)
                    print(f"启用的API配置数量: {active_count}")
                    return result
                else:
                    print("没有找到数据")
                    return []
            
            # 调用分析函数
            data = analyze_api_data()
            """;
    }
    
    /**
     * 生成表结构测试数据
     */
    public static String generateTestTableSchema() {
        return """
            表名: api_config
            说明: API配置表
            字段:
            - id: BIGINT, 主键，API配置ID
            - api_name: VARCHAR(100), API名称
            - api_path: VARCHAR(100), API路径
            - api_key: VARCHAR(100), API密钥
            - db_config_id: BIGINT, 关联的数据库配置ID
            - table_id: BIGINT, 关联的数据表ID
            - description: VARCHAR(500), API描述
            - status: TINYINT, 状态（1启用 0禁用）
            - call_count: BIGINT, 调用次数
            - last_call_time: DATETIME, 最后调用时间
            - user_id: BIGINT, 创建用户ID
            - create_time: DATETIME, 创建时间
            - update_time: DATETIME, 更新时间
            - rate_limit: INT, 速率限制（每分钟最大请求数）
            - timeout: INT, 超时时间（秒）
            """;
    }
}