<template>
  <div class="layout-container">
    <el-container>
      <!-- 头部 -->
      <el-header class="layout-header">
        <div class="header-left">
          <h3>精准问数管理系统</h3>
        </div>
        <div class="header-right">
          <el-dropdown trigger="hover" @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ userInfo.username }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-container>
        <!-- 侧边栏 -->
        <el-aside class="layout-aside" width="200px">
          <el-menu
            :default-active="activeMenu"
            class="el-menu-vertical"
            router
            @select="handleMenuSelect"
          >
            <el-menu-item index="/admin/api">
              <el-icon><Connection /></el-icon>
              <span>接口管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/data">
              <el-icon><Document /></el-icon>
              <span>数据管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/qa">
              <el-icon><Document /></el-icon>
              <span>知识管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/tools">
              <el-icon><Tools /></el-icon>
              <span>工具管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/chat-interface">
              <el-icon><ChatDotRound /></el-icon>
              <span>数据问答</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <!-- 主内容区 -->
        <el-main class="layout-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()

const userInfo = ref({})

const activeMenu = computed(() => {
  return route.path
})

// 获取用户信息
onMounted(() => {
  const savedUserInfo = localStorage.getItem('userInfo')
  if (savedUserInfo) {
    userInfo.value = JSON.parse(savedUserInfo)
  }
})

// 菜单选择
const handleMenuSelect = (index) => {
  router.push(index)
}

// 下拉菜单命令处理
const handleCommand = (command) => {
  if (command === 'logout') {
    handleLogout()
  }
}

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm(
    '确定要退出登录吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    localStorage.removeItem('userInfo')
    localStorage.removeItem('isLogin')
    ElMessage.success('退出成功')
    router.push('/login')
  })
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.layout-header {
  background-color: #545c64;
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
  line-height: 60px;
}

.header-left h3 {
  margin: 0;
  color: white;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: white;
  gap: 5px;
}

.user-info:hover {
  color: #409eff;
}

.layout-aside {
  background-color: #304156;
  height: calc(100vh - 60px);
  overflow-y: auto;
}

.el-menu-vertical {
  border: none;
  background-color: #304156;
  height: 100%;
}

.el-menu-vertical .el-menu-item {
  color: #bfcbd9;
}

.el-menu-vertical .el-menu-item:hover {
  background-color: #263445;
  color: #409eff;
}

.el-menu-vertical .el-menu-item.is-active {
  background-color: #409eff;
  color: white;
}

.layout-main {
  background-color: #f0f2f5;
  padding: 20px;
  height: calc(100vh - 60px);
  overflow-y: auto;
}

/* 全局重置Element Plus容器样式 */
:deep(.el-container) {
  height: 100%;
}

:deep(.el-aside) {
  height: auto;
}

:deep(.el-main) {
  height: auto;
  padding: 0;
}
</style>