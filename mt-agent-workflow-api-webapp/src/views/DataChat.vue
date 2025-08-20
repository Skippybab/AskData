<template>
  <div class="data-chat">
    <!-- 顶部栏 -->
    <div class="chat-header">
      <div class="header-left">
        <h2 class="chat-title">
          <i class="el-icon-chat-dot-round"></i>
          智能数据问答
        </h2>
      </div>
      
      <div class="header-right">
        <el-button 
          type="primary" 
          @click="showNewSessionDialog = true"
          icon="el-icon-plus"
        >
          新建会话
        </el-button>
        <el-button 
          type="text" 
          @click="showHistoryDrawer = true"
          icon="el-icon-time"
        >
          历史会话
        </el-button>
      </div>
    </div>

    <!-- 主体区域 -->
    <div class="chat-body">
      <!-- 左侧会话区 -->
      <div class="chat-main">
        <!-- 当前会话信息栏 -->
        <div class="session-info" v-if="currentSession">
          <div class="session-database">
            <i class="el-icon-coin"></i>
            <span>数据库：{{ currentSession.databaseName }}</span>
          </div>
          <div class="session-tools" v-if="currentSession.tools && currentSession.tools.length > 0">
            <i class="el-icon-tools"></i>
            <span>已启用工具：</span>
            <el-tag 
              v-for="tool in currentSession.tools" 
              :key="tool.id"
              size="small"
              type="info"
            >
              {{ tool.name }}
            </el-tag>
          </div>
        </div>

        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer">
          <!-- 欢迎消息 -->
          <div v-if="!currentSession || messages.length === 0" class="welcome-message">
            <div class="welcome-icon">
              <i class="el-icon-magic-stick"></i>
            </div>
            <h3>欢迎使用智能数据问答</h3>
            <p>请先创建新会话，选择数据库和工具后开始对话</p>
            
            <div class="quick-start">
              <el-button type="primary" @click="showNewSessionDialog = true">
                <i class="el-icon-plus"></i>
                创建新会话
              </el-button>
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
                <div class="message-text">{{ message.content }}</div>
                <div class="message-meta" v-if="message.selectedTable">
                  <el-tag size="mini" type="success">
                    <i class="el-icon-grid"></i>
                    表：{{ message.selectedTable }}
                  </el-tag>
                </div>
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
                
                <!-- SQL查询 -->
                <div v-if="message.sql" class="sql-block">
                  <div class="sql-header">
                    <span>SQL查询</span>
                    <el-button 
                      type="text" 
                      size="mini"
                      @click="copySql(message.sql)"
                    >
                      <i class="el-icon-copy-document"></i>
                      复制
                    </el-button>
                  </div>
                  <pre class="sql-content">{{ message.sql }}</pre>
                </div>
                
                <!-- 查询结果 -->
                <div v-if="message.result" class="result-block">
                  <div class="result-header">
                    <span>查询结果</span>
                    <span class="result-count">共 {{ message.result.length }} 条记录</span>
                  </div>
                  <el-table 
                    :data="message.result.slice(0, 10)" 
                    size="small"
                    max-height="300"
                  >
                    <el-table-column
                      v-for="(value, key) in message.result[0]"
                      :key="key"
                      :prop="key"
                      :label="key"
                      min-width="120"
                    />
                  </el-table>
                  <div v-if="message.result.length > 10" class="result-more">
                    还有 {{ message.result.length - 10 }} 条记录...
                  </div>
                </div>
                
                <!-- 错误信息 -->
                <div v-if="message.error" class="error-block">
                  <i class="el-icon-warning"></i>
                  {{ message.error }}
                </div>
                
                <!-- 回答文本 -->
                <div v-if="message.content" class="answer-text">
                  {{ message.content }}
                </div>
              </div>
            </div>
            
            <div class="message-time">
              {{ formatTime(message.timestamp) }}
            </div>
          </div>

          <!-- 加载中提示 -->
          <div v-if="isLoading" class="loading-message">
            <i class="el-icon-loading"></i>
            <span>{{ loadingText }}</span>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area" v-if="currentSession">
          <!-- 表选择器 -->
          <div class="table-selector">
            <el-select 
              v-model="selectedTable" 
              placeholder="选择要查询的表（可选）"
              clearable
              filterable
              size="small"
              style="width: 250px"
            >
              <el-option
                label="自动选择合适的表"
                :value="null"
              >
                <span class="option-label">
                  <i class="el-icon-magic-stick"></i>
                  自动选择
                </span>
              </el-option>
              <el-option
                v-for="table in availableTables"
                :key="table.id"
                :label="table.tableName"
                :value="table.tableName"
              >
                <span class="option-label">
                  <i class="el-icon-grid"></i>
                  {{ table.tableName }}
                  <span class="table-comment" v-if="table.tableComment">
                    - {{ table.tableComment }}
                  </span>
                </span>
              </el-option>
            </el-select>
          </div>
          
          <!-- 输入框 -->
          <div class="input-box">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              placeholder="请输入您的问题，例如：查询本月销售额最高的10个产品"
              @keydown.enter.ctrl="sendMessage"
            />
            <div class="input-actions">
              <div class="input-tips">
                <span>Ctrl + Enter 发送</span>
                <span v-if="selectedTable">当前表：{{ selectedTable }}</span>
              </div>
              <el-button 
                type="primary" 
                @click="sendMessage"
                :disabled="!inputMessage.trim() || isLoading"
                :loading="isLoading"
              >
                <i class="el-icon-s-promotion"></i>
                发送
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧工具栏 -->
      <div class="chat-sidebar">
        <!-- 快捷问题 -->
        <div class="sidebar-section">
          <h4>快捷问题</h4>
          <div class="quick-questions">
            <div 
              v-for="q in quickQuestions" 
              :key="q"
              class="quick-question-item"
              @click="askQuickQuestion(q)"
            >
              <i class="el-icon-search"></i>
              {{ q }}
            </div>
          </div>
        </div>

        <!-- 常用表 -->
        <div class="sidebar-section" v-if="frequentTables.length > 0">
          <h4>常用表</h4>
          <div class="frequent-tables">
            <el-tag 
              v-for="table in frequentTables" 
              :key="table"
              @click="selectedTable = table"
              :effect="selectedTable === table ? 'dark' : 'plain'"
              style="margin: 5px; cursor: pointer;"
            >
              {{ table }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建会话对话框 -->
    <el-dialog
      title="新建数据问答会话"
      v-model="showNewSessionDialog"
      width="600px"
      class="new-session-dialog"
    >
      <el-form :model="sessionForm" label-width="120px">
        <el-form-item label="会话名称">
          <el-input 
            v-model="sessionForm.name" 
            placeholder="输入会话名称（可选）"
          />
        </el-form-item>
        
        <el-form-item label="选择数据库" required>
          <el-select 
            v-model="sessionForm.databaseId" 
            placeholder="请选择数据库"
            @change="onDatabaseSelect"
            style="width: 100%"
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
                <span class="db-info">{{ db.host }}:{{ db.port }}/{{ db.databaseName }}</span>
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="启用工具">
          <el-checkbox-group v-model="sessionForm.tools">
            <el-checkbox 
              v-for="tool in availableTools" 
              :key="tool.id"
              :label="tool.id"
            >
              {{ tool.name }}
              <el-tooltip :content="tool.description" placement="top">
                <i class="el-icon-question"></i>
              </el-tooltip>
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showNewSessionDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="createSession"
          :disabled="!sessionForm.databaseId"
        >
          创建会话
        </el-button>
      </template>
    </el-dialog>

    <!-- 历史会话抽屉 -->
    <el-drawer
      v-model="showHistoryDrawer"
      title="历史会话"
      direction="rtl"
      size="400px"
    >
      <div class="history-list">
        <div 
          v-for="session in historySessions" 
          :key="session.id"
          class="history-item"
          :class="{ active: currentSession?.id === session.id }"
          @click="loadSession(session)"
        >
          <div class="history-header">
            <span class="history-name">{{ session.name || `会话 ${session.id}` }}</span>
            <span class="history-time">{{ formatDate(session.createTime) }}</span>
          </div>
          <div class="history-info">
            <el-tag size="mini" type="info">
              <i class="el-icon-coin"></i>
              {{ session.databaseName }}
            </el-tag>
            <span class="message-count">{{ session.messageCount || 0 }} 条消息</span>
          </div>
        </div>
        
        <div v-if="historySessions.length === 0" class="empty-history">
          <i class="el-icon-folder-opened"></i>
          <p>暂无历史会话</p>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'

// 数据
const currentSession = ref(null)
const messages = ref([])
const historySessions = ref([])
const databases = ref([])
const availableTables = ref([])
const availableTools = ref([
  { id: 'sql_gen', name: 'SQL生成器', description: '根据自然语言生成SQL查询' },
  { id: 'data_viz', name: '数据可视化', description: '将查询结果可视化展示' },
  { id: 'export', name: '数据导出', description: '支持导出为Excel、CSV格式' }
])
const frequentTables = ref([])
const quickQuestions = ref([
  '查询所有表的记录数',
  '查看最近一周的数据变化',
  '统计各个分类的数据分布',
  '查找异常或重复的数据'
])

// 状态
const showNewSessionDialog = ref(false)
const showHistoryDrawer = ref(false)
const isLoading = ref(false)
const loadingText = ref('正在思考...')

// 表单
const sessionForm = ref({
  name: '',
  databaseId: null,
  tools: ['sql_gen']
})
const inputMessage = ref('')
const selectedTable = ref(null)

// 生命周期
onMounted(() => {
  loadDatabases()
  loadHistorySessions()
})

// 方法
const loadDatabases = async () => {
  try {
    const result = await api.dbConfig.getEnabled()
    databases.value = result.data || []
  } catch (error) {
    console.error('加载数据库失败:', error)
  }
}

const loadHistorySessions = async () => {
  // TODO: 从后端加载历史会话
  historySessions.value = []
}

const onDatabaseSelect = async (dbId) => {
  if (!dbId) {
    availableTables.value = []
    return
  }
  
  try {
    const result = await api.schema.getTables(dbId)
    availableTables.value = result.data || []
    
    // 提取常用表（示例：前5个表）
    frequentTables.value = availableTables.value
      .slice(0, 5)
      .map(t => t.tableName)
  } catch (error) {
    console.error('加载表列表失败:', error)
  }
}

const createSession = async () => {
  if (!sessionForm.value.databaseId) {
    ElMessage.warning('请选择数据库')
    return
  }
  
  try {
    const db = databases.value.find(d => d.id === sessionForm.value.databaseId)
    
    // 创建会话对象
    currentSession.value = {
      id: Date.now(),
      name: sessionForm.value.name || `会话 ${Date.now()}`,
      databaseId: sessionForm.value.databaseId,
      databaseName: db?.name,
      tools: availableTools.value.filter(t => sessionForm.value.tools.includes(t.id)),
      createTime: new Date()
    }
    
    // 加载该数据库的表
    await onDatabaseSelect(sessionForm.value.databaseId)
    
    // 清空消息
    messages.value = []
    
    // 关闭对话框
    showNewSessionDialog.value = false
    
    ElMessage.success('会话创建成功')
    
    // 重置表单
    sessionForm.value = {
      name: '',
      databaseId: null,
      tools: ['sql_gen']
    }
  } catch (error) {
    ElMessage.error('创建会话失败')
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || isLoading.value) {
    return
  }
  
  if (!currentSession.value) {
    ElMessage.warning('请先创建会话')
    return
  }
  
  const userMessage = inputMessage.value.trim()
  const currentTable = selectedTable.value
  inputMessage.value = ''
  
  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: userMessage,
    selectedTable: currentTable,
    timestamp: new Date()
  })
  
  // 添加AI消息占位
  const aiMessage = {
    role: 'assistant',
    content: '',
    thinking: '',
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
    const response = await api.chat.query({
      sessionId: currentSession.value.id,
      question: userMessage,
      dbConfigId: currentSession.value.databaseId,
      tableName: currentTable,
      tools: currentSession.value.tools.map(t => t.id)
    })
    
    if (response.data) {
      // 更新AI消息
      Object.assign(aiMessage, {
        content: response.data.answer || '',
        thinking: response.data.thinking || '',
        sql: response.data.sql || '',
        result: response.data.result || null,
        error: response.data.error || null
      })
    }
  } catch (error) {
    aiMessage.error = error.message || '查询失败，请重试'
  } finally {
    isLoading.value = false
    loadingText.value = '正在思考...'
  }
}

const askQuickQuestion = (question) => {
  inputMessage.value = question
  sendMessage()
}

const loadSession = (session) => {
  currentSession.value = session
  // TODO: 加载会话的消息历史
  messages.value = []
  showHistoryDrawer.value = false
}

const copySql = (sql) => {
  navigator.clipboard.writeText(sql)
  ElMessage.success('SQL已复制到剪贴板')
}

const scrollToBottom = () => {
  nextTick(() => {
    const container = document.querySelector('.messages-container')
    if (container) {
      container.scrollTop = container.scrollHeight
    }
  })
}

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

const formatTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleTimeString('zh-CN')
}

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleDateString('zh-CN')
}
</script>

<style scoped lang="scss">
.data-chat {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f6fa;
  
  .chat-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 15px 20px;
    background: white;
    border-bottom: 1px solid #e8e8e8;
    
    .chat-title {
      margin: 0;
      font-size: 20px;
      font-weight: 500;
      color: #333;
      
      i {
        margin-right: 8px;
        color: #667eea;
      }
    }
  }
  
  .chat-body {
    flex: 1;
    display: flex;
    overflow: hidden;
    
    .chat-main {
      flex: 1;
      display: flex;
      flex-direction: column;
      background: white;
      
      .session-info {
        padding: 10px 20px;
        background: #f8f9fa;
        border-bottom: 1px solid #e8e8e8;
        display: flex;
        align-items: center;
        gap: 20px;
        
        .session-database {
          display: flex;
          align-items: center;
          gap: 5px;
          color: #666;
          
          i {
            color: #667eea;
          }
        }
        
        .session-tools {
          display: flex;
          align-items: center;
          gap: 8px;
          
          i {
            color: #667eea;
          }
        }
      }
      
      .messages-container {
        flex: 1;
        overflow-y: auto;
        padding: 20px;
        
        .welcome-message {
          text-align: center;
          padding: 80px 20px;
          
          .welcome-icon {
            font-size: 64px;
            color: #667eea;
            margin-bottom: 20px;
          }
          
          h3 {
            margin: 0 0 10px;
            font-size: 24px;
            color: #333;
          }
          
          p {
            color: #666;
            margin-bottom: 30px;
          }
        }
        
        .message-item {
          display: flex;
          gap: 15px;
          margin-bottom: 20px;
          
          &.user {
            flex-direction: row-reverse;
            
            .message-avatar {
              background: #667eea;
            }
            
            .message-content {
              align-items: flex-end;
            }
            
            .user-message {
              background: #667eea;
              color: white;
              padding: 12px 16px;
              border-radius: 12px 12px 0 12px;
              max-width: 70%;
              
              .message-meta {
                margin-top: 8px;
              }
            }
          }
          
          &.assistant {
            .message-avatar {
              background: #f0f0f0;
              color: #666;
            }
            
            .ai-message {
              max-width: 80%;
              
              > div {
                margin-bottom: 12px;
                
                &:last-child {
                  margin-bottom: 0;
                }
              }
              
              .thinking-block,
              .sql-block,
              .result-block {
                background: #f8f9fa;
                border-radius: 8px;
                overflow: hidden;
                
                > div:first-child {
                  padding: 10px 15px;
                  background: #e8eaf6;
                  font-size: 13px;
                  font-weight: 500;
                  display: flex;
                  justify-content: space-between;
                  align-items: center;
                  cursor: pointer;
                }
              }
              
              .thinking-content,
              .sql-content {
                padding: 15px;
                font-size: 13px;
                line-height: 1.6;
              }
              
              .sql-content {
                font-family: 'Monaco', 'Menlo', monospace;
                background: #2d2d2d;
                color: #f8f8f2;
                overflow-x: auto;
              }
              
              .result-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 10px 15px;
                background: #e8eaf6;
                
                .result-count {
                  font-size: 12px;
                  color: #666;
                }
              }
              
              .result-more {
                padding: 10px;
                text-align: center;
                color: #666;
                font-size: 13px;
                background: #f8f9fa;
              }
              
              .error-block {
                padding: 12px 15px;
                background: #fff2f0;
                border: 1px solid #ffccc7;
                border-radius: 8px;
                color: #ff4d4f;
                
                i {
                  margin-right: 8px;
                }
              }
              
              .answer-text {
                padding: 12px 16px;
                background: #f0f0f0;
                border-radius: 12px;
                color: #333;
              }
            }
          }
          
          .message-avatar {
            width: 36px;
            height: 36px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
            
            i {
              font-size: 20px;
              color: white;
            }
          }
          
          .message-content {
            display: flex;
            flex-direction: column;
          }
          
          .message-time {
            font-size: 12px;
            color: #999;
            margin-top: 5px;
          }
        }
        
        .loading-message {
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 20px;
          color: #666;
          
          i {
            font-size: 20px;
          }
        }
      }
      
      .input-area {
        border-top: 1px solid #e8e8e8;
        padding: 15px 20px;
        background: white;
        
        .table-selector {
          margin-bottom: 10px;
        }
        
        .input-box {
          .input-actions {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-top: 10px;
            
            .input-tips {
              display: flex;
              gap: 20px;
              font-size: 12px;
              color: #999;
            }
          }
        }
      }
    }
    
    .chat-sidebar {
      width: 280px;
      background: white;
      border-left: 1px solid #e8e8e8;
      padding: 20px;
      overflow-y: auto;
      
      .sidebar-section {
        margin-bottom: 30px;
        
        h4 {
          margin: 0 0 15px;
          font-size: 14px;
          font-weight: 500;
          color: #333;
        }
        
        .quick-questions {
          .quick-question-item {
            padding: 10px;
            margin-bottom: 8px;
            background: #f8f9fa;
            border-radius: 6px;
            cursor: pointer;
            font-size: 13px;
            color: #666;
            transition: all 0.3s;
            
            &:hover {
              background: #e8eaf6;
              color: #667eea;
            }
            
            i {
              margin-right: 5px;
            }
          }
        }
      }
    }
  }
  
  .option-label {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .db-info,
    .table-comment {
      font-size: 12px;
      color: #999;
    }
  }
  
  .history-list {
    .history-item {
      padding: 15px;
      border-bottom: 1px solid #f0f0f0;
      cursor: pointer;
      transition: background 0.3s;
      
      &:hover {
        background: #f8f9fa;
      }
      
      &.active {
        background: #e8eaf6;
      }
      
      .history-header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 8px;
        
        .history-name {
          font-weight: 500;
          color: #333;
        }
        
        .history-time {
          font-size: 12px;
          color: #999;
        }
      }
      
      .history-info {
        display: flex;
        align-items: center;
        gap: 10px;
        
        .message-count {
          font-size: 12px;
          color: #666;
        }
      }
    }
    
    .empty-history {
      text-align: center;
      padding: 50px 20px;
      color: #999;
      
      i {
        font-size: 48px;
        color: #ddd;
      }
      
      p {
        margin-top: 10px;
      }
    }
  }
}
</style>