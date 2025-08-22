# 知识库管理系统

这是一个基于Vue 3 + Element Plus的知识库管理系统，提供了完整的知识库管理功能。

## 功能特性

### 1. 知识库管理
- 创建新知识库
- 管理现有知识库
- 知识库状态管理（启用/禁用）
- 知识库搜索和筛选

### 2. 知识库文件管理
- 文件上传（支持txt、doc、docx、pdf格式）
- 文件列表展示（序号、文件名称、文件标签、文本块个数、字数、状态）
- 文件状态管理（开启/禁用）
- 文件搜索和筛选

### 3. 文件文本块管理
- 文本块列表展示（序号、文本块标签、文本块内容）
- 文本块内容编辑
- 文本块搜索和筛选

### 4. 知识关联管理
- 知识关联列表展示（序号、知识名称、关联知识列表）
- 新增知识关联
- 编辑知识关联
- 关联知识块选择

## 技术栈

- **前端框架**: Vue 3
- **UI组件库**: Element Plus
- **路由管理**: Vue Router 4
- **HTTP客户端**: Axios
- **构建工具**: Vite

## 项目结构

```
src/
├── api/                    # API接口
│   ├── request.js         # 请求配置
│   ├── user.js           # 用户相关API
│   └── knowledge.js      # 知识库相关API
├── views/                 # 页面组件
│   ├── Layout.vue        # 主布局
│   ├── Login.vue         # 登录页面
│   ├── UserManagement.vue # 用户管理
│   ├── KnowledgeManagement.vue # 知识库管理
│   ├── KnowledgeFiles.vue # 知识库文件管理
│   ├── FileBlocks.vue    # 文件文本块管理
│   └── KnowledgeRelations.vue # 知识关联管理
├── router/               # 路由配置
│   └── index.js
├── App.vue              # 根组件
└── main.js             # 入口文件
```

## 安装和运行

### 安装依赖
```bash
npm install
```

### 开发环境运行
```bash
npm run dev
```

### 生产环境构建
```bash
npm run build
```

### 预览构建结果
```bash
npm run preview
```

## 页面路由

- `/login` - 登录页面
- `/admin/knowledge` - 知识库管理主页面
- `/admin/knowledge/:knowledgeId/files` - 知识库文件管理
- `/admin/knowledge/:knowledgeId/files/:fileId/blocks` - 文件文本块管理
- `/admin/knowledge/:knowledgeId/relations` - 知识关联管理
- `/admin/user` - 用户管理

## API接口

### 知识库相关接口
- `GET /knowledge/list` - 获取知识库列表
- `POST /knowledge/create` - 创建知识库
- `GET /knowledge/:id` - 获取知识库详情
- `PUT /knowledge/update` - 更新知识库
- `DELETE /knowledge/:id` - 删除知识库

### 文件相关接口
- `GET /knowledge/:knowledgeId/files` - 获取知识库文件列表
- `POST /knowledge/:knowledgeId/upload` - 上传文件
- `DELETE /knowledge/:knowledgeId/files/:fileId` - 删除文件

### 文本块相关接口
- `GET /knowledge/:knowledgeId/files/:fileId/blocks` - 获取文件文本块列表
- `PUT /knowledge/:knowledgeId/files/:fileId/blocks/:blockId` - 更新文本块

### 知识关联相关接口
- `GET /knowledge/:knowledgeId/relations` - 获取知识关联列表
- `PUT /knowledge/:knowledgeId/relations/:relationId` - 更新知识关联

## 使用说明

1. **登录系统**: 访问登录页面，输入用户名和密码
2. **知识库管理**: 
   - 创建新知识库：点击"创建知识库"按钮
   - 管理现有知识库：点击"管理文件"按钮进入文件管理
   - 知识关联管理：点击"知识关联"按钮进入关联管理
3. **文件管理**:
   - 上传文件：点击"上传文件"按钮
   - 查看文本块：点击"查看文本块"按钮
   - 文件状态管理：点击"开启/禁用"按钮
4. **文本块管理**:
   - 编辑文本块：点击"编辑"按钮
   - 搜索文本块：使用搜索功能
5. **知识关联管理**:
   - 新增关联：点击"新增关联"按钮
   - 编辑关联：点击"编辑"按钮
   - 选择关联知识块：在下拉框中选择或输入

## 注意事项

- 文件上传支持格式：txt、doc、docx、pdf
- 文件大小限制：10MB
- 文本块内容最大长度：5000字符
- 系统需要后端API支持，请确保后端服务正常运行
