<template>
  <div class="association-review">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button @click="exit" size="small"><el-icon><Close /></el-icon>退出审核</el-button>
            <span class="title">知识关联审查</span>
          </div>
        </div>
      </template>

      <div class="meta">
        <div><b>当前审核项</b>：{{ currentItem.name }}</div>
        <div><b>知识项说明</b>：{{ currentItem.intro }}</div>
      </div>

      <div class="content-box">
        <div class="content-title">自动匹配结果：</div>
        <div class="match-list">
          <div v-for="(m, idx) in matches" :key="m.number" class="match-item">
            <div class="match-index">{{ idx + 1 }}.</div>
            <div class="match-body">
              文本块编号：{{ m.number }}，标签：{{ m.label }}
              <div class="snippet">匹配片段：{{ m.textSnippet }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="status">待审核状态：共 {{ matches.length }} 个匹配结果，当前为第 {{ currentIndex + 1 }} 项</div>

      <div class="actions">
        <el-button type="primary" @click="confirmAll">确认匹配</el-button>
        <el-input v-model="addInput" placeholder="添加 [文本块编号1,文本块编号2]" style="width: 200px;" />
        <el-button @click="addMatches">添加</el-button>
        <el-input v-model="delInput" placeholder="删除 [文本块编号1,文本块编号2]" style="width: 200px;" />
        <el-button type="warning" @click="removeMatches">删除</el-button>
        <el-button type="info" @click="rematch">重新匹配</el-button>
        <el-button @click="skip">跳过</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

// 审核项列表（示例）
const items = ref([
  { name: '全职员工数量', intro: '企业在岗全职员工总数统计口径说明' },
  { name: '研发人员数量', intro: '企业参与研发活动人员的统计口径' },
  { name: '营业收入', intro: '企业营业收入定义及计算口径' }
])
const currentIndex = ref(0)
const currentItem = computed(() => items.value[currentIndex.value] || { name: '', intro: '' })

// 匹配结果（示例）
const matches = ref([])
const addInput = ref('')
const delInput = ref('')

onMounted(() => {
  bootstrapMatches()
})

const bootstrapMatches = () => {
  // 初始从切分结果中生成匹配样例
  const seg = JSON.parse(localStorage.getItem('qa_segmentation_result') || '[]')
  matches.value = seg.slice(0, 3).map((b, i) => ({
    number: b.id || `A${i+1}`,
    label: b.tag || '未命名',
    textSnippet: (b.content || '').slice(0, 60) + '…'
  }))
  if (!matches.value.length) {
    matches.value = [
      { number: 'A1-1-1', label: '研发人员定义', textSnippet: '……匹配段1' },
      { number: 'A3-3-5', label: '研发人员统计', textSnippet: '……匹配段2' }
    ]
  }
}

const confirmAll = () => {
  ElMessage.success(`“${currentItem.value.name}”匹配已确认`)
  gotoNext()
}

const parseIdxList = (val) => {
  return (val || '').split(/[,，\s]+/).map(v => v.trim()).filter(Boolean)
}

const addMatches = () => {
  const ids = parseIdxList(addInput.value)
  ids.forEach(id => {
    if (!matches.value.find(m => m.number === id)) {
      matches.value.push({ number: id, label: '自定义', textSnippet: '用户添加的匹配' })
    }
  })
  addInput.value = ''
}

const removeMatches = () => {
  const ids = parseIdxList(delInput.value)
  matches.value = matches.value.filter(m => !ids.includes(m.number))
  delInput.value = ''
}

const rematch = () => {
  // 简单重算：置换顺序
  matches.value = [...matches.value].reverse()
  ElMessage.success('已重新匹配')
}

const skip = () => {
  ElMessage.info(`“${currentItem.value.name}”已跳过，标记为待处理`)
  gotoNext()
}

const gotoNext = () => {
  if (currentIndex.value < items.value.length - 1) {
    currentIndex.value++
    bootstrapMatches()
  } else {
    ElMessage.success('所有填报项审核完成')
    router.push('/admin/knowledge')
  }
}

const exit = () => {
  ElMessage.success('已保存审核进度')
  router.push('/admin/qa')
}
</script>

<style scoped>
.association-review { height: 100%; }
.header-left { display: flex; align-items: center; gap: 10px; }
.title { font-size: 16px; font-weight: 600; }
.meta { margin-bottom: 10px; }
.content-title { margin: 10px 0; font-weight: 600; }
.match-list { display: flex; flex-direction: column; gap: 12px; }
.match-item { display: flex; gap: 8px; padding: 10px; background: #fafafa; border-radius: 6px; }
.match-index { width: 28px; color: #999; }
.snippet { color: #444; margin-top: 4px; }
.status { color: #666; margin-top: 8px; }
.actions { display: flex; gap: 10px; margin-top: 16px; align-items: center; flex-wrap: wrap; }
</style>
