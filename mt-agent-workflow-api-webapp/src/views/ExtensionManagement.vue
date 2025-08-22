<template>
  <div class="extension-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">
        <i class="el-icon-magic-stick"></i>
        拓展管理
      </h1>
      <p class="page-subtitle">管理和配置MCP工具与扩展功能</p>
    </div>

    <!-- 主内容区 -->
    <div class="content-wrapper">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button-group>
            <el-button 
              :type="filterType === 'all' ? 'primary' : 'default'"
              @click="filterType = 'all'"
            >
              全部工具
            </el-button>
            <el-button 
              :type="filterType === 'enabled' ? 'primary' : 'default'"
              @click="filterType = 'enabled'"
            >
              已启用
            </el-button>
            <el-button 
              :type="filterType === 'disabled' ? 'primary' : 'default'"
              @click="filterType = 'disabled'"
            >
              已禁用
            </el-button>
          </el-button-group>
        </div>
        
        <div class="toolbar-right">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索工具名称或描述"
            prefix-icon="el-icon-search"
            clearable
            style="width: 300px"
          />
        </div>
      </div>

      <!-- 工具分类 -->
      <div class="tool-categories">
        <div 
          v-for="category in toolCategories" 
          :key="category.id"
          class="category-section"
        >
          <div class="category-header">
            <div class="category-title">
              <i :class="category.icon"></i>
              <span>{{ category.name }}</span>
              <span class="tool-count">{{ getCategoryTools(category.id).length }}</span>
            </div>
            <el-switch
              v-model="category.enabled"
              @change="toggleCategory(category)"
              active-text="启用全部"
              inactive-text="禁用全部"
            />
          </div>

          <!-- 工具列表 -->
          <div class="tool-grid">
            <div 
              v-for="tool in getCategoryTools(category.id)" 
              :key="tool.id"
              class="tool-card"
              :class="{ 'enabled': tool.enabled, 'disabled': !tool.enabled }"
            >
              <div class="tool-header">
                <div class="tool-icon">
                  <i :class="tool.icon || 'el-icon-setting'"></i>
                </div>
                <el-switch
                  v-model="tool.enabled"
                  @change="updateToolStatus(tool)"
                  class="tool-switch"
                />
              </div>
              
              <div class="tool-body">
                <h4 class="tool-name">{{ tool.name }}</h4>
                <p class="tool-description">{{ tool.description }}</p>
                
                <div class="tool-meta">
                  <span class="meta-item">
                    <i class="el-icon-time"></i>
                    {{ tool.callCount || 0 }} 次调用
                  </span>
                  <span class="meta-item">
                    <i class="el-icon-user"></i>
                    {{ tool.provider || '系统' }}
                  </span>
                </div>
                
                <!-- 工具参数配置 -->
                <div class="tool-params" v-if="tool.params && tool.params.length > 0">
                  <div class="params-title">
                    <span>参数配置</span>
                    <el-button 
                      type="text" 
                      size="mini"
                      @click="tool.showParams = !tool.showParams"
                    >
                      {{ tool.showParams ? '收起' : '展开' }}
                    </el-button>
                  </div>
                  
                  <el-collapse-transition>
                    <div v-show="tool.showParams" class="params-list">
                      <div 
                        v-for="param in tool.params" 
                        :key="param.name"
                        class="param-item"
                      >
                        <span class="param-name">{{ param.label }}:</span>
                        <el-input
                          v-if="param.type === 'string'"
                          v-model="param.value"
                          size="mini"
                          :placeholder="param.placeholder"
                          @change="updateToolParam(tool, param)"
                        />
                        <el-input-number
                          v-else-if="param.type === 'number'"
                          v-model="param.value"
                          size="mini"
                          :min="param.min"
                          :max="param.max"
                          @change="updateToolParam(tool, param)"
                        />
                        <el-switch
                          v-else-if="param.type === 'boolean'"
                          v-model="param.value"
                          @change="updateToolParam(tool, param)"
                        />
                      </div>
                    </div>
                  </el-collapse-transition>
                </div>
              </div>
              
              <div class="tool-footer">
                <el-button 
                  type="text" 
                  size="small"
                  @click="testTool(tool)"
                  :disabled="!tool.enabled"
                >
                  <i class="el-icon-position"></i>
                  测试
                </el-button>
                <el-button 
                  type="text" 
                  size="small"
                  @click="viewToolDoc(tool)"
                >
                  <i class="el-icon-document"></i>
                  文档
                </el-button>
                <el-button 
                  type="text" 
                  size="small"
                  @click="viewToolLogs(tool)"
                >
                  <i class="el-icon-tickets"></i>
                  日志
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredTools.length === 0" class="empty-state">
        <i class="el-icon-box"></i>
        <p>暂无可用工具</p>
      </div>
    </div>

    <!-- 工具文档弹窗 -->
    <el-dialog
      :title="`工具文档 - ${currentTool?.name}`"
      v-model="showDocDialog"
      width="800px"
      class="doc-dialog"
    >
      <div class="tool-doc" v-if="currentTool">
        <h3>功能说明</h3>
        <p>{{ currentTool.description }}</p>
        
        <h3>使用方法</h3>
        <div class="code-block">
          <pre><code>{{ currentTool.usage || '暂无使用说明' }}</code></pre>
        </div>
        
        <h3>参数说明</h3>
        <el-table :data="currentTool.params || []" style="width: 100%">
          <el-table-column prop="name" label="参数名" width="150" />
          <el-table-column prop="label" label="标签" width="150" />
          <el-table-column prop="type" label="类型" width="100" />
          <el-table-column prop="required" label="必填" width="80">
            <template #default="{ row }">
              {{ row.required ? '是' : '否' }}
            </template>
          </el-table-column>
          <el-table-column prop="description" label="说明" />
        </el-table>
        
        <h3>返回值</h3>
        <div class="code-block">
          <pre><code>{{ currentTool.returns || '暂无返回值说明' }}</code></pre>
        </div>
      </div>
    </el-dialog>

    <!-- 工具测试弹窗 -->
    <el-dialog
      :title="`测试工具 - ${currentTool?.name}`"
      v-model="showTestDialog"
      width="800px"
      class="test-dialog"
    >
      <div class="tool-test" v-if="currentTool">
        <el-form :model="testForm" label-width="120px">
          <el-form-item 
            v-for="param in currentTool.params" 
            :key="param.name"
            :label="param.label"
            :required="param.required"
          >
            <el-input
              v-if="param.type === 'string'"
              v-model="testForm[param.name]"
              :placeholder="param.placeholder"
            />
            <el-input-number
              v-else-if="param.type === 'number'"
              v-model="testForm[param.name]"
              :min="param.min"
              :max="param.max"
              style="width: 100%"
            />
            <el-switch
              v-else-if="param.type === 'boolean'"
              v-model="testForm[param.name]"
            />
          </el-form-item>
        </el-form>
        
        <div class="test-result" v-if="testResult">
          <h4>执行结果</h4>
          <div class="result-content">
            <pre>{{ JSON.stringify(testResult, null, 2) }}</pre>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="showTestDialog = false">关闭</el-button>
        <el-button type="primary" @click="runTest" :loading="testing">执行测试</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

// 数据
const toolCategories = ref([
  {
    id: 'data',
    name: '数据处理',
    icon: 'el-icon-data-analysis',
    enabled: true
  },
  {
    id: 'ai',
    name: 'AI能力',
    icon: 'el-icon-cpu',
    enabled: true
  },
  {
    id: 'integration',
    name: '第三方集成',
    icon: 'el-icon-connection',
    enabled: true
  },
  {
    id: 'utility',
    name: '实用工具',
    icon: 'el-icon-tools',
    enabled: true
  }
])

const tools = ref([
  {
    id: 1,
    categoryId: 'data',
    name: 'SQL查询执行器',
    description: '执行SQL查询并返回结果',
    icon: 'el-icon-search',
    enabled: true,
    callCount: 1523,
    provider: '系统',
    params: [
      {
        name: 'timeout',
        label: '超时时间',
        type: 'number',
        value: 30,
        min: 5,
        max: 300,
        required: false,
        description: '查询超时时间（秒）'
      },
      {
        name: 'maxRows',
        label: '最大行数',
        type: 'number',
        value: 1000,
        min: 1,
        max: 10000,
        required: false,
        description: '返回结果的最大行数'
      }
    ],
    showParams: false
  },
  {
    id: 2,
    categoryId: 'data',
    name: '数据导出工具',
    description: '将查询结果导出为Excel或CSV格式',
    icon: 'el-icon-download',
    enabled: true,
    callCount: 342,
    provider: '系统',
    params: [],
    showParams: false
  },
  {
    id: 3,
    categoryId: 'ai',
    name: 'GPT文本生成',
    description: '使用GPT模型生成文本内容',
    icon: 'el-icon-edit',
    enabled: true,
    callCount: 892,
    provider: 'OpenAI',
    params: [
      {
        name: 'model',
        label: '模型',
        type: 'string',
        value: 'gpt-3.5-turbo',
        placeholder: '模型名称',
        required: true,
        description: '使用的GPT模型'
      },
      {
        name: 'temperature',
        label: '温度',
        type: 'number',
        value: 0.7,
        min: 0,
        max: 2,
        required: false,
        description: '控制生成文本的随机性'
      }
    ],
    showParams: false
  },
  {
    id: 4,
    categoryId: 'ai',
    name: '图像识别',
    description: '识别图像中的对象和文字',
    icon: 'el-icon-picture',
    enabled: false,
    callCount: 156,
    provider: 'Vision API',
    params: [],
    showParams: false
  },
  {
    id: 5,
    categoryId: 'integration',
    name: '邮件发送',
    description: '发送邮件通知',
    icon: 'el-icon-message',
    enabled: true,
    callCount: 423,
    provider: 'SMTP',
    params: [
      {
        name: 'smtpHost',
        label: 'SMTP服务器',
        type: 'string',
        value: 'smtp.gmail.com',
        placeholder: 'SMTP服务器地址',
        required: true,
        description: 'SMTP服务器地址'
      },
      {
        name: 'smtpPort',
        label: '端口',
        type: 'number',
        value: 587,
        min: 1,
        max: 65535,
        required: true,
        description: 'SMTP端口号'
      }
    ],
    showParams: false
  },
  {
    id: 6,
    categoryId: 'utility',
    name: '文件处理',
    description: '处理和转换各种文件格式',
    icon: 'el-icon-files',
    enabled: true,
    callCount: 234,
    provider: '系统',
    params: [],
    showParams: false
  }
])

// 状态
const filterType = ref('all')
const searchKeyword = ref('')
const showDocDialog = ref(false)
const showTestDialog = ref(false)
const currentTool = ref(null)
const testForm = ref({})
const testResult = ref(null)
const testing = ref(false)

// 计算属性
const filteredTools = computed(() => {
  let result = tools.value
  
  // 按状态过滤
  if (filterType.value === 'enabled') {
    result = result.filter(tool => tool.enabled)
  } else if (filterType.value === 'disabled') {
    result = result.filter(tool => !tool.enabled)
  }
  
  // 按关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(tool => 
      tool.name.toLowerCase().includes(keyword) ||
      tool.description.toLowerCase().includes(keyword)
    )
  }
  
  return result
})

// 方法
const getCategoryTools = (categoryId) => {
  return filteredTools.value.filter(tool => tool.categoryId === categoryId)
}

const toggleCategory = (category) => {
  const categoryTools = tools.value.filter(tool => tool.categoryId === category.id)
  categoryTools.forEach(tool => {
    tool.enabled = category.enabled
  })
  ElMessage.success(`已${category.enabled ? '启用' : '禁用'}分类下所有工具`)
}

const updateToolStatus = async (tool) => {
  // TODO: 调用API更新工具状态
  ElMessage.success(`工具已${tool.enabled ? '启用' : '禁用'}`)
}

const updateToolParam = async (tool, param) => {
  // TODO: 调用API更新工具参数
  console.log('更新工具参数:', tool.name, param.name, param.value)
}

const testTool = (tool) => {
  currentTool.value = tool
  testForm.value = {}
  testResult.value = null
  
  // 初始化测试表单
  if (tool.params) {
    tool.params.forEach(param => {
      testForm.value[param.name] = param.value
    })
  }
  
  showTestDialog.value = true
}

const viewToolDoc = (tool) => {
  currentTool.value = tool
  showDocDialog.value = true
}

const viewToolLogs = (tool) => {
  ElMessage.info('日志功能开发中')
}

const runTest = async () => {
  testing.value = true
  
  // 模拟测试执行
  setTimeout(() => {
    testResult.value = {
      success: true,
      message: '工具执行成功',
      data: {
        tool: currentTool.value.name,
        params: testForm.value,
        result: '这是测试结果示例',
        executionTime: '123ms'
      }
    }
    testing.value = false
    ElMessage.success('测试执行成功')
  }, 2000)
}

// 生命周期
onMounted(() => {
  // 加载工具列表
})
</script>

<style scoped lang="scss">
.extension-management {
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
  
  .page-header {
    text-align: center;
    color: white;
    margin-bottom: 30px;
    
    .page-title {
      font-size: 32px;
      font-weight: 300;
      margin: 0;
      letter-spacing: 2px;
      
      i {
        font-size: 36px;
        margin-right: 10px;
      }
    }
    
    .page-subtitle {
      font-size: 14px;
      opacity: 0.9;
      margin-top: 10px;
    }
  }
  
  .content-wrapper {
    max-width: 1400px;
    margin: 0 auto;
    
    .toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      background: white;
      padding: 15px 20px;
      border-radius: 12px;
      
      .el-button-group {
        .el-button {
          border-radius: 20px;
          
          &:first-child {
            border-top-right-radius: 0;
            border-bottom-right-radius: 0;
          }
          
          &:last-child {
            border-top-left-radius: 0;
            border-bottom-left-radius: 0;
          }
        }
      }
    }
    
    .tool-categories {
      .category-section {
        margin-bottom: 30px;
        
        .category-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          background: white;
          padding: 15px 20px;
          border-radius: 12px;
          margin-bottom: 15px;
          
          .category-title {
            display: flex;
            align-items: center;
            gap: 10px;
            font-size: 18px;
            font-weight: 500;
            color: #333;
            
            i {
              font-size: 24px;
              color: #667eea;
            }
            
            .tool-count {
              display: inline-block;
              padding: 2px 8px;
              background: #f0f0f0;
              border-radius: 10px;
              font-size: 14px;
              color: #666;
            }
          }
        }
        
        .tool-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
          gap: 15px;
          
          .tool-card {
            background: white;
            border-radius: 12px;
            padding: 15px;
            transition: all 0.3s ease;
            
            &.enabled {
              border: 2px solid #67c23a;
            }
            
            &.disabled {
              opacity: 0.7;
              border: 2px solid #dcdfe6;
            }
            
            &:hover {
              transform: translateY(-2px);
              box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
            }
            
            .tool-header {
              display: flex;
              justify-content: space-between;
              align-items: center;
              margin-bottom: 12px;
              
              .tool-icon {
                font-size: 32px;
                color: #667eea;
              }
              
              .tool-switch {
                --el-switch-on-color: #67c23a;
              }
            }
            
            .tool-body {
              .tool-name {
                margin: 0 0 8px;
                font-size: 16px;
                color: #333;
              }
              
              .tool-description {
                margin: 0 0 12px;
                font-size: 13px;
                color: #666;
                line-height: 1.5;
              }
              
              .tool-meta {
                display: flex;
                gap: 15px;
                margin-bottom: 12px;
                
                .meta-item {
                  font-size: 12px;
                  color: #999;
                  display: flex;
                  align-items: center;
                  gap: 3px;
                  
                  i {
                    font-size: 14px;
                  }
                }
              }
              
              .tool-params {
                border-top: 1px solid #f0f0f0;
                padding-top: 12px;
                margin-top: 12px;
                
                .params-title {
                  display: flex;
                  justify-content: space-between;
                  align-items: center;
                  margin-bottom: 8px;
                  font-size: 13px;
                  color: #666;
                }
                
                .params-list {
                  .param-item {
                    display: flex;
                    align-items: center;
                    gap: 10px;
                    margin-bottom: 8px;
                    
                    .param-name {
                      font-size: 12px;
                      color: #999;
                      width: 80px;
                      flex-shrink: 0;
                    }
                  }
                }
              }
            }
            
            .tool-footer {
              display: flex;
              justify-content: space-around;
              border-top: 1px solid #f0f0f0;
              padding-top: 10px;
              margin-top: 10px;
              
              .el-button {
                font-size: 12px;
              }
            }
          }
        }
      }
    }
    
    .empty-state {
      text-align: center;
      padding: 100px 20px;
      background: white;
      border-radius: 12px;
      
      i {
        font-size: 64px;
        color: #ddd;
      }
      
      p {
        margin: 20px 0;
        font-size: 16px;
        color: #999;
      }
    }
  }
  
  .doc-dialog,
  .test-dialog {
    .tool-doc,
    .tool-test {
      h3, h4 {
        color: #333;
        margin: 20px 0 10px;
        
        &:first-child {
          margin-top: 0;
        }
      }
      
      .code-block {
        background: #f8f8f8;
        padding: 15px;
        border-radius: 5px;
        
        pre {
          margin: 0;
          font-family: 'Monaco', 'Menlo', monospace;
          font-size: 13px;
        }
      }
      
      .test-result {
        margin-top: 20px;
        
        .result-content {
          background: #f8f8f8;
          padding: 15px;
          border-radius: 5px;
          max-height: 300px;
          overflow-y: auto;
          
          pre {
            margin: 0;
            font-family: 'Monaco', 'Menlo', monospace;
            font-size: 12px;
          }
        }
      }
    }
  }
}
</style>