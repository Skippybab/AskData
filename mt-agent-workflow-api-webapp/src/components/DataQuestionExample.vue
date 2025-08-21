<template>
  <div class="data-question-example">
    <h3>数据问答使用示例</h3>
    
    <!-- 数据库和表选择 -->
    <div class="config-section">
      <el-select v-model="selectedDb" placeholder="选择数据库" @change="onDbChange">
        <el-option 
          v-for="db in databases" 
          :key="db.id" 
          :label="db.name" 
          :value="db.id"
        />
      </el-select>
      
      <el-select v-model="selectedTable" placeholder="选择表（可选）" clearable>
        <el-option 
          v-for="table in tables" 
          :key="table.id" 
          :label="table.tableName" 
          :value="table.id"
        />
      </el-select>
    </div>
    
    <!-- 问题输入 -->
    <div class="input-section">
      <el-input 
        v-model="question" 
        placeholder="请输入您的问题..."
        @keyup.enter="sendQuestion"
      />
      <el-button 
        type="primary" 
        @click="sendQuestion" 
        :loading="loading"
        :disabled="!currentSession || !selectedDb"
      >
        发送
      </el-button>
    </div>
    
    <!-- 结果展示 -->
    <div class="result-section" v-if="lastResult">
      <!-- 思考过程 -->
      <el-collapse v-if="lastResult.thinking">
        <el-collapse-item title="AI思考过程">
          <pre>{{ lastResult.thinking }}</pre>
        </el-collapse-item>
      </el-collapse>
      
      <!-- Python代码 -->
      <el-collapse v-if="lastResult.pythonCode">
        <el-collapse-item title="Python代码">
          <pre class="code-block">{{ lastResult.pythonCode }}</pre>
        </el-collapse-item>
      </el-collapse>
      
      <!-- SQL语句 -->
      <div v-if="lastResult.sql" class="sql-section">
        <h4>SQL查询语句</h4>
        <pre class="sql-block">{{ lastResult.sql }}</pre>
        <el-button size="small" @click="copySql">复制SQL</el-button>
      </div>
      
      <!-- 查询结果 -->
      <div v-if="lastResult.result" class="query-result">
        <h4>查询结果</h4>
        
        <!-- 表格形式展示 -->
        <template v-if="lastResult.resultType === 'table' && parsedResult">
          <el-table :data="parsedResult" max-height="400">
            <el-table-column 
              v-for="(col, index) in resultColumns" 
              :key="index"
              :prop="col"
              :label="col"
              min-width="120"
            />
          </el-table>
          <p class="result-info">{{ lastResult.resultInfo }}</p>
        </template>
        
        <!-- 文本形式展示 -->
        <template v-else>
          <div class="text-result">{{ lastResult.result }}</div>
        </template>
      </div>
      
      <!-- 错误信息 -->
      <el-alert 
        v-if="lastResult.error" 
        type="error" 
        :title="lastResult.error"
        show-icon
      />
      
      <!-- 执行时间 -->
      <div v-if="lastResult.duration" class="execution-time">
        执行耗时: {{ (lastResult.duration / 1000).toFixed(2) }}秒
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { dataQuestionApi, sessionApi, dbConfigApi } from '@/api/dataQuestion'

// 状态管理
const currentSession = ref(null)
const selectedDb = ref(null)
const selectedTable = ref(null)
const databases = ref([])
const tables = ref([])
const question = ref('')
const loading = ref(false)
const lastResult = ref(null)

// 计算属性
const parsedResult = computed(() => {
  if (!lastResult.value?.result || lastResult.value.resultType !== 'table') {
    return null
  }
  
  try {
    // 尝试解析结果为JSON
    const resultStr = lastResult.value.result
    if (typeof resultStr === 'string' && resultStr.includes('[{')) {
      // 提取JSON数组部分
      const startIdx = resultStr.indexOf('[{')
      const endIdx = resultStr.lastIndexOf('}]') + 2
      const jsonStr = resultStr.substring(startIdx, endIdx)
        .replace(/'/g, '"')
        .replace(/None/g, 'null')
        .replace(/True/g, 'true')
        .replace(/False/g, 'false')
      
      return JSON.parse(jsonStr)
    }
    return null
  } catch (e) {
    console.error('解析结果失败:', e)
    return null
  }
})

const resultColumns = computed(() => {
  if (parsedResult.value && parsedResult.value.length > 0) {
    return Object.keys(parsedResult.value[0])
  }
  return []
})

// 方法
const initSession = async () => {
  try {
    // 创建新会话
    const response = await sessionApi.create({
      sessionName: '数据问答会话 ' + new Date().toLocaleString(),
      dbConfigId: selectedDb.value
    })
    
    if (response.code === 200) {
      currentSession.value = response.data
      ElMessage.success('会话创建成功')
    }
  } catch (error) {
    ElMessage.error('创建会话失败')
  }
}

const loadDatabases = async () => {
  try {
    const response = await dbConfigApi.list()
    if (response.code === 200) {
      databases.value = response.data || []
      // 自动选择第一个数据库
      if (databases.value.length > 0) {
        selectedDb.value = databases.value[0].id
        await onDbChange()
      }
    }
  } catch (error) {
    console.error('加载数据库列表失败:', error)
  }
}

const onDbChange = async () => {
  if (!selectedDb.value) {
    tables.value = []
    return
  }
  
  try {
    const response = await dbConfigApi.getTables(selectedDb.value)
    if (response.code === 200) {
      tables.value = response.data || []
    }
    
    // 重新创建会话
    await initSession()
  } catch (error) {
    console.error('加载表列表失败:', error)
  }
}

const sendQuestion = async () => {
  if (!question.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  
  if (!currentSession.value) {
    ElMessage.warning('请先创建会话')
    return
  }
  
  if (!selectedDb.value) {
    ElMessage.warning('请选择数据库')
    return
  }
  
  loading.value = true
  const startTime = Date.now()
  
  try {
    // 调用数据问答接口
    const result = await dataQuestionApi.ask({
      sessionId: currentSession.value.id,
      question: question.value,
      dbConfigId: selectedDb.value,
      tableId: selectedTable.value
    })
    
    if (result.success) {
      lastResult.value = result.data
      // 添加实际执行时间（如果后端没有返回）
      if (!lastResult.value.duration) {
        lastResult.value.duration = Date.now() - startTime
      }
      ElMessage.success('查询完成')
    } else {
      ElMessage.error(result.error || '查询失败')
      lastResult.value = {
        error: result.error,
        duration: Date.now() - startTime
      }
    }
    
    // 清空输入
    question.value = ''
    
  } catch (error) {
    console.error('发送问题失败:', error)
    ElMessage.error('请求失败: ' + error.message)
    lastResult.value = {
      error: error.message,
      duration: Date.now() - startTime
    }
  } finally {
    loading.value = false
  }
}

const copySql = () => {
  if (lastResult.value?.sql) {
    navigator.clipboard.writeText(lastResult.value.sql)
    ElMessage.success('SQL已复制到剪贴板')
  }
}

// 生命周期
onMounted(() => {
  loadDatabases()
})
</script>

<style scoped lang="scss">
.data-question-example {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
  
  h3 {
    margin-bottom: 20px;
    color: #333;
  }
  
  .config-section {
    margin-bottom: 20px;
    display: flex;
    gap: 10px;
    
    .el-select {
      width: 200px;
    }
  }
  
  .input-section {
    margin-bottom: 20px;
    display: flex;
    gap: 10px;
    
    .el-input {
      flex: 1;
    }
  }
  
  .result-section {
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    padding: 20px;
    background: #f5f7fa;
    
    .el-collapse {
      margin-bottom: 15px;
    }
    
    .sql-section {
      margin-bottom: 15px;
      
      h4 {
        margin-bottom: 10px;
        color: #606266;
      }
      
      .sql-block {
        background: #2d2d2d;
        color: #f8f8f2;
        padding: 10px;
        border-radius: 4px;
        font-family: 'Monaco', monospace;
        font-size: 14px;
        overflow-x: auto;
      }
      
      .el-button {
        margin-top: 10px;
      }
    }
    
    .query-result {
      margin-bottom: 15px;
      
      h4 {
        margin-bottom: 10px;
        color: #606266;
      }
      
      .text-result {
        background: white;
        padding: 15px;
        border-radius: 4px;
        white-space: pre-wrap;
      }
      
      .result-info {
        margin-top: 10px;
        color: #909399;
        font-size: 14px;
      }
    }
    
    .code-block {
      background: #f4f4f4;
      padding: 10px;
      border-radius: 4px;
      font-family: monospace;
      font-size: 14px;
      overflow-x: auto;
    }
    
    .execution-time {
      margin-top: 15px;
      text-align: right;
      color: #909399;
      font-size: 14px;
    }
  }
}
</style>