# 智能数据问答平台 - API使用指南

## 功能概述

本平台已成功实现了以下两个关键功能：

### 1. SQL查询结果前端展示优化
- ✅ 实现了查询结果的智能解析和展示
- ✅ 支持大量数据的折叠展示功能
- ✅ 提供数据表格组件（DataTable.vue）用于结构化数据展示
- ✅ 支持数据导出、字段筛选等高级功能

### 2. API接口管理和对外暴露
- ✅ 将原有的"用户管理"模块改造为"接口管理"模块
- ✅ 实现了完整的API配置管理功能（创建、编辑、删除、启用/禁用）
- ✅ 提供了对外暴露的RESTful API接口
- ✅ 支持API密钥认证、速率限制、超时控制等功能

## 数据库配置

在使用前，请先执行以下SQL脚本创建API配置表：

```bash
# 执行SQL脚本创建api_config表
mysql -u root -p < create_api_config_table.sql
```

## API接口管理使用说明

### 1. 访问接口管理页面

登录系统后，在左侧菜单中点击"接口管理"进入管理页面。

### 2. 创建API接口

1. 点击"创建API"按钮
2. 填写以下信息：
   - **API名称**：给接口起一个易于识别的名称
   - **API路径**：设置唯一的API访问路径（如：user-query）
   - **数据库**：选择要查询的数据库
   - **数据表**：选择要查询的数据表
   - **速率限制**：设置每分钟最大请求次数（默认60次/分钟）
   - **超时时间**：设置请求超时时间（默认30秒）
   - **描述**：添加API的详细说明

3. 点击"确定"创建API，系统会自动生成API密钥

### 3. 管理API接口

- **查看密钥**：点击眼睛图标查看完整的API密钥
- **复制密钥**：点击复制图标将密钥复制到剪贴板
- **编辑配置**：修改API的各项配置
- **启用/禁用**：控制API的可用状态
- **查看文档**：查看API的详细使用文档和示例
- **删除API**：删除不再使用的API配置

## 对外API调用说明

### 1. API端点

```
POST http://your-domain:8080/open-api/v1/query/{apiPath}
```

其中 `{apiPath}` 是您在创建API时设置的路径。

### 2. 请求格式

```json
{
  "apiKey": "sk-xxxxxxxxxxxxx",  // API密钥
  "question": "查询最近7天的销售数据"  // 自然语言查询问题
}
```

### 3. 响应格式

成功响应（200）：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "success": true,
    "thinking": "正在分析您的问题...",  // AI思考过程
    "data": "查询结果...",  // 查询结果文本
    "structuredData": "{...}",  // 结构化数据（如果有）
    "apiPath": "user-query",
    "sessionId": 12345,
    "executionTime": 1523  // 执行时间（毫秒）
  }
}
```

错误响应：
```json
{
  "code": 401,  // 错误码
  "msg": "API密钥无效"  // 错误信息
}
```

### 4. 错误码说明

| 错误码 | 说明 |
|-------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | API密钥无效 |
| 403 | API已禁用 |
| 404 | API不存在 |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |

### 5. 调用示例

#### cURL示例
```bash
curl -X POST "http://localhost:8080/open-api/v1/query/user-query" \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "sk-xxxxxxxxxxxxx",
    "question": "查询年龄大于25岁的用户"
  }'
```

#### Python示例
```python
import requests
import json

url = "http://localhost:8080/open-api/v1/query/user-query"
payload = {
    "apiKey": "sk-xxxxxxxxxxxxx",
    "question": "查询年龄大于25岁的用户"
}

response = requests.post(url, json=payload)
result = response.json()

if result["code"] == 200:
    print("查询成功")
    print("结果:", result["data"]["data"])
else:
    print("查询失败:", result["msg"])
```

#### JavaScript示例
```javascript
const apiUrl = 'http://localhost:8080/open-api/v1/query/user-query';
const requestData = {
    apiKey: 'sk-xxxxxxxxxxxxx',
    question: '查询年龄大于25岁的用户'
};

fetch(apiUrl, {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify(requestData)
})
.then(response => response.json())
.then(result => {
    if (result.code === 200) {
        console.log('查询成功');
        console.log('结果:', result.data.data);
    } else {
        console.error('查询失败:', result.msg);
    }
})
.catch(error => {
    console.error('请求失败:', error);
});
```

## 注意事项

1. **API密钥安全**：请妥善保管API密钥，不要在公开代码中暴露
2. **速率限制**：请遵守设置的速率限制，避免频繁请求
3. **超时处理**：对于复杂查询，可能需要较长时间，请适当设置超时时间
4. **错误处理**：请在调用端做好错误处理，根据错误码采取相应措施
5. **数据权限**：API只能访问创建时指定的数据库和表

## 项目结构说明

### 后端新增文件
- `/mt-agent-workflow-api-core/src/main/java/com/mt/agent/workflow/api/entity/ApiConfig.java` - API配置实体
- `/mt-agent-workflow-api-core/src/main/java/com/mt/agent/workflow/api/mapper/ApiConfigMapper.java` - API配置Mapper
- `/mt-agent-workflow-api-core/src/main/java/com/mt/agent/workflow/api/service/ApiConfigService.java` - API配置服务接口
- `/mt-agent-workflow-api-core/src/main/java/com/mt/agent/workflow/api/service/impl/ApiConfigServiceImpl.java` - API配置服务实现
- `/mt-agent-workflow-api-core/src/main/java/com/mt/agent/workflow/api/controller/ApiConfigController.java` - API配置管理控制器
- `/mt-agent-workflow-api-core/src/main/java/com/mt/agent/workflow/api/controller/OpenApiController.java` - 对外API控制器

### 前端修改文件
- `/mt-agent-workflow-api-webapp/src/views/ApiManagement.vue` - 接口管理页面（替代原UserManagement.vue）
- `/mt-agent-workflow-api-webapp/src/api/apiConfig.js` - API配置相关的API调用
- `/mt-agent-workflow-api-webapp/src/router/index.js` - 路由配置更新
- `/mt-agent-workflow-api-webapp/src/views/Layout.vue` - 菜单项更新

### 数据库变更
- 新增 `api_config` 表用于存储API配置信息

## 启动项目

### 后端启动
```bash
cd mt-agent-workflow-api-core
mvn clean install
mvn spring-boot:run
```

### 前端启动
```bash
cd mt-agent-workflow-api-webapp
npm install
npm run dev
```

## 总结

项目已成功实现了两个关键功能：
1. **SQL查询结果展示优化**：前端现在能够正确接收和展示后端返回的查询结果，包括折叠展示大量数据的功能
2. **API接口管理**：将用户管理模块改造为接口管理模块，实现了完整的API配置管理和对外暴露功能

用户现在可以：
- 在接口管理页面创建和管理API配置
- 为每个API设置独立的数据库、表、速率限制等参数
- 通过生成的API密钥，让外部系统安全地调用数据问答功能
- 查看详细的API文档和调用示例

这样就实现了将智能数据问答功能作为服务对外提供的目标。