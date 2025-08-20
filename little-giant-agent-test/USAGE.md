# 小巨人代理测试模块使用说明

## 快速开始

### 1. 生成示例数据

在开始测试之前，首先生成示例数据：

```bash
# 进入测试模块目录
cd little-giant-agent-test

# 生成示例数据
mvn test -Dtest=LittleGiantAgentTestApplicationTests#generateSampleData
```

这将在项目根目录生成以下文件：
- `test-executor.xlsx` - 执行器测试数据
- `test-planner.xlsx` - 规划器测试数据

### 2. 执行测试

生成示例数据后，可以执行各种测试：

```bash
# 执行执行器测试
mvn test -Dtest=LittleGiantAgentTestApplicationTests#testExecutorCase1a

# 执行规划器测试
mvn test -Dtest=LittleGiantAgentTestApplicationTests#testPlannerCase1a

# 执行完整的测试流程
mvn test -Dtest=LittleGiantAgentTestApplicationTests#fullTestExample
```

### 3. 查看测试结果

测试完成后，会在项目根目录生成结果文件：
- `executor-result-case1a-{timestamp}.xlsx` - 执行器测试结果
- `planner-result-case1a-{timestamp}.xlsx` - 规划器测试结果

### 4. 分析测试结果

执行器测试支持结果分析：

```bash
# 统计执行器测试结果
mvn test -Dtest=LittleGiantAgentTestApplicationTests#statisticsExecutorResult
```

注意：需要在代码中指定要分析的结果文件名。

## 测试数据格式

### 执行器测试数据 (test-executor.xlsx)

| 列名 | 说明 |
|------|------|
| 问题编号 | 测试问题的唯一标识符 |
| 用户输入 | 用户的查询请求 |
| 历史输入 | 历史对话内容 |
| Python代码 | 预期的Python代码示例 |

### 规划器测试数据 (test-planner.xlsx)

| 列名 | 说明 |
|------|------|
| 问题编号 | 测试问题的唯一标识符（支持轮次格式如1-1, 1-2） |
| 用户输入 | 用户的查询请求 |
| 历史输入 | 历史对话内容 |
| 上一轮规划 | 上一轮的规划结果 |
| 针对上一轮的问题回复内容 | 针对上一轮问题的回复 |

## 环境要求

1. **Java 17+**
2. **Maven 3.6+**
3. **Python 3.x**（用于Python代码执行）
4. **数据库连接**（用于SQL执行）
5. **Coze API配置**（用于规划器测试）

## 配置说明

### 数据库配置

在 `application-dev.yml` 中配置数据库连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_database
    username: your_username
    password: your_password
```

### Coze工作流配置

在测试类中修改工作流ID：

```java
private static final String COZE_WORKFLOW_ID = "your_workflow_id";
```

## 常见问题

### 1. 编译失败

如果遇到编译错误，请确保：
1. 已经安装了所有依赖模块
2. 使用正确的Java版本
3. Maven配置正确

### 2. 测试执行失败

如果测试执行失败，请检查：
1. 数据库连接是否正常
2. Python环境是否可用
3. Coze API配置是否正确

### 3. 结果文件未生成

如果没有生成结果文件，请检查：
1. 文件写入权限
2. 磁盘空间是否充足
3. 检查日志中的错误信息

## 自定义测试

### 添加新的测试配置

1. 在 `executeExecutorTest` 方法中添加新的case分支
2. 实现对应的测试方法
3. 更新文档说明

### 扩展数据模型

1. 修改测试数据模型类
2. 更新Excel工具类
3. 调整测试方法逻辑

## 性能调优

### 多线程配置

在 `ThreadPoolUtil` 中调整线程池参数：

```java
private static final int CORE_POOL_SIZE = 5;
private static final int MAX_POOL_SIZE = 10;
```

### 超时设置

在 `PythonExecutor` 中调整超时时间：

```java
private static final int TIMEOUT_SECONDS = 30;
```

## 监控和日志

测试过程中的详细日志可以通过以下方式查看：

1. 控制台输出
2. 日志文件（如果配置了日志文件）
3. 测试结果Excel文件中的错误信息

## 联系支持

如果遇到问题或需要帮助，请查看：
1. 项目README.md文件
2. 代码中的详细注释
3. 联系开发团队 