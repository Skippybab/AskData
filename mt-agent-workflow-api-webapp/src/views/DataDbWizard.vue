<template>
  <div class="db-wizard">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button @click="goBack" size="small"><el-icon><ArrowLeft /></el-icon>返回</el-button>
            <span class="title">{{ pageTitle }}</span>
          </div>
        </div>
      </template>

      <!-- 数据库配置表单（查看模式隐藏） -->
      <div class="pane pane-center" v-if="!isViewOnly">
        <el-form :model="form" label-width="120px" class="form-wrap">
          <el-form-item label="数据库名称">
            <el-input v-model="form.name" placeholder="如：生产库/报表库" />
          </el-form-item>
          <el-form-item label="简要介绍">
            <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请简要描述该数据库的用途和业务场景" />
          </el-form-item>
          <el-form-item label="主机">
            <el-input v-model="form.host" placeholder="127.0.0.1" />
          </el-form-item>
          <el-form-item label="端口">
            <el-input v-model="form.port" placeholder="3306" />
          </el-form-item>
          <el-form-item label="数据库">
            <el-input v-model="form.databaseName" placeholder="数据库名" />
          </el-form-item>
          <el-form-item label="账号">
            <el-input v-model="form.username" placeholder="root" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.rawPassword" type="password" show-password placeholder="******" />
          </el-form-item>
          <el-form-item>
            <el-button :loading="saving" @click="saveConfigOnly">保存配置</el-button>
            <el-button type="primary" :loading="testing" @click="testConn">测试连接</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 展示同步结果（仅查看模式） -->
      <div class="pane" v-if="isViewOnly">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-tag v-if="schemaStatus" type="info">版本: {{ schemaStatus.versionNo }} | 状态: {{ statusText(schemaStatus.status) }}</el-tag>
          </div>
          <div class="toolbar-right">
            <el-button :loading="loadingTables" @click="loadTables">刷新表清单</el-button>
            <el-button type="primary" :loading="syncing" @click="startSync">同步数据库信息</el-button>
          </div>
        </div>
        <el-table :data="tables" row-key="id" border style="margin-bottom: 12px">
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="expand-align">
                <el-table :data="row.columns" border>
                <el-table-column prop="columnName" label="字段名" width="200" />
                <el-table-column prop="dbDataType" label="源类型" width="140" />
                <el-table-column prop="normType" label="规范类型" width="120" />
                <el-table-column prop="isNullable" label="可空" width="80" />
                <el-table-column prop="columnDefault" label="默认值" width="140" />
                <el-table-column prop="columnComment" label="注释" />
                </el-table>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="tableName" label="表名" width="260" />
          <el-table-column prop="tableComment" label="表描述" />
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { dbApi } from '../api/db.js'

const router = useRouter()
const route = useRoute()
const testing = ref(false)
const saving = ref(false)
const syncing = ref(false)
const loadingTables = ref(false)

const form = ref({
  id: null,
  name: '',
  description: '',
  dbType: 'mysql',
  host: '',
  port: '3306',
  databaseName: '',
  username: '',
  rawPassword: ''
})

const schemaStatus = ref(null)
const dbConfigId = ref(null)
const tables = ref([])

const goBack = () => router.push('/admin/data')

const statusText = (s) => ({ 0:'进行中', 1:'可用', 2:'失败' }[s] || '-')
const isViewOnly = computed(() => route.query.view === '1' || route.query.view === 1)
const pageTitle = computed(() => {
  if (route.query.id && isViewOnly.value) return '查看数据库表信息'
  if (route.query.id && !isViewOnly.value) return '编辑数据库配置'
  return '创建新数据库'
})

// 加载已有配置并展示表信息（查看/编辑场景）
onMounted(async () => {
  const id = route.query.id
  if (id) {
    dbConfigId.value = Number(id)
    if (isViewOnly.value) {
      try {
        const st = await dbApi.getSchemaStatus(dbConfigId.value)
        schemaStatus.value = st.data
        await loadTables()
      } catch (_) {}
    } else {
      try {
        const cfg = await dbApi.getConfig(dbConfigId.value)
        if (cfg && cfg.data) {
          form.value = {
            id: cfg.data.id,
            name: cfg.data.name || '',
            description: cfg.data.description || '',
            dbType: cfg.data.dbType || 'mysql',
            host: cfg.data.host || '',
            port: String(cfg.data.port || '3306'),
            databaseName: cfg.data.databaseName || '',
            username: cfg.data.username || '',
            rawPassword: ''
          }
        }
      } catch (_) {}
    }
  }
})

const saveConfigOnly = async () => {
  saving.value = true
  try {
    const res = await dbApi.saveConfig(form.value)
    dbConfigId.value = res.data.id
    form.value.id = res.data.id
    ElMessage.success('配置已保存')
    // 可选：保存后尝试拉取一次最新状态（若之前已同步）
    try {
      const st = await dbApi.getSchemaStatus(dbConfigId.value)
      schemaStatus.value = st.data
    } catch (_) {}
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const testConn = async () => {
  testing.value = true
  try {
    const res = await dbApi.saveConfig(form.value)
    dbConfigId.value = res.data.id
    form.value.id = res.data.id
    await dbApi.verifyConfig(dbConfigId.value)
    ElMessage.success('连接成功')
  } catch (e) {
    ElMessage.error('连接失败')
  } finally { testing.value = false }
}

const startSync = async () => {
  if (!dbConfigId.value) {
    const res = await dbApi.saveConfig(form.value)
    dbConfigId.value = res.data.id
    form.value.id = res.data.id
  }
  syncing.value = true
  try {
    await dbApi.startSync(dbConfigId.value)
    let count = 0
    const timer = setInterval(async () => {
      count++
      const st = await dbApi.getSchemaStatus(dbConfigId.value)
      schemaStatus.value = st.data
      if (st.data && st.data.status === 1) {
        clearInterval(timer)
        ElMessage.success('同步完成')
        await loadTables()
        syncing.value = false
      } else if (count > 30) {
        clearInterval(timer)
        ElMessage.warning('同步超时，请稍后刷新')
        syncing.value = false
      }
    }, 1000)
  } catch (e) {
    ElMessage.error('同步失败')
    syncing.value = false
  }
}

const loadTables = async () => {
  if (!dbConfigId.value) return
  loadingTables.value = true
  try {
    const t = await dbApi.listTables(dbConfigId.value, 0)
    const tableList = t.data || []
    const withCols = await Promise.all(tableList.map(async (tbl) => {
      const cs = await dbApi.listColumns(dbConfigId.value, tbl.id)
      return { ...tbl, columns: cs.data || [] }
    }))
    tables.value = withCols
  } catch (e) {
    ElMessage.error('加载表失败')
  } finally { loadingTables.value = false }
}
</script>

<style scoped>
.header-left { display:flex; align-items:center; gap:10px; }
.title { font-size: 16px; font-weight: 600; }
.pane { display:flex; flex-direction:column; gap:12px; }
.pane-center { max-width: 720px; margin: 0 auto; }
.form-wrap { max-width: 720px; }

.section-header { display:flex; justify-content: space-between; align-items:center; margin: 6px 0 8px; }
.section-title { font-weight: 600; }
.section-ops { display:flex; gap: 10px; }
.toolbar { display:flex; justify-content: space-between; align-items:center; gap:10px; margin-bottom: 8px; }
.toolbar-right { display:flex; gap: 8px; }
/* 让展开区域与上层表格列对齐（不贴左边） */
.expand-align { padding-left: 305px; }
</style>
