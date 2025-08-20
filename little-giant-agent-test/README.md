# Little Giant Agent Test Module

## 项目概述

`little-giant-agent-test` 是MT Little Giant智能代理系统的专用测试模块，提供全面的测试功能，包括执行器测试、规划器测试和智能结果分析。该模块采用现代化的Spring Boot架构，支持多线程并发测试、批量数据处理和智能错误分析。

## 核心功能

### 1. 执行器测试 (ExecutorTest)
- 支持case1a到case1e多种测试配置
- 支持数据集重复测试（roundNumber参数指定重复次数）
- 自动解析Python代码中的gen_sql参数
- 多线程并发执行测试用例
- SQL生成和执行验证

### 2. 规划器测试 (PlannerTest)
- 支持多种case配置（case1a-case1e, case2a, case3a），每个case对应不同的Coze工作流ID
- 支持数据集重复测试（roundNumber参数指定重复次数）
- Coze工作流集成测试
- 用户隔离确保测试独立性
- Python代码执行和验证
- 多轮对话测试支持

### 3. 测试结果分析 (TestResultAnalysisService)
- 智能错误分类和统计
- 字段匹配算法分析
- 数据库错误检测
- 统计报告生成

## TestResultAnalysisService 详细处理逻辑

### 核心方法：statisticsExecutorResult

#### 1. 数据读取阶段
```java
List<ExecutorTestResult> results = excelUtil.readExecutorTestResults(resultFileName);
```
- 从指定Excel文件读取测试结果
- 每条记录包含：问题ID、轮数、用户输入、历史输入、Python代码、SQL语句、执行结果等
- 过滤空行和无效数据

#### 2. 结果分析阶段
对每个测试结果执行智能分析：

```java
for (ExecutorTestResult result : results) {
    ExecutorTestResult analyzed = analyzeExecutorResult(result);
    analyzedResults.add(analyzed);
}
```

##### 分析流程：
1. **成功检查**：如果`sqlExecuteSuccess = true`，标记为"SQL成功执行"
2. **错误分类**：如果执行失败，进入错误类型分析

#### 3. 错误类型分析算法

##### 3.1 字段匹配检查 - `isFieldMismatchError`

**算法逻辑**：
```java
// 1. 从Python代码提取变量名
Set<String> pythonVariables = extractPythonVariables(pythonCode);

// 2. 从SQL语句提取字段名  
Set<String> sqlFields = extractSqlFields(sqlStatement);

// 3. 检查匹配性
for (String variable : pythonVariables) {
    if (!sqlFields.contains(variable)) {
        return true; // 发现不匹配字段
    }
}
```

**Python变量提取**：
- 使用正则表达式：`data\\['([^']+)'\\]`
- 匹配模式：`data['field_name']`
- 提取示例：
  ```python
  # 从这段代码中：
  print(f"2021年销售额: {data['sales_2021']}")
  print(f"2022年销售额: {data['sales_2022']}")
  
  # 提取得到：['sales_2021', 'sales_2022']
  ```

**SQL字段提取**：
- 使用正则表达式：`SELECT\\s+(.+?)\\s+FROM`
- 解析SELECT到FROM之间的字段列表
- 处理别名和反引号
- 提取示例：
  ```sql
  -- 从这个SQL中：
  SELECT `year`, `total_sales` AS sales, `region` FROM sales_data
  
  -- 提取得到：['year', 'total_sales', 'region']
  ```

##### 3.2 数据库字段检查 - `isDatabaseFieldNotExistError`

**检查关键词**：
```java
String[] fieldNotExistKeywords = {
    "Unknown column",      // MySQL错误
    "不存在的列",          // 中文错误信息  
    "Column not found",    // 通用错误
    "Invalid column name", // SQL Server错误
    "字段不存在"           // 自定义错误
};
```

**匹配逻辑**：
- 遍历错误日志，检查是否包含上述关键词
- 支持中英文错误信息识别
- 优先级低于字段匹配检查

##### 3.3 错误分类优先级

```
1. SQL成功执行 (最高优先级)
   └─ sqlExecuteSuccess = true

2. SQL无法执行 - SQL查询字段与Python代码实际使用不匹配
   └─ isFieldMismatchError() = true

3. SQL无法执行 - SQL查询字段在数据库不存在  
   └─ isDatabaseFieldNotExistError() = true

4. SQL无法执行 - 其他（需人工排查）(最低优先级)
   └─ 其他未知错误
```

#### 4. 统计报告生成

##### 4.1 分类统计
```java
Map<String, Integer> categoryCount = new HashMap<>();           // 每种错误类型的数量
Map<String, List<String>> categoryQuestions = new HashMap<>();  // 每种错误类型关联的问题
```

##### 4.2 Excel报告结构
| 列名 | 说明 | 示例 |
|------|------|------|
| 结果分类类型 | 错误类型描述 | "SQL无法执行 - 字段不匹配" |
| 次数 | 该类型错误出现次数 | 15 |
| 占比 | 占总测试数量百分比 | "25.67%" |
| 分类关联的问题编号以及测试轮数 | 具体问题标识 | "Q001-1, Q003-2, Q005-1" |

##### 4.3 文件命名规则

**执行器测试文件**：
```
测试结果文件：executor-result-{testConfig}-rounds{roundNumber}-{timestamp}.xlsx
统计分析文件：executor-statistics-{testConfig}-rounds{roundNumber}-{timestamp}-{analysisTimestamp}.xlsx

示例：
原文件：executor-result-case1a-rounds2-20240115_143022.xlsx
统计文件：executor-statistics-case1a-rounds2-20240115_143022-20240115_152030.xlsx
```

**规划器测试文件**：
```
测试结果文件：planner-result-{testConfig}-rounds{roundNumber}-{timestamp}.xlsx

示例：
原文件：planner-result-case1a-rounds1-20240115_143022.xlsx
多轮测试：planner-result-case3a-rounds3-20240115_143022.xlsx
```

### 使用示例

#### 执行器测试基本调用
```java
@Autowired
private ExecutorTestService executorTestService;

// 执行case1a测试，数据集重复测试1次
executorTestService.executeTest(1, "case1a");

// 执行case1b测试，数据集重复测试3次
executorTestService.executeTest(3, "case1b");
```

**roundNumber参数说明**：
- `roundNumber`表示对整个数据集重复测试的次数
- 如果`roundNumber = 2`，表示数据集将被完整测试两遍
- 每轮测试都会为数据集中的每个测试用例分配正确的轮次号
- 最终结果文件会包含所有轮次的测试结果，按问题ID和轮次排序

#### 规划器测试基本调用
```java
@Autowired
private PlannerTestService plannerTestService;

// 执行case1a测试，数据集重复测试1次
plannerTestService.executeTest(1, "case1a", "test-planner.xlsx");

// 执行case3a测试，数据集重复测试2次
plannerTestService.executeTest(2, "case3a", "test-planner.xlsx");

// 使用便捷方法执行case1b测试
plannerTestService.executeCase1bTest(1, "test-planner.xlsx");
```

**Case配置说明**：
- 每个case对应不同的Coze工作流ID
- 支持的case：`case1a`, `case1b`, `case1c`, `case1d`, `case1e`, `case2a`, `case3a`
- 默认情况下，未知配置会使用case1a的工作流ID
- 文件命名格式：`planner-result-{testConfig}-rounds{roundNumber}-{timestamp}.xlsx`

**工作流ID配置**：
```java
// 在PlannerTestService中配置各case对应的工作流ID
private static final String CASE1A_COZE_WORKFLOW_ID = "7518982183785283594";
private static final String CASE1B_COZE_WORKFLOW_ID = "7518982183785283595"; // 需要根据实际情况配置
// ... 其他case的工作流ID
```

#### 结果分析服务调用
```java
@Autowired
private TestResultAnalysisService analysisService;

// 分析测试结果（注意：文件名需要根据实际生成的文件名调整）
analysisService.statisticsExecutorResult("executor-result-case1a-rounds2-20240115_143022.xlsx");
```

#### 典型的错误分析案例

**案例1：字段不匹配错误**
```
Python代码：print(f"销售额: {data['sales_amount']}")
SQL语句：  SELECT total_sales, region FROM sales_data  
结果：     SQL无法执行 - SQL查询字段与Python代码实际使用不匹配
原因：     Python要求'sales_amount'字段，但SQL只查询了'total_sales'
```

**案例2：数据库字段不存在**
```
SQL语句：  SELECT non_exist_field FROM table_name
错误日志：  Unknown column 'non_exist_field' in 'field list'
结果：     SQL无法执行 - SQL查询字段在数据库不存在
```

**案例3：成功执行**
```
Python代码：print(f"年份: {data['year']}, 销售额: {data['sales']}")
SQL语句：  SELECT year, sales FROM sales_data
执行结果：  成功返回数据
结果：     SQL成功执行
```

### 输出报告示例

执行`statisticsExecutorResult`后生成的Excel报告示例：

| 结果分类类型 | 次数 | 占比 | 分类关联的问题编号以及测试轮数 |
|-------------|------|------|----------------------------|
| SQL成功执行 | 45 | 60.00% | Q001-1, Q002-1, Q003-1, ... |
| SQL无法执行 - SQL查询字段与Python代码实际使用不匹配 | 20 | 26.67% | Q004-1, Q007-2, Q010-1, ... |
| SQL无法执行 - SQL查询字段在数据库不存在 | 8 | 10.67% | Q015-1, Q018-1, Q020-2, ... |
| SQL无法执行 - 其他（需人工排查） | 2 | 2.67% | Q025-1, Q030-2 |

### 技术特性

#### 1. 智能字段匹配算法
- **高精度**：使用正则表达式精确提取Python变量和SQL字段
- **容错性**：处理SQL别名、反引号、空格等复杂情况
- **扩展性**：支持多种Python代码模式和SQL语法

#### 2. 多语言错误检测
- **中英文支持**：同时识别中文和英文数据库错误信息
- **数据库兼容**：支持MySQL、SQL Server等主流数据库错误格式
- **关键词匹配**：基于关键词库的智能错误分类

#### 3. 详细统计报告
- **分类统计**：按错误类型自动分类统计
- **百分比分析**：提供占比信息便于识别主要问题
- **问题追踪**：精确定位每个错误对应的具体测试案例

#### 4. 文件管理
- **时间戳命名**：自动生成带时间戳的文件名避免覆盖
- **Excel格式**：使用标准Excel格式便于查看和进一步分析
- **自动调整**：自动调整列宽优化显示效果

### 配置说明

#### Excel文件格式要求

**输入文件（测试结果）必须包含以下列**：
- questionId：问题ID
- roundNumber：测试轮数  
- userInput：用户输入
- historyInput：历史输入
- pythonCode：Python代码
- sqlStatement：SQL语句
- sqlGenerateTime：SQL生成时间
- sqlExecuteSuccess：SQL执行是否成功
- sqlExecuteResult：SQL执行结果
- sqlErrorLog：SQL错误日志

#### 日志配置
系统使用SLF4J进行日志记录，支持以下级别：
- INFO：正常处理流程
- ERROR：异常情况和错误
- DEBUG：详细调试信息

### 最佳实践

#### 1. 测试数据准备
- 确保Excel文件格式正确，包含所有必需字段
- Python代码应使用标准的`data['field_name']`格式访问字段
- SQL语句应格式规范，避免复杂嵌套

#### 2. 结果分析
- 优先解决"字段不匹配"问题，这通常是代码生成逻辑的问题
- "数据库字段不存在"问题需要检查数据源配置
- "其他错误"需要人工查看具体错误日志

#### 3. 性能优化
- 大量数据时建议分批处理
- 定期清理临时文件和日志
- 监控内存使用情况

### 扩展开发

#### 添加新的错误类型检测
```java
private String analyzeErrorType(ExecutorTestResult result) {
    // 添加新的检查逻辑
    if (isYourCustomError(result)) {
        return "自定义错误类型";
    }
    
    // 现有逻辑...
}
```

#### 自定义字段提取规则
```java
private Set<String> extractPythonVariables(String pythonCode) {
    Set<String> variables = new HashSet<>();
    
    // 添加新的模式匹配
    Pattern customPattern = Pattern.compile("your_pattern_here");
    // ...
    
    return variables;
}
```

## 相关文档

- [执行器测试指南](docs/executor-test-guide.md)
- [规划器测试指南](docs/planner-test-guide.md)  
- [API文档](docs/api-documentation.md)

## 版本信息

- 当前版本：v1.0.0
- 最后更新：2025-01-15
- 维护团队：MT Agent Team
