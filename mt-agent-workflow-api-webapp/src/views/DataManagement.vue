<template>
  <div class="data-management">
    <el-container>
      <el-header>
        <h2>数据管理</h2>
      </el-header>
      
      <el-main>
        <el-tabs v-model="activeTab" type="border-card">
          <!-- 数据库配置 -->
          <el-tab-pane label="数据库配置" name="db-config">
            <div class="tab-content">
              <div class="toolbar">
                <el-button type="primary" @click="showDbConfigDialog = true">
                  添加数据库
                </el-button>
              </div>
              
              <el-table :data="dbConfigs" style="width: 100%">
                <el-table-column prop="name" label="名称" />
                <el-table-column prop="host" label="主机" />
                <el-table-column prop="port" label="端口" width="80" />
                <el-table-column prop="databaseName" label="数据库" />
                <el-table-column prop="username" label="用户名" />
                <el-table-column label="状态" width="120">
                  <template #default="scope">
                    <el-switch
                      v-model="scope.row.status"
                      :active-value="1"
                      :inactive-value="0"
                      @change="updateConfigStatus(scope.row)"
                      :loading="scope.row.statusUpdating"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="320" fixed="right">
                  <template #default="scope">
                    <el-button
                      size="small"
                      type="primary"
                      @click="testConnection(scope.row)"
                    >
                      测试连接
                    </el-button>
                    <el-button
                      size="small"
                      type="warning"
                      @click="syncSchema(scope.row)"
                    >
                      同步结构
                    </el-button>
                    <el-button
                      size="small"
                      type="success"
                      @click="editDbConfig(scope.row)"
                    >
                      编辑
                    </el-button>
                    <el-button
                      size="small"
                      type="danger"
                      @click="deleteDbConfig(scope.row)"
                    >
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
          
          <!-- 表权限管理 -->
          <el-tab-pane label="表权限管理" name="table-permissions">
            <div class="tab-content">
              <div class="toolbar">
                <el-select v-model="selectedDbConfig" placeholder="选择数据库" @change="loadTables">
                  <el-option
                    v-for="config in dbConfigs"
                    :key="config.id"
                    :label="config.name"
                    :value="config.id"
                  />
                </el-select>
                <el-button type="primary" @click="batchUpdatePermissions" :disabled="!selectedDbConfig">
                  批量设置
                </el-button>
              </div>
              
              <el-table :data="tables" style="width: 100%" @selection-change="handleSelectionChange">
                <el-table-column type="selection" width="55" />
                <el-table-column prop="tableName" label="表名" />
                <el-table-column prop="tableComment" label="说明" />
                <el-table-column prop="rowsEstimate" label="估计行数" />
                <el-table-column label="允许使用" width="120">
                  <template #default="scope">
                    <el-switch
                      v-model="scope.row.enabled"
                      :active-value="true"
                      :inactive-value="false"
                      @change="updateTablePermission(scope.row)"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120">
                  <template #default="scope">
                    <el-button size="small" @click="viewColumns(scope.row)">
                      查看字段
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-main>
    </el-container>

    <!-- 数据库配置对话框 -->
    <el-dialog v-model="showDbConfigDialog" title="数据库配置" width="600px">
      <el-form :model="dbConfigForm" :rules="dbConfigRules" ref="dbConfigFormRef" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="dbConfigForm.name" placeholder="请输入数据库配置名称" />
        </el-form-item>
        <el-form-item label="数据库类型" prop="dbType">
          <el-select v-model="dbConfigForm.dbType" placeholder="选择数据库类型">
            <el-option label="MySQL" value="mysql" />
            <el-option label="PostgreSQL" value="postgresql" />
          </el-select>
        </el-form-item>
        <el-form-item label="主机" prop="host">
          <el-input v-model="dbConfigForm.host" placeholder="请输入主机地址" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="dbConfigForm.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="数据库名" prop="databaseName">
          <el-input v-model="dbConfigForm.databaseName" placeholder="请输入数据库名" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="dbConfigForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="rawPassword">
          <el-input v-model="dbConfigForm.rawPassword" type="password" placeholder="请输入密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDbConfigDialog = false">取消</el-button>
        <el-button type="primary" @click="saveDbConfig">保存</el-button>
      </template>
    </el-dialog>

    <!-- 字段查看对话框 -->
    <el-dialog v-model="showColumnsDialog" title="表字段" width="800px">
      <el-table :data="columns" style="width: 100%">
        <el-table-column prop="columnName" label="字段名" />
        <el-table-column prop="dbDataType" label="数据类型" />
        <el-table-column prop="isNullable" label="可空">
          <template #default="scope">
            <el-tag :type="scope.row.isNullable === 1 ? 'warning' : 'success'">
              {{ scope.row.isNullable === 1 ? '可空' : '非空' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="columnDefault" label="默认值" />
        <el-table-column prop="columnComment" label="说明" />
      </el-table>
    </el-dialog>

    <!-- 批量权限设置对话框 -->
    <el-dialog v-model="showBatchDialog" title="批量设置权限" width="400px">
      <el-form label-width="100px">
        <el-form-item label="允许使用">
          <el-switch v-model="batchEnabled" :active-value="true" :inactive-value="false" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBatchDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmBatchUpdate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { dbApi } from '@/api/db'

export default {
  name: 'DataManagement',
  setup() {
    const activeTab = ref('db-config')
    const showDbConfigDialog = ref(false)
    const showColumnsDialog = ref(false)
    const showBatchDialog = ref(false)
    
    const dbConfigs = ref([])
    const tables = ref([])
    const columns = ref([])
    const selectedDbConfig = ref(null)
    const selectedTables = ref([])
    const batchEnabled = ref(true)
    
    const dbConfigForm = reactive({
      name: '',
      dbType: 'mysql',
      host: '',
      port: 3306,
      databaseName: '',
      username: '',
      rawPassword: ''
    })
    
    const dbConfigRules = {
      name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
      host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
      port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
      databaseName: [{ required: true, message: '请输入数据库名', trigger: 'blur' }],
      username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
      rawPassword: [{ required: true, message: '请输入密码', trigger: 'blur' }]
    }

    // 加载数据库配置
    const loadDbConfigs = async () => {
      try {
        const response = await dbApi.listConfigs({ current: 1, size: 100 })
        dbConfigs.value = response.data.records || []
      } catch (error) {
        ElMessage.error('加载数据库配置失败')
      }
    }

    // 加载表列表
    const loadTables = async () => {
      if (!selectedDbConfig.value) return
      
      try {
        const response = await dbApi.listTables(selectedDbConfig.value)
        tables.value = response.data || []
      } catch (error) {
        ElMessage.error('加载表列表失败')
      }
    }

    // 测试连接
    const testConnection = async (config) => {
      try {
        ElMessage.info('正在测试连接...')
        await dbApi.verifyConfig(config.id)
        ElMessage.success('连接成功')
        // 更新配置状态
        config.status = 1
      } catch (error) {
        console.error('连接测试失败:', error)
        let errorMessage = '连接失败'
        
        if (error.response) {
          // 服务器响应了错误状态码
          if (error.response.status === 500) {
            errorMessage = '服务器内部错误，可能是数据库配置问题或网络连接问题'
          } else if (error.response.status === 403) {
            errorMessage = '权限不足，无法访问该数据库配置'
          } else if (error.response.status === 404) {
            errorMessage = '数据库配置不存在'
          } else {
            errorMessage = `服务器错误: ${error.response.status}`
          }
        } else if (error.request) {
          // 请求已发出但没有收到响应
          errorMessage = '网络连接失败，请检查网络设置'
        } else {
          // 其他错误
          errorMessage = error.message || '未知错误'
        }
        
        ElMessage.error(errorMessage)
        // 更新配置状态
        config.status = 0
      }
    }

    // 同步结构
    const syncSchema = async (config) => {
      try {
        await dbApi.startSync(config.id)
        ElMessage.success('开始同步数据库结构')
        // 等待同步完成后刷新表列表
        setTimeout(() => {
          if (selectedDbConfig.value === config.id) {
            loadTables()
          }
        }, 3000)
      } catch (error) {
        ElMessage.error('同步失败: ' + error.message)
      }
    }

    // 编辑数据库配置
    const editDbConfig = (config) => {
      Object.assign(dbConfigForm, config)
      showDbConfigDialog.value = true
    }

    // 更新数据库配置状态
    const updateConfigStatus = async (config) => {
      try {
        config.statusUpdating = true
        await dbApi.updateConfigStatus(config.id, { status: config.status })
        ElMessage.success('状态更新成功')
        
        // 如果启用，尝试建立连接池
        if (config.status === 1) {
          ElMessage.info('正在建立数据库连接...')
        }
      } catch (error) {
        // 恢复原状态
        config.status = config.status === 1 ? 0 : 1
        ElMessage.error('状态更新失败: ' + error.message)
      } finally {
        config.statusUpdating = false
      }
    }

    // 删除数据库配置
    const deleteDbConfig = async (config) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除数据库配置 "${config.name}" 吗？此操作不可逆。`,
          '删除确认',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          }
        )
        
        await dbApi.deleteConfig(config.id)
        ElMessage.success('删除成功')
        loadDbConfigs()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败: ' + error.message)
        }
      }
    }

    // 保存数据库配置
    const saveDbConfig = async () => {
      try {
        await dbApi.saveConfig(dbConfigForm)
        ElMessage.success('保存成功')
        showDbConfigDialog.value = false
        loadDbConfigs()
        // 重置表单
        Object.assign(dbConfigForm, {
          name: '',
          dbType: 'mysql',
          host: '',
          port: 3306,
          databaseName: '',
          username: '',
          rawPassword: ''
        })
      } catch (error) {
        ElMessage.error('保存失败: ' + error.message)
      }
    }

    // 更新表权限
    const updateTablePermission = async (table) => {
      try {
        console.log('更新表权限:', {
          dbConfigId: selectedDbConfig.value,
          tableId: table.id,
          enabled: table.enabled
        })
        
        const response = await fetch(`http://localhost:8080/api/db/schema/${selectedDbConfig.value}/tables/${table.id}/enabled`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
            // 最小闭环阶段，不需要权限验证
          },
          body: JSON.stringify({ enabled: table.enabled })
        })
        
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }
        
        const result = await response.json()
        if (result.code === 200) {
          ElMessage.success('更新成功')
        } else {
          throw new Error(result.message || '更新失败')
        }
      } catch (error) {
        console.error('更新表权限失败:', error)
        ElMessage.error('更新失败: ' + error.message)
        // 恢复原状态
        table.enabled = !table.enabled
      }
    }

    // 查看字段
    const viewColumns = async (table) => {
      try {
        const response = await dbApi.listColumns(selectedDbConfig.value, table.id)
        columns.value = response.data || []
        showColumnsDialog.value = true
      } catch (error) {
        ElMessage.error('加载字段失败')
      }
    }

    // 选择变化
    const handleSelectionChange = (selection) => {
      selectedTables.value = selection
    }

    // 批量更新权限
    const batchUpdatePermissions = () => {
      if (selectedTables.value.length === 0) {
        ElMessage.warning('请选择要设置的表')
        return
      }
      showBatchDialog.value = true
    }

    // 确认批量更新
    const confirmBatchUpdate = async () => {
      try {
        const tableIds = selectedTables.value.map(table => table.id)
        console.log('批量更新表权限:', {
          dbConfigId: selectedDbConfig.value,
          tableIds: tableIds,
          enabled: batchEnabled.value
        })
        
        const response = await fetch(`http://localhost:8080/api/db/schema/${selectedDbConfig.value}/tables/enabled`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
            // 最小闭环阶段，不需要权限验证
          },
          body: JSON.stringify({ 
            tableIds: tableIds,
            enabled: batchEnabled.value 
          })
        })
        
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }
        
        const result = await response.json()
        if (result.code === 200) {
          ElMessage.success('批量设置成功')
          showBatchDialog.value = false
          loadTables() // 刷新列表
        } else {
          throw new Error(result.message || '批量设置失败')
        }
      } catch (error) {
        console.error('批量更新表权限失败:', error)
        ElMessage.error('批量设置失败: ' + error.message)
      }
    }

    onMounted(() => {
      loadDbConfigs()
    })

    return {
      activeTab,
      showDbConfigDialog,
      showColumnsDialog,
      showBatchDialog,
      dbConfigs,
      tables,
      columns,
      selectedDbConfig,
      selectedTables,
      batchEnabled,
      dbConfigForm,
      dbConfigRules,
      loadTables,
      testConnection,
      syncSchema,
      editDbConfig,
      saveDbConfig,
      updateTablePermission,
      viewColumns,
      handleSelectionChange,
      batchUpdatePermissions,
      confirmBatchUpdate
    }
  }
}
</script>

<style scoped>
.data-management {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.el-header {
  background: white;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  padding: 0 20px;
}

.el-header h2 {
  margin: 0;
  color: #303133;
}

.el-main {
  flex: 1;
  padding: 20px;
  background: #f5f5f5;
}

.tab-content {
  background: white;
  border-radius: 4px;
  padding: 20px;
}

.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.el-table {
  margin-top: 20px;
}
</style>
