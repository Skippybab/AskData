# 表选择功能测试说明

## 功能概述
已成功实现从选择数据库配置改为选择具体数据库表的功能。

## 主要修改

### 前端修改
1. **UI界面**：
   - 在数据库选择器下方添加了表选择器
   - 表选择器显示表名和启用状态
   - 修改了欢迎消息和按钮状态

2. **状态管理**：
   - 添加了 `selectedTable`、`availableTables`、`loadingTables` 状态
   - 修改了发送消息和新对话的验证逻辑

3. **交互逻辑**：
   - 数据库切换时自动加载对应的表列表
   - 表切换时提示用户确认（会开启新对话）
   - 会话切换时同步表配置

### 后端修改
1. **接口参数**：
   - `ChatController.sendMessage` 添加 `tableId` 参数
   - `ChatController.createSession` 添加 `tableId` 参数

2. **服务层**：
   - `ChatOrchestratorService.processDataQuestion` 添加 `tableId` 参数
   - `ChatService.createSession` 添加 `tableId` 参数

3. **数据层**：
   - `ChatSession` 实体类添加 `tableId` 字段
   - `TableInfoService` 添加 `getFormattedTableStructure` 方法

## 数据库迁移
需要执行以下SQL来添加table_id字段：
```sql
ALTER TABLE `chat_session` 
ADD COLUMN `table_id` bigint DEFAULT NULL COMMENT '关联的表ID' AFTER `db_config_id`;

ALTER TABLE `chat_session` 
ADD INDEX `idx_table_id` (`table_id`);
```

## 测试步骤
1. 执行数据库迁移脚本
2. 启动后端服务
3. 启动前端服务
4. 在数据问答界面：
   - 选择数据库配置
   - 选择具体的表
   - 发送查询消息
   - 验证历史对话显示数据库名和表名

## 预期效果
- 用户需要先选择数据库，再选择具体的表才能开始对话
- `all_table_names` 参数只包含用户选择的单个表的结构信息
- 历史对话按数据库和表分类显示
- 查询结果更加精确，避免大模型混淆多个表的结构
