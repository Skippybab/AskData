<template>
  <div class="api-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">
        <i class="el-icon-link"></i>
        接口管理
      </h1>
      <p class="page-subtitle">创建和管理数据问答API接口</p>
    </div>

    <!-- 主内容区 -->
    <div class="content-wrapper">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button type="primary" @click="showCreateDialog = true" class="create-btn">
            <i class="el-icon-plus"></i>
            创建API接口
          </el-button>
        </div>
        
        <div class="toolbar-right">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索API名称或路径"
            prefix-icon="el-icon-search"
            clearable
            style="width: 300px"
          />
        </div>
      </div>

      <!-- API列表 -->
      <div class="api-grid">
        <div 
          v-for="api in filteredApis" 
          :key="api.id"
          class="api-card"
          :class="{ 'active': api.status === 1 }"
        >
          <!-- 卡片头部 -->
          <div class="card-header">
            <div class="api-name">
              <i class="el-icon-api-icon"></i>
              {{ api.apiName }}
            </div>
            <el-switch
              v-model="api.status"
              :active-value="1"
              :inactive-value="0"
              @change="updateApiStatus(api)"
              class="status-switch"
            />
          </div>

          <!-- API信息 -->
          <div class="card-body">
            <div class="info-item">
              <span class="info-label">API路径:</span>
              <span class="info-value api-path">
                /open-api/v2/{{ api.apiPath }}
                <el-button
                  type="text"
                  size="mini"
                  @click="copyApiPath(api)"
                  class="copy-btn"
                >
                  <i class="el-icon-copy-document"></i>
                </el-button>
              </span>
            </div>
            
            <div class="info-item">
              <span class="info-label">API密钥:</span>
              <span class="info-value api-key">
                <span v-if="!api.showKey">{{ maskApiKey(api.apiKey) }}</span>
                <span v-else>{{ api.apiKey }}</span>
                <el-button
                  type="text"
                  size="mini"
                  @click="api.showKey = !api.showKey"
                  class="toggle-btn"
                >
                  <i :class="api.showKey ? 'el-icon-view' : 'el-icon-hide'"></i>
                </el-button>
                <el-button
                  type="text"
                  size="mini"
                  @click="copyApiKey(api)"
                  class="copy-btn"
                >
                  <i class="el-icon-copy-document"></i>
                </el-button>
              </span>
            </div>
            
            <div class="info-item">
              <span class="info-label">数据库:</span>
              <span class="info-value">{{ api.databaseName || '-' }}</span>
            </div>
            
            <div class="info-item">
              <span class="info-label">数据表:</span>
              <span class="info-value">{{ api.tableName || '所有表' }}</span>
            </div>
            
            <div class="info-item">
              <span class="info-label">频率限制:</span>
              <span class="info-value">{{ api.rateLimit }} 次/分钟</span>
            </div>
            
            <div class="info-item">
              <span class="info-label">超时时间:</span>
              <span class="info-value">{{ api.timeout }} 秒</span>
            </div>
          </div>

          <!-- 统计信息 -->
          <div class="card-stats">
            <div class="stat-item">
              <span class="stat-value">{{ api.callCount || 0 }}</span>
              <span class="stat-label">总调用次数</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ api.todayCount || 0 }}</span>
              <span class="stat-label">今日调用</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ formatLastCallTime(api.lastCallTime) }}</span>
              <span class="stat-label">最后调用</span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="card-footer">
            <el-button
              type="text"
              size="small"
              @click="showApiDoc(api)"
            >
              <i class="el-icon-document"></i>
              查看文档
            </el-button>
            <el-button
              type="text"
              size="small"
              @click="testApi(api)"
            >
              <i class="el-icon-position"></i>
              测试接口
            </el-button>
            <el-button
              type="text"
              size="small"
              @click="editApi(api)"
            >
              <i class="el-icon-edit"></i>
              编辑
            </el-button>
            <el-button
              type="text"
              size="small"
              @click="regenerateKey(api)"
            >
              <i class="el-icon-refresh"></i>
              重置密钥
            </el-button>
            <el-button
              type="text"
              size="small"
              class="danger-btn"
              @click="deleteApi(api)"
            >
              <i class="el-icon-delete"></i>
              删除
            </el-button>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="filteredApis.length === 0" class="empty-state">
          <i class="el-icon-connection"></i>
          <p>暂无API接口</p>
          <el-button type="primary" @click="showCreateDialog = true">
            创建第一个API接口
          </el-button>
        </div>
      </div>
    </div>

    <!-- 创建API弹窗 -->
    <el-dialog
      title="创建API接口"
      v-model="showCreateDialog"
      width="600px"
      class="create-dialog"
    >
      <el-form :model="apiForm" label-width="120px">
        <el-form-item label="接口名称" required>
          <el-input 
            v-model="apiForm.apiName" 
            placeholder="请输入接口名称，例如：销售数据查询接口"
          />
        </el-form-item>
        
        <el-form-item label="接口描述">
          <el-input 
            v-model="apiForm.description" 
            type="textarea"
            rows="3"
            placeholder="请输入接口描述"
          />
        </el-form-item>
        
        <el-form-item label="数据库" required>
          <el-select 
            v-model="apiForm.dbConfigId" 
            placeholder="选择数据库"
            @change="onDbChange"
            style="width: 100%"
          >
            <el-option
              v-for="db in databases"
              :key="db.id"
              :label="db.name"
              :value="db.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="数据表">
          <el-select 
            v-model="apiForm.tableId" 
            placeholder="选择数据表（可选，不选则可访问所有表）"
            style="width: 100%"
            clearable
          >
            <el-option
              v-for="table in availableTables"
              :key="table.id"
              :label="`${table.tableName} ${table.tableComment ? '(' + table.tableComment + ')' : ''}`"
              :value="table.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="频率限制">
          <el-input-number 
            v-model="apiForm.rateLimit" 
            :min="0" 
            :max="1000"
            style="width: 200px"
          />
          <span style="margin-left: 10px">次/分钟（0表示不限制）</span>
        </el-form-item>
        
        <el-form-item label="超时时间">
          <el-input-number 
            v-model="apiForm.timeout" 
            :min="5" 
            :max="300"
            style="width: 200px"
          />
          <span style="margin-left: 10px">秒</span>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createApi">创建</el-button>
      </template>
    </el-dialog>

    <!-- API文档弹窗 -->
    <el-dialog
      title="API文档"
      v-model="showDocDialog"
      width="800px"
      class="doc-dialog"
    >
      <div class="api-doc" v-if="currentApi">
        <h3>接口信息</h3>
        <div class="doc-section">
          <p><strong>接口地址：</strong>{{ getFullApiUrl(currentApi) }}</p>
          <p><strong>请求方法：</strong>POST</p>
          <p><strong>Content-Type：</strong>application/json</p>
          <p><strong>认证方式：</strong>请求头 X-API-Key</p>
        </div>

        <h3>请求示例</h3>
        <div class="code-example">
          <pre><code>{{ getApiExample(currentApi) }}</code></pre>
        </div>

        <h3>响应示例</h3>
        <div class="code-example">
          <pre><code>{{ getResponseExample() }}</code></pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 数据
const apis = ref([])
const databases = ref([])
const availableTables = ref([])
const searchKeyword = ref('')

// 弹窗控制
const showCreateDialog = ref(false)
const showDocDialog = ref(false)
const currentApi = ref(null)

// 表单数据
const apiForm = ref({
  apiName: '',
  description: '',
  dbConfigId: null,
  tableId: null,
  rateLimit: 60,
  timeout: 30
})

// 计算属性
const filteredApis = computed(() => {
  if (!searchKeyword.value) return apis.value
  
  const keyword = searchKeyword.value.toLowerCase()
  return apis.value.filter(api => 
    api.apiName.toLowerCase().includes(keyword) ||
    api.apiPath.toLowerCase().includes(keyword)
  )
})

// 生命周期
onMounted(() => {
  loadApis()
  loadDatabases()
})

// 方法
const loadApis = async () => {
  try {
    const response = await fetch('/api/api-config/list', {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    const result = await response.json()
    if (result.code === 200) {
      apis.value = result.data.map(api => ({
        ...api,
        showKey: false
      }))
    }
  } catch (error) {
    console.error('加载API列表失败:', error)
    ElMessage.error('加载API列表失败')
  }
}

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
    }
  } catch (error) {
    console.error('加载数据库失败:', error)
  }
}

const onDbChange = async (dbId) => {
  if (!dbId) {
    availableTables.value = []
    return
  }
  
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
  }
}

const createApi = async () => {
  if (!apiForm.value.apiName || !apiForm.value.dbConfigId) {
    ElMessage.warning('请填写必填项')
    return
  }
  
  try {
    const response = await fetch('/api/api-config/create', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(apiForm.value)
    })
    const result = await response.json()
    if (result.code === 200) {
      ElMessage.success('API创建成功')
      showCreateDialog.value = false
      loadApis()
      
      // 重置表单
      apiForm.value = {
        apiName: '',
        description: '',
        dbConfigId: null,
        tableId: null,
        rateLimit: 60,
        timeout: 30
      }
    } else {
      ElMessage.error(result.message || '创建失败')
    }
  } catch (error) {
    console.error('创建API失败:', error)
    ElMessage.error('创建失败')
  }
}

const updateApiStatus = async (api) => {
  try {
    const response = await fetch(`/api/api-config/status/${api.id}`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ status: api.status })
    })
    const result = await response.json()
    if (result.code === 200) {
      ElMessage.success(api.status === 1 ? 'API已启用' : 'API已禁用')
    } else {
      api.status = api.status === 1 ? 0 : 1 // 回滚
      ElMessage.error(result.message || '更新失败')
    }
  } catch (error) {
    api.status = api.status === 1 ? 0 : 1 // 回滚
    ElMessage.error('更新失败')
  }
}

const maskApiKey = (key) => {
  if (!key) return ''
  return key.substring(0, 10) + '****' + key.substring(key.length - 4)
}

const copyApiPath = (api) => {
  const fullPath = `/open-api/v2/${api.apiPath}`
  navigator.clipboard.writeText(fullPath)
  ElMessage.success('API路径已复制')
}

const copyApiKey = (api) => {
  navigator.clipboard.writeText(api.apiKey)
  ElMessage.success('API密钥已复制')
}

const formatLastCallTime = (time) => {
  if (!time) return '从未'
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleDateString('zh-CN')
}

const showApiDoc = (api) => {
  currentApi.value = api
  showDocDialog.value = true
}

const getFullApiUrl = (api) => {
  return `${window.location.origin}/open-api/v2/intelligent-query`
}

const getApiExample = (api) => {
  return `curl -X POST ${getFullApiUrl(api)} \\
  -H "X-API-Key: ${api.apiKey}" \\
  -H "Content-Type: application/json" \\
  -d '{
    "question": "查询本月销售额最高的10个产品",
    "dbConfigId": ${api.dbConfigId},
    "tableId": ${api.tableId || 'null'}
  }'`
}

const getResponseExample = () => {
  return `{
  "code": 200,
  "message": "success",
  "data": {
    "success": true,
    "sessionId": "session_123456",
    "question": "查询本月销售额最高的10个产品",
    "sql": "SELECT product_name, SUM(amount) as total FROM ...",
    "data": [
      {
        "product_name": "产品A",
        "total": 100000
      },
      ...
    ],
    "executionTime": 1523,
    "timestamp": "2024-01-15T10:30:45"
  }
}`
}

const testApi = (api) => {
  // TODO: 实现API测试功能
  ElMessage.info('测试功能开发中')
}

const editApi = (api) => {
  // TODO: 实现编辑功能
  ElMessage.info('编辑功能开发中')
}

const regenerateKey = async (api) => {
  await ElMessageBox.confirm('重置密钥后原密钥将失效，确定要重置吗？', '确认重置', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    const response = await fetch(`/api/api-config/regenerate-key/${api.id}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    const result = await response.json()
    if (result.code === 200) {
      ElMessage.success('密钥已重置')
      loadApis()
    } else {
      ElMessage.error(result.message || '重置失败')
    }
  } catch (error) {
    ElMessage.error('重置失败')
  }
}

const deleteApi = async (api) => {
  await ElMessageBox.confirm('确定要删除这个API接口吗？', '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    const response = await fetch(`/api/api-config/${api.id}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    const result = await response.json()
    if (result.code === 200) {
      ElMessage.success('删除成功')
      loadApis()
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}
</script>

<style scoped lang="scss">
.api-management {
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
  
  .page-header {
    text-align: center;
    color: white;
    margin-bottom: 30px;
    
    .page-title {
      font-size: 32px;
      font-weight: 300;
      margin: 0;
      letter-spacing: 2px;
      
      i {
        font-size: 36px;
        margin-right: 10px;
      }
    }
    
    .page-subtitle {
      font-size: 14px;
      opacity: 0.9;
      margin-top: 10px;
    }
  }
  
  .content-wrapper {
    max-width: 1400px;
    margin: 0 auto;
    
    .toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      
      .create-btn {
        background: white;
        color: #667eea;
        border: none;
        border-radius: 20px;
        padding: 10px 20px;
        font-weight: 500;
        
        &:hover {
          background: #f0f0f0;
        }
      }
    }
    
    .api-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
      gap: 20px;
      
      .api-card {
        background: white;
        border-radius: 15px;
        padding: 20px;
        box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
        transition: all 0.3s ease;
        
        &:hover {
          transform: translateY(-5px);
          box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
        }
        
        &.active {
          border: 2px solid #667eea;
        }
        
        .card-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 15px;
          padding-bottom: 15px;
          border-bottom: 1px solid #f0f0f0;
          
          .api-name {
            font-size: 18px;
            font-weight: 500;
            color: #333;
            
            i {
              margin-right: 8px;
              color: #667eea;
            }
          }
          
          .status-switch {
            --el-switch-on-color: #667eea;
          }
        }
        
        .card-body {
          margin-bottom: 15px;
          
          .info-item {
            display: flex;
            align-items: center;
            margin-bottom: 10px;
            font-size: 13px;
            
            .info-label {
              color: #666;
              width: 80px;
              flex-shrink: 0;
            }
            
            .info-value {
              color: #333;
              flex: 1;
              display: flex;
              align-items: center;
              
              &.api-path,
              &.api-key {
                font-family: 'Monaco', 'Menlo', monospace;
                font-size: 12px;
                background: #f5f5f5;
                padding: 2px 8px;
                border-radius: 4px;
              }
              
              .copy-btn,
              .toggle-btn {
                margin-left: 5px;
                padding: 0;
                font-size: 14px;
                color: #999;
                
                &:hover {
                  color: #667eea;
                }
              }
            }
          }
        }
        
        .card-stats {
          display: flex;
          justify-content: space-around;
          padding: 15px 0;
          margin: 15px 0;
          border-top: 1px solid #f0f0f0;
          border-bottom: 1px solid #f0f0f0;
          
          .stat-item {
            text-align: center;
            
            .stat-value {
              display: block;
              font-size: 20px;
              font-weight: 500;
              color: #667eea;
              margin-bottom: 5px;
            }
            
            .stat-label {
              font-size: 12px;
              color: #999;
            }
          }
        }
        
        .card-footer {
          display: flex;
          justify-content: space-between;
          gap: 5px;
          
          .el-button {
            padding: 0;
            font-size: 13px;
            
            i {
              margin-right: 3px;
            }
          }
          
          .danger-btn {
            color: #f56c6c;
            
            &:hover {
              color: #f23c3c;
            }
          }
        }
      }
      
      .empty-state {
        grid-column: 1 / -1;
        text-align: center;
        padding: 80px 20px;
        background: white;
        border-radius: 15px;
        
        i {
          font-size: 64px;
          color: #ddd;
        }
        
        p {
          margin: 20px 0;
          font-size: 16px;
          color: #999;
        }
      }
    }
  }
  
  .doc-dialog {
    .api-doc {
      h3 {
        color: #333;
        margin: 20px 0 10px;
        
        &:first-child {
          margin-top: 0;
        }
      }
      
      .doc-section {
        background: #f8f8f8;
        padding: 15px;
        border-radius: 5px;
        margin-bottom: 20px;
        
        p {
          margin: 5px 0;
          
          strong {
            color: #666;
          }
        }
      }
      
      .code-example {
        background: #2d2d2d;
        color: #f8f8f2;
        padding: 15px;
        border-radius: 5px;
        overflow-x: auto;
        
        pre {
          margin: 0;
          
          code {
            font-family: 'Monaco', 'Menlo', monospace;
            font-size: 13px;
          }
        }
      }
    }
  }
}
</style>