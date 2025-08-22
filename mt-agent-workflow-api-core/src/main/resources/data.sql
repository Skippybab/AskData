-- 初始化数据

-- 插入默认用户（如果不存在）
INSERT IGNORE INTO sys_user (id, username, password, status, deleted, remark) 
VALUES (1, 'admin', '21232f297a57a5a743894a0e4a801fc3', '0', 0, '系统管理员');

-- 插入测试数据库配置（如果不存在）
INSERT IGNORE INTO db_config (
    id, user_id, name, db_type, host, port, username, password_cipher, database_name, status, 
    created_at, updated_at
) VALUES (
    1, 1, '本地测试数据库', 'mysql', 'rm-7xv29oc5w125d296iyo.mysql.rds.aliyuncs.com', 3306, 
    'mobirit_test', 'Cq9CbS6wDXiRaEzzIHZgVW7JOBM8DVQJZiL0sYH8aDXo6FeHZCfd5w==', 'ai-common-api', 1,
    NOW(), NOW()
);

-- 插入表信息（如果不存在）
INSERT IGNORE INTO table_info (id, db_config_id, table_name, table_comment, table_ddl, enabled) VALUES
(1, 1, 'api_config', 'API配置表', '', 1),
(2, 1, 'sys_user', '系统用户表', '', 1),
(3, 1, 'db_config', '数据库配置表', '', 1),
(4, 1, 'chat_session', '聊天会话表', '', 1),
(5, 1, 'chat_message', '聊天消息表', '', 1);

-- 插入默认API配置（如果不存在）
INSERT IGNORE INTO api_config (
    id, api_name, api_path, api_key, db_config_id, table_id, description, 
    status, call_count, user_id, create_time, update_time, rate_limit, timeout
) VALUES (
    1, '默认数据问答API', 'default_api', 'sk_default_api_key_123456789', 1, 0, 
    '默认的数据问答API接口', 1, 0, 1, NOW(), NOW(), 60, 30
);