<template>
  <div class="chat-interface-v2">
    <!-- 顶部栏 -->
    <div class="chat-header">
      <div class="header-left">
        <h2 class="chat-title">
          <i class="el-icon-chat-dot-round"></i>
          智能数据问答
        </h2>
      </div>
      
      <div class="header-center">
        <!-- 数据源选择器 -->
        <div class="data-selector">
          <el-select 
            v-model="selectedDatabase" 
            placeholder="选择数据库"
            @change="onDatabaseChange"
            class="db-selector"
            clearable
          >
            <el-option
              v-for="db in databases"
              :key="db.id"
              :label="db.name"
              :value="db.id"
            >
              <span class="option-label">
                <i class="el-icon-coin"></i>
                {{ db.name }}
                <span class="db-info">{{ db.host }}:{{ db.port }}</span>
              </span>
            </el-option>
          </el-select>
          
          <i class="el-icon-arrow-right separator"></i>
          
          <el-select 
            v-model="selectedTable" 
            placeholder="选择数据表（可选）"
            :disabled="!selectedDatabase"
            @change="onTableChange"
            class="table-selector"
            clearable
            filterable
          >
            <el-option
              label="全部表"
              :value="null"
            >
              <span class="option-label">
                <i class="el-icon-s-grid"></i>
                全部表
                <span class="table-comment">可访问所有表</span>
              </span>
            </el-option>
            <el-option
              v-for="table in availableTables"
              :key="table.id"
              :label="table.tableName"
              :value="table.id"
            >
              <span class="option-label">
                <i class="el-icon-grid"></i>
                {{ table.tableName }}
                <span class="table-comment" v-if="table.tableComment">
                  ({{ table.tableComment }})
                </span>
              </span>
            </el-option>
          </el-select>
          
          <!-- 当前选择状态提示 -->
          <el-tag 
            v-if="selectedDatabase && selectedTable"
            type="success"
            size="small"
            class="selection-tag"
          >
            {{ getCurrentTableName() }}
          </el-tag>
        </div>
      </div>
      
      <div class="header-right">
        <el-button 
          type="text" 
          @click="showHistory = true"
          class="history-btn"
        >
          <i class="el-icon-time"></i>
          历史记录
        </el-button>
        <el-button 
          type="text" 
          @click="clearChat"
          class="clear-btn"
        >
          <i class="el-icon-delete"></i>
          清空对话
        </el-button>
      </div>
    </div>

    <!-- 对话区域 -->
    <div class="chat-container" ref="chatContainer">
      <div class="messages-wrapper">
        <!-- 欢迎消息 -->
        <div v-if="messages.length === 0" class="welcome-message">
          <div class="welcome-icon">
            <i class="el-icon-magic-stick"></i>
          </div>
          <h3>欢迎使用智能数据问答</h3>
          <p>选择数据库和表后，用自然语言提问即可查询数据</p>
          
          <div class="quick-examples">
            <h4>示例问题：</h4>
            <div class="example-list">
              <div 
                class="example-item"
                v-for="example in quickExamples"
                :key="example"
                @click="askQuestion(example)"
              >
                <i class="el-icon-search"></i>
                {{ example }}
              </div>
            </div>
          </div>
        </div>

        <!-- 消息列表 -->
        <div 
          v-for="(message, index) in messages" 
          :key="index"
          class="message-item"
          :class="[message.role]"
        >
          <div class="message-avatar">
            <i :class="message.role === 'user' ? 'el-icon-user' : 'el-icon-robot'"></i>
          </div>
          
          <div class="message-content">
            <!-- 用户消息 -->
            <div v-if="message.role === 'user'" class="user-message">
              {{ message.content }}
            </div>
            
            <!-- AI消息 -->
            <div v-else class="ai-message">
              <!-- 思考过程 -->
              <div v-if="message.thinking" class="thinking-block">
                <div class="thinking-header" @click="message.thinkingExpanded = !message.thinkingExpanded">
                  <i :class="message.thinkingExpanded ? 'el-icon-arrow-down' : 'el-icon-arrow-right'"></i>
                  <span>思考过程</span>
                </div>
                <div v-show="message.thinkingExpanded" class="thinking-content">
                  {{ message.thinking }}
                </div>
              </div>
              
              <!-- Python代码 -->
              <div v-if="message.pythonCode" class="code-block">
                <div class="code-header">
                  <span>Python代码</span>
                  <el-button 
                    type="text" 
                    size="mini"
                    @click="copyCode(message.pythonCode)"
                  >
                    <i class="el-icon-copy-document"></i>
                    复制
                  </el-button>
                </div>
                <pre class="code-content"><code>{{ message.pythonCode }}</code></pre>
              </div>
              
              <!-- SQL查询 -->
              <div v-if="message.sql" class="sql-block">
                <div class="sql-header">
                  <span>SQL查询</span>
                  <el-button 
                    type="text" 
                    size="mini"
                    @click="copyCode(message.sql)"
                  >
                    <i class="el-icon-copy-document"></i>
                    复制
                  </el-button>
                </div>
                <pre class="sql-content"><code>{{ message.sql }}</code></pre>
              </div>
              
              <!-- 查询结果 -->
              <div v-if="message.result" class="result-block">
                <div class="result-header">
                  <span>查询结果</span>
                  <span class="result-info" v-if="message.resultInfo">
                    {{ message.resultInfo }}
                  </span>
                </div>
                
                <!-- 表格形式展示 -->
                <div v-if="message.resultType === 'table'" class="result-table">
                  <el-table 
                    :data="message.result" 
                    style="width: 100%"
                    max-height="400"
                    size="small"
                  >
                    <el-table-column
                      v-for="(value, key) in message.result[0]"
                      :key="key"
                      :prop="key"
                      :label="formatColumnLabel(key)"
                      min-width="100"
                    />
                  </el-table>
                </div>
                
                <!-- 单值展示 -->
                <div v-else-if="message.resultType === 'single'" class="result-single">
                  <div class="single-value">
                    <span class="value-label">{{ message.variableName || '结果' }}:</span>
                    <span class="value-content">{{ message.result }}</span>
                  </div>
                </div>
                
                <!-- 文本展示 -->
                <div v-else class="result-text">
                  <pre>{{ formatResult(message.result) }}</pre>
                </div>
                
                <!-- 导出按钮 -->
                <div class="result-actions" v-if="message.result">
                  <el-button 
                    size="mini"
                    @click="exportResult(message.result, 'csv')"
                  >
                    <i class="el-icon-download"></i>
                    导出CSV
                  </el-button>
                  <el-button 
                    size="mini"
                    @click="exportResult(message.result, 'json')"
                  >
                    <i class="el-icon-download"></i>
                    导出JSON
                  </el-button>
                </div>
              </div>
              
              <!-- 错误信息 -->
              <div v-if="message.error" class="error-block">
                <i class="el-icon-warning"></i>
                {{ message.error }}
              </div>
            </div>
          </div>
          
          <div class="message-time">
            {{ formatTime(message.timestamp) }}
          </div>
        </div>
        
        <!-- 加载中提示 -->
        <div v-if="isLoading" class="loading-message">
          <div class="loading-avatar">
            <i class="el-icon-loading"></i>
          </div>
          <div class="loading-content">
            <span class="loading-text">{{ loadingText }}</span>
            <span class="loading-dots">...</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input">
      <div class="input-wrapper">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="2"
          placeholder="请输入您的问题，例如：查询本月销售额最高的10个产品"
          @keydown.enter.ctrl="sendMessage"
          :disabled="!selectedTable || isLoading"
          class="message-input"
        />
        
        <div class="input-actions">
          <el-tooltip content="Ctrl+Enter 发送" placement="top">
            <el-button 
              type="primary"
              @click="sendMessage"
              :disabled="!inputMessage.trim() || !selectedTable || isLoading"
              :loading="isLoading"
              class="send-btn"
            >
              <i v-if="!isLoading" class="el-icon-s-promotion"></i>
              发送
            </el-button>
          </el-tooltip>
        </div>
      </div>
      
      <div class="input-tips">
        <span v-if="!selectedDatabase">请先选择数据库</span>
        <span v-else-if="!selectedTable">请选择要查询的数据表</span>
        <span v-else>当前表: {{ currentTableInfo }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'

// 数据
const databases = ref([])
const availableTables = ref([])
const messages = ref([])
const inputMessage = ref('')

// 选择状态
const selectedDatabase = ref(null)
const selectedTable = ref(null)
const currentSession = ref(null)

// UI状态
const isLoading = ref(false)
const loadingText = ref('正在思考')
const showHistory = ref(false)

// 示例问题
const quickExamples = ref([
  '查询销售额最高的10个产品',
  '统计本月的总收入',
  '分析各地区的销售占比',
  '查看最近7天的订单数量趋势',
  '计算产品的平均利润率'
])

// 计算属性
const currentTableInfo = computed(() => {
  if (!selectedTable.value) return ''
  const table = availableTables.value.find(t => t.id === selectedTable.value)
  return table ? `${table.tableName} ${table.tableComment ? `(${table.tableComment})` : ''}` : ''
})

// 生命周期
onMounted(() => {
  loadDatabases()
  loadSessionFromStorage()
})

// 监听数据库变化
watch(selectedDatabase, async (newVal) => {
  if (newVal) {
    await loadTables(newVal)
  } else {
    availableTables.value = []
    selectedTable.value = null
  }
})

// 方法
const loadDatabases = async () => {
  try {
    const response = await fetch('/api/db/list', {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    const result = await response.json()
    if (result.code === 200) {
      databases.value = result.data.filter(db => db.status === 1)
      
      // 自动选择第一个数据库
      if (databases.value.length > 0 && !selectedDatabase.value) {
        selectedDatabase.value = databases.value[0].id
      }
    }
  } catch (error) {
    console.error('加载数据库失败:', error)
    ElMessage.error('加载数据库失败')
  }
}

const loadTables = async (dbId) => {
  try {
    const response = await fetch(`/api/db/schema/tables?dbConfigId=${dbId}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    const result = await response.json()
    if (result.code === 200) {
      availableTables.value = result.data.filter(table => table.enabled !== false)
    }
  } catch (error) {
    console.error('加载表列表失败:', error)
    ElMessage.error('加载表列表失败')
  }
}

const onDatabaseChange = () => {
  // 清空表选择
  selectedTable.value = null
  // 保存到本地存储
  if (selectedDatabase.value) {
    localStorage.setItem('selectedDatabase', selectedDatabase.value)
  }
}

const onTableChange = () => {
  // 保存到本地存储
  localStorage.setItem('selectedTable', selectedTable.value || '')
  
  // 显示切换提示
  if (selectedTable.value) {
    const table = availableTables.value.find(t => t.id === selectedTable.value)
    ElMessage.success(`已切换到表: ${table?.tableName}`)
  } else {
    ElMessage.info('已切换到全部表模式')
  }
  
  // 创建或切换会话
  createOrSwitchSession()
}

// 获取当前选中的表名
const getCurrentTableName = () => {
  if (!selectedTable.value) return '全部表'
  const table = availableTables.value.find(t => t.id === selectedTable.value)
  return table?.tableName || '未知表'
}

const createOrSwitchSession = async () => {
  try {
    const response = await fetch('/api/chat/session/create', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        dbConfigId: selectedDatabase.value,
        tableId: selectedTable.value,
        sessionName: `会话-${new Date().toLocaleString()}`
      })
    })
    const result = await response.json()
    if (result.code === 200) {
      currentSession.value = result.data
      // 清空当前消息
      messages.value = []
    }
  } catch (error) {
    console.error('创建会话失败:', error)
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || !selectedDatabase.value || isLoading.value) {
    if (!selectedDatabase.value) {
      ElMessage.warning('请先选择数据库')
    }
    return
  }
  
  const userMessage = inputMessage.value.trim()
  inputMessage.value = ''
  
  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: userMessage,
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
    error: null,
    timestamp: new Date(),
    thinkingExpanded: false
  }
  messages.value.push(aiMessage)
  
  // 滚动到底部
  scrollToBottom()
  
  // 发送请求
  isLoading.value = true
  updateLoadingText()
  
  try {
    const response = await fetch('/api/chat/query', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        sessionId: currentSession.value?.id,
        question: userMessage,
        dbConfigId: selectedDatabase.value,
        tableId: selectedTable.value
      })
    })
    
    if (!response.ok) {
      throw new Error('请求失败')
    }
    
    // 处理SSE流式响应
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      
      const chunk = decoder.decode(value)
      const lines = chunk.split('\n')
      
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.substring(5).trim()
          if (data) {
            try {
              const event = JSON.parse(data)
              handleStreamEvent(event, aiMessage)
            } catch (e) {
              console.error('解析事件失败:', e)
            }
          }
        }
      }
    }
    
  } catch (error) {
    console.error('查询失败:', error)
    aiMessage.error = '查询失败: ' + error.message
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}

const handleStreamEvent = (event, aiMessage) => {
  switch (event.type) {
    case 'thinking':
      aiMessage.thinking += event.content
      break
    case 'python_code':
      aiMessage.pythonCode += event.content
      break
    case 'sql':
      aiMessage.sql += event.content
      break
    case 'sql_result':
    case 'python_result':
      // 解析结果
      if (event.parsedData) {
        try {
          const data = JSON.parse(event.parsedData)
          aiMessage.result = data
          
          // 判断结果类型
          if (Array.isArray(data) && data.length > 0) {
            aiMessage.resultType = 'table'
            aiMessage.resultInfo = `共 ${data.length} 条记录`
          } else if (typeof data === 'object' && !Array.isArray(data)) {
            aiMessage.resultType = 'single'
            aiMessage.variableName = event.variableName
          } else {
            aiMessage.resultType = 'text'
          }
        } catch (e) {
          aiMessage.result = event.content
          aiMessage.resultType = 'text'
        }
      } else {
        aiMessage.result = event.content
        aiMessage.resultType = 'text'
      }
      break
    case 'error':
      aiMessage.error = event.content
      break
  }
  
  // 滚动到底部
  nextTick(() => scrollToBottom())
}

const updateLoadingText = () => {
  const texts = ['正在思考', '分析问题中', '生成查询', '执行中', '处理结果']
  let index = 0
  
  const interval = setInterval(() => {
    if (!isLoading.value) {
      clearInterval(interval)
      return
    }
    loadingText.value = texts[index % texts.length]
    index++
  }, 1000)
}

const askQuestion = (question) => {
  inputMessage.value = question
  sendMessage()
}

const copyCode = (code) => {
  navigator.clipboard.writeText(code)
  ElMessage.success('已复制到剪贴板')
}

const formatResult = (result) => {
  if (typeof result === 'string') {
    return result
  }
  return JSON.stringify(result, null, 2)
}

const formatColumnLabel = (key) => {
  // 将下划线命名转换为更友好的显示
  return key.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())
}

const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const exportResult = (data, format) => {
  let content = ''
  let filename = ''
  let mimeType = ''
  
  if (format === 'csv') {
    // 转换为CSV
    if (Array.isArray(data) && data.length > 0) {
      const headers = Object.keys(data[0])
      content = headers.join(',') + '\n'
      data.forEach(row => {
        content += headers.map(h => row[h] || '').join(',') + '\n'
      })
    } else {
      content = typeof data === 'object' ? JSON.stringify(data) : data
    }
    filename = `查询结果_${new Date().getTime()}.csv`
    mimeType = 'text/csv'
  } else {
    // JSON格式
    content = JSON.stringify(data, null, 2)
    filename = `查询结果_${new Date().getTime()}.json`
    mimeType = 'application/json'
  }
  
  // 创建下载
  const blob = new Blob([content], { type: mimeType })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
  
  ElMessage.success('导出成功')
}

const clearChat = () => {
  messages.value = []
  ElMessage.success('对话已清空')
}

const scrollToBottom = () => {
  nextTick(() => {
    const container = document.querySelector('.chat-container')
    if (container) {
      container.scrollTop = container.scrollHeight
    }
  })
}

const loadSessionFromStorage = () => {
  const savedDb = localStorage.getItem('selectedDatabase')
  const savedTable = localStorage.getItem('selectedTable')
  
  if (savedDb) {
    selectedDatabase.value = parseInt(savedDb)
  }
  if (savedTable) {
    selectedTable.value = parseInt(savedTable)
  }
}
</script>

<style scoped lang="scss">
.chat-interface-v2 {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  
  .chat-header {
    background: white;
    padding: 15px 20px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
    
    .header-left {
      .chat-title {
        margin: 0;
        font-size: 20px;
        color: #333;
        display: flex;
        align-items: center;
        
        i {
          margin-right: 8px;
          color: #667eea;
        }
      }
    }
    
    .header-center {
      flex: 1;
      display: flex;
      justify-content: center;
      
      .data-selector {
        display: flex;
        align-items: center;
        gap: 10px;
        
        .db-selector,
        .table-selector {
          width: 200px;
        }
        
        .separator {
          color: #999;
          font-size: 18px;
        }
        
        .option-label {
          display: flex;
          align-items: center;
          
          i {
            margin-right: 5px;
            color: #667eea;
          }
          
          .table-comment {
            margin-left: 5px;
            color: #999;
            font-size: 12px;
          }
        }
      }
    }
    
    .header-right {
      display: flex;
      gap: 10px;
      
      .history-btn,
      .clear-btn {
        color: #666;
        
        &:hover {
          color: #667eea;
        }
      }
    }
  }
  
  .chat-container {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    
    &::-webkit-scrollbar {
      width: 8px;
    }
    
    &::-webkit-scrollbar-track {
      background: rgba(255, 255, 255, 0.1);
    }
    
    &::-webkit-scrollbar-thumb {
      background: rgba(255, 255, 255, 0.3);
      border-radius: 4px;
    }
    
    .messages-wrapper {
      max-width: 1200px;
      margin: 0 auto;
      
      .welcome-message {
        text-align: center;
        padding: 60px 20px;
        color: white;
        
        .welcome-icon {
          font-size: 64px;
          margin-bottom: 20px;
          
          i {
            background: linear-gradient(45deg, #fff, #f0f0f0);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
          }
        }
        
        h3 {
          font-size: 28px;
          font-weight: 300;
          margin: 0 0 10px;
        }
        
        p {
          font-size: 16px;
          opacity: 0.9;
        }
        
        .quick-examples {
          margin-top: 40px;
          
          h4 {
            font-size: 18px;
            margin-bottom: 20px;
            opacity: 0.9;
          }
          
          .example-list {
            display: flex;
            flex-wrap: wrap;
            justify-content: center;
            gap: 10px;
            
            .example-item {
              background: rgba(255, 255, 255, 0.2);
              padding: 10px 20px;
              border-radius: 20px;
              cursor: pointer;
              transition: all 0.3s;
              
              &:hover {
                background: rgba(255, 255, 255, 0.3);
                transform: translateY(-2px);
              }
              
              i {
                margin-right: 5px;
              }
            }
          }
        }
      }
      
      .message-item {
        display: flex;
        margin-bottom: 20px;
        
        &.user {
          justify-content: flex-end;
          
          .message-avatar {
            order: 2;
            margin-left: 10px;
            margin-right: 0;
          }
          
          .message-content {
            background: white;
            color: #333;
            max-width: 60%;
            
            .user-message {
              padding: 12px 16px;
              border-radius: 15px 15px 0 15px;
            }
          }
          
          .message-time {
            order: 0;
          }
        }
        
        &.assistant {
          .message-avatar {
            margin-right: 10px;
          }
          
          .message-content {
            background: rgba(255, 255, 255, 0.95);
            max-width: 80%;
            border-radius: 15px 15px 15px 0;
            overflow: hidden;
            
            .ai-message {
              .thinking-block,
              .code-block,
              .sql-block,
              .result-block,
              .error-block {
                padding: 12px 16px;
                border-bottom: 1px solid #f0f0f0;
                
                &:last-child {
                  border-bottom: none;
                }
              }
              
              .thinking-block {
                .thinking-header {
                  display: flex;
                  align-items: center;
                  cursor: pointer;
                  color: #666;
                  font-size: 14px;
                  
                  i {
                    margin-right: 5px;
                  }
                }
                
                .thinking-content {
                  margin-top: 10px;
                  padding: 10px;
                  background: #f8f8f8;
                  border-radius: 5px;
                  font-size: 13px;
                  color: #666;
                }
              }
              
              .code-block,
              .sql-block {
                .code-header,
                .sql-header {
                  display: flex;
                  justify-content: space-between;
                  align-items: center;
                  margin-bottom: 10px;
                  font-weight: 500;
                }
                
                .code-content,
                .sql-content {
                  background: #2d2d2d;
                  color: #f8f8f2;
                  padding: 12px;
                  border-radius: 5px;
                  overflow-x: auto;
                  
                  code {
                    font-family: 'Monaco', 'Menlo', monospace;
                    font-size: 13px;
                  }
                }
              }
              
              .result-block {
                .result-header {
                  display: flex;
                  justify-content: space-between;
                  align-items: center;
                  margin-bottom: 10px;
                  font-weight: 500;
                  
                  .result-info {
                    font-size: 12px;
                    color: #999;
                  }
                }
                
                .result-table {
                  border-radius: 5px;
                  overflow: hidden;
                }
                
                .result-single {
                  .single-value {
                    padding: 10px;
                    background: #f8f8f8;
                    border-radius: 5px;
                    
                    .value-label {
                      font-weight: 500;
                      margin-right: 10px;
                    }
                    
                    .value-content {
                      font-size: 18px;
                      color: #667eea;
                    }
                  }
                }
                
                .result-text {
                  pre {
                    background: #f8f8f8;
                    padding: 10px;
                    border-radius: 5px;
                    overflow-x: auto;
                  }
                }
                
                .result-actions {
                  margin-top: 10px;
                  display: flex;
                  gap: 10px;
                }
              }
              
              .error-block {
                color: #f56c6c;
                display: flex;
                align-items: center;
                
                i {
                  margin-right: 8px;
                  font-size: 18px;
                }
              }
            }
          }
        }
        
        .message-avatar {
          width: 40px;
          height: 40px;
          border-radius: 50%;
          background: white;
          display: flex;
          align-items: center;
          justify-content: center;
          box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
          
          i {
            font-size: 20px;
            color: #667eea;
          }
        }
        
        .message-time {
          font-size: 12px;
          color: rgba(255, 255, 255, 0.7);
          align-self: flex-end;
          margin: 0 10px;
        }
      }
      
      .loading-message {
        display: flex;
        align-items: center;
        
        .loading-avatar {
          width: 40px;
          height: 40px;
          border-radius: 50%;
          background: white;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-right: 10px;
          
          i {
            font-size: 20px;
            color: #667eea;
            animation: spin 1s linear infinite;
          }
        }
        
        .loading-content {
          background: rgba(255, 255, 255, 0.95);
          padding: 12px 16px;
          border-radius: 15px 15px 15px 0;
          
          .loading-text {
            color: #666;
          }
          
          .loading-dots {
            animation: dots 1.5s infinite;
          }
        }
      }
    }
  }
  
  .chat-input {
    background: white;
    padding: 20px;
    box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
    
    .input-wrapper {
      display: flex;
      gap: 10px;
      max-width: 1200px;
      margin: 0 auto;
      
      .message-input {
        flex: 1;
        
        :deep(.el-textarea__inner) {
          border-radius: 10px;
          padding: 10px 15px;
          font-size: 14px;
          resize: none;
        }
      }
      
      .input-actions {
        display: flex;
        align-items: flex-end;
        
        .send-btn {
          height: 40px;
          border-radius: 20px;
          padding: 0 20px;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          border: none;
          
          i {
            margin-right: 5px;
          }
        }
      }
    }
    
    .input-tips {
      text-align: center;
      margin-top: 10px;
      font-size: 12px;
      color: #999;
    }
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes dots {
  0%, 20% {
    content: '.';
  }
  40% {
    content: '..';
  }
  60%, 100% {
    content: '...';
  }
}
</style>
