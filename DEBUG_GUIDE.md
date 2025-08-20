# 调试指南 - 数据问答系统

## 概述
本文档说明了在数据问答系统中添加的调试信息，帮助定位前端无法显示SQL查询结果的问题。

## 添加的调试信息

### 1. 后端调试信息

#### ChatOrchestratorServiceImpl.java
- **位置**: `updateMessageAndSendResult()` 方法
- **调试信息**:
  - 🔍 [数据问答] 开始更新消息并发送结果
  - 🔍 [数据问答] SQL查询结果数据长度
  - 🔍 [数据问答] SQL查询结果前200字符
  - 🔍 [数据问答] 数据截断处理信息
  - 🔍 [数据问答] 消息更新完成状态

#### PythonExecutorServiceImpl.java
- **位置**: `updateExecutionResult()` 方法
- **调试信息**:
  - 🔍 [Python执行器] 开始更新执行结果到数据库
  - 🔍 [Python执行器] 执行结果已更新到数据库
  - 🔍 [Python执行器] 执行结果前200字符

- **位置**: `handleExecSql()` 方法
- **调试信息**:
  - 🔍 [Python执行器] 开始执行SQL
  - 🔍 [Python执行器] SQL执行成功，结果类型和大小
  - 🔍 [Python执行器] SQL查询返回行数和列信息

#### SSE消息发送
- **位置**: `sendSseMessage()` 方法
- **调试信息**:
  - 🔍 [SSE] 发送消息详情
  - 🔍 [SSE] 消息发送成功状态

### 2. 前端调试信息

#### ChatInterface.vue
- **位置**: `sendMessage()` 方法
- **调试信息**:
  - 🔍 [前端调试] 发送消息到后端
  - 🔍 [前端调试] 收到后端响应
  - 🔍 [前端调试] 响应内容前500字符

- **位置**: `parseSseResponse()` 方法
- **调试信息**:
  - 🔍 [前端调试] 开始解析SSE响应
  - 🔍 [前端调试] 解析每一行数据
  - 🔍 [前端调试] 处理llm_token事件
  - 🔍 [前端调试] SQL结果格式检查
  - 🔍 [前端调试] JSON解析尝试

#### DataTable.vue
- **位置**: `parseData()` 方法
- **调试信息**:
  - 🔍 [DataTable调试] 开始解析数据
  - 🔍 [DataTable调试] 接收到的数据长度和内容
  - 🔍 [DataTable调试] 数据格式检查
  - 🔍 [DataTable调试] JSON解析结果
  - 🔍 [DataTable调试] Python字典列表解析结果

- **位置**: `parsePythonDictList()` 方法
- **调试信息**:
  - 🔍 [DataTable调试] 开始解析Python字典列表字符串
  - 🔍 [DataTable调试] 原始字符串和转换后的内容
  - 🔍 [DataTable调试] 解析成功状态

## 使用方法

### 1. 启动调试模式
1. 启动后端服务
2. 启动前端服务
3. 打开浏览器开发者工具

### 2. 执行数据查询
1. 选择数据库配置和表
2. 创建新对话
3. 输入自然语言查询
4. 观察控制台输出

### 3. 关键调试点

#### 后端日志检查
```bash
# 查看后端日志
tail -f logs/application.log | grep "🔍"
```

#### 前端控制台检查
1. 打开浏览器开发者工具 (F12)
2. 切换到 Console 标签
3. 过滤调试信息: `🔍`

### 4. 问题定位流程

#### 步骤1: 检查后端执行
- 查看 Python 执行器日志
- 确认 SQL 查询是否成功执行
- 检查执行结果格式

#### 步骤2: 检查数据传输
- 查看 SSE 消息发送日志
- 确认数据是否正确发送给前端

#### 步骤3: 检查前端接收
- 查看前端接收到的响应
- 确认数据格式是否正确
- 检查解析过程

#### 步骤4: 检查数据展示
- 查看 DataTable 组件解析日志
- 确认数据是否正确渲染

## 常见问题排查

### 1. 数据格式问题
- **现象**: 前端显示"数据解析失败"
- **排查**: 检查后端返回的数据格式是否符合前端期望
- **期望格式**: `{"dataType":"python_dict_list","parsedData":"[...]"}`

### 2. 数据截断问题
- **现象**: 数据被截断，显示不完整
- **排查**: 检查数据长度是否超过1MB限制
- **解决**: 调整截断阈值或优化数据格式

### 3. SSE连接问题
- **现象**: 前端收不到数据
- **排查**: 检查网络连接和后端SSE实现
- **解决**: 确认后端正确发送SSE事件

### 4. JSON解析问题
- **现象**: JSON解析失败
- **排查**: 检查数据中的特殊字符和格式
- **解决**: 确保数据是有效的JSON格式

## 调试命令

### 后端调试
```bash
# 查看实时日志
tail -f logs/application.log | grep "🔍"

# 查看错误日志
tail -f logs/application.log | grep "ERROR"

# 查看Python执行器日志
tail -f logs/application.log | grep "🐍"
```

### 前端调试
```javascript
// 在浏览器控制台中过滤调试信息
console.log = (function(old_function) {
    return function() {
        if (arguments[0] && arguments[0].includes('🔍')) {
            old_function.apply(this, arguments);
        }
    };
})(console.log);
```

## 注意事项

1. **日志级别**: 确保后端日志级别设置为 DEBUG 或 INFO
2. **浏览器缓存**: 清除浏览器缓存确保使用最新代码
3. **网络问题**: 检查前后端网络连接
4. **数据大小**: 注意大数据量可能导致的性能问题

## 联系支持

如果问题仍然存在，请提供以下信息：
1. 后端日志文件
2. 前端控制台输出
3. 网络请求详情
4. 复现步骤
