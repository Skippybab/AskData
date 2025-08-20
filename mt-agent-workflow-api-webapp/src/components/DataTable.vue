<template>
  <div class="data-table-container">
    <!-- 数据概览 -->
    <div class="data-overview">
      <el-card class="overview-card" shadow="never">
        <template #header>
          <div class="overview-header">
            <span class="overview-title">📊 数据概览</span>
                         <el-button 
               type="primary" 
               size="small" 
               @click="toggleTable"
             >
               <el-icon>
                 <component :is="isTableVisible ? 'ArrowUp' : 'ArrowDown'" />
               </el-icon>
              {{ isTableVisible ? '收起表格' : '展开表格' }}
            </el-button>
          </div>
        </template>
        <div class="overview-content">
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-number">{{ dataInfo.recordCount }}</div>
                <div class="stat-label">记录数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-number">{{ dataInfo.fieldCount }}</div>
                <div class="stat-label">字段数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-number">{{ dataInfo.dataSize }}</div>
                <div class="stat-label">数据大小</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-number">{{ dataInfo.parseTime }}ms</div>
                <div class="stat-label">解析时间</div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-card>
    </div>

    <!-- 数据表格 -->
    <div v-if="isTableVisible" class="data-table-section">
      <el-card class="table-card" shadow="never">
        <template #header>
          <div class="table-header">
            <span class="table-title">📋 详细数据</span>
            <div class="table-actions">
                             <el-button 
                 size="small" 
                 @click="exportData"
               >
                 <el-icon>
                   <component :is="'Download'" />
                 </el-icon>
                导出数据
              </el-button>
                             <el-button 
                 size="small" 
                 @click="toggleFieldSelector"
               >
                 <el-icon>
                   <component :is="'Setting'" />
                 </el-icon>
                字段设置
              </el-button>
            </div>
          </div>
        </template>

        <!-- 字段选择器 -->
        <div v-if="showFieldSelector" class="field-selector">
          <el-checkbox-group v-model="selectedFields" @change="updateTableColumns">
            <el-checkbox 
              v-for="field in allFields" 
              :key="field" 
              :label="field"
            >
              {{ field }}
            </el-checkbox>
          </el-checkbox-group>
        </div>

        <!-- 表格 -->
        <div class="table-container">
                     <el-table 
             :data="tableData" 
             stripe 
             border 
             size="small"
             max-height="600"
             style="width: 100%"
           >
            <el-table-column 
              v-for="column in displayColumns" 
              :key="column.prop"
              :prop="column.prop" 
              :label="column.label"
              :width="column.width"
              show-overflow-tooltip
            >
              <template #default="scope">
                <span v-if="scope.row[column.prop] === null || scope.row[column.prop] === undefined">
                  <el-tag size="small" type="info">空值</el-tag>
                </span>
                <span v-else-if="typeof scope.row[column.prop] === 'number'">
                  {{ formatNumber(scope.row[column.prop]) }}
                </span>
                <span v-else>
                  {{ scope.row[column.prop] }}
                </span>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="tableData.length"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowUp, ArrowDown, Download, Setting } from '@element-plus/icons-vue'

export default {
  name: 'DataTable',
  props: {
    data: {
      type: String,
      required: true
    }
  },
  setup(props) {
    const isTableVisible = ref(false)
    const showFieldSelector = ref(false)
    const currentPage = ref(1)
    const pageSize = ref(20)
    const selectedFields = ref([])
    
    const tableData = ref([])
    const allFields = ref([])
    const tableColumns = ref([])
    const dataInfo = ref({
      recordCount: 0,
      fieldCount: 0,
      dataSize: '0 KB',
      parseTime: 0
    })

    // 解析Python字典列表数据
    const parseData = () => {
      const startTime = Date.now()
      
      try {
        // 检测是否为JSON格式的数据响应
        if (props.data.startsWith('{') && props.data.includes('"dataType":"python_dict_list"')) {
          const response = JSON.parse(props.data)
          const dictListStr = response.parsedData
          
          // 解析Python字典列表
          const parsedData = parsePythonDictList(dictListStr)
          
          if (parsedData && parsedData.length > 0) {
            tableData.value = parsedData
            allFields.value = Object.keys(parsedData[0])
            selectedFields.value = allFields.value.slice(0, 10) // 默认显示前10个字段
            
            // 更新数据信息
            dataInfo.value = {
              recordCount: parsedData.length,
              fieldCount: allFields.value.length,
              dataSize: formatFileSize(JSON.stringify(parsedData).length),
              parseTime: Date.now() - startTime
            }
            
            updateTableColumns()
            isTableVisible.value = true
          }
        }
      } catch (error) {
        console.error('解析数据失败:', error)
        ElMessage.error('数据解析失败，请检查数据格式')
      }
    }

    // 解析Python字典列表字符串
    const parsePythonDictList = (dictListStr) => {
      try {
        // 简单的Python字典列表解析
        // 将Python格式转换为JSON格式
        let jsonStr = dictListStr
          .replace(/'/g, '"')  // 单引号转双引号
          .replace(/None/g, 'null')  // None转null
          .replace(/True/g, 'true')  // True转true
          .replace(/False/g, 'false')  // False转false
        
        return JSON.parse(jsonStr)
      } catch (error) {
        console.error('Python字典列表解析失败:', error)
        return null
      }
    }

    // 更新表格列
    const updateTableColumns = () => {
      tableColumns.value = selectedFields.value.map(field => ({
        prop: field,
        label: field,
        width: getColumnWidth(field)
      }))
    }

    // 获取列宽
    const getColumnWidth = (field) => {
      const fieldLength = field.length
      if (fieldLength <= 10) return 120
      if (fieldLength <= 20) return 150
      return 200
    }

    // 格式化数字
    const formatNumber = (num) => {
      if (typeof num !== 'number') return num
      if (num === 0) return '0'
      if (Math.abs(num) >= 1e6) return (num / 1e6).toFixed(2) + 'M'
      if (Math.abs(num) >= 1e3) return (num / 1e3).toFixed(2) + 'K'
      return num.toFixed(2)
    }

    // 格式化文件大小
    const formatFileSize = (bytes) => {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    }

    // 切换表格显示
    const toggleTable = () => {
      isTableVisible.value = !isTableVisible.value
    }

    // 切换字段选择器
    const toggleFieldSelector = () => {
      showFieldSelector.value = !showFieldSelector.value
    }

    // 导出数据
    const exportData = () => {
      try {
        const csvContent = convertToCSV(tableData.value, selectedFields.value)
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
        const link = document.createElement('a')
        const url = URL.createObjectURL(blob)
        link.setAttribute('href', url)
        link.setAttribute('download', 'data_export.csv')
        link.style.visibility = 'hidden'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        ElMessage.success('数据导出成功')
      } catch (error) {
        console.error('导出失败:', error)
        ElMessage.error('数据导出失败')
      }
    }

    // 转换为CSV
    const convertToCSV = (data, fields) => {
      const headers = fields.join(',')
      const rows = data.map(row => 
        fields.map(field => {
          const value = row[field]
          if (value === null || value === undefined) return ''
          return `"${String(value).replace(/"/g, '""')}"`
        }).join(',')
      )
      return [headers, ...rows].join('\n')
    }

    // 分页处理
    const handleSizeChange = (val) => {
      pageSize.value = val
      currentPage.value = 1
    }

    const handleCurrentChange = (val) => {
      currentPage.value = val
    }

    // 计算显示的列
    const displayColumns = computed(() => {
      return tableColumns.value
    })

    onMounted(() => {
      parseData()
    })

    return {
      isTableVisible,
      showFieldSelector,
      currentPage,
      pageSize,
      selectedFields,
      tableData,
      allFields,
      tableColumns,
      dataInfo,
      displayColumns,
      toggleTable,
      toggleFieldSelector,
      exportData,
      updateTableColumns,
      formatNumber,
      handleSizeChange,
      handleCurrentChange
    }
  }
}
</script>

<style scoped>
.data-table-container {
  margin: 16px 0;
}

.data-overview {
  margin-bottom: 16px;
}

.overview-card {
  border: 1px solid #e4e7ed;
}

.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.overview-title {
  font-weight: 600;
  color: #303133;
}

.overview-content {
  padding: 8px 0;
}

.stat-item {
  text-align: center;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.data-table-section {
  margin-top: 16px;
}

.table-card {
  border: 1px solid #e4e7ed;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-title {
  font-weight: 600;
  color: #303133;
}

.table-actions {
  display: flex;
  gap: 8px;
}

.field-selector {
  margin-bottom: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 6px;
}

.field-selector .el-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.table-container {
  overflow-x: auto;
}

.pagination-container {
  margin-top: 16px;
  text-align: right;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .overview-header,
  .table-header {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }
  
  .table-actions {
    width: 100%;
    justify-content: flex-end;
  }
  
  .stat-item {
    margin-bottom: 8px;
  }
}
</style>
