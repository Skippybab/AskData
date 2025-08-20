# AskData - 精准问数平台

## 项目简介

AskData是一个面向业务用户的智能数据服务平台，通过自然语言交互、知识管理及工具配置，提供数据问答、知识沉淀与智能体体验功能，提升业务决策效率。

## 技术架构

- **后端**: Spring Boot + MySQL + Redis
- **前端**: Vue3 + Element Plus
- **AI模型**: 集成大模型接口进行自然语言转SQL
- **数据库**: MySQL + Redis缓存

## 项目结构

```
├── mt-agent-workflow-api-core/     # 后端核心服务
│   ├── src/main/java/             # Java源代码
│   ├── src/main/resources/        # 配置文件
│   ├── pom.xml                    # Maven配置
│   └── start.bat                  # 启动脚本
│
└── mt-agent-workflow-api-webapp/  # 前端应用
    ├── src/                       # Vue源代码
    ├── public/                    # 静态资源
    ├── package.json               # Node.js配置
    └── vite.config.js             # Vite配置
```

## 核心功能模块

### 1. 数据问答模块
- 支持各类数据相关自然语言提问输入
- 调用大模型接口将提问转译为可执行SQL语句
- 安全执行SQL查询，确保数据库连接稳定
- 清晰展示查询结果，支持结果导出

### 2. 知识管理模块
- 支持文档、表格等多种格式文件上传
- 自动解析上传文件并切分嵌入知识库
- 提供知识库配置管理功能

### 3. 工具管理模块
- 内置SQL查询、知识库问答等多种工具
- 支持工具配置给智能体
- 支持工具参数设置和状态管理

### 4. 受控体验模块
- 整合用户配置的数据和工具生成专属智能体
- 提供界面对话和API接口调用两种体验形式
- 确保不同用户及对话session的历史隔离

## 快速开始

### 后端启动

1. 进入后端目录：
```bash
cd mt-agent-workflow-api-core
```

2. 使用Maven编译：
```bash
mvn clean install
```

3. 启动应用：
```bash
./start.bat
```

### 前端启动

1. 进入前端目录：
```bash
cd mt-agent-workflow-api-webapp
```

2. 安装依赖：
```bash
npm install
```

3. 启动开发服务器：
```bash
npm run dev
```

## 环境要求

- Java 8+
- Node.js 16+
- MySQL 5.7+
- Redis 6.0+

## 配置说明

### 数据库配置
在 `application.yml` 中配置数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/askdata
    username: your_username
    password: your_password
```

### Redis配置
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: your_redis_password
```

## 安全特性

- 加密存储传输用户敏感数据
- 严格用户权限管理
- 数据库连接安全防护
- API接口访问控制

## 性能优化

- Redis缓存提升查询速度
- 数据库连接池优化
- 前端资源压缩和CDN加速
- 支持并发访问优化

## 许可证

本项目采用 MIT 许可证。

## 贡献指南

欢迎提交 Issue 和 Pull Request 来改进项目。

## 联系方式

如有问题，请通过 GitHub Issues 联系我们。
>>>>>>> master
