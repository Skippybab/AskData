<template>
  <div class="new-knowledge-wizard">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button @click="goBack" size="small">
              <el-icon><ArrowLeft /></el-icon>
              返回
            </el-button>
            <span class="title">创建新知识库</span>
          </div>
        </div>
      </template>

      <el-steps :active="step" finish-status="success" class="steps">
        <el-step title="选择类型" description="选择文档类型" />
        <el-step title="上传文档" description="上传源文件" />
        <el-step title="解析文档" description="等待解析完成" />
      </el-steps>

      <div v-if="step === 0" class="step-pane step-center">
        <el-radio-group v-model="docType" class="nkw-row">
          <el-radio-button label="text">文本类（pdf/docx/txt）</el-radio-button>
          <el-radio-button label="table">表格类（csv/excel）</el-radio-button>
        </el-radio-group>
        <div class="hint">请选择要上传的文档类型</div>
        <div class="nkw-row">
          <el-button type="primary" size="large" class="next-btn" @click="nextStep">下一步</el-button>
        </div>
      </div>

      <div v-else-if="step === 1" class="step-pane step-center">
        <el-upload
          :action="uploadUrl"
          :headers="uploadHeaders"
          :before-upload="beforeUpload"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          :show-file-list="true"
          :auto-upload="true"
          :limit="3"
          :accept="acceptExt"
          class="nkw-row"
        >
          <el-button type="primary" size="large">
            <el-icon><Upload /></el-icon>
            选择并上传文件
          </el-button>
          <template #tip>
            <div class="el-upload__tip">
              支持 {{ acceptExt }}，单文件不超过10MB
            </div>
          </template>
        </el-upload>
      </div>

      <div v-else class="step-pane">
        <div class="parse-pane">
          <el-result icon="info" title="正在解析文档..." sub-title="请稍候，解析完成后将进入文本切分审查">
            <template #extra>
              <el-progress :percentage="progress" status="active" style="width: 360px;" />
            </template>
          </el-result>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const step = ref(0)
const docType = ref('text')
const progress = ref(0)

const uploadUrl = computed(() => '/api/qa/upload')
const uploadHeaders = computed(() => ({ 
  // 最小闭环阶段，不需要权限验证
}))
const acceptExt = computed(() => docType.value === 'text' ? '.pdf,.doc,.docx,.txt' : '.csv,.xls,.xlsx')

const goBack = () => {
  router.push('/admin/qa')
}

const nextStep = () => {
  step.value = 1
}

const beforeUpload = (file) => {
  const limits = docType.value === 'text'
    ? ['application/pdf','application/msword','application/vnd.openxmlformats-officedocument.wordprocessingml.document','text/plain']
    : ['text/csv','application/vnd.ms-excel','application/vnd.openxmlformats-officedocument.spreadsheetml.sheet']
  if (!limits.includes(file.type)) {
    ElMessage.error('文件类型不符合要求')
    return false
  }
  if (file.size / 1024 / 1024 > 10) {
    ElMessage.error('单文件大小不能超过10MB')
    return false
  }
  return true
}

const handleUploadSuccess = () => {
  ElMessage.success('上传成功，开始解析')
  step.value = 2
  // 模拟解析进度
  const timer = setInterval(() => {
    progress.value = Math.min(progress.value + 10, 100)
    if (progress.value >= 100) {
      clearInterval(timer)
      // 模拟解析结构写入本地缓存
      const docId = Date.now().toString()
      localStorage.setItem('qa_uploaded_doc', JSON.stringify({
        docId,
        label: '优质中小企业梯度培育管理暂行办法',
        blocks: [
          { id: 'A1-1-1', tag: '第一章总则', content: '第一条 为提升中小企业创新能力和专业化水平，促进中小企业高质量发展，……' },
          { id: 'A1-1-2', tag: '第一章总则', content: '第二条 优质中小企业是指在产品、技术、管理、模式等方面创新能力强……' },
          { id: 'A1-1-3', tag: '第一章总则', content: '第三条 参评优质中小企业应在中华人民共和国境内工商注册登记……' }
        ]
      }))
      router.push('/admin/qa/segmentation')
    }
  }, 250)
}

const handleUploadError = () => {
  ElMessage.error('上传失败，请重试')
}
</script>

<style scoped>
.new-knowledge-wizard { height: 100%; }
.header-left { display: flex; align-items: center; gap: 10px; }
.title { font-size: 16px; font-weight: bold; }
.steps { margin: 0 auto 20px; max-width: 880px; }
.step-pane { display: flex; flex-direction: column; gap: 16px; }
.step-center { max-width: 720px; margin: 0 auto; }
.nkw-row { display: flex; justify-content: center; gap: 12px; flex-wrap: wrap; }
.next-btn { width: 200px; }
.hint { color: #666; text-align: center; }
.parse-pane { display: flex; justify-content: center; }
</style>
