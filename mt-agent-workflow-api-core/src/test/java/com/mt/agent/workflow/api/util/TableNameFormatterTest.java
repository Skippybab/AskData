package com.mt.agent.workflow.api.util;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * TableNameFormatter测试类
 * 用于展示生成的all_table_name格式
 */
@SpringBootTest
public class TableNameFormatterTest {

    private final TableNameFormatter formatter = new TableNameFormatter();

    @Test
    public void testFormatTableNameForDify() {
        // 测试用的DDL语句
        String testDdl = """
            CREATE TABLE `cons_conversation_record` (
              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
              `user_id` bigint NOT NULL COMMENT '用户ID',
              `question` text COMMENT '客户问题原文',
              `intent_text` varchar(200) COMMENT '用户意图的详细文本',
              `chat_time` varchar(16) COMMENT '生成对话的时间',
              `datasource_id` bigint COMMENT '数据源唯一标识',
              `fun_code` varchar(16) COMMENT '系统功能编号',
              `summary` text COMMENT '该轮对话的概括性总结',
              `result` text COMMENT '工作流执行结果',
              `operations` text COMMENT '针对可视化组件的操作',
              `status` smallint COMMENT '对话状态 1.成功 2.失败',
              `available` smallint COMMENT '记录的有效状态：0.无效已过期 1.有效',
              PRIMARY KEY (`id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单轮对话记录表';
            """;

        String result = formatter.formatTableNameForDify(
            "cons_conversation_record",
            "单轮对话记录表",
            testDdl
        );

        System.out.println("生成的all_table_name格式：");
        System.out.println("=".repeat(80));
        System.out.println(result);
        System.out.println("=".repeat(80));
    }

    @Test
    public void testFormatTableNameForDifyWithComplexDdl() {
        // 测试复杂DDL语句
        String complexDdl = """
            CREATE TABLE `declaration_item_change_stats` (
              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
              `enterprise_name` varchar(100) NOT NULL COMMENT '企业名称',
              `enterprise_industry` varchar(50) COMMENT '行业名称',
              `declaration_item` varchar(200) NOT NULL COMMENT '申报项名称',
              `change_count` int DEFAULT 0 COMMENT '该申报项被修改的次数',
              `consultation_count` int DEFAULT 0 COMMENT '该申报项被咨询的次数',
              `last_updated` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
              `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              PRIMARY KEY (`id`),
              KEY `idx_enterprise` (`enterprise_name`),
              KEY `idx_item` (`declaration_item`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申报项变化统计表';
            """;

        String result = formatter.formatTableNameForDify(
            "declaration_item_change_stats",
            "申报项变化统计表",
            complexDdl
        );

        System.out.println("生成的复杂表all_table_name格式：");
        System.out.println("=".repeat(80));
        System.out.println(result);
        System.out.println("=".repeat(80));
    }
}
