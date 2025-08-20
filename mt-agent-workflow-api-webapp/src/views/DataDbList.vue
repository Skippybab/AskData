<template>
  <div class="db-list">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button @click="goBack" size="small"><el-icon><ArrowLeft /></el-icon>返回</el-button>
            <span>管理现有数据库</span>
          </div>
        </div>
      </template>

      <div class="search-bar">
        <div class="search-left">
          <el-input v-model="query.name" placeholder="请输入数据库名称" style="width: 200px; margin-right: 10px;" clearable />
          <el-input v-model="query.host" placeholder="请输入IP" style="width: 160px; margin-right: 10px;" clearable />
          <el-button type="primary" @click="search"><el-icon><Search/></el-icon>搜索</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
        <div class="search-right">
          <el-button type="primary" @click="create"><el-icon><Plus/></el-icon>新建</el-button>
        </div>
      </div>

      <el-table :data="table" v-loading="loading" border>
        <el-table-column prop="id" label="序号" width="80" />
        <el-table-column prop="name" label="数据库名称" width="160"/>
        <el-table-column prop="host" label="主机" width="350" />
        <el-table-column prop="port" label="端口" width="100" />
        <el-table-column prop="database" label="数据库" width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status==='1'?'success':'danger'">{{ row.status==='1'?'启用':'禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="op-group">
              <el-button class="op-btn" size="small" type="primary" @click="view(row)">查看</el-button>
              <el-button class="op-btn" size="small" :type="row.status==='1'?'warning':'success'" @click="toggle(row)">{{ row.status==='1'?'禁用':'启用' }}</el-button>
              <el-button class="op-btn" size="small" type="primary" @click="edit(row)">编辑</el-button>
              <el-button class="op-btn" size="small" type="danger" @click="remove(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10,20,50,100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="onSize"
          @current-change="onPage"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { dbApi } from '../api/db.js'

const router = useRouter()
const query = reactive({ name: '', host: '' })
const table = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const fetchList = async () => {
  loading.value = true
  try {
    const params = { current: page.value, size: size.value, name: query.name, host: query.host }
    const res = await dbApi.listConfigs(params)
    const records = (res.data && res.data.records) || []
    table.value = records.map(x => ({
      id: x.id,
      name: x.name,
      host: x.host,
      port: String(x.port || ''),
      database: x.databaseName,
      status: String(x.status || '1')
    }))
    total.value = (res.data && res.data.total) || 0
  } catch (e) {
    ElMessage.error('加载失败')
  } finally { loading.value = false }
}

const search = () => { page.value = 1; fetchList() }
const reset = () => { query.name=''; query.host=''; page.value=1; fetchList() }
const onSize = (v) => { size.value = v; page.value = 1; fetchList() }
const onPage = (v) => { page.value = v; fetchList() }

const create = () => router.push('/admin/data/new')
const edit = (row) => router.push({ path: '/admin/data/new', query: { id: row.id } })
const view = (row) => router.push({ path: '/admin/data/new', query: { id: row.id, view: 1 } })
const goBack = () => router.push('/admin/data')
const toggle = async (row) => {
  // 后端暂未提供启停接口，这里仅提示，后续可扩展
  ElMessage.warning('暂未提供启用/禁用接口')
}
const remove = (row) => {
  ElMessageBox.confirm(`确定删除数据库“${row.name}”吗？`, '提示', { type: 'warning' }).then(() => {
    ElMessage.warning('暂未提供删除接口')
  })
}

onMounted(fetchList)
</script>

<style scoped>
.search-bar { display:flex; justify-content: space-between; align-items:center; margin-bottom: 12px; }
.search-left { display:flex; align-items:center; }
.pagination { display:flex; justify-content:center; margin-top: 12px; }
.op-group { display:flex; gap:8px; }
.op-btn { width: 72px; padding-left:0; padding-right:0; }
</style>
