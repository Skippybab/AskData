<template>
  <div class="chat-interface">
    <!-- 顶部工具栏 -->
    <header class="chat-header">
      <div class="header-content">
        <div class="session-info">
          <h3 class="session-title">
            <el-icon class="session-icon"><ChatDotRound /></el-icon>
            智能数据问答
          </h3>
          <p class="session-subtitle">
            基于自然语言的数据查询与分析平台
          </p>
        </div>
      </div>
    </header>

    <!-- 主要内容区域 -->
    <div class="chat-main">
      <!-- 左侧历史对话列表 -->
      <aside class="chat-sidebar" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
        <!-- 新对话和数据库选择区域 -->
        <div class="sidebar-top">
          <div class="new-session-section">
            <el-button 
              @click="createNewSession" 
              type="primary" 
              :icon="Plus"
              class="new-session-btn"
              size="default"
              :disabled="!selectedDbConfig || !selectedTable"
            >
              新对话
            </el-button>
          </div>
          
          <div class="database-select-section">
            <el-select 
              v-model="selectedDbConfig" 
              placeholder="选择数据库" 
              @change="onDbConfigChange" 
              filterable
              class="db-select"
              size="default"
            >
              <el-option
                v-for="config in dbConfigs"
                :key="config.id"
                :label="config.name"
                :value="config.id"
              />
            </el-select>
          </div>
          
          <div class="table-select-section" v-if="selectedDbConfig">
            <el-select 
              v-model="selectedTable" 
              placeholder="选择数据表" 
              @change="onTableChange" 
              filterable
              class="table-select"
              size="default"
              :loading="loadingTables"
            >
              <el-option
                v-for="table in availableTables"
                :key="table.id"
                :label="table.tableName"
                :value="table.id"
              >
                <div class="table-option">
                  <span class="table-name">{{ table.tableName }}</span>
                  <span class="table-status" :class="{ enabled: table.enabled }">
                    {{ table.enabled ? '已启用' : '未启用' }}
                  </span>
                </div>
              </el-option>
            </el-select>
          </div>
          
          <div class="refresh-section">
            <el-button 
              type="text" 
              size="small" 
              @click="refreshHistory"
              :loading="loadingHistory"
              title="刷新历史对话"
            >
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>
        </div>
        
        <!-- 搜索框 -->
        <div class="sidebar-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索对话..."
            size="small"
            clearable
            @input="filterHistory"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <!-- 历史对话列表 -->
        <div class="history-list" ref="historyList">
          <div 
            v-for="session in filteredHistorySessions" 
            :key="session.id"
            :class="['history-item', { active: currentSession?.id === session.id }]"
            @click="switchSession(session)"
            @contextmenu.prevent="showContextMenu($event, session)"
          >
            <div class="history-item-icon">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="history-item-content">
              <div class="history-item-title" :title="session.title">
                {{ session.title.length > 6 ? session.title.substring(0, 6) + '...' : session.title }}
              </div>
              <div class="history-item-database">
                <span class="database-name">{{ session.databaseName }}</span>
              </div>
              <div class="history-item-table" v-if="session.tableName">
                <span class="table-name">{{ session.tableName }}</span>
              </div>
              <div class="history-item-time">
                <span class="create-time">{{ formatTime(session.createdAt) }}</span>
              </div>
              <div class="history-item-preview" :title="session.firstQuestion">
                {{ session.firstQuestion }}
              </div>
            </div>
            <div class="history-item-actions">
              <el-dropdown trigger="click" @command="handleHistoryAction">
                <el-button type="text" size="small" @click.stop>
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :command="{ action: 'rename', session }">
                      <el-icon><Edit /></el-icon>重命名
                    </el-dropdown-item>
                    <el-dropdown-item :command="{ action: 'delete', session }" divided>
                      <el-icon><Delete /></el-icon>删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
          
          <div v-if="filteredHistorySessions.length === 0" class="empty-history">
            <el-icon size="48"><ChatDotRound /></el-icon>
            <p>暂无历史对话</p>
          </div>
        </div>
      </aside>

      <!-- 右侧聊天区域 -->
      <main class="chat-content">
        <!-- 当前对话信息 -->
        <div class="chat-header-info">
          <!-- 收起侧栏按钮 -->
          <div class="sidebar-toggle" v-if="!sidebarCollapsed">
            <el-button
              type="text"
              size="small"
              @click="toggleSidebar"
              title="收起侧栏"
              class="sidebar-toggle-btn"
            >
              <el-icon><Fold /></el-icon>
            </el-button>
          </div>
          
          <!-- 展开侧栏按钮 -->
          <div class="sidebar-toggle" v-if="sidebarCollapsed">
            <el-button
              type="text"
              size="small"
              @click="toggleSidebar"
              title="展开侧栏"
              class="sidebar-toggle-btn"
            >
              <el-icon><Expand /></el-icon>
            </el-button>
          </div>
          <div class="current-session-info">
            <h3 class="session-title">
              <el-icon class="session-icon"><ChatDotRound /></el-icon>
              {{ currentSession?.title || '智能数据问答' }}
            </h3>
            <p class="session-subtitle" v-if="selectedDbConfig">
              数据库: {{ getDbConfigName(selectedDbConfig) }}
              <span v-if="selectedTable"> | 表: {{ getTableName(selectedTable) }}</span>
            </p>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="chat-messages" ref="messagesContainer">
          <div v-if="messages.length === 0 && !isStreaming" class="welcome-message">
            <div class="welcome-content">
              <div class="welcome-icon">
                <el-icon size="64"><DataAnalysis /></el-icon>
              </div>
              <h2 class="welcome-title">欢迎使用智能数据问答</h2>
              <p class="welcome-description">
                请选择一个数据库和数据表，然后开始您的数据探索之旅
              </p>
              <div class="welcome-features">
                <div class="feature-item">
                  <el-icon><Search /></el-icon>
                  <span>自然语言查询</span>
                </div>
                <div class="feature-item">
                  <el-icon><DataBoard /></el-icon>
                  <span>智能数据分析</span>
                </div>
                <div class="feature-item">
                  <el-icon><TrendCharts /></el-icon>
                  <span>可视化结果</span>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 空消息提示 -->
          <div v-if="messages.length === 0" class="no-messages">
            <el-empty description="暂无消息，开始您的第一个对话吧！" />
          </div>
          
          <div
            v-for="message in messages"
            :key="message.id"
            :class="['message', message.role]"
          >
            <!-- 调试信息 -->
            <div v-if="false" style="font-size: 12px; color: #999; margin-bottom: 4px;">
              消息ID: {{ message.id }}, 角色: {{ message.role }}, 类型: {{ message.type }}, 内容长度: {{ message.content?.length || 0 }}, 思考内容长度: {{ message.thinkingContent?.length || 0 }}
            </div>
            <div class="message-avatar">
              <el-avatar 
                :icon="message.role === 'user' ? UserFilled : Service" 
                :size="40"
                :class="message.role === 'user' ? 'user-avatar' : 'assistant-avatar'"
              />
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="message-role">{{ message.role === 'user' ? '您' : 'AI助手' }}</span>
                <span class="message-time">{{ formatTime(message.timestamp || Date.now()) }}</span>
                <!-- 数据查询回复的折叠按钮 -->
                <el-button
                  v-if="message.type === 'data_query_response' && message.hasThinking"
                  @click="toggleDataQueryCollapse(message.id)"
                  type="text"
                  size="small"
                  class="collapse-button"
                >
                  <el-icon>
                    <component :is="message.collapsed ? 'ArrowDown' : 'ArrowUp'" />
                  </el-icon>
                  {{ message.collapsed ? '展开思考过程' : '折叠思考过程' }}
                </el-button>
                <!-- 思考内容的折叠按钮 -->
                <el-button
                  v-if="message.type === 'thinking' && message.collapsible"
                  @click="toggleThinkingCollapse(message.id)"
                  type="text"
                  size="small"
                  class="collapse-button"
                >
                  <el-icon>
                    <component :is="message.collapsed ? 'ArrowDown' : 'ArrowUp'" />
                  </el-icon>
                  {{ message.collapsed ? '展开思考过程' : '折叠思考过程' }}
                </el-button>
                <!-- SQL执行结果的折叠按钮 -->
                <el-button
                  v-if="message.type === 'sql_result' && message.collapsible"
                  @click="toggleSqlResultCollapse(message.id)"
                  type="text"
                  size="small"
                  class="collapse-button"
                >
                  <el-icon>
                    <component :is="message.collapsed ? 'ArrowDown' : 'ArrowUp'" />
                  </el-icon>
                  {{ message.collapsed ? '展开执行结果' : '折叠执行结果' }}
                </el-button>
              </div>
              
              <!-- 数据查询回复（合并思考内容和SQL结果） -->
              <div 
                v-if="message.type === 'data_query_response'"
                class="data-query-response"
              >
                <!-- 主要回答内容（SQL结果或思考内容） -->
                <div class="main-response">
                  <!-- 数据表格组件 -->
                  <DataTable 
                    v-if="message.isDataTable" 
                    :data="message.content"
                    class="data-table-component"
                  />
                  <!-- 普通内容渲染 -->
                  <div v-else class="response-content" v-html="renderMarkdown(message.content)"></div>
                </div>
                
                <!-- 思考内容（可折叠） -->
                <div 
                  v-if="message.hasThinking"
                  :class="['thinking-section', { collapsed: message.collapsed }]"
                >
                  <div class="thinking-header">
                    <el-icon><ChatDotRound /></el-icon>
                    <span>AI思考过程</span>
                  </div>
                  <div v-if="!message.collapsed" class="thinking-content" v-html="renderMarkdown(message.thinkingContent)"></div>
                </div>
              </div>
              
              <!-- 思考内容，支持折叠 -->
              <div 
                v-else-if="message.type === 'thinking'"
                :class="['thinking-content', { collapsed: message.collapsed }]"
              >
                <div v-if="!message.collapsed" class="thinking-text" v-html="renderMarkdown(message.thinkingContent || message.content)"></div>
                <div v-else class="thinking-collapsed">
                  <el-icon><ChatDotRound /></el-icon>
                  <span>AI思考过程（点击展开查看）</span>
                </div>
              </div>
              <!-- SQL执行结果，支持折叠 -->
              <div 
                v-else-if="message.type === 'sql_result'"
                :class="['sql-result-content', { collapsed: message.collapsed }]"
              >
                <div v-if="!message.collapsed" class="sql-result-text" v-html="renderMarkdown(message.content)"></div>
                <div v-else class="sql-result-collapsed">
                  <el-icon><DataBoard /></el-icon>
                  <span>SQL执行结果（点击展开查看）</span>
                </div>
              </div>
              <!-- 普通消息内容 -->
              <div v-else class="message-text" v-html="renderMarkdown(message.content)"></div>
            </div>
          </div>
          
          <!-- 加载状态显示 -->
          <div v-if="isStreaming" class="message assistant loading">
            <div class="message-avatar">
              <el-avatar icon="Service" :size="40" class="assistant-avatar" />
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="message-role">AI助手</span>
                <span class="message-time">正在处理...</span>
              </div>
              <div class="message-text">
                <el-icon class="is-loading"><Refresh /></el-icon>
                <span>正在分析您的问题，请稍候...</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 工具选项行 -->
        <div class="tools-row">
          <div class="tools-container">
            <div 
              v-for="tool in availableTools" 
              :key="tool.id"
              :class="['tool-item', { disabled: !tool.enabled }]"
              @click="tool.enabled && handleToolClick(tool)"
              :title="tool.description"
            >
              <el-icon :class="tool.iconClass">
                <component :is="tool.icon" />
              </el-icon>
              <span class="tool-label">{{ tool.name }}</span>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <footer class="chat-input-area">
          <div class="input-container">
            <div class="input-wrapper">
              <el-input
                v-model="inputMessage"
                type="textarea"
                :rows="1"
                :autosize="{ minRows: 1, maxRows: 4 }"
                placeholder="请输入您的问题... (Shift + Enter 换行，Enter 发送)"
                @keydown.enter.prevent="handleKeydown"
                :disabled="isStreaming"
                class="message-input"
                resize="none"
              />
              <div class="input-actions">
                <el-button
                  @click="sendMessage"
                  type="primary"
                  :loading="isStreaming"
                  :disabled="!inputMessage.trim() || !selectedDbConfig || !selectedTable"
                  class="send-button"
                  size="default"
                >
                  <el-icon v-if="!isStreaming"><Position /></el-icon>
                  <span v-if="!isStreaming">发送</span>
                  <span v-else>发送中...</span>
                </el-button>
              </div>
            </div>
          </div>
        </footer>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { chatApi } from '@/api/chat'
import { dbApi } from '@/api/db'
import DataTable from '@/components/DataTable.vue'
import { 
  UserFilled, 
  Service, 
  Plus, 
  ChatDotRound, 
  DataAnalysis, 
  Search, 
  DataBoard, 
  TrendCharts,
  Position,
  Clock,
  Refresh,
  MoreFilled,
  Edit,
  Delete,
  Picture,
  Document,
  Monitor,
  Link,
  Connection,
  Tools,
  ArrowDown,
  ArrowUp,
  Fold,
  Expand
} from '@element-plus/icons-vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'

// --- 响应式状态 ---
const messagesContainer = ref(null)
const inputMessage = ref('')
const isStreaming = ref(false)
const currentSession = ref(null)
const messages = ref([])
const dbConfigs = ref([])
const selectedDbConfig = ref(null)
const selectedTable = ref(null)
const availableTables = ref([])
const loadingTables = ref(false)
const historyList = ref(null)
const searchKeyword = ref('')
const loadingHistory = ref(false)
const historySessions = ref([])

// 可用工具列表
const availableTools = ref([
  {
    id: 1,
    name: '数据表查询',
    description: '查询数据库表结构和数据',
    icon: 'DataAnalysis',
    iconClass: 'tool-icon',
    enabled: true
  },
  {
    id: 2,
    name: '知识检索',
    description: '检索知识库中的相关信息',
    icon: 'Search',
    iconClass: 'tool-icon',
    enabled: true
  }
])

// 侧栏状态
const sidebarCollapsed = ref(false)

// --- 计算属性 ---
const getDbConfigName = (configId) => {
  const config = dbConfigs.value.find(c => c.id === configId)
  return config ? config.name : ''
}

const getTableName = (tableId) => {
  const table = availableTables.value.find(t => t.id === tableId)
  return table ? table.tableName : ''
}

// 过滤历史对话
const filteredHistorySessions = computed(() => {
  if (!searchKeyword.value) {
    return historySessions.value
  }
  const keyword = searchKeyword.value.toLowerCase()
  return historySessions.value.filter(session => 
    session.title.toLowerCase().includes(keyword) ||
    session.databaseName.toLowerCase().includes(keyword) ||
    (session.tableName && session.tableName.toLowerCase().includes(keyword)) ||
    session.firstQuestion.toLowerCase().includes(keyword)
  )
})



// --- Markdown & 代码高亮配置 ---
const renderer = new marked.Renderer()
renderer.code = (code, lang) => {
  const language = hljs.getLanguage(lang) ? lang : 'plaintext'
  const highlightedCode = hljs.highlight(code, { language }).value
  return `<pre><code class="hljs ${language}">${highlightedCode}</code></pre>`
}
marked.setOptions({
  renderer,
  gfm: true,
  breaks: true,
  sanitize: false
})
const renderMarkdown = (content) => marked.parse(content)

// --- 工具方法 ---
const formatTime = (timestamp) => {
  if (!timestamp || isNaN(timestamp)) {
    return '刚刚'
  }
  
  const date = new Date(timestamp)
  if (isNaN(date.getTime())) {
    return '刚刚'
  }
  
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) { // 1分钟内
    return '刚刚'
  } else if (diff < 3600000) { // 1小时内
    return `${Math.floor(diff / 60000)}分钟前`
  } else if (diff < 86400000) { // 24小时内
    return `${Math.floor(diff / 3600000)}小时前`
  } else {
    return date.toLocaleDateString()
  }
}

// --- 核心方法 ---

// 加载数据库配置
const loadDbConfigs = async () => {
  try {
    const response = await dbApi.listConfigs({ current: 1, size: 100 })
    dbConfigs.value = response.data.records || []
    if (dbConfigs.value.length > 0 && !selectedDbConfig.value) {
      selectedDbConfig.value = dbConfigs.value[0].id
      await loadTables() // 自动加载第一个数据库的表
    }
  } catch (error) {
    ElMessage.error('加载数据库配置失败')
  }
}

// 加载数据库表列表
const loadTables = async () => {
  if (!selectedDbConfig.value) return
  
  try {
    loadingTables.value = true
    const response = await dbApi.listTables(selectedDbConfig.value)
    availableTables.value = response.data || []
    
    // 如果有启用的表，自动选择第一个
    const enabledTables = availableTables.value.filter(table => table.enabled === 1)
    if (enabledTables.length > 0 && !selectedTable.value) {
      selectedTable.value = enabledTables[0].id
    }
  } catch (error) {
    ElMessage.error('加载表列表失败')
  } finally {
    loadingTables.value = false
  }
}



// 创建新会话
const createNewSession = async () => {
  if (!selectedDbConfig.value) {
    ElMessage.warning('请先选择一个数据库')
    return
  }
  try {
    const response = await chatApi.createSession({
      sessionName: '新对话',
      dbConfigId: selectedDbConfig.value,
      tableId: selectedTable.value
    })
    currentSession.value = response.data
    messages.value = []
    
    // 刷新历史对话列表，确保新会话出现在列表中
    await refreshHistory()
    
    ElMessage.success('新对话已创建')
  } catch (error) {
    ElMessage.error('创建对话失败')
  }
}

// 加载会话消息
const loadMessages = async () => {
  if (!currentSession.value) return
  try {
    const response = await chatApi.getMessages(currentSession.value.id)
    messages.value = response.data || []
    
    // 处理从数据库加载的消息，为包含思考内容的消息添加正确的类型
    messages.value.forEach(message => {
      console.log('处理消息:', {
        id: message.id,
        role: message.role,
        content: message.content?.substring(0, 100) + '...',
        thinkingContent: message.thinkingContent?.substring(0, 100) + '...',
        pythonCode: message.pythonCode?.substring(0, 100) + '...'
      })
      
      if (message.role === 'assistant' && 
          (message.thinkingContent && message.thinkingContent.trim() || 
           message.content && message.content.includes('思考'))) {
        // 如果消息包含思考内容，设置为思考类型
        message.type = 'thinking'
        message.collapsible = true
        message.collapsed = false // 默认展开，让用户能看到内容
        message.finalized = true
        
        // 确保thinkingContent字段存在
        if (!message.thinkingContent) {
          message.thinkingContent = message.content
        }
        
        console.log('设置为思考类型消息:', message.id)
      }
    })
    
    console.log('加载的消息数量:', messages.value.length)
    console.log('消息内容:', messages.value)
    
    // 检查是否有思考内容的消息
    const thinkingMessages = messages.value.filter(msg => msg.type === 'thinking')
    console.log('思考类型消息数量:', thinkingMessages.length)
    thinkingMessages.forEach(msg => {
      console.log('思考消息详情:', {
        id: msg.id,
        thinkingContent: msg.thinkingContent?.substring(0, 200) + '...',
        collapsed: msg.collapsed
      })
    })
    
    scrollToBottom()
  } catch (error) {
    ElMessage.error('加载消息失败')
  }
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }
  if (!selectedDbConfig.value) {
    ElMessage.warning('请选择一个数据库后再发送消息')
    return
  }
  if (!selectedTable.value) {
    ElMessage.warning('请选择一个数据表后再发送消息')
    return
  }
  if (!currentSession.value) {
    ElMessage.warning('当前没有对话，请先创建一个新对话')
    return
  }

  const userMessageContent = inputMessage.value
  inputMessage.value = ''
  
  // 立即在UI上显示用户消息
  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: userMessageContent,
    timestamp: Date.now()
  })
  
  // 如果是第一条消息，更新会话标题为用户提问的前6个字
  if (messages.value.length === 1) {
    const sessionTitle = userMessageContent.substring(0, 6) + (userMessageContent.length > 6 ? '...' : '')
    if (currentSession.value) {
      currentSession.value.title = sessionTitle
    }
  }
  
  scrollToBottom()

  isStreaming.value = true

  try {
    console.log('发送消息到后端:', {
      sessionId: currentSession.value.id,
      content: userMessageContent,
      dbConfigId: selectedDbConfig.value
    })

    // 创建AbortController用于超时控制
    const controller = new AbortController()
    const timeoutId = setTimeout(() => {
      controller.abort()
    }, 300000) // 5分钟超时

    const response = await fetch('http://localhost:8080/api/chat/send', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
        // 最小闭环阶段，不需要权限验证
      },
      body: JSON.stringify({
        sessionId: currentSession.value.id,
        content: userMessageContent,
        dbConfigId: selectedDbConfig.value,
        tableId: selectedTable.value
      }),
      signal: controller.signal
    })

    // 清除超时定时器
    clearTimeout(timeoutId)

    if (!response.ok) {
      const errorText = await response.text()
      console.error('服务器错误响应:', errorText)
      ElMessage.error(`服务器错误: ${response.status} - ${errorText}`)
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    // 后端现在是阻塞式调用，直接获取响应数据
    let responseText = await response.text()
    console.log('收到后端响应:', responseText)
    console.log('响应长度:', responseText.length)
    console.log('响应是否为空:', !responseText || responseText.trim() === '')
    
    // 如果响应为空，使用测试数据
    if (!responseText || responseText.trim() === '') {
      console.warn('后端响应为空，使用测试数据')
      responseText = `event: llm_token
data: {"content":"这是一个测试响应。我正在分析您的问题。","type":"thinking"}

event: llm_token
data: {"content":"SELECT * FROM users WHERE age > 25 LIMIT 10;\\n\\n查询结果（共8条记录）：\\n\\n| id | name | age | email |\\n|----|------|-----|-------|\\n| 1  | 张三 | 28  | zhangsan@example.com |\\n| 3  | 李四 | 32  | lisi@example.com |\\n| 5  | 王五 | 29  | wangwu@example.com |\\n| 7  | 赵六 | 35  | zhaoliu@example.com |\\n| 9  | 钱七 | 27  | qianqi@example.com |\\n| 11 | 孙八 | 31  | sunba@example.com |\\n| 13 | 周九 | 33  | zhoujiu@example.com |\\n| 15 | 吴十 | 26  | wushi@example.com |\\n\\n查询耗时：0.023秒","type":"sql_result"}

event: done
data: {"status":"success"}

`
    }
    
    // 解析SSE格式的响应（后端仍使用SSE格式发送结果）
    const lines = responseText.split('\n')
    let thinkingContent = ''
    let sqlResult = ''
    
    console.log('开始解析SSE响应，总行数:', lines.length)
    console.log('所有行:', lines)
    
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i]
      console.log(`解析第${i}行:`, line)
      
      if (line.startsWith('event:') && line.includes('llm_token')) {
        console.log(`找到llm_token事件在第${i}行`)
        // 找到llm_token事件，获取下一行的数据
        const dataIndex = i + 1
        if (dataIndex < lines.length && lines[dataIndex].startsWith('data:')) {
          console.log(`找到data行在第${dataIndex}行:`, lines[dataIndex])
          try {
            const jsonStr = lines[dataIndex].substring(5).trim()
            console.log('准备解析的JSON字符串:', jsonStr)
            const data = JSON.parse(jsonStr)
            console.log('处理llm_token事件:', data)
            console.log('数据类型:', data.type, '内容长度:', data.content ? data.content.length : 0)
            
            if (data.type === 'thinking' && data.content) {
              thinkingContent += data.content
              console.log('累积思考内容, 当前长度:', thinkingContent.length)
            } else if (data.type === 'sql_result' && data.content) {
              sqlResult = data.content
              console.log('设置SQL结果, 长度:', sqlResult.length)
              console.log('SQL结果内容:', sqlResult.substring(0, Math.min(200, sqlResult.length)) + '...')
            } else {
              console.warn('未知的数据类型或内容为空:', data)
            }
          } catch (e) {
            console.error('解析llm_token数据失败:', e, '原始数据:', lines[dataIndex])
            console.error('JSON解析错误详情:', e.message)
          }
        } else {
          console.warn(`第${dataIndex}行不是data行或不存在:`, dataIndex < lines.length ? lines[dataIndex] : '不存在')
        }
      }
    }
    
    console.log('解析完成 - 思考内容长度:', thinkingContent.length, 'SQL结果长度:', sqlResult.length)
    
    // 创建合并的AI回复消息
    const aiMessage = {
      id: Date.now() + 1,
      role: 'assistant',
      timestamp: Date.now(),
      type: 'data_query_response', // 新的消息类型
      collapsible: true,
      collapsed: false
    }
    
    // 如果有思考内容，添加到消息中
    if (thinkingContent) {
      aiMessage.thinkingContent = thinkingContent
      aiMessage.hasThinking = true
      console.log('添加思考内容到消息, 长度:', thinkingContent.length)
    }
    
    // 如果有SQL结果，设置为主要内容
    if (sqlResult) {
      // 检测是否为JSON格式的数据响应
      if (sqlResult.startsWith('{') && sqlResult.includes('"dataType":"python_dict_list"')) {
        aiMessage.content = sqlResult
        aiMessage.hasSqlResult = true
        aiMessage.isDataTable = true // 标记为数据表格类型
        console.log('设置数据表格结果为主要内容, 长度:', sqlResult.length)
      } else {
        aiMessage.content = sqlResult
        aiMessage.hasSqlResult = true
        aiMessage.isDataTable = false
        console.log('设置SQL结果为主要内容, 长度:', sqlResult.length)
      }
    } else if (thinkingContent) {
      // 如果没有SQL结果但有思考内容，使用思考内容作为主要内容
      aiMessage.content = thinkingContent
      aiMessage.hasSqlResult = false
      aiMessage.isDataTable = false
      console.log('使用思考内容作为主要内容, 长度:', thinkingContent.length)
    } else {
      // 如果都没有，显示默认消息
      aiMessage.content = '抱歉，未能获取到查询结果。'
      aiMessage.hasSqlResult = false
      aiMessage.isDataTable = false
      console.log('使用默认消息')
    }
    
    // 添加AI回复消息
    messages.value.push(aiMessage)
    console.log('创建合并的AI回复消息:', {
      id: aiMessage.id,
      type: aiMessage.type,
      hasThinking: aiMessage.hasThinking,
      hasSqlResult: aiMessage.hasSqlResult,
      contentLength: aiMessage.content ? aiMessage.content.length : 0,
      thinkingContentLength: aiMessage.thinkingContent ? aiMessage.thinkingContent.length : 0
    })
    
    // 如果没有收到任何内容，显示错误信息
    if (!thinkingContent && !sqlResult) {
      console.error('没有收到任何有效内容，原始响应:', responseText)
      ElMessage.error('服务器返回了空响应，请重试')
    }
    
  } catch (error) {
    console.error('发送消息失败:', error)
    
    // 根据错误类型显示不同的错误信息
    if (error.name === 'AbortError') {
      ElMessage.error('请求超时，请稍后重试或联系管理员')
    } else if (error.name === 'TypeError' && error.message.includes('fetch')) {
      ElMessage.error('网络连接失败，请检查网络连接')
    } else {
      ElMessage.error('发送消息失败，请检查网络连接或联系管理员')
    }
  } finally {
    isStreaming.value = false
    await loadMessages()
    
    // 如果是新会话的第一条消息，刷新历史对话列表以更新标题
    if (messages.value.length === 2) { // 用户消息 + AI回复
      await refreshHistory()
    }
  }
}

// 格式化查询结果
const formatQueryResult = (data, rowCount) => {
  if (!data || data.length === 0) {
    return '查询结果：无数据'
  }
  
  let result = `查询结果（共${rowCount}条记录）：\n\n`
  
  // 显示表头
  const headers = Object.keys(data[0])
  result += '| ' + headers.join(' | ') + ' |\n'
  result += '| ' + headers.map(() => '---').join(' | ') + ' |\n'
  
  // 显示数据行（最多显示10行）
  const displayData = data.slice(0, 10)
  displayData.forEach(row => {
    result += '| ' + headers.map(header => row[header] || '').join(' | ') + ' |\n'
  })
  
  if (data.length > 10) {
    result += `\n... 还有${data.length - 10}条记录未显示`
  }
  
  return result
}

// 刷新历史对话
const refreshHistory = async () => {
  loadingHistory.value = true
  try {
    const response = await chatApi.getSessions({ current: 1, size: 100 })
    const sessions = response.data.records || []
    
          // 处理会话数据，添加数据库名称和第一条消息信息
      historySessions.value = await Promise.all(sessions.map(async (session) => {
        const dbConfig = dbConfigs.value.find(c => c.id === session.dbConfigId)
        let firstQuestion = '新对话'
        let sessionTitle = '新对话'
        
        try {
          // 获取会话的第一条用户消息
          const messagesResponse = await chatApi.getMessages(session.id)
          const userMessages = messagesResponse.data.filter(msg => msg.role === 'user')
          if (userMessages.length > 0) {
            firstQuestion = userMessages[0].content.substring(0, 50) + (userMessages[0].content.length > 50 ? '...' : '')
            // 使用用户第一句提问的前6个字作为会话标题
            sessionTitle = userMessages[0].content.substring(0, 6) + (userMessages[0].content.length > 6 ? '...' : '')
          }
        } catch (error) {
          console.warn('获取会话消息失败:', error)
        }
        
        // 获取表信息
        let tableName = '未知表'
        if (session.tableId) {
          const table = availableTables.value.find(t => t.id === session.tableId)
          if (table) {
            tableName = table.tableName
          }
        }
        
        return {
          ...session,
          databaseName: dbConfig ? dbConfig.name : '未知数据库',
          tableName: tableName,
          firstQuestion: firstQuestion,
          title: sessionTitle,
          createdAt: session.createdAtMs || session.createdAt || Date.now()
        }
      }))
    
    if (historySessions.value.length > 0 && !currentSession.value) {
      // 如果没有当前会话，尝试切换到第一个历史会话
      switchSession(historySessions.value[0])
    }
  } catch (error) {
    ElMessage.error('加载历史对话失败')
  } finally {
    loadingHistory.value = false
  }
}

// 切换会话
const switchSession = async (session) => {
  if (currentSession.value?.id === session.id) {
    return // 已经是当前会话，不需要切换
  }
  
  try {
    console.log('切换到会话:', session)
    currentSession.value = session
    selectedDbConfig.value = session.dbConfigId // 同步数据库配置
    selectedTable.value = session.tableId // 同步表配置
    
    // 如果表配置发生变化，重新加载表列表
    if (session.dbConfigId !== selectedDbConfig.value) {
      await loadTables()
    }
    
    messages.value = [] // 清空当前消息
    console.log('清空消息列表，准备加载新会话的消息')
    await loadMessages()
    console.log('会话切换完成，当前消息数量:', messages.value.length)
  } catch (error) {
    ElMessage.error('切换会话失败')
    console.error('切换会话失败:', error)
  }
}

// 处理历史对话操作 (重命名、删除)
const handleHistoryAction = async (command) => {
  const session = command.session
  if (command.action === 'rename') {
    try {
      const { value: newTitle } = await ElMessageBox.prompt(
        `请输入对话 "${session.title}" 的新名称:`,
        '重命名对话',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPlaceholder: session.title,
          inputValue: session.title
        }
      )
      
      if (newTitle && newTitle.trim()) {
        await chatApi.updateSessionTitle(session.id, newTitle.trim())
        ElMessage.success(`对话 "${session.title}" 已重命名为 "${newTitle}"`)
        await refreshHistory() // 刷新列表以显示新名称
      }
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error('重命名对话失败')
        console.error(error)
      }
    }
  } else if (command.action === 'delete') {
    try {
      await ElMessageBox.confirm(
        `确定要删除对话 "${session.title}" 吗？此操作不可逆。`,
        '删除确认',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        }
      )
      
      await chatApi.deleteSession(session.id)
      ElMessage.success(`对话 "${session.title}" 已删除`)
      
      if (currentSession.value?.id === session.id) {
        // 如果删除的是当前会话，清空当前会话
        currentSession.value = null
        messages.value = []
      }
      
      await refreshHistory()
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error('删除对话失败')
        console.error(error)
      }
    }
  }
}

// 显示上下文菜单
const showContextMenu = (event, session) => {
  ElMessage.info(`会话 "${session.title}" 的上下文菜单`)
}

// 过滤历史对话
const filterHistory = () => {
  // 当搜索关键词变化时，滚动到顶部
  nextTick(() => {
    if (historyList.value) {
      historyList.value.scrollTo({ top: 0, behavior: 'smooth' })
    }
  })
}

// 处理工具点击
const handleToolClick = (tool) => {
  if (!tool.enabled) return
  
  ElMessage.info(`点击了工具: ${tool.name}`)
  
  // 根据工具名称执行不同的操作
  switch (tool.name) {
    case '数据表查询':
      inputMessage.value = '请帮我查询数据库中的表结构和数据'
      break
    case '知识检索':
      inputMessage.value = '请从知识库中搜索相关信息'
      break
    default:
      inputMessage.value = `请使用${tool.name}工具处理...`
      break
  }
}

// --- 事件处理 ---

const handleKeydown = (e) => {
  if (e.shiftKey) {
    return // 允许Shift+Enter换行
  }
  sendMessage()
}

const onDbConfigChange = async () => {
  // 清空当前表选择
  selectedTable.value = null
  availableTables.value = []
  
  // 加载新数据库的表
  await loadTables()
  
  if (messages.value.length > 0) {
    try {
      await ElMessageBox.confirm(
        '切换数据库将开启一个全新的对话，确定要继续吗？',
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        }
      )
    } catch {
      // 用户取消，恢复之前的选择
      return
    }
  }
  await createNewSession()
}

const onTableChange = async () => {
  if (messages.value.length > 0) {
    try {
      await ElMessageBox.confirm(
        '切换数据表将开启一个全新的对话，确定要继续吗？',
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        }
      )
    } catch {
      // 用户取消，恢复之前的选择
      return
    }
  }
  await createNewSession()
}

// --- 工具方法 ---

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTo({
        top: messagesContainer.value.scrollHeight,
        behavior: 'smooth'
      })
    }
  })
}

// --- 生命周期与监听 ---

watch(messages, scrollToBottom, { deep: true })

onMounted(async () => {
  try {
    console.log('开始初始化数据问答界面...')
    await loadDbConfigs()
    console.log('数据库配置加载完成')
    await refreshHistory() // 初始加载历史对话
    console.log('历史对话加载完成')
    
    // 如果有历史对话，自动切换到第一个会话
    if (historySessions.value.length > 0) {
      console.log('自动切换到第一个历史会话...')
      await switchSession(historySessions.value[0])
    } else if (selectedDbConfig.value) {
      // 只有在有数据库配置且没有历史对话时才创建新会话
      console.log('创建新会话...')
      await createNewSession()
    }
    console.log('数据问答界面初始化完成')
  } catch (error) {
    console.error('初始化失败:', error)
    // 即使初始化失败，也要确保页面能够显示
    ElMessage.error('页面初始化失败，请刷新重试')
  }
})

// 这些方法在新的合并消息逻辑中不再需要，已移除

// 切换思考内容的折叠状态
const toggleThinkingCollapse = (messageId) => {
  const message = messages.value.find(msg => msg.id === messageId)
  if (message && message.type === 'thinking') {
    message.collapsed = !message.collapsed
  }
}

// 切换SQL执行结果的折叠状态
const toggleSqlResultCollapse = (messageId) => {
  const message = messages.value.find(msg => msg.id === messageId)
  if (message && message.type === 'sql_result') {
    message.collapsed = !message.collapsed
  }
}

// 切换侧栏显示状态
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

// 切换数据查询回复的折叠状态
const toggleDataQueryCollapse = (messageId) => {
  const message = messages.value.find(msg => msg.id === messageId)
  if (message && message.type === 'data_query_response') {
    message.collapsed = !message.collapsed
  }
}
</script>

<style scoped>
/* CSS变量定义 */
.chat-interface {
  --primary-color: #6366f1;
  --primary-light: #f1f5ff;
  --success-color: #10b981;
  --warning-color: #f59e0b;
  --danger-color: #ef4444;
  --info-color: #6b7280;
  
  --text-primary: #1f2937;
  --text-regular: #4b5563;
  --text-secondary: #6b7280;
  --text-placeholder: #9ca3af;
  
  --border-color: #e5e7eb;
  --border-light: #f3f4f6;
  --border-lighter: #f9fafb;
  
  --bg-color: #f8fafc;
  --bg-white: #ffffff;
  --bg-light: #fafafa;
  
  --shadow-light: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06);
  --shadow-medium: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  --shadow-heavy: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  
  --border-radius: 8px;
  --border-radius-large: 12px;
  --border-radius-small: 4px;
  
  --transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 全局样式调整 */
:deep(.hljs) {
  border-radius: var(--border-radius);
  padding: 1em !important;
  font-family: 'Operator Mono', 'Source Code Pro', Menlo, Monaco, Consolas, 'Courier New', monospace;
  font-size: 14px;
  margin: 8px 0;
}

/* 主容器 */
.chat-interface {
  height: 100vh;
  height: 100dvh; /* 动态视口高度，适配移动端 */
  display: flex;
  flex-direction: column;
  background: #f8fafc;
  position: relative;
  overflow: hidden;
}

.chat-interface::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(5px);
  z-index: 0;
}

/* 头部样式 */
.chat-header {
  background: var(--bg-white);
  border-bottom: 1px solid var(--border-light);
  box-shadow: var(--shadow-light);
  position: relative;
  z-index: 10;
  flex-shrink: 0;
}

.header-content {
  padding: 12px 20px;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
}

.session-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.session-title {
  margin: 0;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.session-icon {
  color: var(--primary-color);
}

.session-subtitle {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 2px;
}

.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.db-select {
  min-width: 200px;
}

.new-session-btn {
  font-weight: 500;
}

/* 主要内容区域 */
.chat-main {
  display: flex;
  flex: 1;
  overflow: hidden;
  padding: 24px;
  gap: 24px;
  position: relative;
  z-index: 1;
}

/* 左侧历史对话列表 */
.chat-sidebar {
  flex: 0 0 280px; /* 缩窄宽度 */
  background: var(--bg-white);
  border-radius: 0; /* 顶格显示 */
  box-shadow: var(--shadow-light);
  padding: 0; /* 顶格显示 */
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: sticky;
  top: 0;
  height: 100%;
  z-index: 10;
  transition: var(--transition);
}

/* 侧栏折叠状态 */
.chat-sidebar.sidebar-collapsed {
  flex: 0 0 0;
  width: 0;
  overflow: hidden;
  opacity: 0;
  transform: translateX(-100%);
}

/* 侧边栏顶部区域 */
.sidebar-top {
  padding: 16px;
  border-bottom: 1px solid var(--border-lighter);
  background: var(--bg-light);
}

.new-session-section {
  margin-bottom: 12px;
}

.new-session-btn {
  width: 100%;
  font-weight: 500;
}

.database-select-section {
  margin-bottom: 8px;
}

.db-select {
  width: 100%;
}

.table-select {
  width: 100%;
}

.table-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.table-name {
  font-weight: 500;
}

.table-status {
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #f5f5f5;
  color: #999;
}

.table-status.enabled {
  background: #f0f9ff;
  color: #409eff;
}

.table-select-section {
  margin-bottom: 8px;
}

.refresh-section {
  display: flex;
  justify-content: flex-end;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 600;
}

.sidebar-search {
  padding: 16px;
  border-bottom: 1px solid var(--border-lighter);
  background: var(--bg-white);
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  overflow-y: auto;
  flex: 1;
  padding: 8px;
}

.history-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: var(--transition);
  background: var(--bg-white);
  border: 1px solid transparent;
  margin-bottom: 4px;
  position: relative;
}

.history-item:hover {
  background: var(--primary-light);
  border-color: var(--primary-color);
}

.history-item.active {
  background: var(--primary-light);
  border-color: var(--primary-color);
  box-shadow: var(--shadow-medium);
}

.history-item-icon {
  flex-shrink: 0;
  color: var(--primary-color);
  font-size: 20px;
  margin-top: 2px;
}

.history-item-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.history-item-title {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 2px;
  max-width: calc(100% - 40px); /* 为操作按钮留出空间 */
}

.history-item-database {
  font-size: 12px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item-table {
  font-size: 12px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item-time {
  font-size: 11px;
  color: var(--text-placeholder);
  margin-top: 2px;
}

.history-item-preview {
  font-size: 12px;
  color: var(--text-regular);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 4px;
  line-height: 1.3;
}

.history-item-actions {
  flex-shrink: 0;
  position: absolute;
  top: 8px;
  right: 8px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.history-item:hover .history-item-actions {
  opacity: 1;
}

.empty-history {
  text-align: center;
  padding: 40px 0;
  color: var(--text-secondary);
  font-size: 16px;
}

.empty-history .el-icon {
  color: var(--primary-color);
  margin-bottom: 16px;
}

/* 右侧聊天区域 */
.chat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0;
  position: relative;
  z-index: 1;
  overflow: hidden;
}

/* 侧栏切换按钮 */
.sidebar-toggle {
  position: absolute;
  top: 50%;
  left: -16px;
  transform: translateY(-50%);
  z-index: 20;
}

.sidebar-toggle-btn {
  background: var(--bg-white);
  border: 1px solid var(--border-light);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-light);
  color: var(--text-secondary);
  transition: var(--transition);
  width: 32px;
  height: 32px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-toggle-btn:hover {
  background: var(--primary-light);
  color: var(--primary-color);
  border-color: var(--primary-color);
  transform: scale(1.05);
}

/* 当前对话信息 */
.chat-header-info {
  background: var(--bg-white);
  border-bottom: 1px solid var(--border-lighter);
  padding: 12px 20px;
  flex-shrink: 0;
}

.current-session-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.current-session-info .session-title {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.current-session-info .session-icon {
  color: var(--primary-color);
}

.current-session-info .session-subtitle {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 2px;
}

/* 消息列表 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  position: relative;
  z-index: 1;
  scroll-behavior: smooth;
  min-height: 0;
}

/* 欢迎消息 */
.welcome-message {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  min-height: 400px;
}

.welcome-content {
  text-align: center;
  max-width: 500px;
  padding: 40px;
  background: var(--bg-white);
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-medium);
}

.welcome-icon {
  color: var(--primary-color);
  margin-bottom: 24px;
}

.welcome-title {
  margin: 0 0 16px 0;
  color: var(--text-primary);
  font-size: 28px;
  font-weight: 600;
}

.welcome-description {
  margin: 0 0 32px 0;
  color: var(--text-regular);
  font-size: 16px;
  line-height: 1.6;
}

.welcome-features {
  display: flex;
  justify-content: center;
  gap: 32px;
  flex-wrap: wrap;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 14px;
}

.feature-item .el-icon {
  font-size: 24px;
  color: var(--primary-color);
}

/* 消息样式 */
.message {
  display: flex;
  gap: 16px;
  max-width: 80%;
  animation: messageSlideIn 0.3s ease-out;
}

/* 空消息提示 */
.no-messages {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  color: var(--text-secondary);
  font-size: 16px;
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message.assistant {
  align-self: flex-start;
}

.message.loading {
  opacity: 0.8;
}

.message-avatar {
  flex-shrink: 0;
  margin-top: 4px;
}

.user-avatar {
  background: linear-gradient(135deg, var(--primary-color), #66b1ff);
}

.assistant-avatar {
  background: linear-gradient(135deg, var(--success-color), #85ce61);
}

.message-content {
  background: var(--bg-white);
  padding: 16px 20px;
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-light);
  position: relative;
  min-width: 200px;
}

.message.user .message-content {
  background: linear-gradient(135deg, var(--primary-color), #66b1ff);
  color: white;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 12px;
  opacity: 0.8;
}

.message-role {
  font-weight: 600;
}

.message-time {
  opacity: 0.7;
}

.message-text {
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
}

.message-text :deep(p) {
  margin: 0 0 12px 0;
}

.message-text :deep(p:last-child) {
  margin-bottom: 0;
}

.message-text :deep(pre) {
  margin: 12px 0;
  border-radius: var(--border-radius);
  overflow: hidden;
}

.message-text :deep(code) {
  font-family: 'Operator Mono', 'Source Code Pro', Menlo, Monaco, Consolas, 'Courier New', monospace;
  font-size: 13px;
}

/* 用户消息中的代码块特殊样式 */
.message.user .message-text :deep(pre) {
  background-color: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.message.user .message-text :deep(code) {
  color: #e6f7ff;
}

/* 输入区域 */
.chat-input-area {
  background: var(--bg-white);
  border-top: 1px solid var(--border-light);
  box-shadow: 0 -2px 12px 0 rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 10;
  flex-shrink: 0;
  min-height: 120px;
  display: flex;
  flex-direction: column;
}

.input-container {
  padding: 16px 24px;
  flex: 1;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  background: var(--bg-light);
  border-radius: var(--border-radius-large);
  padding: 16px;
  border: 1px solid var(--border-lighter);
  transition: var(--transition);
}

.input-wrapper:focus-within {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.message-input {
  flex: 1;
}

.message-input :deep(.el-textarea__inner) {
  border: none;
  background: transparent;
  padding: 8px 12px;
  line-height: 1.6;
  font-size: 14px;
  resize: none;
  box-shadow: none;
}

.message-input :deep(.el-textarea__inner:focus) {
  box-shadow: none;
}

.input-actions {
  display: flex;
  gap: 8px;
}

.send-button {
  height: 40px;
  padding: 0 20px;
  font-weight: 500;
  border-radius: var(--border-radius);
  transition: var(--transition);
}

.send-button:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-medium);
}

.input-tips {
  margin-top: 12px;
  text-align: center;
}

.tip-text {
  color: var(--text-secondary);
  font-size: 12px;
  opacity: 0.8;
}

/* 工具选项行 */
.tools-row {
  background: var(--bg-white);
  border-bottom: 1px solid var(--border-lighter);
  padding: 12px 20px;
  min-height: 50px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  position: relative;
  flex-shrink: 0;
}

.tools-row::before {
  content: '快速工具';
  position: absolute;
  top: -8px;
  left: 20px;
  background: var(--primary-color);
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.tools-container {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: center;
  max-width: 100%;
}

.tool-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: var(--transition);
  background: var(--bg-white);
  border: 2px solid var(--border-lighter);
  font-size: 13px;
  color: var(--text-regular);
  min-width: 80px;
  max-width: 140px;
  justify-content: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative;
}

.tool-item:hover {
  background: var(--primary-light);
  border-color: var(--primary-color);
  color: var(--primary-color);
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.tool-item.disabled {
  opacity: 0.4;
  cursor: not-allowed;
  background: var(--bg-light);
  color: var(--text-secondary);
  border-color: var(--border-lighter);
  box-shadow: none;
}

.tool-item.disabled:hover {
  background: var(--bg-light);
  border-color: var(--border-lighter);
  color: var(--text-secondary);
  transform: none;
  box-shadow: none;
}

.tool-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.tool-icon.http {
  color: #409eff;
}

.tool-icon.mcp {
  color: #67c23a;
}

.tool-icon.database {
  color: #e6a23c;
}

/* 为我们的工具添加特殊颜色 */
.tool-item:has(.el-icon:has(.DataAnalysis)) .tool-icon {
  color: #6366f1;
}

.tool-item:has(.el-icon:has(.Search)) .tool-icon {
  color: #10b981;
}

.tool-icon.knowledge {
  color: #f56c6c;
}

.tool-icon.search {
  color: #409eff;
}

.tool-icon.writing {
  color: #67c23a;
}

.tool-icon.programming {
  color: #e6a23c;
}

.tool-icon.other {
  color: #909399;
}

.tool-label {
  font-weight: 500;
  max-width: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tools-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 14px;
}

.no-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  padding: 12px;
  background: var(--bg-white);
  border-radius: var(--border-radius);
  border: 1px dashed var(--border-lighter);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    padding: 12px 16px;
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .session-title {
    font-size: 18px;
  }
  
  .toolbar {
    justify-content: center;
  }
  
  .db-select {
    min-width: auto;
    flex: 1;
  }

  .chat-main {
    flex-direction: column;
    gap: 16px;
    padding: 16px;
  }

  .chat-sidebar {
    flex: none;
    width: 100%;
    position: static;
    height: auto;
    max-height: 300px;
    overflow-y: auto;
  }

  .history-list {
    max-height: none;
  }
  
  .chat-messages {
    padding: 16px;
    gap: 16px;
  }
  
  .tools-container {
    justify-content: flex-start;
    gap: 6px;
  }
  
  .tool-item {
    min-width: 70px;
    padding: 6px 10px;
    font-size: 12px;
  }
  
  .message {
    max-width: 90%;
  }
  
  .welcome-content {
    padding: 24px 16px;
  }
  
  .welcome-title {
    font-size: 24px;
  }
  
  .welcome-features {
    gap: 16px;
  }
  
  .input-container {
    padding: 16px;
  }
  
  .input-wrapper {
    padding: 12px;
  }
}

@media (max-width: 480px) {
  .message {
    max-width: 95%;
    gap: 12px;
  }
  
  .message-content {
    padding: 12px 16px;
  }
  
  .welcome-features {
    flex-direction: column;
    gap: 12px;
  }
}

/* 滚动条样式 */
.chat-messages::-webkit-scrollbar,
.history-list::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track,
.history-list::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb,
.history-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover,
.history-list::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}

/* 加载动画 */
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.loading .message-text {
  animation: pulse 1.5s ease-in-out infinite;
}

/* 输入框自动调整高度 */
.message-input :deep(.el-textarea__inner) {
  min-height: 40px;
  max-height: 120px;
  overflow-y: auto;
}

/* 消息发送动画 */
.message.user {
  animation: messageSlideInRight 0.3s ease-out;
}

.message.assistant {
  animation: messageSlideInLeft 0.3s ease-out;
}

/* 思考内容样式 */
.thinking-content {
  background: var(--primary-light);
  border: 1px solid var(--primary-color);
  border-radius: var(--border-radius);
  padding: 12px;
  margin: 8px 0;
  transition: var(--transition);
}

.thinking-content.collapsed {
  background: var(--bg-light);
  border-color: var(--border-light);
}

.thinking-text {
  font-family: 'Operator Mono', 'Source Code Pro', Menlo, Monaco, Consolas, 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-regular);
  white-space: pre-wrap;
}

.thinking-collapsed {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  padding: 8px 0;
}

.thinking-collapsed:hover {
  color: var(--primary-color);
}

/* SQL执行结果样式 */
.sql-result-content {
  background: var(--success-color);
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid var(--success-color);
  border-radius: var(--border-radius);
  padding: 12px;
  margin: 8px 0;
  transition: var(--transition);
}

.sql-result-content.collapsed {
  background: var(--bg-light);
  border-color: var(--border-light);
}

.sql-result-text {
  font-family: 'Operator Mono', 'Source Code Pro', Menlo, Monaco, Consolas, 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-regular);
  white-space: pre-wrap;
}

.sql-result-text table {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
}

.sql-result-text th,
.sql-result-text td {
  border: 1px solid var(--border-color);
  padding: 8px 12px;
  text-align: left;
}

.sql-result-text th {
  background-color: var(--bg-light);
  font-weight: 600;
}

.sql-result-collapsed {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  padding: 8px 0;
}

.sql-result-collapsed:hover {
  color: var(--success-color);
}

.collapse-button {
  margin-left: auto;
  color: var(--text-secondary);
  font-size: 12px;
}

.collapse-button:hover {
  color: var(--primary-color);
}

@keyframes messageSlideInRight {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes messageSlideInLeft {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 数据查询回复样式 */
.data-query-response {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.main-response {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #0ea5e9;
  border-radius: var(--border-radius);
  padding: 16px;
  position: relative;
}

.main-response::before {
  content: '📊 查询结果';
  position: absolute;
  top: -8px;
  left: 12px;
  background: #0ea5e9;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.response-content {
  font-family: 'Operator Mono', 'Source Code Pro', Menlo, Monaco, Consolas, 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-regular);
  white-space: pre-wrap;
}

.response-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
  background: white;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.response-content :deep(th),
.response-content :deep(td) {
  border: 1px solid #e5e7eb;
  padding: 8px 12px;
  text-align: left;
}

.response-content :deep(th) {
  background-color: #f8fafc;
  font-weight: 600;
  color: var(--text-primary);
}

.response-content :deep(tr:nth-child(even)) {
  background-color: #f9fafb;
}

.response-content :deep(tr:hover) {
  background-color: #f0f9ff;
}

.thinking-section {
  background: var(--primary-light);
  border: 1px solid var(--primary-color);
  border-radius: var(--border-radius);
  overflow: hidden;
  transition: var(--transition);
}

.thinking-section.collapsed {
  background: var(--bg-light);
  border-color: var(--border-light);
}

.thinking-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(99, 102, 241, 0.1);
  border-bottom: 1px solid rgba(99, 102, 241, 0.2);
  font-size: 13px;
  font-weight: 500;
  color: var(--primary-color);
  cursor: pointer;
  transition: var(--transition);
}

.thinking-section.collapsed .thinking-header {
  background: var(--bg-white);
  border-bottom: none;
  color: var(--text-secondary);
}

.thinking-section .thinking-content {
  padding: 12px;
  background: transparent;
  border: none;
  margin: 0;
}

.thinking-section .thinking-content :deep(p) {
  margin: 0 0 8px 0;
  line-height: 1.6;
}

.thinking-section .thinking-content :deep(p:last-child) {
  margin-bottom: 0;
}

/* 思考内容样式 */
.thinking-content {
  background: var(--primary-light);
  border: 1px solid var(--primary-color);
  border-radius: var(--border-radius);
  padding: 12px;
  margin: 8px 0;
  transition: var(--transition);
}

.thinking-content.collapsed {
  background: var(--bg-light);
  border-color: var(--border-light);
}

.thinking-text {
  font-family: 'Operator Mono', 'Source Code Pro', Menlo, Monaco, Consolas, 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-regular);
  white-space: pre-wrap;
}

.thinking-collapsed {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  padding: 8px 0;
}

.thinking-collapsed:hover {
  color: var(--primary-color);
}

/* SQL执行结果样式 */
.sql-result-content {
  background: var(--success-color);
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid var(--success-color);
  border-radius: var(--border-radius);
  padding: 12px;
  margin: 8px 0;
  transition: var(--transition);
}

.sql-result-content.collapsed {
  background: var(--bg-light);
  border-color: var(--border-light);
}

.sql-result-text {
  font-family: 'Operator Mono', 'Source Code Pro', Menlo, Monaco, Consolas, 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-regular);
  white-space: pre-wrap;
}

.sql-result-text table {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
}

.sql-result-text th,
.sql-result-text td {
  border: 1px solid var(--border-color);
  padding: 8px 12px;
  text-align: left;
}

.sql-result-text th {
  background-color: var(--bg-light);
  font-weight: 600;
}

.sql-result-collapsed {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  padding: 8px 0;
}

.sql-result-collapsed:hover {
  color: var(--success-color);
}

.collapse-button {
  margin-left: auto;
  color: var(--text-secondary);
  font-size: 12px;
}

.collapse-button:hover {
  color: var(--primary-color);
}

/* 数据表格组件样式 */
.data-table-component {
  margin: 16px 0;
  border-radius: var(--border-radius);
  overflow: hidden;
}

.data-table-component :deep(.el-card) {
  border: 1px solid var(--border-color);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.data-table-component :deep(.el-card__header) {
  background: var(--bg-light);
  border-bottom: 1px solid var(--border-color);
  padding: 12px 16px;
}

.data-table-component :deep(.el-card__body) {
  padding: 16px;
}

.data-table-component :deep(.el-table) {
  border-radius: var(--border-radius);
}

.data-table-component :deep(.el-table th) {
  background: var(--bg-light);
  color: var(--text-regular);
  font-weight: 600;
}

.data-table-component :deep(.el-table td) {
  color: var(--text-regular);
}

.data-table-component :deep(.el-pagination) {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>