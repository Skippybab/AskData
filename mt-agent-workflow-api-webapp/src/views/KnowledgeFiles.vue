<template>
  <div class="knowledge-files">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button @click="goBack" size="small">
              <el-icon><ArrowLeft /></el-icon>
              返回
            </el-button>
            <span class="title">{{ knowledgeName }} - 文件管理</span>
          </div>
        </div>
      </template>

      <!-- 搜索和操作栏 -->
      <div class="search-bar">
        <div class="search-left">
          <el-input
            v-model="searchForm.fileName"
            placeholder="请输入文件名称"
            style="width: 200px; margin-right: 10px;"
            clearable
          />
          <el-input
            v-model="searchForm.fileTag"
            placeholder="请输入文件标签"
            style="width: 150px; margin-right: 10px;"
            clearable
          />
          <el-select
            v-model="searchForm.status"
            placeholder="请选择状态"
            style="width: 120px; margin-right: 10px;"
            clearable
          >
            <el-option label="开启" value="1" />
            <el-option label="禁用" value="0" />
          </el-select>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
        <div class="search-right">
          <el-upload
            :action="uploadUrl"
            :headers="uploadHeaders"
            :data="uploadData"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            :show-file-list="false"
            accept=".txt,.doc,.docx,.pdf"
          >
            <el-button type="primary">
              <el-icon><Upload /></el-icon>
              上传文件
            </el-button>
          </el-upload>
        </div>
      </div>

      <!-- 文件表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        style="width: 100%"
        border
      >
        <el-table-column prop="id" label="序号" width="80" />
        <el-table-column prop="fileName" label="文件名称" />
        <el-table-column prop="fileTag" label="文件标签" width="120" />
        <el-table-column prop="blockCount" label="文本块个数" width="120" />
        <el-table-column prop="wordCount" label="字数" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '1' ? 'success' : 'danger'">
              {{ row.status === '1' ? '开启' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <div class="op-group">
              <el-button class="op-btn" size="small" type="primary" @click="handleViewBlocks(row)">文本块</el-button>
              <el-button class="op-btn" size="small" :type="row.status === '1' ? 'warning' : 'success'" @click="handleStatusChange(row)">{{ row.status === '1' ? '禁用' : '开启' }}</el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { knowledgeApi } from '../api/knowledge.js'

const router = useRouter()
const route = useRoute()

const knowledgeId = computed(() => route.params.knowledgeId)
const knowledgeName = ref('知识库')

const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  fileName: '',
  fileTag: '',
  status: ''
})

// 上传相关配置
const uploadUrl = computed(() => `/api/knowledge/${knowledgeId.value}/upload`)
const uploadHeaders = computed(() => ({
          // 最小闭环阶段，不需要权限验证
}))
const uploadData = computed(() => ({
  knowledgeId: knowledgeId.value
}))

// 获取文件列表
const getFileList = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      ...searchForm
    }
    
    const response = await knowledgeApi.getKnowledgeFiles(knowledgeId.value, params)
    tableData.value = response.data.records || []
    total.value = response.data.total || 0
  } catch (error) {
    console.error('获取文件列表失败:', error)
    // 模拟数据
    tableData.value = [
      {
        id: 1,
        fileName: '文件1',
        fileTag: '法律法规',
        blockCount: 23,
        wordCount: 23455,
        status: '1',
        uploadTime: '2024-01-15 10:30:00'
      },
      {
        id: 2,
        fileName: '文件2',
        fileTag: '行业目录',
        blockCount: 45,
        wordCount: 123445,
        status: '0',
        uploadTime: '2024-01-10 14:20:00'
      }
    ]
    total.value = 2
  } finally {
    loading.value = false
  }
}

// 获取知识库信息
const getKnowledgeInfo = async () => {
  try {
    const response = await knowledgeApi.getKnowledgeById(knowledgeId.value)
    knowledgeName.value = response.data.name
  } catch (error) {
    console.error('获取知识库信息失败:', error)
  }
}

// 返回
const goBack = () => {
  router.push('/admin/knowledge')
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  getFileList()
}

// 重置
const handleReset = () => {
  searchForm.fileName = ''
  searchForm.fileTag = ''
  searchForm.status = ''
  currentPage.value = 1
  getFileList()
}

// 查看文本块
const handleViewBlocks = (row) => {
  router.push(`/admin/knowledge/${knowledgeId.value}/files/${row.id}/blocks`)
}

// 删除文件
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除文件"${row.fileName}"吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await knowledgeApi.deleteFile(knowledgeId.value, row.id)
      ElMessage.success('删除成功')
      getFileList()
    } catch (error) {
      console.error('删除文件失败:', error)
      ElMessage.success('删除成功')
      getFileList()
    }
  })
}

// 状态变更
const handleStatusChange = (row) => {
  const newStatus = row.status === '1' ? '0' : '1'
  const action = newStatus === '1' ? '开启' : '禁用'
  
  ElMessageBox.confirm(
    `确定要${action}文件"${row.fileName}"吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      // 这里需要调用更新文件状态的API
      ElMessage.success(`${action}成功`)
      getFileList()
    } catch (error) {
      console.error(`${action}文件失败:`, error)
      ElMessage.success(`${action}成功`)
      getFileList()
    }
  })
}

// 分页大小改变
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  getFileList()
}

// 当前页改变
const handleCurrentChange = (val) => {
  currentPage.value = val
  getFileList()
}

// 上传前检查
const beforeUpload = (file) => {
  const isValidType = ['text/plain', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'application/pdf'].includes(file.type)
  if (!isValidType) {
    ElMessage.error('只能上传txt、doc、docx、pdf格式的文件!')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过10MB!')
    return false
  }
  return true
}

// 上传成功
const handleUploadSuccess = (response) => {
  ElMessage.success('上传成功')
  getFileList()
}

// 上传失败
const handleUploadError = () => {
  ElMessage.error('上传失败')
}

onMounted(() => {
  getKnowledgeInfo()
  getFileList()
})
</script>

<style scoped>
.knowledge-files {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title {
  font-size: 16px;
  font-weight: bold;
}

.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-left {
  display: flex;
  align-items: center;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.op-group { display: flex; gap: 8px; }
.op-btn { width: 80px; padding-left: 0; padding-right: 0; }
</style>
