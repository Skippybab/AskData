<template>
  <div class="data-question">
    <!-- 顶部栏 -->
    <div class="header">
      <h2>智能数据问答</h2>
      <div class="header-actions">
        <el-button type="text" @click="showSessionList = !showSessionList" icon="el-icon-menu">
          会话列表
        </el-button>
        <el-button type="primary" @click="createSession">新建会话</el-button>
      </div>
    </div>

    <!-- 主体区域 -->
    <div class="main-content">
      <!-- 会话列表侧栏 -->
      <div class="session-sidebar" v-if="showSessionList">
        <div class="session-sidebar-header">
          <h3>历史会话</h3>
          <el-button type="text" @click="showSessionList = false" icon="el-icon-close" />
        </div>
        <div class="session-list">
          <div v-if="sessionLoading" class="session-loading">
            <i class="el-icon-loading"></i>
            加载中...
          </div>
          <div 
            v-for="session in historySessions" 
            :key="session.id"
            class="session-item"
            :class="{ active: currentSession?.id === session.id }"
            @click="switchToSession(session)"
          >
            <div class="session-name">{{ session.sessionName }}</div>
            <div class="session-meta">
              <span class="session-time">{{ formatSessionTime(session.lastMessageAtMs || session.createdAtMs) }}</span>
              <span class="session-count">{{ session.messageCount || 0 }}条</span>
            </div>
          </div>
          <div v-if="historySessions.length === 0 && !sessionLoading" class="empty-sessions">
            暂无历史会话
          </div>
        </div>
      </div>
      
      <!-- 主要内容区域 -->
      <div class="content-area" :class="{ 'with-sidebar': showSessionList }">
        <!-- 会话信息 -->
        <div class="session-info" v-if="currentSession">
          <span>当前会话: {{ currentSession.sessionName }}</span>
          <span v-if="currentDatabase">数据库: {{ currentDatabase.name }}</span>
          <span v-if="selectedTable">表: {{ selectedTable }}</span>
        </div>

        <!-- 消息列表 -->
        <div class="messages-container" ref="messagesContainer">
        <div v-for="(msg, index) in messages" :key="index" class="message-item" :class="msg.role">
          <div class="message-content">
            <!-- 用户消息 -->
            <template v-if="msg.role === 'user'">
              <div class="user-text">{{ msg.content }}</div>
            </template>
            
            <!-- AI消息 -->
            <template v-else>
              <!-- 思考过程 -->
              <div v-if="msg.thinking" class="thinking-section">
                <div class="section-header" @click="msg.showThinking = !msg.showThinking">
                  <i :class="msg.showThinking ? 'el-icon-arrow-down' : 'el-icon-arrow-right'"></i>
                  思考过程
                </div>
                <div v-show="msg.showThinking" class="section-content">
                  {{ msg.thinking }}
                </div>
              </div>
              
              <!-- Python代码 -->
              <div v-if="msg.pythonCode" class="code-section">
                <div class="section-header" @click="msg.showCode = !msg.showCode">
                  <i :class="msg.showCode ? 'el-icon-arrow-down' : 'el-icon-arrow-right'"></i>
                  Python代码
                </div>
                <pre v-show="msg.showCode" class="code-content">{{ msg.pythonCode }}</pre>
              </div>
              
              <!-- SQL语句 -->
              <div v-if="msg.sql" class="sql-section">
                <div class="section-header">
                  SQL查询
                  <el-button size="mini" @click="copySql(msg.sql)">复制</el-button>
                </div>
                <pre class="sql-content">{{ msg.sql }}</pre>
              </div>
              
              <!-- 查询结果 -->
              <div v-if="msg.result" class="result-section">
                <div class="section-header">
                  查询结果
                  <div class="result-actions">
                    <el-button size="mini" @click="copyResult(msg.result)">复制结果</el-button>
                    <el-button size="mini" @click="msg.showFullResult = !msg.showFullResult">
                      {{ msg.showFullResult ? '收起' : '展开全部' }}
                    </el-button>
                  </div>
                </div>
                <div class="result-content" :class="{ 'expanded': msg.showFullResult }">
                  <!-- 表格形式展示 -->
                  <el-table 
                    v-if="msg.resultType === 'table'" 
                    :data="msg.showFullResult ? msg.resultData : msg.resultData.slice(0, 10)" 
                    :max-height="msg.showFullResult ? '600' : '300'"
                    stripe
                    border
                    size="small"
                    style="width: 100%"
                  >
                    <el-table-column 
                      v-for="(col, idx) in msg.resultColumns" 
                      :key="idx"
                      :prop="col"
                      :label="col"
                      min-width="120"
                      show-overflow-tooltip
                    />
                  </el-table>
                  <!-- 文本形式展示 -->
                  <div v-else class="text-result" :class="{ 'collapsed': !msg.showFullResult }">
                    <pre class="result-text">{{ msg.showFullResult ? msg.result : (msg.result.length > 1500 ? msg.result.substring(0, 1500) + '\n...\n[内容已截断，点击展开查看完整内容]' : msg.result) }}</pre>
                  </div>
                  
                  <!-- 显示记录数量提示 -->
                  <div v-if="msg.resultType === 'table' && msg.resultData && msg.resultData.length > 10 && !msg.showFullResult" class="result-summary">
                    <i class="el-icon-info"></i>
                    仅显示前10条记录，共{{ msg.resultData.length }}条。点击"展开全部"查看所有数据。
                  </div>
                  
                  <!-- 数据截断提示 -->
                  <div v-if="msg.truncated" class="truncation-notice">
                    <i class="el-icon-warning"></i>
                    结果数据过大，已截断显示。原始大小: {{ formatBytes(msg.originalSize) }}
                  </div>
                </div>
              </div>
              
              <!-- 错误信息 -->
              <div v-if="msg.error" class="error-section">
                <i class="el-icon-warning"></i>
                {{ msg.error }}
              </div>
            </template>
          </div>
          <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
        </div>
        
        <!-- 加载中 -->
        <div v-if="isLoading" class="loading-indicator">
          <i class="el-icon-loading"></i>
          {{ loadingText }}
        </div>
      </div>

        <!-- 输入区域 -->
        <div class="input-area">
        <el-select 
          v-model="selectedDatabase" 
          placeholder="选择数据库" 
          @change="onDatabaseChange"
          style="width: 250px; margin-right: 10px"
        >
          <el-option 
            v-for="db in databases" 
            :key="db.id" 
            :label="db.name" 
            :value="db.id"
          >
            <span style="float: left">{{ db.name }}</span>
            <span style="float: right; color: #8492a6; font-size: 13px">
              <i v-if="db.connectionStatus === 'checking'" class="el-icon-loading" style="color: #409eff"></i>
              <i v-else-if="db.connectionStatus === 'connected'" class="el-icon-success" style="color: #67c23a"></i>
              <i v-else-if="db.connectionStatus === 'failed'" class="el-icon-error" style="color: #f56c6c"></i>
            </span>
          </el-option>
        </el-select>
        
        <el-select v-model="selectedTable" placeholder="选择表（可选）" clearable style="width: 200px">
          <el-option v-for="table in tables" :key="table.id" :label="table.name" :value="table.id" />
        </el-select>
        
        <el-input
          v-model="inputMessage"
          placeholder="请输入您的问题..."
          @keyup.enter="sendMessage"
          :disabled="isLoading || !currentSession"
        />
        
        <el-button type="primary" @click="sendMessage" :disabled="isLoading || !currentSession">
          发送
        </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'

// 状态管理
const currentSession = ref(null)
const messages = ref([])
const inputMessage = ref('')
const selectedTable = ref(null)
const selectedDatabase = ref(null)
const tables = ref([])
const databases = ref([])
const currentDatabase = ref(null)
const isLoading = ref(false)
const loadingText = ref('正在思考...')

// 会话管理
const historySessions = ref([])
const showSessionList = ref(false)
const sessionLoading = ref(false)

// 加载历史会话列表
const loadHistorySessions = async () => {
  sessionLoading.value = true
  try {
    const response = await fetch('/api/chat/sessions?current=1&size=50')
    const result = await response.json()
    if (result.code === 200 && result.data) {
      historySessions.value = result.data.records || []
      
      // 如果没有当前会话且有历史会话，自动选择最近的一个
      if (!currentSession.value && historySessions.value.length > 0) {
        const recentSession = historySessions.value[0]
        await switchToSession(recentSession)
      }
    }
  } catch (error) {
    console.error('加载历史会话失败', error)
    ElMessage.error('加载历史会话失败')
  } finally {
    sessionLoading.value = false
  }
}

// 切换到指定会话
const switchToSession = async (session) => {
  try {
    currentSession.value = session
    
    // 设置当前数据库
    const database = databases.value.find(db => db.id === session.dbConfigId)
    if (database) {
      selectedDatabase.value = database.id
      currentDatabase.value = database
    }
    
    // 加载会话消息
    await loadSessionMessages(session.id)
    
    // 加载表列表
    if (session.dbConfigId) {
      await loadTables()
    }
    
    // 关闭会话列表
    showSessionList.value = false
    
    ElMessage.success('切换到会话: ' + session.sessionName)
  } catch (error) {
    console.error('切换会话失败', error)
    ElMessage.error('切换会话失败')
  }
}

// 加载会话消息
const loadSessionMessages = async (sessionId) => {
  try {
    const response = await fetch(`/api/chat/sessions/${sessionId}/messages`)
    const result = await response.json()
    if (result.code === 200 && result.data) {
      // 转换后端消息格式为前端格式
      messages.value = result.data.map(msg => convertBackendMessage(msg))
      scrollToBottom()
    }
  } catch (error) {
    console.error('加载会话消息失败', error)
    ElMessage.error('加载会话消息失败')
  }
}

// 转换后端消息格式为前端格式
const convertBackendMessage = (backendMsg) => {
  const frontendMsg = {
    role: backendMsg.role,
    content: backendMsg.content,
    timestamp: new Date(backendMsg.createdAtMs),
    showThinking: false,
    showCode: false,
    showFullResult: false,
    truncated: false,
    originalSize: 0
  }
  
  // 如果是AI消息，解析额外的字段
  if (backendMsg.role === 'assistant') {
    frontendMsg.thinking = backendMsg.thinkingContent || ''
    frontendMsg.pythonCode = backendMsg.pythonCode || ''
    frontendMsg.sql = extractSqlFromContent(backendMsg.content)
    frontendMsg.result = backendMsg.executionResult || ''
    frontendMsg.error = backendMsg.errorMessage || ''
    
    // 尝试解析结果为表格数据
    if (frontendMsg.result) {
      if (tryParseAsTable(frontendMsg.result)) {
        frontendMsg.resultType = 'table'
      } else {
        frontendMsg.resultType = 'text'
      }
    }
  }
  
  return frontendMsg
}

// 从内容中提取SQL（简化实现）
const extractSqlFromContent = (content) => {
  if (!content) return ''
  const sqlMatch = content.match(/```sql\s*([\s\S]*?)\s*```/i)
  return sqlMatch ? sqlMatch[1].trim() : ''
}

// 格式化会话时间
const formatSessionTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diffMs = now - date
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
  
  if (diffDays === 0) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else if (diffDays === 1) {
    return '昨天'
  } else if (diffDays < 7) {
    return `${diffDays}天前`
  } else {
    return date.toLocaleDateString('zh-CN')
  }
}

// 加载数据库配置列表
const loadDatabases = async () => {
  try {
    const response = await fetch('/api/db/configs/enabled')
    const result = await response.json()
    if (result.code === 200) {
      databases.value = result.data || []
      
      // 为每个数据库检查连接状态
      for (let db of databases.value) {
        db.connectionStatus = 'checking'
        checkDatabaseConnection(db)
      }
      
      // 如果有可用数据库，自动选择第一个连接正常的
      if (databases.value.length > 0 && !selectedDatabase.value) {
        // 等待连接检查完成后选择第一个可用的数据库
        setTimeout(() => {
          const availableDb = databases.value.find(db => db.connectionStatus === 'connected')
          if (availableDb) {
            selectedDatabase.value = availableDb.id
          } else if (databases.value.length > 0) {
            // 如果没有连接成功的，也选择第一个（用户可以手动选择其他的）
            selectedDatabase.value = databases.value[0].id
          }
        }, 2000)
      }
    }
  } catch (error) {
    console.error('加载数据库列表失败', error)
    ElMessage.error('加载数据库列表失败')
  }
}

// 检查数据库连接状态
const checkDatabaseConnection = async (database) => {
  try {
    const response = await fetch(`/api/db/config/${database.id}/verify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    })
    const result = await response.json()
    
    if (result.code === 200 && result.data === true) {
      database.connectionStatus = 'connected'
    } else {
      database.connectionStatus = 'failed'
      console.warn(`数据库连接失败: ${database.name}`, result.message)
    }
  } catch (error) {
    database.connectionStatus = 'failed'
    console.error(`检查数据库连接失败: ${database.name}`, error)
  }
}

// 创建新会话
const createSession = async () => {
  if (!selectedDatabase.value) {
    ElMessage.warning('请先选择数据库配置')
    return
  }
  
  try {
    const response = await fetch('/api/chat/sessions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionName: '新对话 ' + new Date().toLocaleString(),
        dbConfigId: selectedDatabase.value
      })
    })
    
    const result = await response.json()
    if (result.code === 200) {
      currentSession.value = result.data
      messages.value = []
      ElMessage.success('会话创建成功')
      
      // 设置当前数据库
      currentDatabase.value = databases.value.find(db => db.id === selectedDatabase.value)
      
      // 刷新历史会话列表
      await loadHistorySessions()
      
      // 加载表列表
      await loadTables()
    }
  } catch (error) {
    ElMessage.error('创建会话失败')
  }
}

// 当数据库选择改变时，重新创建会话
const onDatabaseChange = () => {
  if (selectedDatabase.value) {
    createSession()
  }
}

// 加载表列表
const loadTables = async () => {
  if (!currentSession.value) return
  
  try {
    const response = await fetch(`/api/table-info/list?dbConfigId=${currentSession.value.dbConfigId}`)
    const result = await response.json()
    if (result.code === 200) {
      tables.value = result.data
    }
  } catch (error) {
    console.error('加载表列表失败', error)
  }
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || isLoading.value || !currentSession.value) {
    return
  }
  
  const question = inputMessage.value.trim()
  inputMessage.value = ''
  
  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: question,
    timestamp: new Date()
  })
  
  // 添加AI消息占位
  const aiMessage = {
    role: 'assistant',
    content: '',
    thinking: '',
    pythonCode: '',
    sql: '',
    result: null,
    resultType: null,
    resultData: null,
    resultColumns: null,
    error: null,
    timestamp: new Date(),
    showThinking: false,
    showCode: false,
    showFullResult: false,
    truncated: false,
    originalSize: 0
  }
  messages.value.push(aiMessage)
  
  scrollToBottom()
  isLoading.value = true
  let loadingInterval = updateLoadingText()
  
  try {
    // 调用数据问答接口，处理阻塞式响应
    const response = await fetch('/api/data-question/ask', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        sessionId: currentSession.value.id,
        question: question,
        dbConfigId: currentSession.value.dbConfigId,
        tableId: selectedTable.value || null
      })
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    // 读取JSON响应
    const result = await response.json()
    console.log('收到响应:', result)
    
    if (result.code === 200 && result.data) {
      const data = result.data
      
      // 更新AI消息内容
      aiMessage.thinking = data.thinking || ''
      aiMessage.pythonCode = data.pythonCode || ''
      aiMessage.sql = data.sql || ''
      
      // 处理查询结果
      if (data.result) {
        aiMessage.result = data.result
        aiMessage.resultType = data.resultType || 'text'
        
        // 如果是表格数据，解析为数组
        if (data.resultType === 'table' && typeof data.result === 'string') {
          try {
            // 提取JSON数组
            const match = data.result.match(/\[{.*?}\]/s)
            if (match) {
              const jsonStr = match[0]
                .replace(/'/g, '"')
                .replace(/None/g, 'null')
                .replace(/True/g, 'true')
                .replace(/False/g, 'false')
              
              const tableData = JSON.parse(jsonStr)
              aiMessage.resultData = tableData
              aiMessage.resultColumns = tableData.length > 0 ? Object.keys(tableData[0]) : []
              aiMessage.content = `查询成功，返回 ${tableData.length} 条记录`
            } else {
              // 文本结果
              aiMessage.content = data.result
            }
          } catch (parseError) {
            console.log('解析JSON失败，使用原始文本:', parseError)
            aiMessage.content = data.result
          }
        } else {
          // 非表格类型结果，直接显示
          aiMessage.content = data.result
        }
      } else {
        // 如果没有结果，根据其他内容设置提示
        if (data.thinking) {
          aiMessage.content = '分析完成'
        } else {
          aiMessage.content = '处理完成，但未返回查询结果'
        }
      }
      
    } else {
      // 处理错误响应
      const errorMsg = result.message || result.error || '查询失败'
      aiMessage.error = errorMsg
      aiMessage.content = ''
    }
    
  } catch (error) {
    console.error('请求失败:', error)
    aiMessage.error = error.message || '查询失败，请重试'
  } finally {
    // 确保清理加载状态
    isLoading.value = false
    loadingText.value = '正在思考...'
    
    // 清除加载文本定时器
    if (loadingInterval) {
      clearInterval(loadingInterval)
    }
  }
}

// SSE相关函数已删除，现在使用阻塞式通信

// 复制结果
const copyResult = (result) => {
  navigator.clipboard.writeText(result)
  ElMessage.success('结果已复制')
}

// 复制SQL
const copySql = (sql) => {
  navigator.clipboard.writeText(sql)
  ElMessage.success('SQL已复制')
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    const container = messagesContainer.value
    if (container) {
      container.scrollTop = container.scrollHeight
    }
  })
}

// 更新加载文本
const updateLoadingText = () => {
  const texts = ['正在思考...', '分析问题中...', '生成SQL...', '执行查询...']
  let index = 0
  const interval = setInterval(() => {
    if (!isLoading.value) {
      clearInterval(interval)
      return
    }
    loadingText.value = texts[index % texts.length]
    index++
  }, 2000)
  return interval // 返回定时器ID，以便清理
}

// 格式化时间
const formatTime = (date) => {
  return new Date(date).toLocaleTimeString('zh-CN')
}

// 格式化文件大小
const formatBytes = (bytes) => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// DOM引用
const messagesContainer = ref(null)

// 初始化
onMounted(async () => {
  // 先加载数据库列表和历史会话
  await loadDatabases()
  await loadHistorySessions()
  
  // 如果没有当前会话且没有历史会话，且有可用数据库，自动创建会话
  if (!currentSession.value && historySessions.value.length === 0 && selectedDatabase.value) {
    await createSession()
  }
})
</script>

<style scoped lang="scss">
.data-question {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f6fa;
  
  .header {
    padding: 15px 20px;
    background: white;
    border-bottom: 1px solid #e8e8e8;
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    h2 {
      margin: 0;
      font-size: 20px;
    }
  }
  
  .main-content {
    flex: 1;
    display: flex;
    overflow: hidden;
    
    .session-sidebar {
      width: 300px;
      background: white;
      border-right: 1px solid #e8e8e8;
      display: flex;
      flex-direction: column;
      
      .session-sidebar-header {
        padding: 15px 20px;
        border-bottom: 1px solid #e8e8e8;
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        h3 {
          margin: 0;
          font-size: 16px;
          color: #333;
        }
      }
      
      .session-list {
        flex: 1;
        overflow-y: auto;
        
        .session-loading {
          padding: 20px;
          text-align: center;
          color: #666;
          
          i {
            margin-right: 8px;
          }
        }
        
        .session-item {
          padding: 15px 20px;
          border-bottom: 1px solid #f0f0f0;
          cursor: pointer;
          transition: background-color 0.3s;
          
          &:hover {
            background: #f8f9fa;
          }
          
          &.active {
            background: #e8eaf6;
            border-right: 3px solid #667eea;
          }
          
          .session-name {
            font-size: 14px;
            color: #333;
            margin-bottom: 5px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
          
          .session-meta {
            display: flex;
            justify-content: space-between;
            font-size: 12px;
            color: #999;
            
            .session-time {
              flex: 1;
            }
            
            .session-count {
              margin-left: 10px;
            }
          }
        }
        
        .empty-sessions {
          padding: 50px 20px;
          text-align: center;
          color: #999;
        }
      }
    }
    
    .content-area {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      
      &.with-sidebar {
        border-left: 1px solid #e8e8e8;
      }
    }
    
    .session-info {
      padding: 10px 20px;
      background: #fafafa;
      border-bottom: 1px solid #e8e8e8;
      font-size: 14px;
      color: #666;
      
      span {
        margin-right: 20px;
      }
    }
    
    .messages-container {
      flex: 1;
      overflow-y: auto;
      padding: 20px;
      max-height: calc(100vh - 300px);
      min-height: 400px;
      
      .message-item {
        margin-bottom: 20px;
        
        &.user {
          .message-content {
            text-align: right;
            
            .user-text {
              display: inline-block;
              background: #667eea;
              color: white;
              padding: 10px 15px;
              border-radius: 10px;
              max-width: 70%;
            }
          }
        }
        
        &.assistant {
          .message-content {
            .thinking-section,
            .code-section,
            .sql-section,
            .result-section,
            .error-section {
              margin-bottom: 10px;
              border: 1px solid #e8e8e8;
              border-radius: 5px;
              overflow: hidden;
              
              .section-header {
                padding: 8px 12px;
                background: #f8f9fa;
                font-size: 14px;
                font-weight: 500;
                cursor: pointer;
                display: flex;
                justify-content: space-between;
                align-items: center;
                
                i {
                  margin-right: 5px;
                }
              }
              
              .section-content,
              .code-content,
              .sql-content,
              .result-content {
                padding: 12px;
                font-size: 14px;
              }
              
              .code-content,
              .sql-content {
                background: #2d2d2d;
                color: #f8f8f2;
                font-family: 'Monaco', monospace;
                overflow-x: auto;
              }
            }
            
            .error-section {
              background: #fee;
              color: #c00;
              padding: 10px;
              
              i {
                margin-right: 5px;
              }
            }
            
            // 结果展示特殊样式
            .result-section {
              .section-header {
                .result-actions {
                  display: flex;
                  gap: 8px;
                }
              }
              
              .result-content {
                max-height: 400px;
                overflow: auto;
                
                &.expanded {
                  max-height: none;
                }
                
                .text-result {
                  &.collapsed {
                    max-height: 300px;
                    overflow: hidden;
                    position: relative;
                    
                    &::after {
                      content: '';
                      position: absolute;
                      bottom: 0;
                      left: 0;
                      right: 0;
                      height: 50px;
                      background: linear-gradient(transparent, rgba(255, 255, 255, 0.9));
                    }
                  }
                  
                  .result-text {
                    margin: 0;
                    font-family: 'Monaco', 'Consolas', monospace;
                    font-size: 13px;
                    line-height: 1.5;
                    white-space: pre-wrap;
                    word-break: break-all;
                    background: #f8f9fa;
                    padding: 12px;
                    border-radius: 4px;
                    max-width: 100%;
                    overflow-x: auto;
                  }
                }
                
                .result-summary {
                  background: #e8f4fd;
                  color: #1890ff;
                  padding: 8px 12px;
                  margin-top: 8px;
                  border-radius: 4px;
                  border: 1px solid #b3d8ff;
                  font-size: 13px;
                  
                  i {
                    margin-right: 8px;
                    color: #1890ff;
                  }
                }
                
                .truncation-notice {
                  background: #fff3cd;
                  color: #856404;
                  padding: 8px 12px;
                  margin-top: 8px;
                  border-radius: 4px;
                  border: 1px solid #ffeaa7;
                  
                  i {
                    margin-right: 8px;
                    color: #f39c12;
                  }
                }
              }
              
              // 表格样式优化
              .el-table {
                border-radius: 4px;
                
                .el-table__header-wrapper {
                  .el-table__header {
                    th {
                      background: #f8f9fa;
                      color: #495057;
                      font-weight: 600;
                    }
                  }
                }
                
                .el-table__body-wrapper {
                  .el-table__body {
                    tr {
                      &:hover > td {
                        background: #f5f5f5;
                      }
                    }
                  }
                }
              }
            }
          }
        }
        
        .message-time {
          font-size: 12px;
          color: #999;
          margin-top: 5px;
        }
      }
      
      .loading-indicator {
        text-align: center;
        color: #666;
        
        i {
          margin-right: 5px;
        }
      }
    }
    
    .input-area {
      padding: 15px 20px;
      background: white;
      border-top: 1px solid #e8e8e8;
      display: flex;
      gap: 10px;
      align-items: center;
      
      .el-input {
        flex: 1;
      }
    }
  }
}
</style>