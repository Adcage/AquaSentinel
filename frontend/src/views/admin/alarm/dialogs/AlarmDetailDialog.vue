<template>
  <el-dialog
    :model-value="modelValue"
    title="报警详情"
    width="700px"
    @close="handleClose"
  >
    <div v-loading="loading">
      <el-descriptions v-if="detailData" :column="2" border>
        <el-descriptions-item label="报警ID">
          {{ detailData.alertUid || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="报警类型">
          <el-tag :type="getAlarmTypeColor(detailData.alertType)">
            {{ getAlarmTypeLabel(detailData.alertType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="处理状态">
          <el-tag :type="getAlarmStatusColor(detailData.alertStatus)">
            {{ getAlarmStatusLabel(detailData.alertStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="触发时间">
          {{ detailData.createdAt || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="摄像头ID">
          {{ detailData.cameraId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="场馆ID">
          {{ detailData.venueId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="事件位置" :span="2">
          {{ detailData.incidentLocation || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="紧急联系人">
          {{ detailData.emergencyContactName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="联系电话">
          {{ detailData.emergencyContactPhone || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="处理救生员ID">
          {{ detailData.lifeguardId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="解决时间">
          {{ detailData.resolvedTime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="视频流地址" :span="2">
          <el-link v-if="detailData.videoStreamUrl" :href="detailData.videoStreamUrl" target="_blank" type="primary">
            {{ detailData.videoStreamUrl }}
          </el-link>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="推送到APP">
          {{ detailData.pushedToApp ? '是' : '否' }}
        </el-descriptions-item>
        <el-descriptions-item label="推送到PC">
          {{ detailData.pushedToPc ? '是' : '否' }}
        </el-descriptions-item>
        <el-descriptions-item label="首次推送时间" :span="2">
          {{ detailData.firstPushTime || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="!detailData && !loading" class="empty-state">
        暂无详情数据
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getAlertRecordVoById } from '@/api/alertRecordController'
import { unwrapApiData } from '@/services/serviceUtils'

interface Props {
  modelValue: boolean
  alarmId?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const loading = ref(false)
const detailData = ref<API.AlertRecordVO | null>(null)

watch(
  () => props.modelValue,
  async (visible) => {
    if (visible && props.alarmId) {
      await loadDetailData()
    }
  }
)

const loadDetailData = async () => {
  if (!props.alarmId) return

  try {
    loading.value = true
    const response = await getAlertRecordVoById({ id: Number(props.alarmId) })
    detailData.value = unwrapApiData<API.AlertRecordVO>(response, '获取报警详情失败')
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  } finally {
    loading.value = false
  }
}

const getAlarmTypeLabel = (type?: string) => {
  if (type === 'DROWNING') return '溺水'
  if (type === 'CROSS_BORDER') return '越界'
  if (type === 'OVER_CAPACITY') return '超员'
  return type || '-'
}

const getAlarmTypeColor = (type?: string) => {
  if (type === 'DROWNING') return 'danger'
  if (type === 'CROSS_BORDER') return 'warning'
  if (type === 'OVER_CAPACITY') return 'info'
  return ''
}

const getAlarmStatusLabel = (status?: string) => {
  if (status === 'PENDING') return '未处理'
  if (status === 'PROCESSING') return '处理中'
  if (status === 'RESOLVED') return '已处理'
  if (status === 'FALSE_ALARM') return '误报'
  return status || '-'
}

const getAlarmStatusColor = (status?: string) => {
  if (status === 'PENDING') return 'danger'
  if (status === 'PROCESSING') return 'warning'
  if (status === 'RESOLVED') return 'success'
  if (status === 'FALSE_ALARM') return 'info'
  return ''
}

const handleClose = () => {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #909399;
  font-size: 14px;
}
</style>
