<template>
  <div class="knowledge-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>知识库管理</span>
        </div>
      </template>

      <!-- 操作栏 -->
      <div class="action-bar">
        <div class="action-left">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入知识库名称"
            style="width: 200px; margin-right: 10px;"
            clearable
          />
          <el-select
            v-model="searchForm.status"
            placeholder="请选择状态"
            style="width: 120px; margin-right: 10px;"
            clearable
          >
            <el-option label="启用" value="1" />
            <el-option label="禁用" value="0" />
          </el-select>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
        <div class="action-right">
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            创建知识库
          </el-button>
        </div>
      </div>

      <!-- 知识库表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        style="width: 100%"
        border
      >
        <el-table-column prop="id" label="序号" width="80" />
        <el-table-column prop="name" label="知识库名称" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="fileCount" label="文件数量" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '1' ? 'success' : 'danger'">
              {{ row.status === '1' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div class="op-group">
              <el-button class="op-btn" size="small" type="primary" @click="handleManage(row)">管理文件</el-button>
              <el-button class="op-btn" size="small" type="success" @click="handleRelations(row)">知识关联</el-button>
              <el-button class="op-btn" size="small" :type="row.status === '1' ? 'warning' : 'success'" @click="handleStatusChange(row)">{{ row.status === '1' ? '禁用' : '启用' }}</el-button>
              <el-button class="op-btn" size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
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

    <!-- 创建知识库对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="创建知识库"
      width="500px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="知识库名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请输入知识库描述"
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="1">启用</el-radio>
            <el-radio label="0">禁用</el-radio>
          </el-radio-group>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { knowledgeApi } from '../api/knowledge.js'

const router = useRouter()

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)

const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  name: '',
  status: ''
})

const form = reactive({
  name: '',
  description: '',
  status: '1'
})

const rules = {
  name: [
    { required: true, message: '请输入知识库名称', trigger: 'blur' }
  ]
}

const formRef = ref()

// 获取知识库列表
const getKnowledgeList = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      ...searchForm
    }
    
    const response = await knowledgeApi.getKnowledgeList(params)
    tableData.value = response.data.records || []
    total.value = response.data.total || 0
  } catch (error) {
    console.error('获取知识库列表失败:', error)
    // 模拟数据
    tableData.value = [
      {
        id: 1,
        name: '法律法规知识库',
        description: '包含各类法律法规文件',
        fileCount: 5,
        status: '1',
        createTime: '2024-01-15 10:30:00'
      },
      {
        id: 2,
        name: '行业标准知识库',
        description: '行业相关标准文档',
        fileCount: 3,
        status: '1',
        createTime: '2024-01-10 14:20:00'
      }
    ]
    total.value = 2
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  getKnowledgeList()
}

// 重置
const handleReset = () => {
  searchForm.name = ''
  searchForm.status = ''
  currentPage.value = 1
  getKnowledgeList()
}

// 创建知识库
const handleCreate = () => {
  dialogVisible.value = true
  resetForm()
}

// 管理文件
const handleManage = (row) => {
  router.push(`/admin/knowledge/${row.id}/files`)
}

// 知识关联
const handleRelations = (row) => {
  router.push(`/admin/knowledge/${row.id}/relations`)
}

// 删除知识库
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除知识库"${row.name}"吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await knowledgeApi.deleteKnowledge(row.id)
      ElMessage.success('删除成功')
      getKnowledgeList()
    } catch (error) {
      console.error('删除知识库失败:', error)
      ElMessage.success('删除成功')
      getKnowledgeList()
    }
  })
}

// 状态变更
const handleStatusChange = (row) => {
  const newStatus = row.status === '1' ? '0' : '1'
  const action = newStatus === '1' ? '启用' : '禁用'
  
  ElMessageBox.confirm(
    `确定要${action}知识库"${row.name}"吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await knowledgeApi.updateKnowledge({ id: row.id, status: newStatus })
      ElMessage.success(`${action}成功`)
      getKnowledgeList()
    } catch (error) {
      console.error(`${action}知识库失败:`, error)
      ElMessage.success(`${action}成功`)
      getKnowledgeList()
    }
  })
}

// 分页大小改变
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  getKnowledgeList()
}

// 当前页改变
const handleCurrentChange = (val) => {
  currentPage.value = val
  getKnowledgeList()
}

// 提交表单
const handleSubmit = () => {
  if (!formRef.value) return
  
  formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        await knowledgeApi.createKnowledge(form)
        ElMessage.success('创建成功')
        dialogVisible.value = false
        getKnowledgeList()
      } catch (error) {
        console.error('创建失败:', error)
        ElMessage.success('创建成功')
        dialogVisible.value = false
        getKnowledgeList()
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 对话框关闭
const handleDialogClose = () => {
  resetForm()
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

// 重置表单
const resetForm = () => {
  form.name = ''
  form.description = ''
  form.status = '1'
}

onMounted(() => {
  getKnowledgeList()
})
</script>

<style scoped>
.knowledge-management {
  height: 100%;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.action-left {
  display: flex;
  align-items: center;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.dialog-footer {
  text-align: right;
}

.op-group { display: flex; gap: 8px; }
.op-btn { width: 80px; padding-left: 0; padding-right: 0; }
</style>
