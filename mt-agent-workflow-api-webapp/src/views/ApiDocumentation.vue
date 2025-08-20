<template>
  <div class="api-documentation">
    <div class="doc-header">
      <h1 class="doc-title">
        <i class="el-icon-document"></i>
        API 接口文档
      </h1>
      <el-button @click="$router.back()" icon="el-icon-arrow-left">
        返回
      </el-button>
    </div>
    
    <div class="doc-content">
      <!-- API基本信息 -->
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
            <el-tag :type="apiInfo.status === 1 ? 'success' : 'danger'">
              {{ apiInfo.status === 1 ? '已启用' : '已禁用' }}
            </el-tag>
          </div>
        </template>
        
        <el-descriptions :column="2" border>
          <el-descriptions-item label="API名称">
            {{ apiInfo.name }}
          </el-descriptions-item>
          <el-descriptions-item label="数据库">
            {{ apiInfo.databaseName }}
          </el-descriptions-item>
          <el-descriptions-item label="API Key">
            <div class="api-key-display">
              <span v-if="!showKey">{{ maskedKey }}</span>
              <code v-else class="api-key-code">{{ apiInfo.apiKey }}</code>
              <el-button 
                type="text" 
                size="small"
                @click="toggleKeyVisibility"
              >
                <i :class="showKey ? 'el-icon-view' : 'el-icon-hide'"></i>
              </el-button>
              <el-button 
                type="text" 
                size="small"
                @click="copyKey"
              >
                <i class="el-icon-copy-document"></i>
              </el-button>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDate(apiInfo.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">
            {{ apiInfo.description || '暂无描述' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
      
      <!-- 接口调用说明 -->
      <el-card class="usage-card">
        <template #header>
          <div class="card-header">
            <span>接口调用说明</span>
          </div>
        </template>
        
        <div class="usage-section">
          <h3>1. 接口地址</h3>
          <div class="code-block">
            <code>{{ baseUrl }}/api/chat/query</code>
            <el-button type="text" size="small" @click="copyUrl">
              <i class="el-icon-copy-document"></i> 复制
            </el-button>
          </div>
        </div>
        
        <div class="usage-section">
          <h3>2. 请求方式</h3>
          <el-tag type="success">POST</el-tag>
        </div>
        
        <div class="usage-section">
          <h3>3. 请求头</h3>
          <div class="code-block">
            <pre>Content-Type: application/json
X-API-Key: {{ showKey ? apiInfo.apiKey : 'your-api-key' }}</pre>
          </div>
        </div>
        
        <div class="usage-section">
          <h3>4. 请求参数</h3>
          <el-table :data="requestParams" border>
            <el-table-column prop="name" label="参数名" width="150" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="required" label="必填" width="80">
              <template #default="{ row }">
                <el-tag :type="row.required ? 'danger' : 'info'" size="small">
                  {{ row.required ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" />
          </el-table>
        </div>
        
        <div class="usage-section">
          <h3>5. 响应格式</h3>
          <div class="code-block">
            <pre>{
  "code": 200,
  "message": "success",
  "data": {
    "sql": "生成的SQL语句",
    "result": [
      // 查询结果数组
    ],
    "answer": "自然语言回答",
    "executionTime": 123
  }
}</pre>
          </div>
        </div>
      </el-card>
      
      <!-- 代码示例 -->
      <el-card class="example-card">
        <template #header>
          <div class="card-header">
            <span>代码示例</span>
            <el-radio-group v-model="selectedLang" size="small">
              <el-radio-button label="curl">cURL</el-radio-button>
              <el-radio-button label="python">Python</el-radio-button>
              <el-radio-button label="javascript">JavaScript</el-radio-button>
              <el-radio-button label="java">Java</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        
        <div class="code-example">
          <div v-if="selectedLang === 'curl'" class="code-block">
            <pre>{{ curlExample }}</pre>
            <el-button type="text" size="small" @click="copyExample('curl')">
              <i class="el-icon-copy-document"></i> 复制
            </el-button>
          </div>
          
          <div v-if="selectedLang === 'python'" class="code-block">
            <pre>{{ pythonExample }}</pre>
            <el-button type="text" size="small" @click="copyExample('python')">
              <i class="el-icon-copy-document"></i> 复制
            </el-button>
          </div>
          
          <div v-if="selectedLang === 'javascript'" class="code-block">
            <pre>{{ javascriptExample }}</pre>
            <el-button type="text" size="small" @click="copyExample('javascript')">
              <i class="el-icon-copy-document"></i> 复制
            </el-button>
          </div>
          
          <div v-if="selectedLang === 'java'" class="code-block">
            <pre>{{ javaExample }}</pre>
            <el-button type="text" size="small" @click="copyExample('java')">
              <i class="el-icon-copy-document"></i> 复制
            </el-button>
          </div>
        </div>
      </el-card>
      
      <!-- 测试工具 -->
      <el-card class="test-card">
        <template #header>
          <div class="card-header">
            <span>在线测试</span>
          </div>
        </template>
        
        <div class="test-tool">
          <el-form :model="testForm" label-width="100px">
            <el-form-item label="查询问题">
              <el-input 
                v-model="testForm.question" 
                type="textarea"
                :rows="3"
                placeholder="例如：查询销售额最高的10个产品"
              />
            </el-form-item>
            
            <el-form-item label="数据表">
              <el-select 
                v-model="testForm.tableName" 
                placeholder="选择数据表（可选）"
                clearable
              >
                <el-option
                  v-for="table in availableTables"
                  :key="table.id"
                  :label="table.tableName"
                  :value="table.tableName"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item>
              <el-button 
                type="primary" 
                @click="testApi"
                :loading="testing"
              >
                发送测试
              </el-button>
              <el-button @click="clearTest">清空</el-button>
            </el-form-item>
          </el-form>
          
          <div v-if="testResult" class="test-result">
            <h4>测试结果</h4>
            <el-tabs v-model="activeTab">
              <el-tab-pane label="响应数据" name="response">
                <pre class="result-json">{{ JSON.stringify(testResult, null, 2) }}</pre>
              </el-tab-pane>
              
              <el-tab-pane label="SQL语句" name="sql" v-if="testResult.data?.sql">
                <pre class="result-sql">{{ testResult.data.sql }}</pre>
              </el-tab-pane>
              
              <el-tab-pane label="查询结果" name="result" v-if="testResult.data?.result">
                <el-table 
                  :data="testResult.data.result.slice(0, 10)" 
                  border
                  size="small"
                  max-height="300"
                >
                  <el-table-column
                    v-for="(value, key) in testResult.data.result[0]"
                    :key="key"
                    :prop="key"
                    :label="key"
                    min-width="120"
                  />
                </el-table>
                <div v-if="testResult.data.result.length > 10" class="result-more">
                  还有 {{ testResult.data.result.length - 10 }} 条记录...
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api'

const route = useRoute()
const router = useRouter()

// 数据
const apiInfo = ref({
  id: null,
  name: '',
  apiKey: '',
  databaseName: '',
  status: 1,
  description: '',
  createTime: null
})
const showKey = ref(false)
const selectedLang = ref('curl')
const availableTables = ref([])
const testing = ref(false)
const testResult = ref(null)
const activeTab = ref('response')

// 表单
const testForm = ref({
  question: '',
  tableName: null
})

// 请求参数说明
const requestParams = ref([
  { name: 'question', type: 'String', required: true, description: '自然语言查询问题' },
  { name: 'tableName', type: 'String', required: false, description: '指定查询的数据表名（可选）' },
  { name: 'sessionId', type: 'String', required: false, description: '会话ID，用于保持上下文（可选）' }
])

// 计算属性
const baseUrl = computed(() => window.location.origin)
const maskedKey = computed(() => {
  if (!apiInfo.value.apiKey) return ''
  const key = apiInfo.value.apiKey
  return key.substring(0, 8) + '****' + key.substring(key.length - 4)
})

// 代码示例
const curlExample = computed(() => `curl -X POST '${baseUrl.value}/api/chat/query' \\
  -H 'Content-Type: application/json' \\
  -H 'X-API-Key: ${showKey.value ? apiInfo.value.apiKey : 'your-api-key'}' \\
  -d '{
    "question": "查询销售额最高的10个产品",
    "tableName": "sales_data"
  }'`)

const pythonExample = computed(() => `import requests
import json

url = '${baseUrl.value}/api/chat/query'
headers = {
    'Content-Type': 'application/json',
    'X-API-Key': '${showKey.value ? apiInfo.value.apiKey : 'your-api-key'}'
}
data = {
    'question': '查询销售额最高的10个产品',
    'tableName': 'sales_data'
}

response = requests.post(url, headers=headers, json=data)
result = response.json()
print(json.dumps(result, indent=2))`)

const javascriptExample = computed(() => `const apiKey = '${showKey.value ? apiInfo.value.apiKey : 'your-api-key'}';
const url = '${baseUrl.value}/api/chat/query';

const requestData = {
  question: '查询销售额最高的10个产品',
  tableName: 'sales_data'
};

fetch(url, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-API-Key': apiKey
  },
  body: JSON.stringify(requestData)
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error('Error:', error));`)

const javaExample = computed(() => `import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;

public class ApiExample {
    public static void main(String[] args) throws Exception {
        String apiKey = "${showKey.value ? apiInfo.value.apiKey : 'your-api-key'}";
        String apiUrl = "${baseUrl.value}/api/chat/query";
        
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-API-Key", apiKey);
        conn.setDoOutput(true);
        
        String jsonInput = "{\\"question\\": \\"查询销售额最高的10个产品\\", \\"tableName\\": \\"sales_data\\"}";
        
        try(OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInput.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        try(BufferedReader br = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.out.println(response.toString());
        }
    }
}`)

// 生命周期
onMounted(() => {
  loadApiInfo()
})

// 方法
const loadApiInfo = async () => {
  const apiId = route.params.id
  if (!apiId) {
    ElMessage.error('API ID不存在')
    router.back()
    return
  }
  
  // TODO: 从后端加载API详情
  // 这里使用模拟数据
  apiInfo.value = {
    id: apiId,
    name: '销售数据查询API',
    apiKey: 'sk-mt-' + Math.random().toString(36).substring(2, 15),
    databaseName: '销售数据库',
    status: 1,
    description: '用于查询销售相关数据的API接口',
    createTime: new Date()
  }
  
  // 加载可用的数据表
  loadTables()
}

const loadTables = async () => {
  // TODO: 根据API关联的数据库加载表列表
  availableTables.value = [
    { id: 1, tableName: 'sales_data' },
    { id: 2, tableName: 'products' },
    { id: 3, tableName: 'customers' },
    { id: 4, tableName: 'orders' }
  ]
}

const toggleKeyVisibility = () => {
  showKey.value = !showKey.value
}

const copyKey = () => {
  navigator.clipboard.writeText(apiInfo.value.apiKey)
  ElMessage.success('API Key已复制')
}

const copyUrl = () => {
  navigator.clipboard.writeText(`${baseUrl.value}/api/chat/query`)
  ElMessage.success('接口地址已复制')
}

const copyExample = (lang) => {
  let code = ''
  switch(lang) {
    case 'curl':
      code = curlExample.value
      break
    case 'python':
      code = pythonExample.value
      break
    case 'javascript':
      code = javascriptExample.value
      break
    case 'java':
      code = javaExample.value
      break
  }
  navigator.clipboard.writeText(code)
  ElMessage.success('代码已复制')
}

const testApi = async () => {
  if (!testForm.value.question) {
    ElMessage.warning('请输入查询问题')
    return
  }
  
  testing.value = true
  testResult.value = null
  
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1500))
    
    testResult.value = {
      code: 200,
      message: 'success',
      data: {
        sql: `SELECT product_name, SUM(amount) as total_sales 
FROM sales_data 
GROUP BY product_name 
ORDER BY total_sales DESC 
LIMIT 10`,
        result: [
          { product_name: '产品A', total_sales: 150000 },
          { product_name: '产品B', total_sales: 120000 },
          { product_name: '产品C', total_sales: 98000 },
          { product_name: '产品D', total_sales: 87000 },
          { product_name: '产品E', total_sales: 76000 }
        ],
        answer: '根据查询结果，销售额最高的5个产品分别是：产品A（150,000元）、产品B（120,000元）、产品C（98,000元）、产品D（87,000元）和产品E（76,000元）。',
        executionTime: 123
      }
    }
    
    ElMessage.success('测试成功')
  } catch (error) {
    ElMessage.error('测试失败：' + error.message)
  } finally {
    testing.value = false
  }
}

const clearTest = () => {
  testForm.value = {
    question: '',
    tableName: null
  }
  testResult.value = null
}

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleString('zh-CN')
}
</script>

<style scoped lang="scss">
.api-documentation {
  padding: 20px;
  background: #f5f6fa;
  min-height: 100vh;
  
  .doc-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    .doc-title {
      margin: 0;
      font-size: 24px;
      color: #333;
      
      i {
        margin-right: 10px;
        color: #667eea;
      }
    }
  }
  
  .doc-content {
    max-width: 1200px;
    margin: 0 auto;
    
    .el-card {
      margin-bottom: 20px;
      border-radius: 8px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
      
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 16px;
        font-weight: 500;
      }
    }
    
    .info-card {
      .api-key-display {
        display: flex;
        align-items: center;
        gap: 10px;
        
        .api-key-code {
          padding: 2px 8px;
          background: #f0f0f0;
          border-radius: 4px;
          font-family: monospace;
        }
      }
    }
    
    .usage-card {
      .usage-section {
        margin-bottom: 30px;
        
        &:last-child {
          margin-bottom: 0;
        }
        
        h3 {
          margin: 0 0 15px;
          font-size: 15px;
          color: #333;
        }
        
        .code-block {
          position: relative;
          background: #2d2d2d;
          border-radius: 6px;
          padding: 15px;
          
          code, pre {
            margin: 0;
            color: #f8f8f2;
            font-family: 'Monaco', 'Menlo', monospace;
            font-size: 13px;
            line-height: 1.6;
          }
          
          .el-button {
            position: absolute;
            top: 10px;
            right: 10px;
            color: #999;
            
            &:hover {
              color: #667eea;
            }
          }
        }
      }
    }
    
    .example-card {
      .code-example {
        .code-block {
          position: relative;
          background: #2d2d2d;
          border-radius: 6px;
          padding: 15px;
          
          pre {
            margin: 0;
            color: #f8f8f2;
            font-family: 'Monaco', 'Menlo', monospace;
            font-size: 13px;
            line-height: 1.6;
            overflow-x: auto;
          }
          
          .el-button {
            position: absolute;
            top: 10px;
            right: 10px;
            color: #999;
            
            &:hover {
              color: #667eea;
            }
          }
        }
      }
    }
    
    .test-card {
      .test-tool {
        .test-result {
          margin-top: 30px;
          padding-top: 30px;
          border-top: 1px solid #e8e8e8;
          
          h4 {
            margin: 0 0 15px;
            font-size: 15px;
            color: #333;
          }
          
          .result-json,
          .result-sql {
            background: #f8f9fa;
            border: 1px solid #e8e8e8;
            border-radius: 6px;
            padding: 15px;
            font-family: monospace;
            font-size: 13px;
            line-height: 1.6;
            overflow-x: auto;
          }
          
          .result-more {
            text-align: center;
            padding: 10px;
            color: #666;
            font-size: 13px;
            background: #f8f9fa;
            margin-top: 10px;
          }
        }
      }
    }
  }
}
</style>