<template>
  <div class="data-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">
        <i class="el-icon-coin"></i>
        数据管理
      </h1>
      <p class="page-subtitle">配置和管理数据库与知识库</p>
    </div>

    <!-- 主内容区 -->
    <div class="content-wrapper">
      <!-- 标签页 -->
      <el-tabs v-model="activeTab" class="data-tabs">
        <!-- 数据库标签页 -->
        <el-tab-pane label="数据库" name="database">
          <div class="tab-content">
            <!-- 工具栏 -->
            <div class="toolbar">
              <el-button type="primary" @click="showAddDbDialog = true" class="add-btn">
                <i class="el-icon-plus"></i>
                添加数据库
              </el-button>
              <el-input
                v-model="dbSearchKeyword"
                placeholder="搜索数据库名称"
                prefix-icon="el-icon-search"
                clearable
                style="width: 300px"
              />
            </div>

            <!-- 数据库列表 -->
            <div class="data-list">
              <div 
                v-for="db in filteredDatabases" 
                :key="db.id"
                class="data-card"
                :class="{ 'active': db.status === 1 }"
                @click="selectDatabase(db)"
              >
                <div class="card-icon">
                  <i class="el-icon-data-board"></i>
                </div>
                <div class="card-info">
                  <h3>{{ db.name }}</h3>
                  <p class="db-type">{{ db.dbType || 'MySQL' }}</p>
                  <p class="db-host">{{ db.host }}:{{ db.port }}/{{ db.databaseName }}</p>
                  <p class="db-status">
                    <span :class="['status-dot', db.status === 1 ? 'active' : 'inactive']"></span>
                    {{ db.status === 1 ? '已启用' : '已禁用' }}
                  </p>
                </div>
                <div class="card-actions">
                  <el-switch
                    v-model="db.status"
                    :active-value="1"
                    :inactive-value="0"
                    @change="updateDbStatus(db)"
                    @click.stop
                  />
                  <el-button 
                    type="text" 
                    @click.stop="editDatabase(db)"
                    icon="el-icon-edit"
                  >编辑</el-button>
                  <el-button 
                    type="text" 
                    @click.stop="deleteDatabase(db)"
                    icon="el-icon-delete"
                    class="danger-btn"
                  >删除</el-button>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 知识库标签页 -->
        <el-tab-pane label="知识库" name="knowledge">
          <div class="tab-content">
            <!-- 工具栏 -->
            <div class="toolbar">
              <el-button type="primary" @click="showAddKnowledgeDialog = true" class="add-btn">
                <i class="el-icon-plus"></i>
                添加知识库
              </el-button>
              <el-input
                v-model="knowledgeSearchKeyword"
                placeholder="搜索知识库名称"
                prefix-icon="el-icon-search"
                clearable
                style="width: 300px"
              />
            </div>

            <!-- 知识库列表 -->
            <div class="data-list">
              <div 
                v-for="kb in filteredKnowledgeBases" 
                :key="kb.id"
                class="data-card"
                :class="{ 'active': kb.status === 1 }"
              >
                <div class="card-icon">
                  <i class="el-icon-notebook-2"></i>
                </div>
                <div class="card-info">
                  <h3>{{ kb.name }}</h3>
                  <p class="kb-type">{{ kb.type }}</p>
                  <p class="kb-desc">{{ kb.description || '暂无描述' }}</p>
                  <p class="kb-stats">
                    文档数: {{ kb.documentCount || 0 }} | 
                    大小: {{ formatSize(kb.size || 0) }}
                  </p>
                </div>
                <div class="card-actions">
                  <el-switch
                    v-model="kb.status"
                    :active-value="1"
                    :inactive-value="0"
                    @change="updateKnowledgeStatus(kb)"
                    @click.stop
                  />
                  <el-button 
                    type="text" 
                    @click.stop="manageKnowledge(kb)"
                    icon="el-icon-setting"
                  >管理</el-button>
                  <el-button 
                    type="text" 
                    @click.stop="deleteKnowledge(kb)"
                    icon="el-icon-delete"
                    class="danger-btn"
                  >删除</el-button>
                </div>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-if="filteredKnowledgeBases.length === 0" class="empty-state">
              <i class="el-icon-folder-opened"></i>
              <p>暂无知识库</p>
              <el-button type="primary" @click="showAddKnowledgeDialog = true">
                创建第一个知识库
              </el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 数据库表管理弹窗 -->
    <el-dialog
      :title="`管理数据库表 - ${selectedDatabase?.name}`"
      v-model="showTableManageDialog"
      width="900px"
      class="table-manage-dialog"
    >
      <div class="table-manage-content" v-if="selectedDatabase">
        <!-- 表格工具栏 -->
        <div class="table-toolbar">
          <el-button @click="syncDatabaseSchema" :loading="syncing">
            <i class="el-icon-refresh"></i>
            同步表结构
          </el-button>
          <el-input
            v-model="tableSearchKeyword"
            placeholder="搜索表名"
            prefix-icon="el-icon-search"
            clearable
            style="width: 250px"
          />
        </div>

        <!-- 表格列表 -->
        <el-table 
          :data="filteredTables" 
          style="width: 100%"
          max-height="500px"
        >
          <el-table-column prop="tableName" label="表名" width="200">
            <template #default="{ row }">
              <div class="table-name-cell">
                <span>{{ row.tableName }}</span>
                <el-button
                  type="text"
                  size="mini"
                  @click="showTableDetails(row)"
                  icon="el-icon-view"
                />
              </div>
            </template>
          </el-table-column>
          
          <el-table-column prop="tableComment" label="表注释" width="250">
            <template #default="{ row }">
              <div class="editable-cell">
                <span v-if="!row.editing">{{ row.tableComment || '-' }}</span>
                <el-input
                  v-else
                  v-model="row.tempComment"
                  size="mini"
                  @blur="saveTableComment(row)"
                  @keyup.enter="saveTableComment(row)"
                />
                <el-button
                  type="text"
                  size="mini"
                  @click="editTableComment(row)"
                  icon="el-icon-edit"
                  v-if="!row.editing"
                />
              </div>
            </template>
          </el-table-column>
          
          <el-table-column prop="rowCount" label="记录数" width="100" />
          
          <el-table-column prop="dataSize" label="数据大小" width="100">
            <template #default="{ row }">
              {{ formatSize(row.dataSize || 0) }}
            </template>
          </el-table-column>
          

          
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button
                type="text"
                size="small"
                @click="manageColumns(row)"
              >
                管理字段
              </el-button>

              <el-button
                type="text"
                size="small"
                @click="previewData(row)"
              >
                预览数据
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- 添加数据库弹窗 -->
    <el-dialog
      title="添加数据库"
      v-model="showAddDbDialog"
      width="600px"
    >
      <el-form :model="dbForm" label-width="120px">
        <el-form-item label="数据库名称" required>
          <el-input v-model="dbForm.name" placeholder="请输入数据库名称" />
        </el-form-item>
        
        <el-form-item label="数据库类型">
          <el-select v-model="dbForm.dbType" style="width: 100%">
            <el-option label="MySQL" value="mysql" />
            <el-option label="PostgreSQL" value="postgresql" />
            <el-option label="Oracle" value="oracle" />
            <el-option label="SQL Server" value="sqlserver" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="主机地址" required>
          <el-input v-model="dbForm.host" placeholder="例如: 127.0.0.1" />
        </el-form-item>
        
        <el-form-item label="端口" required>
          <el-input-number v-model="dbForm.port" :min="1" :max="65535" />
        </el-form-item>
        
        <el-form-item label="数据库名" required>
          <el-input v-model="dbForm.databaseName" placeholder="数据库名称" />
        </el-form-item>
        
        <el-form-item label="用户名" required>
          <el-input v-model="dbForm.username" placeholder="数据库用户名" />
        </el-form-item>
        
        <el-form-item label="密码" required>
          <el-input v-model="dbForm.password" type="password" placeholder="数据库密码" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showAddDbDialog = false">取消</el-button>
        <el-button type="primary" @click="testConnection" :loading="testing">测试连接</el-button>
        <el-button type="primary" @click="saveDatabase" :disabled="!connectionValid">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑数据库弹窗 -->
    <el-dialog
      title="编辑数据库"
      v-model="showEditDbDialog"
      width="600px"
    >
      <el-form :model="editingDb" label-width="120px">
        <el-form-item label="数据库名称" required>
          <el-input v-model="editingDb.name" placeholder="请输入数据库名称" />
        </el-form-item>
        
        <el-form-item label="数据库类型">
          <el-select v-model="editingDb.dbType" style="width: 100%">
            <el-option label="MySQL" value="mysql" />
            <el-option label="PostgreSQL" value="postgresql" />
            <el-option label="Oracle" value="oracle" />
            <el-option label="SQL Server" value="sqlserver" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="主机地址" required>
          <el-input v-model="editingDb.host" placeholder="例如: 127.0.0.1" />
        </el-form-item>
        
        <el-form-item label="端口" required>
          <el-input-number v-model="editingDb.port" :min="1" :max="65535" />
        </el-form-item>
        
        <el-form-item label="数据库名" required>
          <el-input v-model="editingDb.databaseName" placeholder="数据库名称" />
        </el-form-item>
        
        <el-form-item label="用户名" required>
          <el-input v-model="editingDb.username" placeholder="数据库用户名" />
        </el-form-item>
        
        <el-form-item label="密码">
          <el-input v-model="editingDb.password" type="password" placeholder="留空表示不修改密码" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showEditDbDialog = false">取消</el-button>
        <el-button type="primary" @click="updateDatabase" :loading="testing">保存修改</el-button>
      </template>
    </el-dialog>

    <!-- 字段管理对话框 -->
    <el-dialog 
      v-model="showColumnManageDialog" 
      title="字段管理" 
      width="800px"
      @closed="currentTable = null; tableColumns = []"
    >
      <div v-if="currentTable">
        <h4>表名：{{ currentTable.tableName }}</h4>
        <el-table :data="tableColumns" border stripe>
          <el-table-column prop="name" label="字段名" width="150" />
          <el-table-column prop="type" label="类型" width="120" />
          <el-table-column label="属性" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.isPrimaryKey" type="danger" size="small">主键</el-tag>
              <el-tag v-if="row.isAutoIncrement" type="warning" size="small">自增</el-tag>
              <el-tag v-if="!row.nullable" type="info" size="small">非空</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="defaultValue" label="默认值" width="100" />
          <el-table-column label="备注" min-width="200">
            <template #default="{ row }">
              <div v-if="!row.editing">
                <span>{{ row.comment || '无备注' }}</span>
                <el-button 
                  type="text" 
                  size="small" 
                  @click="editColumnComment(row)"
                  style="margin-left: 10px"
                >
                  编辑
                </el-button>
              </div>
              <div v-else>
                <el-input 
                  v-model="row.tempComment" 
                  size="small"
                  @keyup.enter="saveColumnComment(row)"
                  @blur="cancelEditColumnComment(row)"
                />
                <div style="margin-top: 5px">
                  <el-button type="primary" size="small" @click="saveColumnComment(row)">保存</el-button>
                  <el-button size="small" @click="cancelEditColumnComment(row)">取消</el-button>
                </div>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>


  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

// 数据
const activeTab = ref('database')
const databases = ref([])
const knowledgeBases = ref([])
const tables = ref([])
const selectedDatabase = ref(null)
const currentTable = ref(null)
const tableColumns = ref([])


// 搜索关键词
const dbSearchKeyword = ref('')
const knowledgeSearchKeyword = ref('')
const tableSearchKeyword = ref('')

// 弹窗控制
const showTableManageDialog = ref(false)
const showAddDbDialog = ref(false)
const showEditDbDialog = ref(false)
const showAddKnowledgeDialog = ref(false)
const showColumnManageDialog = ref(false)


// 表单数据
const dbForm = ref({
  name: '',
  dbType: 'mysql',
  host: '',
  port: 3306,
  databaseName: '',
  username: '',
  password: ''
})

const editingDb = ref({
  id: null,
  name: '',
  dbType: 'mysql',
  host: '',
  port: 3306,
  databaseName: '',
  username: '',
  password: ''
})

// 状态
const syncing = ref(false)
const testing = ref(false)
const connectionValid = ref(false)

// 计算属性
const filteredDatabases = computed(() => {
  if (!dbSearchKeyword.value) return databases.value
  const keyword = dbSearchKeyword.value.toLowerCase()
  return databases.value.filter(db => 
    db.name.toLowerCase().includes(keyword) ||
    db.host.toLowerCase().includes(keyword) ||
    db.databaseName.toLowerCase().includes(keyword)
  )
})

const filteredKnowledgeBases = computed(() => {
  if (!knowledgeSearchKeyword.value) return knowledgeBases.value
  const keyword = knowledgeSearchKeyword.value.toLowerCase()
  return knowledgeBases.value.filter(kb => 
    kb.name.toLowerCase().includes(keyword) ||
    (kb.description && kb.description.toLowerCase().includes(keyword))
  )
})

const filteredTables = computed(() => {
  if (!tableSearchKeyword.value) return tables.value
  const keyword = tableSearchKeyword.value.toLowerCase()
  return tables.value.filter(table => 
    table.tableName.toLowerCase().includes(keyword) ||
    (table.tableComment && table.tableComment.toLowerCase().includes(keyword))
  )
})

// 生命周期
onMounted(() => {
  loadDatabases()
  loadKnowledgeBases()
})

// 方法
const loadDatabases = async () => {
  try {
    const result = await api.dbConfig.list({ size: 100 })
    if (result.data && result.data.records) {
      databases.value = result.data.records
    } else if (Array.isArray(result.data)) {
      databases.value = result.data
    } else {
      databases.value = []
    }
  } catch (error) {
    console.error('加载数据库失败:', error)
    ElMessage.error('加载数据库列表失败')
  }
}

const loadKnowledgeBases = async () => {
  // TODO: 实现知识库加载
  knowledgeBases.value = []
}

const selectDatabase = async (db) => {
  selectedDatabase.value = db
  showTableManageDialog.value = true
  await loadTables(db.id)
}

const loadTables = async (dbId) => {
  try {
    const result = await api.schema.getTables(dbId)
    tables.value = (result.data || []).map(table => ({
      ...table,
      editing: false,
      tempComment: table.tableComment
    }))
  } catch (error) {
    console.error('加载表列表失败:', error)
    ElMessage.error('加载表列表失败')
  }
}

const updateDbStatus = async (db) => {
  try {
    const response = await fetch(`/api/db/config/${db.id}/status`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ status: db.status })
    })
    const result = await response.json()
    if (result.code === 200) {
      ElMessage.success(db.status === 1 ? '数据库已启用' : '数据库已禁用')
    } else {
      db.status = db.status === 1 ? 0 : 1 // 回滚
      ElMessage.error(result.message || '更新失败')
    }
  } catch (error) {
    db.status = db.status === 1 ? 0 : 1 // 回滚
    ElMessage.error('更新失败')
  }
}

const editDatabase = (db) => {
  editingDb.value = { ...db }
  showEditDbDialog.value = true
}

const deleteDatabase = async (db) => {
  await ElMessageBox.confirm('确定要删除这个数据库配置吗？', '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    const result = await api.dbConfig.delete(db.id)
    ElMessage.success('删除成功')
    loadDatabases()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

const syncDatabaseSchema = async () => {
  if (!selectedDatabase.value) return
  
  syncing.value = true
  try {
    const result = await api.dbConfig.syncSchema(selectedDatabase.value.id)
    ElMessage.success('同步成功')
    await loadTables(selectedDatabase.value.id)
  } catch (error) {
    ElMessage.error(error.message || '同步失败')
  } finally {
    syncing.value = false
  }
}

const editTableComment = (row) => {
  row.editing = true
  row.tempComment = row.tableComment
}

const saveTableComment = async (row) => {
  row.editing = false
  if (row.tempComment === row.tableComment) return
  
  try {
    const result = await api.schema.updateTableComment(row.id, row.tempComment)
    row.tableComment = row.tempComment
    ElMessage.success('注释更新成功')
  } catch (error) {
    row.tempComment = row.tableComment
    ElMessage.error(error.message || '更新失败')
  }
}



const showTableDetails = (table) => {
  // TODO: 显示表详情
  ElMessage.info('表详情功能开发中')
}

const manageColumns = async (table) => {
  try {
    // 获取表字段信息
    const result = await api.tableInfo.getTableColumns(selectedDatabase.value.id, table.id)
    if (result.code === 200) {
      currentTable.value = table
      tableColumns.value = result.data || []
      showColumnManageDialog.value = true
    } else {
      ElMessage.error(result.message || '获取字段信息失败')
    }
  } catch (error) {
    console.error('获取字段信息失败:', error)
    ElMessage.error('获取字段信息失败')
  }
}

// 字段备注编辑
const editColumnComment = (column) => {
  column.editing = true
  column.tempComment = column.comment
}

const saveColumnComment = async (column) => {
  try {
    const result = await api.tableInfo.updateColumnComment(
      selectedDatabase.value.id, 
      currentTable.value.id, 
      column.name, 
      column.tempComment || ''
    )
    if (result.code === 200) {
      column.comment = column.tempComment
      column.editing = false
      ElMessage.success('字段备注更新成功')
    } else {
      ElMessage.error(result.message || '更新失败')
    }
  } catch (error) {
    console.error('更新字段备注失败:', error)
    ElMessage.error('更新字段备注失败')
  }
}

const cancelEditColumnComment = (column) => {
  column.editing = false
  column.tempComment = column.comment
}



const previewData = (table) => {
  // TODO: 预览数据
  ElMessage.info('数据预览功能开发中')
}

const testConnection = async () => {
  testing.value = true
  connectionValid.value = false
  
  try {
    const result = await api.dbConfig.testConnection({
      ...dbForm.value,
      rawPassword: dbForm.value.password
    })
    connectionValid.value = true
    ElMessage.success('连接成功')
  } catch (error) {
    ElMessage.error(error.message || '连接测试失败')
  } finally {
    testing.value = false
  }
}

const saveDatabase = async () => {
  if (!dbForm.value.name || !dbForm.value.host || !dbForm.value.databaseName) {
    ElMessage.warning('请填写必填项')
    return
  }
  
  try {
    const result = await api.dbConfig.save({
      ...dbForm.value,
      rawPassword: dbForm.value.password,
      status: 1
    })
    ElMessage.success('数据库添加成功')
    showAddDbDialog.value = false
    loadDatabases()
    
    // 重置表单
    dbForm.value = {
      name: '',
      dbType: 'mysql',
      host: '',
      port: 3306,
      databaseName: '',
      username: '',
      password: ''
    }
    connectionValid.value = false
  } catch (error) {
    ElMessage.error(error.message || '添加失败')
  }
}

const updateDatabase = async () => {
  if (!editingDb.value.name || !editingDb.value.host || !editingDb.value.databaseName) {
    ElMessage.warning('请填写必填项')
    return
  }
  
  try {
    const updateData = {
      ...editingDb.value
    }
    // 如果密码为空，不更新密码
    if (!updateData.password) {
      delete updateData.password
      delete updateData.rawPassword
    } else {
      updateData.rawPassword = updateData.password
    }
    
    const result = await api.dbConfig.save(updateData)
    ElMessage.success('数据库更新成功')
    showEditDbDialog.value = false
    loadDatabases()
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  }
}

const updateKnowledgeStatus = (kb) => {
  // TODO: 实现知识库状态更新
  ElMessage.info('功能开发中')
}

const manageKnowledge = (kb) => {
  // TODO: 实现知识库管理
  ElMessage.info('功能开发中')
}

const deleteKnowledge = (kb) => {
  // TODO: 实现知识库删除
  ElMessage.info('功能开发中')
}

const formatSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}
</script>

<style scoped lang="scss">
.data-management {
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
    
    .data-tabs {
      background: white;
      border-radius: 15px;
      padding: 20px;
      
      :deep(.el-tabs__header) {
        margin-bottom: 20px;
      }
      
      :deep(.el-tabs__nav-wrap::after) {
        display: none;
      }
      
      :deep(.el-tabs__active-bar) {
        background: linear-gradient(90deg, #667eea, #764ba2);
      }
      
      .tab-content {
        .toolbar {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 20px;
          
          .add-btn {
            background: linear-gradient(135deg, #667eea, #764ba2);
            border: none;
            color: white;
            border-radius: 20px;
            padding: 10px 20px;
            
            &:hover {
              opacity: 0.9;
            }
          }
        }
        
        .data-list {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
          gap: 20px;
          
          .data-card {
            background: #f8f9fa;
            border-radius: 12px;
            padding: 20px;
            cursor: pointer;
            transition: all 0.3s ease;
            display: flex;
            align-items: flex-start;
            gap: 15px;
            
            &:hover {
              transform: translateY(-3px);
              box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
            }
            
            &.active {
              border: 2px solid #667eea;
            }
            
            .card-icon {
              font-size: 48px;
              color: #667eea;
              flex-shrink: 0;
            }
            
            .card-info {
              flex: 1;
              
              h3 {
                margin: 0 0 8px;
                font-size: 18px;
                color: #333;
              }
              
              p {
                margin: 4px 0;
                font-size: 13px;
                color: #666;
                
                &.db-type,
                &.kb-type {
                  display: inline-block;
                  padding: 2px 8px;
                  background: #e8eaf6;
                  border-radius: 4px;
                  color: #667eea;
                  font-size: 12px;
                }
                
                &.db-status {
                  display: flex;
                  align-items: center;
                  gap: 5px;
                  
                  .status-dot {
                    width: 8px;
                    height: 8px;
                    border-radius: 50%;
                    
                    &.active {
                      background: #67c23a;
                    }
                    
                    &.inactive {
                      background: #909399;
                    }
                  }
                }
              }
            }
            
            .card-actions {
              display: flex;
              flex-direction: column;
              gap: 10px;
              align-items: flex-end;
              
              .el-button {
                margin: 0;
                
                &.danger-btn {
                  color: #f56c6c;
                  
                  &:hover {
                    color: #f23c3c;
                  }
                }
              }
            }
          }
        }
        
        .empty-state {
          text-align: center;
          padding: 80px 20px;
          
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
  }
  
  .table-manage-dialog {
    .table-manage-content {
      .table-toolbar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;
      }
      
      .table-name-cell {
        display: flex;
        align-items: center;
        justify-content: space-between;
        
        .el-button {
          opacity: 0;
          transition: opacity 0.3s;
        }
        
        &:hover .el-button {
          opacity: 1;
        }
      }
      
      .editable-cell {
        display: flex;
        align-items: center;
        gap: 5px;
        
        .el-button {
          opacity: 0;
          transition: opacity 0.3s;
        }
        
        &:hover .el-button {
          opacity: 1;
        }
      }
    }
  }
}
</style>