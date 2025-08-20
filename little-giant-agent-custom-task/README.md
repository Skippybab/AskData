# Little Giant Agent Custom Task 模块

## 概述

这个模块提供了两种任务执行方式：
1. **Java指令执行**：将Python代码解析为Java指令执行（原有方式）
2. **Python直接执行**：通过外部Python进程直接执行Python代码（新增功能）

## Python直接执行功能

### 🎯 核心优势

- **完全的Python语法支持**：支持循环、条件判断、异常处理等所有Python特性
- **流式输出**：通过`report()`函数实现实时进度反馈
- **无缝Java集成**：Python函数调用自动转换为Java方法调用
- **简化架构**：不需要复杂的指令解析和转换
- **大模型友好**：大模型可以生成完整、自然的Python代码

### 🏗️ 架构设计

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Java 调用端    │───→│ PythonDirectExecutor │───→│   Python 进程    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │                         │
                                ▼                         ▼
                       ┌──────────────────┐    ┌─────────────────┐
                       │   流式报告器      │    │   系统函数模块    │
                       └──────────────────┘    └─────────────────┘
```

### 📝 支持的Python函数

#### SQL生成函数
```python
def sql_single_industry(query_text: str) -> str:
    '''查询单个行业的单条数据，输入数据库查询相关步骤和条件，返回可执行的SQL查询语句'''

def sql_single_company(query_text: str) -> str:
    '''查询单个企业的单条数据，输入数据库查询相关步骤和条件，返回可执行的SQL查询语句'''

def sql_gen_field_range(field_name: str) -> str:
    '''输入单个字段名称，返回可执行的SQL查询语句'''
```

#### 数据分析函数
```python
def forecast_data(historical_data: List[float], forecast_steps: int) -> List[float]:
    '''输入指标的历史数据序列和预测未来时间点个数，输出预测结果序列'''

def sql_exc(sql_code: str) -> List[Dict[str, Any]]:
    '''输入可执行的SQL代码，返回SQL查询结果'''

def cp_dual_industries(first_ind: str, second_ind: str, query_text: str) -> List[Dict[str, Any]]:
    '''输入对比行业1、2的名称，数据库查询的相关条件、字段限制，数据顺序保持和入参一致'''

def cp_dual_companies(first_ent: str, second_ent: str, query_text: str) -> List[Dict[str, Any]]:
    '''输入对比企业1、2的名称，数据库查询的相关条件、字段限制，数据顺序保持和入参一致'''
```

#### 系统功能函数
```python
def explain_sys_func(question: str) -> str:
    '''输入用户关于系统功能的提问，返回问题的回答文本'''

def steps_summary(step_status: List[str], summary_title: str) -> str:
    '''输入步骤状态列表和标题，返回步骤执行情况的总结文本'''
```

#### 可视化函数
```python
def vis_textbox(content: str) -> None:
    '''输入文本内容，在前端对话界面渲染1个文本框'''

def vis_textblock(title: str, value: float) -> None:
    '''输入标题和数值，在前端对话界面渲染1个指标信息块'''

def vis_single_bar(title: str, x_labels: List[str], y_data: List[float]) -> None:
    '''输入标题、X轴标签列表和Y轴数据列表，在前端对话界面渲染1个单柱状图'''

def vis_clustered_bar(title: str, x_labels: List[str], bar_a_label: str, bar_b_label: str, 
                     group_a: List[float], group_b: List[float]) -> None:
    '''输入标题、X轴标签列表，a、b两组数据的标签和数据，在前端对话界面渲染1个二分组柱状图'''

def vis_pie_chart(title: str, labels: List[str], data: List[float]) -> None:
    '''输入标题、标签列表和数据列表，在前端对话界面渲染1个饼状图'''

def vis_table(title: str, data: List[Dict[str, Any]]) -> None:
    '''输入表格标题和表格数据，在前端对话界面渲染1个二维表格'''
```

#### 流式输出函数
```python
def report(message: str) -> None:
    '''流式输出报告函数，供大模型在Python代码中调用'''
```

### 💡 使用示例

#### 基本使用
```python
# 步骤1：查询行业数据
report("正在查询制造业数据...")
sql = sql_single_industry("查询制造业近三年营收数据")
manufacturing_data = sql_exc(sql)

# 步骤2：数据处理
report("处理数据并生成图表...")
years = [item['年份'] for item in manufacturing_data]
revenues = [item['营收'] for item in manufacturing_data]

# 步骤3：可视化展示
vis_single_bar("制造业营收趋势", years, revenues)

# 步骤4：预测分析
report("基于历史数据进行预测...")
forecast_result = forecast_data(revenues, 2)
vis_textblock("预测2024年营收", forecast_result[0])
vis_textblock("预测2025年营收", forecast_result[1])

report("分析完成！")
```

#### 复杂业务逻辑
```python
# 复杂条件判断和循环
industries = ["制造业", "服务业", "科技业"]
results = []

for industry in industries:
    report(f"正在分析{industry}...")
    
    # 查询数据
    sql = sql_single_industry(f"查询{industry}近年增长率")
    data = sql_exc(sql)
    
    if len(data) > 0:
        growth_rate = data[-1]['增长率']
        results.append({
            "行业": industry,
            "增长率": growth_rate,
            "状态": "高增长" if growth_rate > 10 else "稳定增长"
        })
    else:
        report(f"警告：{industry}数据不足")

# 生成对比分析
if len(results) >= 2:
    vis_table("行业增长率对比", results)
    
    # 找出增长最快的行业
    best_industry = max(results, key=lambda x: x['增长率'])
    vis_textblock(f"增长最快行业", best_industry['增长率'])
```

### 🔧 技术实现

#### 核心组件

1. **PythonDirectExecutorService**：Python执行器服务
2. **Java Bridge**：Python与Java通信桥梁
3. **System Functions**：系统函数模块
4. **CustomTaskService**：统一任务执行接口

#### 通信机制

- Python进程通过标准输入输出与Java通信
- 使用JSON格式传递函数调用请求和响应
- 支持同步调用和异步流式输出

#### 安全特性

- 临时文件自动清理
- 进程超时控制（300秒）
- 异常处理和错误恢复
- 资源泄露防护

### 🚀 快速开始

#### 1. 添加依赖
确保项目中包含Jackson依赖用于JSON处理。

#### 2. 注入服务
```java
@Autowired
private CustomTaskService customTaskService;
```

#### 3. 调用执行
```java
String pythonCode = """
    report("开始执行任务")
    # 你的Python代码
    report("任务完成")
    """;

HashMap<String, Object> paramMap = new HashMap<>();
customTaskService.executePythonCode(pythonCode, paramMap, reporter);
```

### ⚠️ 注意事项

1. **Python环境**：确保系统已安装Python并可通过命令行访问
2. **权限要求**：需要有创建临时文件和启动进程的权限
3. **性能考虑**：每次执行都会创建新的Python进程，适合中小型任务
4. **错误处理**：Python代码错误会被捕获并通过流式输出报告

### 🔄 迁移指南

从Java指令方式迁移到Python直接执行：

1. **替换调用方式**
   ```java
   // 原方式
   customTaskService.executeJavaOrders(javaExecutable, paramMap, reporter);
   
   // 新方式
   customTaskService.executePythonCode(pythonCode, paramMap, reporter);
   ```

2. **更新代码生成**
   - 大模型直接生成完整Python代码
   - 使用`report()`函数添加流式输出
   - 利用完整Python语法特性

3. **测试验证**
   - 运行测试用例验证功能
   - 检查流式输出是否正常
   - 确认可视化组件渲染正确

这个新的Python直接执行功能为系统提供了更强的灵活性和扩展性，让大模型能够充分发挥Python的编程优势。 