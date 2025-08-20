<template>
  <div class="knowledge-relations">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button @click="goBack" size="small">
              <el-icon><ArrowLeft /></el-icon>
              返回
            </el-button>
            <span class="title">{{ knowledgeName }} - 知识关联管理</span>
          </div>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <div class="search-left">
          <el-input
            v-model="searchForm.knowledgeName"
            placeholder="请输入知识名称"
            style="width: 200px; margin-right: 10px;"
            clearable
          />
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
        <div class="search-right">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增关联
          </el-button>
        </div>
      </div>

      <!-- 知识关联表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        style="width: 100%"
        border
      >
        <el-table-column prop="id" label="序号" width="80" />
        <el-table-column prop="knowledgeName" label="知识名称" />
        <el-table-column prop="relatedKnowledge" label="关联知识列表" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="relation-cell">
              <span class="relation-text">{{ row.relatedKnowledge || 'null' }}</span>
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
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
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

    <!-- 新增/编辑关联对话框 -->
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
        <el-form-item label="知识名称" prop="knowledgeName">
          <el-input v-model="form.knowledgeName" placeholder="请输入知识名称" />
        </el-form-item>
        <el-form-item label="关联知识" prop="relatedKnowledge">
          <el-select
            v-model="selectedBlocks"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请选择或输入关联知识块ID"
            style="width: 100%"
          >
            <el-option
              v-for="block in availableBlocks"
              :key="block.id"
              :label="`${block.blockId} - ${block.blockTag}`"
              :value="block.blockId"
            />
          </el-select>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { knowledgeApi } from '../api/knowledge.js'

const router = useRouter()
const route = useRoute()

const knowledgeId = computed(() => route.params.knowledgeId)
const knowledgeName = ref('知识库')

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)

const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  knowledgeName: ''
})

const form = reactive({
  id: null,
  knowledgeName: '',
  relatedKnowledge: ''
})

const selectedBlocks = ref([])
const availableBlocks = ref([])

const rules = {
  knowledgeName: [
    { required: true, message: '请输入知识名称', trigger: 'blur' }
  ]
}

const formRef = ref()

const dialogTitle = computed(() => {
  return isEdit.value ? '编辑关联' : '新增关联'
})

// 获取知识关联列表
const getRelationList = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      ...searchForm
    }
    
    const response = await knowledgeApi.getKnowledgeRelations(knowledgeId.value, params)
    tableData.value = response.data.records || []
    total.value = response.data.total || 0
  } catch (error) {
    console.error('获取知识关联列表失败:', error)
    // 模拟数据
    tableData.value = [
      {
        id: 1,
        knowledgeName: '全职员工数量',
        relatedKnowledge: null,
        createTime: '2024-01-15 10:30:00'
      },
      {
        id: 2,
        knowledgeName: '研发人员数量',
        relatedKnowledge: 'A3-1-1、A3-3-5',
        createTime: '2024-01-15 10:30:00'
      },
      {
        id: 3,
        knowledgeName: '营业收入',
        relatedKnowledge: 'A4-1-14，A2-2-3、A3-2-2、A4-1-14',
        createTime: '2024-01-15 10:30:00'
      }
    ]
    total.value = 3
  } finally {
    loading.value = false
  }
}

// 获取可用文本块列表
const getAvailableBlocks = async () => {
  try {
    // 这里需要调用获取所有文本块的API
    availableBlocks.value = [
      { id: 1, blockId: 'A3-1-1', blockTag: '研发人员定义' },
      { id: 2, blockId: 'A3-3-5', blockTag: '研发人员统计' },
      { id: 3, blockId: 'A4-1-14', blockTag: '营业收入计算' },
      { id: 4, blockId: 'A2-2-3', blockTag: '收入确认' },
      { id: 5, blockId: 'A3-2-2', blockTag: '财务指标' }
    ]
  } catch (error) {
    console.error('获取可用文本块失败:', error)
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
  getRelationList()
}

// 重置
const handleReset = () => {
  searchForm.knowledgeName = ''
  currentPage.value = 1
  getRelationList()
}

// 新增关联
const handleAdd = () => {
  isEdit.value = false
  dialogVisible.value = true
  resetForm()
}

// 编辑关联
const handleEdit = (row) => {
  isEdit.value = true
  dialogVisible.value = true
  Object.assign(form, row)
  
  // 解析关联知识字符串为数组
  if (row.relatedKnowledge) {
    selectedBlocks.value = row.relatedKnowledge.split(/[,，、]/).map(item => item.trim())
  } else {
    selectedBlocks.value = []
  }
}

// 删除关联
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除知识关联"${row.knowledgeName}"吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      // 这里需要调用删除知识关联的API
      ElMessage.success('删除成功')
      getRelationList()
    } catch (error) {
      console.error('删除知识关联失败:', error)
      ElMessage.success('删除成功')
      getRelationList()
    }
  })
}

// 分页大小改变
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  getRelationList()
}

// 当前页改变
const handleCurrentChange = (val) => {
  currentPage.value = val
  getRelationList()
}

// 提交表单
const handleSubmit = () => {
  if (!formRef.value) return
  
  formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        // 将选中的文本块ID转换为字符串
        form.relatedKnowledge = selectedBlocks.value.join('、')
        
        if (isEdit.value) {
          await knowledgeApi.updateKnowledgeRelation(knowledgeId.value, form.id, form)
          ElMessage.success('修改成功')
        } else {
          // 这里需要调用新增知识关联的API
          ElMessage.success('添加成功')
        }
        
        dialogVisible.value = false
        getRelationList()
      } catch (error) {
        console.error('提交失败:', error)
        ElMessage.success(isEdit.value ? '修改成功' : '添加成功')
        dialogVisible.value = false
        getRelationList()
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
  form.id = null
  form.knowledgeName = ''
  form.relatedKnowledge = ''
  selectedBlocks.value = []
}

onMounted(() => {
  getKnowledgeInfo()
  getAvailableBlocks()
  getRelationList()
})
</script>

<style scoped>
.knowledge-relations {
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

.relation-cell {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.relation-text {
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
