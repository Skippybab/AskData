/*
 Navicat Premium Data Transfer

 Source Server         : rm-7xv29oc5w125d296iyo.mysql.rds.aliyuncs.com_3306
 Source Server Type    : MySQL
 Source Server Version : 80028
 Source Host           : rm-7xv29oc5w125d296iyo.mysql.rds.aliyuncs.com:3306
 Source Schema         : ai-common-api

 Target Server Type    : MySQL
 Target Server Version : 80028
 File Encoding         : 65001

 Date: 15/08/2025 17:12:24
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '用户状态（0正常 1停用）',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0未删除 1已删除）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ----------------------------
-- Table structure for chat_session
-- ----------------------------
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `session_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '新对话' COMMENT '会话名称',
  `db_config_id` bigint DEFAULT NULL COMMENT '关联的数据库配置ID',
  `table_id` bigint DEFAULT NULL COMMENT '关联的表ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（1活跃 2已结束 0已删除）',
  `message_count` int NOT NULL DEFAULT '0' COMMENT '消息数量',
  `last_message_at_ms` bigint DEFAULT NULL COMMENT '最后一条消息时间（毫秒）',
  `created_at_ms` bigint NOT NULL COMMENT '创建时间（毫秒）',
  `updated_at_ms` bigint NOT NULL COMMENT '更新时间（毫秒）',
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`,`created_at_ms` DESC),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_table_id` (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色（user, assistant, system）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `content_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'text' COMMENT '内容类型（text, sql, result, error, thinking, python_code）',
  `sql_execution_id` bigint DEFAULT NULL COMMENT '关联的SQL执行ID',
  `dify_trace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Dify调用追踪ID',
  `tokens_used` int DEFAULT NULL COMMENT '消耗的token数量',
  `duration_ms` bigint DEFAULT NULL COMMENT '处理耗时（毫秒）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（1成功 2失败 0处理中）',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `metadata_json` json DEFAULT NULL COMMENT '扩展元数据JSON',
  `thinking_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'AI思考过程内容，存储<think></think>标签内的内容',
  `python_code` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '生成的Python代码，存储```Python代码块```内的内容',
  `execution_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'Python代码执行结果，存储执行后的输出内容',
  `execution_status` tinyint DEFAULT 1 COMMENT '执行状态：1成功,2失败,0执行中',
  `created_at_ms` bigint NOT NULL COMMENT '创建时间（毫秒）',
  PRIMARY KEY (`id`),
  KEY `idx_session_created` (`session_id`,`created_at_ms`),
  KEY `idx_user_created` (`user_id`,`created_at_ms` DESC),
  KEY `idx_role_status` (`role`,`status`),
  KEY `idx_execution_status` (`execution_status`),
  KEY `idx_has_python_code` (`python_code`(100)),
  CONSTRAINT `chat_message_ibfk_1` FOREIGN KEY (`session_id`) REFERENCES `chat_session` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- ----------------------------
-- Table structure for db_config
-- ----------------------------
DROP TABLE IF EXISTS `db_config`;
CREATE TABLE `db_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置名称',
  `db_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '数据库类型',
  `host` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主机地址',
  `port` int NOT NULL COMMENT '端口',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password_cipher` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '加密后的密码',
  `database_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '数据库名称',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（1启用 0禁用）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外部数据库配置表';

-- ----------------------------
-- Table structure for table_info
-- ----------------------------
DROP TABLE IF EXISTS `table_info`;
CREATE TABLE `table_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '表信息ID',
  `db_config_id` bigint NOT NULL COMMENT '关联的数据库配置ID',
  `table_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '表名',
  `table_comment` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '表注释',
  `table_ddl` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '表的DDL语句',
  `enabled` tinyint(1) DEFAULT '0' COMMENT '是否对AI启用（0否 1是）',
  PRIMARY KEY (`id`),
  KEY `idx_db_config_id` (`db_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='同步的表信息表';

-- ----------------------------
-- Table structure for user_tool_config
-- ----------------------------
DROP TABLE IF EXISTS `user_tool_config`;
CREATE TABLE `user_tool_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tool_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工具类型（sql_query, kb_qa）',
  `tool_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工具名称',
  `config_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '工具配置JSON',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用',
  `created_at_ms` bigint NOT NULL COMMENT '创建时间（毫秒）',
  `updated_at_ms` bigint NOT NULL COMMENT '更新时间（毫秒）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tool` (`user_id`,`tool_type`,`tool_name`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户工具配置表';

-- ----------------------------
-- Table structure for sql_execution
-- ----------------------------
DROP TABLE IF EXISTS `sql_execution`;
CREATE TABLE `sql_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '执行ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `db_config_id` bigint NOT NULL COMMENT '数据库配置ID',
  `schema_version_id` bigint DEFAULT NULL COMMENT '执行时所用的schema版本ID',
  `tables_used` varchar(1024) DEFAULT NULL COMMENT '查询用到的表（逗号分隔）',
  `sql_text` text NOT NULL COMMENT '执行的SQL语句',
  `status` int NOT NULL COMMENT '状态（0准备 1执行中 2成功 3失败）',
  `duration_ms` bigint DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `affected_rows` bigint DEFAULT NULL COMMENT '影响行数',
  `error_msg` text COMMENT '错误信息',
  `result_cache_key` varchar(255) DEFAULT NULL COMMENT '结果缓存的Key',
  `created_at_ms` bigint NOT NULL COMMENT '创建时间（毫秒）',
  `finished_at_ms` bigint DEFAULT NULL COMMENT '完成时间（毫秒）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SQL执行记录表';

-- ----------------------------
-- Table structure for db_config_acl
-- ----------------------------
DROP TABLE IF EXISTS `db_config_acl`;
CREATE TABLE `db_config_acl` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `db_config_id` bigint NOT NULL COMMENT '数据库配置ID',
  `subject_type` int NOT NULL COMMENT '授权主体类型（1用户 2角色）',
  `subject_id` bigint NOT NULL COMMENT '授权主体ID',
  `perm_use` int DEFAULT '0' COMMENT '使用权限（0禁止 1允许）',
  `perm_manage` int DEFAULT '0' COMMENT '管理权限（0禁止 1允许）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_db_subject` (`db_config_id`,`subject_type`,`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据库配置访问控制表';

-- ----------------------------
-- Table structure for table_permission
-- ----------------------------
DROP TABLE IF EXISTS `table_permission`;
CREATE TABLE `table_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户ID',
  `db_config_id` bigint NOT NULL COMMENT '数据库配置ID',
  `table_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '表名',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `permission_type` int NOT NULL DEFAULT '1' COMMENT '权限类型：1-查询，2-修改，3-删除',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用：0-禁用，1-启用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_table` (`user_id`,`db_config_id`,`table_name`,`permission_type`),
  KEY `idx_db_config` (`db_config_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表权限控制表';

-- ----------------------------
-- Table structure for sql_guard_policy
-- ----------------------------
DROP TABLE IF EXISTS `sql_guard_policy`;
CREATE TABLE `sql_guard_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `db_config_id` bigint NOT NULL COMMENT '数据库配置ID',
  `read_only` int DEFAULT '1' COMMENT '只读模式（0否 1是）',
  `deny_keywords` text COMMENT '禁止的关键词（逗号分隔）',
  `table_blacklist` text COMMENT '表黑名单（逗号分隔）',
  `table_whitelist` text COMMENT '表白名单（逗号分隔）',
  `max_scan_rows` bigint DEFAULT NULL COMMENT '最大扫描行数限制',
  `max_timeout_ms` int DEFAULT NULL COMMENT '最大超时时间（毫秒）',
  `status` int DEFAULT '1' COMMENT '策略状态（0禁用 1启用）',
  `created_at_ms` bigint NOT NULL,
  `updated_at_ms` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_db_config` (`db_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SQL安全策略表';

-- ----------------------------
-- Table structure for audit_log
-- ----------------------------
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `action` varchar(255) NOT NULL COMMENT '操作动作',
  `object_type` varchar(255) DEFAULT NULL COMMENT '操作对象类型',
  `object_id` bigint DEFAULT NULL COMMENT '操作对象ID',
  `detail_json` json DEFAULT NULL COMMENT '详情JSON',
  `ip` varchar(64) DEFAULT NULL COMMENT '来源IP',
  `created_at_ms` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- ----------------------------
-- Table structure for query_result_cache
-- ----------------------------
DROP TABLE IF EXISTS `query_result_cache`;
CREATE TABLE `query_result_cache` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint DEFAULT NULL,
  `db_config_id` bigint NOT NULL,
  `schema_version_id` bigint DEFAULT NULL,
  `sql_hash` varchar(64) NOT NULL COMMENT 'SQL语句的Hash值',
  `preview_json` json DEFAULT NULL COMMENT '结果预览JSON',
  `result_path` varchar(1024) DEFAULT NULL COMMENT '完整结果文件路径',
  `row_count` bigint DEFAULT NULL COMMENT '总行数',
  `column_headers_json` json DEFAULT NULL COMMENT '列头信息JSON',
  `file_size_bytes` bigint DEFAULT NULL COMMENT '结果文件大小',
  `expired_at_ms` bigint NOT NULL COMMENT '过期时间',
  `created_at_ms` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sql_hash` (`sql_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='查询结果缓存表';

-- ----------------------------
-- Table structure for schema_version
-- ----------------------------
DROP TABLE IF EXISTS `schema_version`;
CREATE TABLE `schema_version` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint DEFAULT NULL,
  `db_config_id` bigint NOT NULL,
  `version_no` int NOT NULL COMMENT '版本号',
  `status` int NOT NULL COMMENT '状态（0准备 1进行中 2成功 3失败）',
  `table_count` int DEFAULT NULL COMMENT '同步的表数量',
  `column_count` int DEFAULT NULL COMMENT '同步的列数量',
  `notes` text COMMENT '备注',
  `created_at_ms` bigint NOT NULL,
  `finished_at_ms` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_db_config` (`db_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Schema同步版本表';

SET FOREIGN_KEY_CHECKS = 1;
