# 共识模块 (Consensus Module)

## 概述

本模块基于Redis和JSON格式实现灵活的共识数据缓存管理，支持动态的数据结构变更，避免了传统实体类的修改复杂性。

## 数据结构

共识数据采用以下JSON结构：

```json
{
    "status": "UNKNOWN",
    "taskName": {
        "name": "",
        "status": "UNKNOWN"
    },
    "taskOutput": {
        "output": [
            {
                "content": "",
                "status": "UNKNOWN"
            }
        ],
        "status": "UNKNOWN"
    },
    "taskSteps": {
        "steps": [
            {
                "content": "",
                "status": "UNKNOWN"
            }
        ],
        "status": "UNKNOWN"
    },
    "taskInputs": {
        "inputs": [
            {
                "content": "",
                "status": "UNKNOWN"
            }
        ],
        "status": "UNKNOWN"
    }
}
```

### 状态说明

- `UNKNOWN`: 未知状态（仅在初始化时使用）
- `KNOWN`: 已知状态（已知但未确认）
- `CONFIRMED`: 已确认状态

### 状态判断规则

1. **子项状态**: 每个具体的子项（output、steps、inputs数组中的元素）都有自己的status
2. **父级状态**: 
   - 只有当下一级的所有子项status都为CONFIRMED时，父级的status才为CONFIRMED
   - 只有当下一级的所有子项status都为UNKNOWN时，父级的status才为UNKNOWN
   - 其他情况下，父级的status为KNOWN
3. **整体状态**: 
   - 只有当所有顶级组件（taskName、taskOutput、taskSteps、taskInputs）的status都为CONFIRMED时，整体status才为CONFIRMED
   - 只有当所有顶级组件的status都为UNKNOWN时，整体status才为UNKNOWN
   - 其他情况下，整体status为KNOWN

例如：
- taskOutput.status 只有当 taskOutput.output 数组中所有元素的status都为CONFIRMED时才为CONFIRMED
- taskOutput.status 只有当 taskOutput.output 数组中所有元素的status都为UNKNOWN时才为UNKNOWN
- 其他情况下 taskOutput.status 为KNOWN
- 整体consensus.status 同样遵循此规则

## 核心工具类

### ConsensusUtil

提供以下核心方法：

#### 1. 获取共识JSON数据

```java
/**
 * 通过key获取Redis缓存中的JSON数据，并转换为JSONObject对象
 * @param key Redis缓存键
 * @return JSONObject对象，如果缓存不存在则返回null
 */
public JSONObject getConsensusJsonByKey(String key)
```

#### 2. 删除共识JSON数据

```java
/**
 * 通过key删除Redis缓存中的数据
 * @param key Redis缓存键
 * @return true表示删除成功，false表示删除失败或缓存不存在
 */
public boolean deleteConsensusJsonByKey(String key)
```

#### 3. 保存共识JSON数据

```java
/**
 * 保存JSON对象到Redis缓存
 * @param key Redis缓存键
 * @param jsonObject JSON对象
 * @param expireTime 过期时间
 * @param timeUnit 时间单位
 * @return true表示保存成功，false表示保存失败
 */
public boolean saveConsensusJson(String key, JSONObject jsonObject, long expireTime, TimeUnit timeUnit)

/**
 * 保存JSON对象到Redis缓存（使用默认过期时间30分钟）
 * @param key Redis缓存键
 * @param jsonObject JSON对象
 * @return true表示保存成功，false表示保存失败
 */
public boolean saveConsensusJson(String key, JSONObject jsonObject)
```

#### 4. 获取用户共识缓存键

```java
/**
 * 获取用户共识缓存键（保持与原有逻辑兼容）
 * @param userId 用户ID
 * @return 缓存键
 */
public String getUserConsensusKey(String userId)
```

#### 5. 状态计算方法

```java
/**
 * 计算并更新共识数据的层级状态
 * 根据子项状态自动计算父级状态
 * @param consensus 共识JSON对象
 * @return 更新后的共识JSON对象
 */
public JSONObject calculateAndUpdateStatus(JSONObject consensus)

/**
 * 获取共识整体状态（智能计算版本）
 * 会根据子项状态自动计算父级状态
 * @param userId 用户ID
 * @return 整体状态字符串：UNKNOWN、KNOWN 或 CONFIRMED
 */
public String getOverallConsensusStatus(String userId)
```

#### 6. 便捷删除方法

```java
/**
 * 通过用户ID删除共识缓存
 * @param userId 用户ID
 * @return true表示删除成功，false表示删除失败或缓存不存在
 */
public boolean deleteConsensusJsonByUserId(String userId)
```

## 使用示例

### 1. 基本使用

```java
@Autowired
private ConsensusUtil consensusUtil;

// 获取用户的共识缓存键
String cacheKey = consensusUtil.getUserConsensusKey("user123");

// 创建共识JSON数据
JSONObject consensus = new JSONObject();
consensus.put("status", "UNKNOWN");

JSONObject taskName = new JSONObject();
taskName.put("name", "数据分析任务");
taskName.put("status", "KNOWN");
consensus.put("taskName", taskName);

// 保存到缓存
boolean saveResult = consensusUtil.saveConsensusJson(cacheKey, consensus);
if (saveResult) {
    System.out.println("共识数据保存成功");
}

// 从缓存获取数据
JSONObject cachedConsensus = consensusUtil.getConsensusJsonByKey(cacheKey);
if (cachedConsensus != null) {
    String taskNameValue = cachedConsensus.getJSONObject("taskName").getStr("name");
    System.out.println("任务名称: " + taskNameValue);
}

// 删除缓存
boolean deleteResult = consensusUtil.deleteConsensusJsonByKey(cacheKey);
if (deleteResult) {
    System.out.println("共识数据删除成功");
}
```

### 2. 复杂数据结构示例

```java
// 创建完整的共识数据结构
JSONObject consensus = new JSONObject();
consensus.put("status", "UNKNOWN"); // 初始状态

// 任务名称
JSONObject taskName = new JSONObject();
taskName.put("name", "销售数据统计分析");
taskName.put("status", "CONFIRMED"); // 用户已确认
consensus.put("taskName", taskName);

// 任务输出
JSONObject taskOutput = new JSONObject();
JSONArray outputArray = new JSONArray();
JSONObject output1 = new JSONObject();
output1.put("content", "销售报表：包含月度销售额、销量趋势图表");
output1.put("status", "CONFIRMED"); // 已确认
outputArray.add(output1);
JSONObject output2 = new JSONObject();
output2.put("content", "数据透视表：按产品类别分析");
output2.put("status", "KNOWN"); // 已知但未确认
outputArray.add(output2);
taskOutput.put("output", outputArray);
taskOutput.put("status", "UNKNOWN"); // 将通过calculateAndUpdateStatus自动计算为KNOWN
consensus.put("taskOutput", taskOutput);

// 任务步骤
JSONObject taskSteps = new JSONObject();
JSONArray stepsArray = new JSONArray();
JSONObject step1 = new JSONObject();
step1.put("content", "从数据库查询销售数据");
step1.put("status", "CONFIRMED");
stepsArray.add(step1);
JSONObject step2 = new JSONObject();
step2.put("content", "数据清洗和处理");
step2.put("status", "CONFIRMED");
stepsArray.add(step2);
taskSteps.put("steps", stepsArray);
taskSteps.put("status", "UNKNOWN"); // 将通过calculateAndUpdateStatus自动计算为CONFIRMED
consensus.put("taskSteps", taskSteps);

// 任务输入
JSONObject taskInputs = new JSONObject();
JSONArray inputsArray = new JSONArray();
JSONObject input1 = new JSONObject();
input1.put("content", "时间范围：2024年1月-12月");
input1.put("status", "CONFIRMED");
inputsArray.add(input1);
taskInputs.put("inputs", inputsArray);
taskInputs.put("status", "UNKNOWN"); // 将通过calculateAndUpdateStatus自动计算为CONFIRMED
consensus.put("taskInputs", taskInputs);

// 保存数据前，自动计算状态
String cacheKey = consensusUtil.getUserConsensusKey("user456");
consensus = consensusUtil.calculateAndUpdateStatus(consensus);
consensusUtil.saveConsensusJson(cacheKey, consensus);

// 获取整体状态
String overallStatus = consensusUtil.getOverallConsensusStatus("user456");
System.out.println("整体状态: " + overallStatus); // 输出: KNOWN (因为output2状态为KNOWN)
```

### 3. 状态计算示例

```java
// 演示状态自动计算的逻辑
String userId = "user789";
String cacheKey = consensusUtil.getUserConsensusKey(userId);

// 创建测试数据
JSONObject consensus = new JSONObject();
consensus.put("status", "UNKNOWN");

// 所有子项都为UNKNOWN时
JSONObject taskName = new JSONObject();
taskName.put("name", "");
taskName.put("status", "UNKNOWN");
consensus.put("taskName", taskName);

// 计算状态
consensus = consensusUtil.calculateAndUpdateStatus(consensus);
System.out.println("所有子项UNKNOWN时整体状态: " + consensus.getStr("status")); // 输出: UNKNOWN

// 部分子项为KNOWN时
taskName.put("status", "KNOWN");
consensus = consensusUtil.calculateAndUpdateStatus(consensus);
System.out.println("部分子项KNOWN时整体状态: " + consensus.getStr("status")); // 输出: KNOWN

// 所有子项都为CONFIRMED时
taskName.put("status", "CONFIRMED");
// 假设其他组件也都设置为CONFIRMED...
consensus = consensusUtil.calculateAndUpdateStatus(consensus);
System.out.println("所有子项CONFIRMED时整体状态: " + consensus.getStr("status")); // 输出: CONFIRMED
```

### 4. 删除用户共识缓存

```java
// 删除指定用户的共识缓存
boolean deleted = consensusUtil.deleteConsensusJsonByUserId("user123");
if (deleted) {
    System.out.println("用户共识缓存删除成功");
}
```

## 依赖说明

- **Redisson**: Redis客户端，用于缓存操作
- **Hutool**: 工具包，用于JSON处理
- **Spring Boot**: 依赖注入和组件管理

## 注意事项

1. **异常处理**: 所有方法都包含完善的异常处理，确保系统稳定性
2. **日志记录**: 详细的日志记录，便于问题排查和监控
3. **缓存过期**: 默认缓存过期时间为30分钟，可根据需要调整
4. **JSON格式**: 使用Hutool的JSONObject，支持灵活的数据结构操作
5. **键命名规范**: 使用统一的键命名前缀 "coze:consensus:"

## 配置要求

确保在application.yml中配置了Redis连接信息：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 2000ms
```

## 版本更新日志

### v2.1.0
- 完善状态管理，支持三种状态：UNKNOWN、KNOWN、CONFIRMED
- 实现层级状态自动计算逻辑
- 添加deleteConsensusJsonByUserId便捷删除方法
- 修改默认缓存过期时间为永久存储
- 优化状态判断规则，支持更精细的状态控制

### v2.0.0
- 重构ConsensusUtil，简化为基于JSON的灵活结构
- 删除所有实体类，提高数据结构变更的灵活性
- 使用Hutool工具包进行JSON操作
- 完善异常处理和日志记录 