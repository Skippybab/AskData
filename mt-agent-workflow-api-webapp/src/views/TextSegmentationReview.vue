<template>
  <div class="segmentation-review">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button @click="exit" size="small"><el-icon><Close /></el-icon>退出</el-button>
            <span class="title">文本切分审查窗口</span>
          </div>
        </div>
      </template>

      <div class="meta">
        <div><b>当前审核文本</b>：{{ doc.label }}（文章标签）</div>
      </div>

      <div class="content-box">
        <div class="content-title">文本内容：</div>
        <div class="blocks">
          <div v-for="(b, idx) in blocks" :key="b.id" class="block-item">
            <div class="block-index">{{ idx + 1 }}.</div>
            <div class="block-body">
              （文本块{{ idx + 1 }}标签）：{{ b.tag }}
              <div class="block-content">{{ b.content }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="status">待审核状态：当前文档共 {{ blocks.length }} 个文本块划分结果</div>

      <div class="actions">
        <el-button type="primary" @click="confirmAll">确认划分</el-button>
        <el-input v-model="splitIndex" placeholder="拆分 [文本块编号]" style="width: 160px;" />
        <el-button @click="splitBlock">拆分</el-button>
        <el-input v-model="deleteIndices" placeholder="删除 [文本块编号1,文本块编号2]" style="width: 220px;" />
        <el-button type="warning" @click="deleteBlocks">删除</el-button>
        <el-button type="info" @click="resegment">重新划分</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

const doc = reactive({ label: '', blocks: [] })
const blocks = ref([])
const splitIndex = ref('')
const deleteIndices = ref('')

onMounted(() => {
  const raw = localStorage.getItem('qa_uploaded_doc')
  if (raw) {
    const parsed = JSON.parse(raw)
    doc.label = parsed.label
    blocks.value = parsed.blocks
  } else {
    // 兜底示例
    doc.label = '示例文档'
    blocks.value = [
      { id: 'B1', tag: '第一章', content: '示例内容1……' },
      { id: 'B2', tag: '第一章', content: '示例内容2……' }
    ]
  }
})

const exit = () => {
  ElMessage.success('已保存当前审核结果')
  router.push('/admin/qa')
}

const confirmAll = () => {
  localStorage.setItem('qa_segmentation_result', JSON.stringify(blocks.value))
  ElMessage.success('切分结果已确认')
  router.push('/admin/qa/association')
}

const splitBlock = () => {
  const idx = Number(splitIndex.value) - 1
  if (Number.isNaN(idx) || idx < 0 || idx >= blocks.value.length) {
    ElMessage.error('编号无效')
    return
  }
  const target = blocks.value[idx]
  // 简单示例拆分：按中间位置拆成两段
  const content = target.content || ''
  const mid = Math.floor(content.length / 2) || 1
  const first = content.slice(0, mid)
  const second = content.slice(mid)
  const newBlocks = [...blocks.value]
  newBlocks.splice(idx, 1, {
    ...target,
    id: target.id + '-1',
    content: first
  }, {
    ...target,
    id: target.id + '-2',
    content: second
  })
  blocks.value = newBlocks
  splitIndex.value = ''
}

const deleteBlocks = () => {
  if (!deleteIndices.value) return
  const indices = deleteIndices.value.split(/[,，\s]+/).map(v => Number(v) - 1).filter(v => !Number.isNaN(v))
  if (!indices.length) return
  const unique = Array.from(new Set(indices)).sort((a,b) => a-b)
  let merged = [...blocks.value]
  // 合并到前一个块
  for (let i = unique.length - 1; i >= 0; i--) {
    const idx = unique[i]
    if (idx <= 0 || idx >= merged.length) continue
    merged[idx - 1].content = (merged[idx - 1].content || '') + (merged[idx].content || '')
    merged.splice(idx, 1)
  }
  blocks.value = merged
  deleteIndices.value = ''
}

const resegment = () => {
  // 简单示例：把所有内容重新合并再伪造切分
  const all = blocks.value.map(b => b.content).join(' ')
  blocks.value = [
    { id: 'R1', tag: '重分块1', content: all.slice(0, Math.min(60, all.length)) + '…' },
    { id: 'R2', tag: '重分块2', content: all.slice(Math.min(60, all.length)) }
  ]
  ElMessage.success('已重新划分')
}
</script>

<style scoped>
.segmentation-review { height: 100%; }
.header-left { display: flex; align-items: center; gap: 10px; }
.title { font-size: 16px; font-weight: 600; }
.meta { margin-bottom: 10px; }
.content-title { margin: 10px 0; font-weight: 600; }
.blocks { display: flex; flex-direction: column; gap: 12px; }
.block-item { display: flex; gap: 8px; padding: 10px; background: #fafafa; border-radius: 6px; }
.block-index { width: 28px; color: #999; }
.block-content { color: #444; margin-top: 4px; }
.actions { display: flex; gap: 10px; margin-top: 16px; align-items: center; flex-wrap: wrap; }
.status { color: #666; margin-top: 8px; }
</style>
