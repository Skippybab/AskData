<template>
  <div class="knowledge-qa">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>知识管理</span>
        </div>
      </template>

      <div class="qa-actions">
        <el-card class="qa-card" shadow="hover">
          <div class="qa-card-body">
            <el-icon class="qa-icon"><Collection /></el-icon>
            <div class="qa-title">管理现有知识库</div>
            <div class="qa-desc">浏览并管理已有知识库的文件、文本块与知识关联</div>
            <el-button type="primary" @click="goManage">进入管理</el-button>
          </div>
        </el-card>

        <el-card class="qa-card" shadow="hover">
          <div class="qa-card-body">
            <el-icon class="qa-icon"><Plus /></el-icon>
            <div class="qa-title">创建新知识库</div>
            <div class="qa-desc">上传文档并完成文本切分与知识关联的交互式审查</div>
            <el-button type="success" @click="goCreate">创建知识库</el-button>
          </div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goManage = () => {
  router.push('/admin/knowledge')
}

const goCreate = () => {
  router.push('/admin/qa/new')
}
</script>

<style scoped>
.knowledge-qa {
  height: 100%;
}

.qa-actions {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.qa-card {
  display: flex;
}

.qa-card-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-start;
}

.qa-icon {
  font-size: 28px;
  color: #409eff;
}

.qa-title {
  font-size: 16px;
  font-weight: 600;
}

.qa-desc {
  color: #666;
  margin-bottom: 8px;
}
</style>
