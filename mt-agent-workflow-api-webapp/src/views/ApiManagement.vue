<template>
  <div class="api-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>接口管理</span>
          <el-tag type="success" size="small" style="margin-left: 10px;">
            管理您的数据问答API接口
          </el-tag>
        </div>
      </template>

      <!-- 搜索和操作栏 -->
      <div class="search-bar">
        <div class="search-left">
          <el-input
            v-model="searchForm.apiName"
            placeholder="请输入API名称"
            style="width: 200px; margin-right: 10px;"
            clearable
          />
          <el-select
            v-model="searchForm.status"
            placeholder="请选择状态"
            style="width: 120px; margin-right: 10px;"
            clearable
          >
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
        <div class="search-right">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            创建API
          </el-button>
        </div>
      </div>

      <!-- API表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        style="width: 100%"
        border
      >
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="apiName" label="API名称" min-width="120" />
        <el-table-column prop="apiPath" label="API路径" min-width="150">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.apiPath }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="apiKey" label="API密钥" min-width="200">
          <template #default="{ row }">
            <div class="api-key-cell">
              <span v-if="!row.showKey">{{ maskApiKey(row.apiKey) }}</span>
              <span v-else>{{ row.apiKey }}</span>
              <el-button
                type="text"
                size="small"
                @click="toggleKeyVisibility(row)"
              >
                <el-icon>
                  <component :is="row.showKey ? 'Hide' : 'View'" />
                </el-icon>
              </el-button>
              <el-button
                type="text"
                size="small"
                @click="copyApiKey(row.apiKey)"
              >
                <el-icon><CopyDocument /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="dbName" label="数据库" min-width="100" />
        <el-table-column prop="tableName" label="数据表" min-width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="callCount" label="调用次数" width="90" />
        <el-table-column prop="rateLimit" label="速率限制" width="100">
          <template #default="{ row }">
            {{ row.rateLimit }}次/分钟
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="handleStatusChange(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button
              size="small"
              type="info"
              @click="handleViewDoc(row)"
            >
              文档
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑API对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="API名称" prop="apiName">
          <el-input v-model="form.apiName" placeholder="请输入API名称" />
        </el-form-item>
        <el-form-item label="API路径" prop="apiPath">
          <el-input v-model="form.apiPath" placeholder="请输入API路径，如：user-query">
            <template #prepend>/open-api/v1/query/</template>
          </el-input>
        </el-form-item>
        <el-form-item label="数据库" prop="dbConfigId">
          <el-select
            v-model="form.dbConfigId"
            placeholder="请选择数据库"
            @change="onDbChange"
            style="width: 100%"
          >
            <el-option
              v-for="db in dbConfigs"
              :key="db.id"
              :label="db.name"
              :value="db.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据表" prop="tableId">
          <el-select
            v-model="form.tableId"
            placeholder="请选择数据表"
            style="width: 100%"
            :loading="loadingTables"
          >
            <el-option
              v-for="table in tables"
              :key="table.id"
              :label="table.tableName"
              :value="table.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="速率限制" prop="rateLimit">
          <el-input-number
            v-model="form.rateLimit"
            :min="0"
            :max="1000"
            placeholder="每分钟最大请求数"
          />
          <span style="margin-left: 10px;">次/分钟（0表示无限制）</span>
        </el-form-item>
        <el-form-item label="超时时间" prop="timeout">
          <el-input-number
            v-model="form.timeout"
            :min="5"
            :max="300"
            placeholder="请求超时时间"
          />
          <span style="margin-left: 10px;">秒</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请输入API描述"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- API文档对话框 -->
    <el-dialog
      v-model="docDialogVisible"
      title="API接口文档"
      width="800px"
    >
      <div class="api-doc" v-if="currentApi">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="API名称">{{ currentApi.apiName }}</el-descriptions-item>
          <el-descriptions-item label="请求地址">
            <el-tag type="info">POST</el-tag>
            <code>{{ getFullApiUrl(currentApi.apiPath) }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="API密钥">
            <code>{{ currentApi.apiKey }}</code>
            <el-button
              type="text"
              size="small"
              @click="copyApiKey(currentApi.apiKey)"
            >
              <el-icon><CopyDocument /></el-icon>
              复制
            </el-button>
          </el-descriptions-item>
          <el-descriptions-item label="速率限制">{{ currentApi.rateLimit }}次/分钟</el-descriptions-item>
          <el-descriptions-item label="超时时间">{{ currentApi.timeout }}秒</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin-top: 20px;">请求示例</h4>
        <el-card shadow="never">
          <pre><code>{{ getRequestExample(currentApi) }}</code></pre>
        </el-card>

        <h4 style="margin-top: 20px;">响应示例</h4>
        <el-card shadow="never">
          <pre><code>{{ getResponseExample() }}</code></pre>
        </el-card>

        <h4 style="margin-top: 20px;">错误码说明</h4>
        <el-table :data="errorCodes" border>
          <el-table-column prop="code" label="错误码" width="100" />
          <el-table-column prop="message" label="说明" />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiConfigApi } from '../api/apiConfig.js'
import { dbApi } from '../api/db.js'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const docDialogVisible = ref(false)
const isEdit = ref(false)
const loadingTables = ref(false)

const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dbConfigs = ref([])
const tables = ref([])
const currentApi = ref(null)

const searchForm = reactive({
  apiName: '',
  status: null
})

const form = reactive({
  id: null,
  apiName: '',
  apiPath: '',
  dbConfigId: null,
  tableId: null,
  description: '',
  status: 1,
  rateLimit: 60,
  timeout: 30
})

const rules = computed(() => ({
  apiName: [
    { required: true, message: '请输入API名称', trigger: 'blur' }
  ],
  apiPath: [
    { required: true, message: '请输入API路径', trigger: 'blur' },
    { pattern: /^[a-z0-9-]+$/, message: 'API路径只能包含小写字母、数字和横线', trigger: 'blur' }
  ],
  dbConfigId: [
    { required: true, message: '请选择数据库', trigger: 'change' }
  ],
  tableId: [
    { required: true, message: '请选择数据表', trigger: 'change' }
  ]
}))

const errorCodes = [
  { code: '200', message: '请求成功' },
  { code: '400', message: '请求参数错误' },
  { code: '401', message: 'API密钥无效' },
  { code: '403', message: 'API已禁用' },
  { code: '404', message: 'API不存在' },
  { code: '429', message: '请求过于频繁' },
  { code: '500', message: '服务器内部错误' }
]

const formRef = ref()

const dialogTitle = computed(() => {
  return isEdit.value ? '编辑API' : '创建API'
})

// 获取API列表
const getApiList = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      apiName: searchForm.apiName || undefined,
      status: searchForm.status
    }
    const res = await apiConfigApi.getApiConfigPage(params)
    if (res.code === 200) {
      tableData.value = res.data.records.map(item => ({
        ...item,
        showKey: false
      }))
      total.value = res.data.total
      
      // 获取数据库和表名称
      for (let item of tableData.value) {
        const db = dbConfigs.value.find(d => d.id === item.dbConfigId)
        if (db) {
          item.dbName = db.name
          // 获取表名
          const tablesRes = await dbApi.getTables(item.dbConfigId)
          if (tablesRes.code === 200) {
            const table = tablesRes.data.find(t => t.id === item.tableId)
            if (table) {
              item.tableName = table.tableName
            }
          }
        }
      }
    }
  } catch (error) {
    ElMessage.error('获取API列表失败')
  } finally {
    loading.value = false
  }
}

// 获取数据库配置列表
const getDbConfigs = async () => {
  try {
    const res = await dbApi.getDbConfigs()
    if (res.code === 200) {
      dbConfigs.value = res.data
    }
  } catch (error) {
    ElMessage.error('获取数据库列表失败')
  }
}

// 数据库改变时获取表列表
const onDbChange = async (dbConfigId) => {
  form.tableId = null
  tables.value = []
  if (!dbConfigId) return
  
  loadingTables.value = true
  try {
    const res = await dbApi.getTables(dbConfigId)
    if (res.code === 200) {
      tables.value = res.data.filter(t => t.enabled)
    }
  } catch (error) {
    ElMessage.error('获取数据表列表失败')
  } finally {
    loadingTables.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  getApiList()
}

// 重置
const handleReset = () => {
  searchForm.apiName = ''
  searchForm.status = null
  currentPage.value = 1
  getApiList()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, {
    id: null,
    apiName: '',
    apiPath: '',
    dbConfigId: null,
    tableId: null,
    description: '',
    status: 1,
    rateLimit: 60,
    timeout: 30
  })
  tables.value = []
  dialogVisible.value = true
}

// 编辑
const handleEdit = async (row) => {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    apiName: row.apiName,
    apiPath: row.apiPath,
    dbConfigId: row.dbConfigId,
    tableId: row.tableId,
    description: row.description,
    status: row.status,
    rateLimit: row.rateLimit,
    timeout: row.timeout
  })
  
  // 加载表列表
  if (row.dbConfigId) {
    await onDbChange(row.dbConfigId)
    form.tableId = row.tableId
  }
  
  dialogVisible.value = true
}

// 提交
const handleSubmit = async () => {
  await formRef.value.validate()
  
  submitLoading.value = true
  try {
    let res
    if (isEdit.value) {
      res = await apiConfigApi.updateApiConfig(form.id, form)
    } else {
      res = await apiConfigApi.createApiConfig(form)
    }
    
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      getApiList()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

// 切换状态
const handleStatusChange = async (row) => {
  try {
    const res = await apiConfigApi.toggleApiStatus(row.id)
    if (res.code === 200) {
      ElMessage.success('状态切换成功')
      getApiList()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除API"${row.apiName}"吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await apiConfigApi.deleteApiConfig(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        getApiList()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

// 查看文档
const handleViewDoc = (row) => {
  currentApi.value = row
  docDialogVisible.value = true
}

// 对话框关闭
const handleDialogClose = () => {
  formRef.value.resetFields()
}

// 分页
const handleSizeChange = (val) => {
  pageSize.value = val
  getApiList()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  getApiList()
}

// 遮罩API密钥
const maskApiKey = (key) => {
  if (!key) return ''
  return key.substring(0, 10) + '****' + key.substring(key.length - 4)
}

// 切换密钥可见性
const toggleKeyVisibility = (row) => {
  row.showKey = !row.showKey
}

// 复制API密钥
const copyApiKey = (key) => {
  navigator.clipboard.writeText(key).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 获取完整API URL
const getFullApiUrl = (path) => {
  return `${window.location.origin}/open-api/v1/query/${path}`
}

// 获取请求示例
const getRequestExample = (api) => {
  return `curl -X POST "${getFullApiUrl(api.apiPath)}" \\
  -H "Content-Type: application/json" \\
  -d '{
    "apiKey": "${api.apiKey}",
    "question": "查询最近7天的销售数据"
  }'`
}

// 获取响应示例
const getResponseExample = () => {
  return `{
  "code": 200,
  "msg": "success",
  "data": {
    "success": true,
    "thinking": "正在分析您的问题...",
    "data": "查询结果...",
    "structuredData": "{...}",
    "apiPath": "user-query",
    "sessionId": 12345,
    "executionTime": 1523
  }
}`
}

onMounted(() => {
  getDbConfigs()
  getApiList()
})
</script>

<style scoped>
.api-management {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
}

.search-bar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.search-left {
  display: flex;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.api-key-cell {
  display: flex;
  align-items: center;
  gap: 5px;
}

.api-doc pre {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  overflow-x: auto;
}

.api-doc code {
  font-family: 'Courier New', monospace;
  font-size: 13px;
}

.api-doc h4 {
  color: #303133;
  margin-bottom: 10px;
}
</style>