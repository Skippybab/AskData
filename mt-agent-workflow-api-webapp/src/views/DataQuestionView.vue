<template>
  <div class="data-question">
    <!-- 顶部栏 -->
    <div class="header">
      <h2>智能数据问答</h2>
      <div class="header-actions">
        <el-button type="primary" @click="createSession">新建会话</el-button>
      </div>
    </div>

    <!-- 主体区域 -->
    <div class="main-content">
      <!-- 会话信息 -->
      <div class="session-info" v-if="currentSession">
        <span>当前会话: {{ currentSession.name }}</span>
        <span>数据库: {{ currentSession.dbName }}</span>
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
                <div class="section-header">查询结果</div>
                <div class="result-content">
                  <!-- 表格形式展示 -->
                  <el-table v-if="msg.resultType === 'table'" :data="msg.resultData" max-height="400">
                    <el-table-column 
                      v-for="(col, idx) in msg.resultColumns" 
                      :key="idx"
                      :prop="col"
                      :label="col"
                      min-width="120"
                    />
                  </el-table>
                  <!-- 文本形式展示 -->
                  <div v-else class="text-result">{{ msg.result }}</div>
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
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'

// 状态管理
const currentSession = ref(null)
const messages = ref([])
const inputMessage = ref('')
const selectedTable = ref(null)
const tables = ref([])
const isLoading = ref(false)
const loadingText = ref('正在思考...')

// 创建新会话
const createSession = async () => {
  try {
    const response = await fetch('/api/chat/sessions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionName: '新对话 ' + new Date().toLocaleString(),
        dbConfigId: 1 // TODO: 从配置中获取
      })
    })
    
    const result = await response.json()
    if (result.code === 200) {
      currentSession.value = result.data
      messages.value = []
      ElMessage.success('会话创建成功')
      loadTables()
    }
  } catch (error) {
    ElMessage.error('创建会话失败')
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
    showCode: false
  }
  messages.value.push(aiMessage)
  
  scrollToBottom()
  isLoading.value = true
  updateLoadingText()
  
  try {
    // 调用数据问答接口
    const response = await fetch('/api/data-question/ask', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId: currentSession.value.id,
        question: question,
        dbConfigId: currentSession.value.dbConfigId,
        tableId: selectedTable.value
      })
    })
    
    const result = await response.json()
    
    if (result.code === 200 && result.data) {
      const data = result.data
      
      // 更新AI消息
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
            }
          } catch (e) {
            console.error('解析表格数据失败', e)
          }
        }
      }
      
      // 处理错误
      if (!data.success && data.error) {
        aiMessage.error = data.error
      }
      
    } else {
      aiMessage.error = result.msg || '查询失败'
    }
    
  } catch (error) {
    console.error('请求失败', error)
    aiMessage.error = '请求失败: ' + error.message
  } finally {
    isLoading.value = false
    loadingText.value = '正在思考...'
  }
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
}

// 格式化时间
const formatTime = (date) => {
  return new Date(date).toLocaleTimeString('zh-CN')
}

// DOM引用
const messagesContainer = ref(null)

// 初始化
onMounted(() => {
  // 自动创建会话
  createSession()
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
    flex-direction: column;
    overflow: hidden;
    
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