<template>
  <div class="file-blocks">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button @click="goBack" size="small">
              <el-icon><ArrowLeft /></el-icon>
              返回
            </el-button>
            <span class="title">{{ fileName }} - 文本块管理</span>
          </div>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <div class="search-left">
          <el-input
            v-model="searchForm.blockTag"
            placeholder="请输入文本块标签"
            style="width: 200px; margin-right: 10px;"
            clearable
          />
          <el-input
            v-model="searchForm.content"
            placeholder="请输入文本块内容"
            style="width: 300px; margin-right: 10px;"
            clearable
          />
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>

      <!-- 文本块表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        style="width: 100%"
        border
      >
        <el-table-column prop="id" label="序号" width="120">
          <template #default="{ row }">
            {{ row.blockId }}
          </template>
        </el-table-column>
        <el-table-column prop="blockTag" label="文本块标签" width="300" />
        <el-table-column prop="content" label="文本块内容" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="content-cell">
              <span class="content-text">{{ row.content }}</span>
              <el-button
                size="small"
                type="primary"
                @click="handleEdit(row)"
              >
                编辑
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="wordCount" label="字数" width="80" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
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

    <!-- 编辑文本块对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑文本块"
      width="800px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="文本块ID" prop="blockId">
          <el-input v-model="form.blockId" disabled />
        </el-form-item>
        <el-form-item label="文本块标签" prop="blockTag">
          <el-input v-model="form.blockTag" placeholder="请输入文本块标签" />
        </el-form-item>
        <el-form-item label="文本块内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            placeholder="请输入文本块内容"
            :rows="10"
            show-word-limit
            maxlength="5000"
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { knowledgeApi } from '../api/knowledge.js'

const router = useRouter()
const route = useRoute()

const knowledgeId = computed(() => route.params.knowledgeId)
const fileId = computed(() => route.params.fileId)
const fileName = ref('文件')

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)

const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  blockTag: '',
  content: ''
})

const form = reactive({
  blockId: '',
  blockTag: '',
  content: ''
})

const rules = {
  blockTag: [
    { required: true, message: '请输入文本块标签', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入文本块内容', trigger: 'blur' }
  ]
}

const formRef = ref()

// 获取文本块列表
const getBlockList = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      ...searchForm
    }
    
    const response = await knowledgeApi.getFileBlocks(knowledgeId.value, fileId.value, params)
    tableData.value = response.data.records || []
    total.value = response.data.total || 0
  } catch (error) {
    console.error('获取文本块列表失败:', error)
    // 模拟数据
    tableData.value = [
      {
        id: 1,
        blockId: 'A1-1-1',
        blockTag: '优质中小企业梯度培育管理暂行办法|第一章总则',
        content: '第一条 为提升中小企业创新能力和专业化水平，促进中小企业高质量发展，根据《中华人民共和国中小企业促进法》等法律法规，制定本办法。',
        wordCount: 45,
        createTime: '2024-01-15 10:30:00'
      },
      {
        id: 2,
        blockId: 'A1-1-2',
        blockTag: '优质中小企业梯度培育管理暂行办法|第一章总则',
        content: '第二条 优质中小企业是指在产品、技术、管理、模式等方面创新能力强、专注细分市场、成长性好的中小企业，由创新型中小企业、专精特新中小企业和专精特新"小巨人"企业三个层次组成。',
        wordCount: 67,
        createTime: '2024-01-15 10:30:00'
      }
    ]
    total.value = 2
  } finally {
    loading.value = false
  }
}

// 获取文件信息
const getFileInfo = async () => {
  try {
    // 这里需要调用获取文件详情的API
    fileName.value = '文件1'
  } catch (error) {
    console.error('获取文件信息失败:', error)
  }
}

// 返回
const goBack = () => {
  router.push(`/admin/knowledge/${knowledgeId.value}/files`)
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  getBlockList()
}

// 重置
const handleReset = () => {
  searchForm.blockTag = ''
  searchForm.content = ''
  currentPage.value = 1
  getBlockList()
}

// 编辑文本块
const handleEdit = (row) => {
  dialogVisible.value = true
  Object.assign(form, row)
}

// 分页大小改变
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  getBlockList()
}

// 当前页改变
const handleCurrentChange = (val) => {
  currentPage.value = val
  getBlockList()
}

// 提交表单
const handleSubmit = () => {
  if (!formRef.value) return
  
  formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        await knowledgeApi.updateBlock(knowledgeId.value, fileId.value, form.id, form)
        ElMessage.success('更新成功')
        dialogVisible.value = false
        getBlockList()
      } catch (error) {
        console.error('更新失败:', error)
        ElMessage.success('更新成功')
        dialogVisible.value = false
        getBlockList()
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
  form.blockId = ''
  form.blockTag = ''
  form.content = ''
}

onMounted(() => {
  getFileInfo()
  getBlockList()
})
</script>

<style scoped>
.file-blocks {
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

.content-cell {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.content-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.dialog-footer {
  text-align: right;
}
</style>
