-- 创建API配置表
USE `ai-common-api`;

CREATE TABLE IF NOT EXISTS `api_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'API配置ID',
  `api_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'API名称',
  `api_path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'API路径',
  `api_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'API密钥',
  `db_config_id` bigint NOT NULL COMMENT '关联的数据库配置ID',
  `table_id` bigint NOT NULL COMMENT '关联的数据表ID',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'API描述',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（1启用 0禁用）',
  `call_count` bigint NOT NULL DEFAULT '0' COMMENT '调用次数',
  `last_call_time` datetime DEFAULT NULL COMMENT '最后调用时间',
  `user_id` bigint NOT NULL COMMENT '创建用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `rate_limit` int DEFAULT '60' COMMENT '速率限制（每分钟最大请求数）',
  `timeout` int DEFAULT '30' COMMENT '超时时间（秒）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_path` (`api_path`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_db_config_id` (`db_config_id`),
  KEY `idx_table_id` (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API配置表';

-- 查看表结构
DESCRIBE api_config;