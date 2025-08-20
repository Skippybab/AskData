# 聊天历史 API 文档

本文档描述了用于管理用户聊天历史的 API 接口。这些接口允许客户端保存和检索用户与 AI 的对话历史记录。

## 通用说明

### 基础URL

所有 API 都使用以下基础 URL:

```
/api
```

### 响应格式

所有 API 都返回 JSON 格式的响应，具有以下通用结构：

```json
{
  "code": 200,       // 状态码：200表示成功，其他表示失败
  "message": "操作成功", // 操作结果描述
  "data": {}         // 响应数据，根据不同接口返回不同内容
}
```

### 认证要求

所有 API 都需要用户登录后才能访问。系统使用 Session 存储用户登录状态。

## API 列表

### 1. 保存聊天历史

保存用户的聊天历史记录，只保留最新的6条对话记录。

#### 请求

```
POST /api/chat/history
```

#### 请求体

```json
{
  "chatHistory": [
    {
      "type": "user",
      "content": "我想查询小巨人企业经营信息相关信息"
    },
    {
      "type": "ai",
      "content": "",
      "rspType": 4,
      "template": "",
      "taskResults": [],
      "recommendedQuestions": []
    },
    // 最多可以包含更多聊天记录，系统会自动只保留最新的6条
  ]
}
```

#### 参数说明

| 字段 | 类型 | 必填 | 描述 |
| ---- | ---- | ---- | ---- |
| chatHistory | Array | 是 | 聊天历史记录列表 |
| chatHistory[].type | String | 是 | 消息类型，"user"表示用户消息，"ai"表示AI回复 |
| chatHistory[].content | String | 是 | 消息内容 |
| chatHistory[].rspType | Integer | 否 | 响应类型，仅AI消息有效 |
| chatHistory[].template | String | 否 | 模板信息，仅AI消息有效 |
| chatHistory[].taskResults | Array | 否 | 任务结果列表，仅AI消息有效 |
| chatHistory[].recommendedQuestions | Array | 否 | 推荐问题列表，仅AI消息有效 |

##### taskResults 参数说明

`taskResults`支持灵活的动态JSON结构，系统默认实现了几种常用类型，但也支持完全自定义的结构。每个任务结果对象都包含一个`type`字段用于标识类型，其他字段可以根据需要动态添加。

以下是一些常见的示例：

1. **IndicatorBlock 类型** - 单一的数值指标

```json
{
  "type": "IndicatorBlock", // 类型标识
  "unit": "万",           // 动态字段
  "label": "营收规模",     // 动态字段
  "value": 331581.06      // 动态字段
}
```

2. **BarChart 类型** - 柱状图数据

```json
{
  "type": "BarChart",   // 类型标识
  "unit": "万",         // 动态字段
  "title": "广州软件和信息技术服务营收趋势", // 动态字段
  "value": [222499.49, 245377.94, 331581.06], // 动态字段，数组值
  "tags": ["2021年", "2022年", "2023年"]      // 动态字段，数组值
}
```

3. **TextList 类型** - 文本列表

```json
{
  "type": "TextList",   // 类型标识
  "value": [            // 动态字段，数组值
    "65 软件和信息技术服务业",
    "34 通用设备制造业",
    // 更多文本项...
  ]
}
```

4. **自定义类型** - 完全自定义的JSON结构

```json
{
  "type": "CustomType",      // 自定义类型标识
  "customField1": "自定义值1", // 任意自定义字段
  "customField2": 123,       // 任意自定义字段
  "nestedData": {            // 支持嵌套对象
    "nestedKey1": "nestedValue1",
    "nestedKey2": 456
  },
  "arrayData": [             // 支持任意嵌套数组
    {"item": 1, "value": "值1"},
    {"item": 2, "value": "值2"}
  ]
}
```

##### recommendedQuestions 参数说明

```json
{
  "questionId": "1-2",  // 问题ID
  "questionDesc": "2023年广州软件和信息技术服务的营收趋势情况" // 问题描述
}
```

#### 响应

##### 成功响应

```json
{
  "code": 200,
  "message": "聊天历史保存成功",
  "data": null
}
```

##### 失败响应

```json
{
  "code": 401,
  "message": "用户未登录",
  "data": null
}
```

```json
{
  "code": 500,
  "message": "系统繁忙，请稍后再试",
  "data": null
}
```

### 2. 获取聊天历史

获取用户的聊天历史记录。

#### 请求

```
GET /api/chat/history
```

#### 响应

##### 成功响应

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "type": "user",
      "content": "我想查询小巨人企业经营信息相关信息"
    },
    {
      "type": "ai",
      "content": "",
      "rspType": 4,
      "template": "",
      "taskResults": [],
      "recommendedQuestions": []
    },
    // 更多聊天记录...
  ]
}
```

##### 失败响应

```json
{
  "code": 401,
  "message": "用户未登录",
  "data": null
}
```

```json
{
  "code": 500,
  "message": "系统繁忙，请稍后再试",
  "data": null
}
```

### 3. 测试灵活任务结果 (开发环境)

用于测试灵活动态任务结果结构的演示接口。

#### 请求

```
GET /api/chat/test-flexible-result
```

#### 响应

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "type": "ai",
    "content": "",
    "rspType": 1,
    "template": "",
    "taskResults": [
      {
        "type": "IndicatorBlock",
        "unit": "万",
        "label": "营收规模",
        "value": 331581.06
      },
      {
        "type": "BarChart",
        "unit": "万",
        "title": "广州软件和信息技术服务营收趋势",
        "value": [222499.49, 245377.94, 331581.06],
        "tags": ["2021年", "2022年", "2023年"]
      },
      {
        "type": "CustomChart",
        "customField1": "自定义值",
        "customField2": 123,
        "nestedData": {
          "nestedKey1": "nestedValue1",
          "nestedKey2": 456
        }
      }
    ],
    "recommendedQuestions": null
  }
}
```

## 任务结果动态属性访问方法

在Java代码中，您可以通过以下方式访问ChatTaskResult的动态属性：

```java
// 获取动态属性
Object value = chatTaskResult.getProperty("propertyName");

// 判断是否有某个属性
boolean hasProperty = chatTaskResult.hasProperty("propertyName");

// 设置动态属性
chatTaskResult.setAdditionalProperty("propertyName", value);

// 获取全部动态属性
Map<String, Object> allProperties = chatTaskResult.getAdditionalProperties();
```

## 注意事项

1. 系统只保存最新的6条聊天记录
2. 如果用户未登录，API将返回401错误
3. 任务结果(`taskResults`)使用灵活的`ChatTaskResult`类型，可以支持任意JSON结构
4. 所有API调用都需要有效的会话，以确保用户已登录

## 实现细节

`ChatTaskResult`使用Jackson的`@JsonAnyGetter`和`@JsonAnySetter`注解实现了动态属性的支持，可以处理任意JSON结构。系统会保留`type`字段作为类型标识，其他所有字段都作为动态属性处理。

## 示例代码

### 保存聊天历史

```javascript
const chatHistory = [
  {
    "type": "user",
    "content": "我想查询小巨人企业经营信息相关信息"
  },
  {
    "type": "ai",
    "content": "",
    "rspType": 4,
    "template": "",
    "taskResults": [
      {
        "type": "IndicatorBlock",
        "unit": "万",
        "label": "营收规模",
        "value": 331581.06
      }
    ],
    "recommendedQuestions": []
  }
];

fetch('/api/chat/history', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ chatHistory }),
  credentials: 'include'  // 包含cookies以保持会话
})
.then(response => response.json())
.then(data => {
  console.log('保存结果:', data);
})
.catch(error => {
  console.error('保存失败:', error);
});
```

### 获取聊天历史

```javascript
fetch('/api/chat/history', {
  method: 'GET',
  credentials: 'include'  // 包含cookies以保持会话
})
.then(response => response.json())
.then(data => {
  if (data.code === 200) {
    console.log('聊天历史:', data.data);
    
    // 处理动态任务结果
    data.data.forEach(message => {
      if (message.type === 'ai' && message.taskResults) {
        message.taskResults.forEach(result => {
          // 根据类型处理不同的结果
          switch(result.type) {
            case 'IndicatorBlock':
              console.log(`指标: ${result.label}, 值: ${result.value} ${result.unit}`);
              break;
            case 'BarChart':
              console.log(`图表: ${result.title}, 数据点: ${result.value.length}`);
              break;
            default:
              console.log(`其他类型: ${result.type}`);
          }
        });
      }
    });
  } else {
    console.error('获取失败:', data.message);
  }
})
.catch(error => {
  console.error('请求失败:', error);
});
```