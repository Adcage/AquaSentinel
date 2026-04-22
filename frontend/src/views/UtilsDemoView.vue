<template>
  <div class="utils-demo">
    <el-card shadow="never">
      <template #header>工具类使用示例</template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="Excel 导入导出" name="excel">
          <el-space direction="vertical" :size="16" fill style="width: 100%">
            <el-card shadow="never">
              <template #header>导出 Excel</template>
              <el-space wrap>
                <el-button type="primary" @click="handleExportExcel">
                  <el-icon><Download /></el-icon>
                  导出用户列表
                </el-button>
                <el-button @click="handleExportMultiSheet">导出多工作表</el-button>
                <el-button @click="handleExportCSV">导出 CSV</el-button>
              </el-space>
            </el-card>

            <el-card shadow="never">
              <template #header>导入 Excel</template>
              <el-space direction="vertical" fill style="width: 100%">
                <el-upload
                  :before-upload="handleBeforeUpload"
                  :show-file-list="false"
                  :auto-upload="false"
                  accept=".xlsx,.xls,.csv"
                >
                  <el-button>
                    <el-icon><Upload /></el-icon>
                    选择文件导入
                  </el-button>
                </el-upload>
                <el-button link @click="handleDownloadTemplate">下载导入模板</el-button>
                <el-alert
                  v-if="importResult"
                  :title="importResult.message"
                  :type="importResult.success ? 'success' : 'error'"
                  closable
                  @close="importResult = null"
                />
              </el-space>
            </el-card>

            <el-card v-if="importedData.length > 0" shadow="never">
              <template #header>导入数据预览</template>
              <el-table :data="importedData" size="small">
                <el-table-column prop="name" label="姓名" />
                <el-table-column prop="age" label="年龄" />
                <el-table-column prop="city" label="城市" />
              </el-table>
            </el-card>
          </el-space>
        </el-tab-pane>

        <el-tab-pane label="PDF 生成" name="pdf">
          <el-space direction="vertical" :size="16" fill style="width: 100%">
            <el-card shadow="never">
              <template #header>导出 PDF</template>
              <el-space wrap>
                <el-button type="primary" @click="handleExportPDF">
                  <el-icon><Document /></el-icon>
                  导出当前页面
                </el-button>
                <el-button @click="handleExportTablePDF">导出表格 PDF</el-button>
                <el-button @click="handlePrint">
                  <el-icon><Printer /></el-icon>
                  打印
                </el-button>
              </el-space>
            </el-card>

            <el-card id="pdf-content" shadow="never">
              <template #header>PDF 预览内容</template>
              <div class="pdf-content-inner">
                <h2>这是一个示例报表</h2>
                <p>生成时间：{{ currentTime }}</p>
                <el-table :data="sampleData" size="small">
                  <el-table-column prop="id" label="ID" />
                  <el-table-column prop="name" label="姓名" />
                  <el-table-column prop="age" label="年龄" />
                  <el-table-column prop="city" label="城市" />
                  <el-table-column prop="salary" label="薪资" />
                </el-table>
              </div>
            </el-card>
          </el-space>
        </el-tab-pane>

        <el-tab-pane label="通用工具" name="common">
          <el-space direction="vertical" :size="16" fill style="width: 100%">
            <el-card shadow="never">
              <template #header>日期格式化</template>
              <p>当前时间：{{ formattedDate }}</p>
              <p>自定义格式：{{ customFormattedDate }}</p>
            </el-card>

            <el-card shadow="never">
              <template #header>数字格式化</template>
              <p>千分位：{{ formattedNumber }}</p>
              <p>金额：{{ formattedCurrency }}</p>
              <p>文件大小：{{ formattedFileSize }}</p>
            </el-card>

            <el-card shadow="never">
              <template #header>数据脱敏</template>
              <p>手机号：{{ maskedPhone }}</p>
              <p>身份证：{{ maskedIdCard }}</p>
              <p>邮箱：{{ maskedEmail }}</p>
            </el-card>

            <el-card shadow="never">
              <template #header>复制到剪贴板</template>
              <el-space>
                <el-input v-model="copyText" placeholder="输入要复制的内容" />
                <el-button @click="handleCopy">
                  <el-icon><CopyDocument /></el-icon>
                  复制
                </el-button>
              </el-space>
            </el-card>
          </el-space>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElLoading, ElMessage, type UploadProps } from 'element-plus'
import { CopyDocument, Document, Download, Printer, Upload } from '@element-plus/icons-vue'
import { CommonUtil, ExcelUtil, PDFUtil } from '@/utils'

interface ImportResult {
  success: boolean
  message: string
}

interface SampleData {
  id: number
  name: string
  age: number
  city: string
  salary: number
}

const activeTab = ref('excel')
const importResult = ref<ImportResult | null>(null)
const importedData = ref<SampleData[]>([])
const copyText = ref('Hello World!')

const sampleData: SampleData[] = [
  { id: 1, name: '张三', age: 25, city: '北京', salary: 8000 },
  { id: 2, name: '李四', age: 30, city: '上海', salary: 12000 },
  { id: 3, name: '王五', age: 28, city: '广州', salary: 10000 },
  { id: 4, name: '赵六', age: 32, city: '深圳', salary: 15000 },
  { id: 5, name: '钱七', age: 27, city: '杭州', salary: 9000 },
]

const currentTime = computed(() => CommonUtil.formatDate(new Date()))
const formattedDate = computed(() => CommonUtil.formatDate(new Date(), 'YYYY-MM-DD HH:mm:ss'))
const customFormattedDate = computed(() => CommonUtil.formatDate(new Date(), 'YYYY年MM月DD日'))
const formattedNumber = computed(() => CommonUtil.formatNumber(1234567.89))
const formattedCurrency = computed(() => CommonUtil.formatCurrency(1234567.89))
const formattedFileSize = computed(() => CommonUtil.formatFileSize(1234567890))
const maskedPhone = computed(() => CommonUtil.maskPhone('13812345678'))
const maskedIdCard = computed(() => CommonUtil.maskIdCard('110101199001011234'))
const maskedEmail = computed(() => CommonUtil.maskEmail('example@email.com'))

const handleExportExcel = () => {
  const result = ExcelUtil.exportExcel(sampleData as unknown as Record<string, unknown>[], '用户列表', {
    header: ['ID', '姓名', '年龄', '城市', '薪资'],
    autoWidth: true,
  })

  if (result.success) {
    ElMessage.success('导出成功')
  } else {
    ElMessage.error('导出失败')
  }
}

const handleExportMultiSheet = () => {
  const sheets = [
    { name: '用户列表', data: sampleData as unknown as Record<string, unknown>[] },
    {
      name: '统计数据',
      data: [
        { type: '总人数', value: sampleData.length },
        { type: '平均年龄', value: 28.4 },
        { type: '平均薪资', value: 10800 },
      ],
    },
  ]

  const result = ExcelUtil.exportMultiSheet(sheets as never, '综合报表')

  if (result.success) {
    ElMessage.success('导出成功')
  } else {
    ElMessage.error('导出失败')
  }
}

const handleExportCSV = () => {
  const result = ExcelUtil.exportCSV(sampleData as unknown as Record<string, unknown>[], '用户列表', {
    header: ['ID', '姓名', '年龄', '城市', '薪资'],
  })

  if (result.success) {
    ElMessage.success('导出成功')
  } else {
    ElMessage.error('导出失败')
  }
}

const handleBeforeUpload: UploadProps['beforeUpload'] = async (file) => {
  const validation = ExcelUtil.validateFile(file)
  if (!validation.valid) {
    ElMessage.error(validation.message)
    return false
  }

  try {
    const result = await ExcelUtil.importExcel(file, {
      header: true,
      transform: (data) =>
        data.map((item) => ({
          name: (item['姓名'] || item['name']) as string,
          age: parseInt(String(item['年龄'] || item['age'])),
          city: (item['城市'] || item['city']) as string,
        })),
    })

    if (result.success) {
      importedData.value = result.data as unknown as SampleData[]
      importResult.value = {
        success: true,
        message: `成功导入 ${result.data.length} 条数据`,
      }
      ElMessage.success('导入成功')
    }
  } catch (error) {
    importResult.value = {
      success: false,
      message: (error as Error).message || '导入失败',
    }
    ElMessage.error('导入失败')
  }

  return false
}

const handleDownloadTemplate = () => {
  const result = ExcelUtil.downloadTemplate(
    ['姓名', '年龄', '城市'],
    '用户导入模板',
    [['张三', 25, '北京'], ['李四', 30, '上海']]
  )

  if (result.success) {
    ElMessage.success('模板下载成功')
  }
}

const handleExportPDF = async () => {
  const loading = ElLoading.service({
    lock: true,
    text: '正在生成 PDF...',
    background: 'rgba(255, 255, 255, 0.75)',
  })

  const result = await PDFUtil.exportFromHTML('#pdf-content', '示例报表', {
    orientation: 'portrait',
    quality: 0.95,
  })

  loading.close()

  if (result.success) {
    ElMessage.success('PDF 生成成功')
  } else {
    ElMessage.error('PDF 生成失败')
  }
}

const handleExportTablePDF = () => {
  const result = PDFUtil.exportTable(sampleData as unknown as Record<string, unknown>[], {
    title: '用户列表报表',
    columns: [
      { header: 'ID', dataKey: 'id' },
      { header: '姓名', dataKey: 'name' },
      { header: '年龄', dataKey: 'age' },
      { header: '城市', dataKey: 'city' },
      { header: '薪资', dataKey: 'salary' },
    ],
    fileName: '用户列表',
  })

  if (result.success) {
    ElMessage.success('PDF 生成成功')
  } else {
    ElMessage.error('PDF 生成失败')
  }
}

const handlePrint = () => {
  const result = PDFUtil.print('#pdf-content', {
    title: '示例报表',
  })

  if (result.success) {
    ElMessage.success('打印成功')
  }
}

const handleCopy = async () => {
  const result = await CommonUtil.copyToClipboard(copyText.value)

  if (result.success) {
    ElMessage.success('复制成功')
  } else {
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped>
.utils-demo {
  padding: 24px;
}

.pdf-content-inner {
  padding: 20px;
}
</style>
