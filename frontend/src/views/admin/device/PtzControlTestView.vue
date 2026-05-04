<template>
  <div class="ptz-control-view admin-page">
    <div class="admin-page-header">
      <h1>云台控制测试</h1>
      <p>用于验证浏览器到设备的控制链路是否联通</p>
    </div>

    <el-card shadow="never" class="admin-table-card">
      <el-form :inline="true">
        <el-form-item label="设备 IP">
          <el-input v-model="deviceIp" placeholder="例如 192.168.137.175" style="width: 320px" clearable />
        </el-form-item>
        <el-form-item>
          <el-button @click="rememberDeviceIp">保存地址</el-button>
          <el-button type="primary" @click="refreshStatus">刷新状态</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :md="16">
        <el-card shadow="never" class="admin-table-card">
          <template #header><span>视频预览</span></template>
          <div class="video-wrap">
            <img v-if="previewUrl" :src="previewUrl" class="preview-image" alt="视频预览" />
            <div v-else class="video-placeholder">当前设备未配置视频流地址</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="never" class="admin-table-card">
          <template #header><span>云台方向控制</span></template>
          <div class="pad-grid">
            <el-button :disabled="!hasDeviceIp" @click="nudge('UP')">上</el-button>
            <div></div>
            <el-button :disabled="!hasDeviceIp" @click="nudge('LEFT')">左</el-button>
            <el-button :disabled="!hasDeviceIp" @click="home">回中</el-button>
            <el-button :disabled="!hasDeviceIp" @click="nudge('RIGHT')">右</el-button>
            <div></div>
            <el-button :disabled="!hasDeviceIp" @click="nudge('DOWN')">下</el-button>
          </div>
          <div class="status-box">
            <div><strong>当前模式：</strong>{{ isCalibrationMode ? '校准模式' : '正常模式' }}</div>
            <div><strong>最近结果：</strong></div>
            <pre>{{ lastResult }}</pre>
          </div>
        </el-card>

        <el-card shadow="never" class="admin-table-card calibration-card">
          <template #header><span>校准操作</span></template>
          <div class="calibration-actions">
            <el-button :disabled="!hasDeviceIp || isCalibrationMode" @click="handleStartCalibration">
              进入校准
            </el-button>
            <el-button :disabled="!hasDeviceIp" @click="handleLoadCalibrationData">
              读取校准参数
            </el-button>
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" @click="handleSaveCalibration">
              保存校准
            </el-button>
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" @click="handleExitCalibration">
              退出校准
            </el-button>
          </div>

          <el-divider content-position="left">PAN 水平校准</el-divider>
          <div class="pulse-row">
            <el-input-number v-model="panPulse" :min="500" :max="2500" :step="10" />
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" type="primary" @click="applyPanPulse">
              应用 PAN 脉宽
            </el-button>
          </div>
          <div class="pulse-presets">
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" @click="setPanPreset(500)">右极限 500</el-button>
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" @click="setPanPreset(1400)">中位 1400</el-button>
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" @click="setPanPreset(2400)">左极限 2400</el-button>
          </div>

          <el-divider content-position="left">TILT 垂直校准</el-divider>
          <div class="pulse-row">
            <el-input-number v-model="tiltPulse" :min="500" :max="2500" :step="10" />
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" type="primary" @click="applyTiltPulse">
              应用 TILT 脉宽
            </el-button>
          </div>
          <div class="calibration-tip">
            说明：方向按钮用于验证控制链路与正常云台动作；校准按钮用于直接下发舵机脉宽，找最小值、最大值和中位值。
          </div>
          <div class="calibration-data">
            <div class="calibration-data__title">当前校准参数</div>
            <div class="calibration-data__grid">
              <span>PAN 最小</span><strong>{{ calibrationData.panMinUs }}</strong>
              <span>PAN 最大</span><strong>{{ calibrationData.panMaxUs }}</strong>
              <span>PAN 中位</span><strong>{{ calibrationData.panCenterUs }}</strong>
              <span>TILT 最小</span><strong>{{ calibrationData.tiltMinUs }}</strong>
              <span>TILT 最大</span><strong>{{ calibrationData.tiltMaxUs }}</strong>
              <span>TILT 中位</span><strong>{{ calibrationData.tiltCenterUs }}</strong>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  controlHome,
  controlNudge,
  exitCalibration,
  listPtzDevices,
  queryCalibrationData,
  queryPtzStatus,
  saveCalibration,
  setCalibrationPanPulse,
  setCalibrationTiltPulse,
  startCalibration,
  type CalibrationSnapshot,
  type PtzDeviceResponse,
} from '@/services/ptzControlService'
import { buildDeviceStreamUrl } from '@/utils/ptzDirectControl'

const LAST_DEVICE_IP_KEY = 'ptz-test-device-ip'

const deviceIp = ref('')
const lastResult = ref('等待控制指令')
const isCalibrationMode = ref(false)
const panPulse = ref(1400)
const tiltPulse = ref(1500)
const calibrationData = ref<CalibrationSnapshot>({
  panMinUs: -1,
  panMaxUs: -1,
  panCenterUs: -1,
  tiltMinUs: -1,
  tiltMaxUs: -1,
  tiltCenterUs: -1,
})

const runAction = async (action: () => Promise<void>) => {
  try {
    await action()
  } catch (error) {
    const message = error instanceof Error ? error.message : '请求失败'
    lastResult.value = message
    ElMessage.error(message)
  }
}

const hasDeviceIp = computed(() => deviceIp.value.trim().length > 0)
const previewUrl = computed(() => buildDeviceStreamUrl(deviceIp.value))

const updateStatusFromResponse = (result: PtzDeviceResponse) => {
  const deviceResponse = result.deviceResponse ?? {}
  const mode = Number(deviceResponse.value3 ?? -1)
  if (mode === 0 || mode === 1) {
    isCalibrationMode.value = mode === 1
  }
  lastResult.value = JSON.stringify(result, null, 2)
}

const rememberDeviceIp = () => {
  const value = deviceIp.value.trim()
  if (!value) {
    ElMessage.warning('请输入设备 IP')
    return
  }
  localStorage.setItem(LAST_DEVICE_IP_KEY, value)
  ElMessage.success('设备地址已保存')
}

const nudge = async (direction: 'LEFT' | 'RIGHT' | 'UP' | 'DOWN') => {
  await runAction(async () => {
    if (!hasDeviceIp.value) {
      return
    }
    const result = await controlNudge(deviceIp.value, direction, 5)
    updateStatusFromResponse(result)
    ElMessage.success(`已发送 ${direction} 控制`)
  })
}

const home = async () => {
  await runAction(async () => {
    if (!hasDeviceIp.value) {
      return
    }
    const result = await controlHome(deviceIp.value)
    updateStatusFromResponse(result)
    ElMessage.success('已发送回中指令')
  })
}

const refreshStatus = async () => {
  await runAction(async () => {
    if (!hasDeviceIp.value) {
      ElMessage.warning('请先输入设备 IP')
      return
    }
    const result = await queryPtzStatus(deviceIp.value)
    updateStatusFromResponse(result)
  })
}

const handleStartCalibration = async () => {
  await runAction(async () => {
    if (!hasDeviceIp.value) {
      return
    }
    const result = await startCalibration(deviceIp.value)
    updateStatusFromResponse(result)
    isCalibrationMode.value = true
    await handleLoadCalibrationData()
    ElMessage.success('已进入校准模式')
  })
}

const handleLoadCalibrationData = async () => {
  await runAction(async () => {
    if (!hasDeviceIp.value) {
      return
    }
    const { result, snapshot } = await queryCalibrationData(deviceIp.value)
    updateStatusFromResponse(result)
    calibrationData.value = snapshot
  })
}

const handleSaveCalibration = async () => {
  await runAction(async () => {
    if (!hasDeviceIp.value) {
      return
    }
    const result = await saveCalibration(deviceIp.value)
    updateStatusFromResponse(result)
    await handleLoadCalibrationData()
    ElMessage.success('校准参数已保存')
  })
}

const handleExitCalibration = async () => {
  await runAction(async () => {
    if (!hasDeviceIp.value) {
      return
    }
    const result = await exitCalibration(deviceIp.value)
    updateStatusFromResponse(result)
    isCalibrationMode.value = false
    ElMessage.success('已退出校准模式')
  })
}

const applyPanPulse = async () => {
  await runAction(async () => {
    if (!hasDeviceIp.value) {
      return
    }
    const result = await setCalibrationPanPulse(deviceIp.value, panPulse.value)
    updateStatusFromResponse(result)
    await handleLoadCalibrationData()
  })
}

const applyTiltPulse = async () => {
  await runAction(async () => {
    if (!hasDeviceIp.value) {
      return
    }
    const result = await setCalibrationTiltPulse(deviceIp.value, tiltPulse.value)
    updateStatusFromResponse(result)
    await handleLoadCalibrationData()
  })
}

const setPanPreset = async (pulse: number) => {
  panPulse.value = pulse
  await applyPanPulse()
}

onMounted(() => {
  deviceIp.value = localStorage.getItem(LAST_DEVICE_IP_KEY) || ''
})
</script>

<style scoped>
.video-wrap {
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  min-height: 360px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.preview-image {
  width: 100%;
  max-height: 540px;
  object-fit: contain;
  background: #000;
}

.video-placeholder {
  color: #909399;
}

.pad-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.status-box {
  margin-top: 16px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
  padding: 10px;
}

.status-box pre {
  margin: 8px 0 0;
  white-space: pre-wrap;
  word-break: break-all;
  color: #303133;
  font-size: 12px;
}

.calibration-card {
  margin-top: 16px;
}

.calibration-actions,
.pulse-row,
.pulse-presets {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.pulse-row {
  align-items: center;
}

.pulse-presets {
  margin-top: 10px;
}

.calibration-tip {
  margin-top: 12px;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}

.calibration-data {
  margin-top: 16px;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafafa;
}

.calibration-data__title {
  margin-bottom: 10px;
  color: #303133;
  font-weight: 600;
}

.calibration-data__grid {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px 12px;
  color: #606266;
}

.calibration-data__grid strong {
  color: #303133;
}
</style>
