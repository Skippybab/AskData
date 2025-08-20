-- 数据库迁移脚本
-- 用于修改 chat_message 表的 execution_result 字段类型

-- 修改 execution_result 字段类型从 text 改为 longtext
ALTER TABLE `chat_message` MODIFY COLUMN `execution_result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'Python代码执行结果，存储执行后的输出内容';

-- 检查修改结果
DESCRIBE `chat_message`;
