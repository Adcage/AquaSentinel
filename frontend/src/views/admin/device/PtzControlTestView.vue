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

<el-card shadow="never" class="admin-table-card">
      <template #header><span>视频预览</span></template>
      <div class="video-wrap">
        <img v-if="previewUrl" :src="previewUrl" class="preview-image" alt="视频预览" />
        <div v-else class="video-placeholder">当前设备未配置视频流地址</div>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="admin-table-card">
          <template #header><span>云台方向控制</span></template>
          <div class="direction-pad">
            <div class="direction-row">
              <div class="direction-empty"></div>
              <el-button :disabled="!hasDeviceIp" class="direction-btn" @click="nudge('UP')">上</el-button>
              <div class="direction-empty"></div>
            </div>
            <div class="direction-row">
              <el-button :disabled="!hasDeviceIp" class="direction-btn" @click="nudge('LEFT')">左</el-button>
              <el-button :disabled="!hasDeviceIp" class="direction-btn direction-home" @click="home">回中</el-button>
              <el-button :disabled="!hasDeviceIp" class="direction-btn" @click="nudge('RIGHT')">右</el-button>
            </div>
            <div class="direction-row">
              <div class="direction-empty"></div>
              <el-button :disabled="!hasDeviceIp" class="direction-btn" @click="nudge('DOWN')">下</el-button>
              <div class="direction-empty"></div>
            </div>
          </div>

          <el-divider content-position="left">直接定位</el-divider>
          <div class="move-to-row">
            <el-form :inline="true" class="move-to-form">
              <el-form-item label="PAN">
                <el-input-number v-model="targetPan" :min="0" :max="180" :step="1" controls-position="right" />
              </el-form-item>
              <el-form-item label="TILT">
                <el-input-number v-model="targetTilt" :min="0" :max="180" :step="1" controls-position="right" />
              </el-form-item>
              <el-form-item>
                <el-button :disabled="!hasDeviceIp || isCalibrationMode" type="primary" @click="handleMoveTo">
                  移动到
                </el-button>
              </el-form-item>
            </el-form>
          </div>
          <div class="move-to-presets">
            <el-button :disabled="!hasDeviceIp || isCalibrationMode" @click="setMoveToPreset(0, 90)">最左 (0°)</el-button>
            <el-button :disabled="!hasDeviceIp || isCalibrationMode" @click="setMoveToPreset(90, 0)">仰视 (0°)</el-button>
            <el-button :disabled="!hasDeviceIp || isCalibrationMode" @click="setMoveToPreset(90, 90)">平视 (90°)</el-button>
            <el-button :disabled="!hasDeviceIp || isCalibrationMode" @click="setMoveToPreset(90, 180)">俯视 (180°)</el-button>
            <el-button :disabled="!hasDeviceIp || isCalibrationMode" @click="setMoveToPreset(180, 90)">最右 (180°)</el-button>
          </div>

          <div class="status-box">
            <div><strong>当前模式：</strong>{{ isCalibrationMode ? '校准模式' : '正常模式' }}</div>
            <div><strong>最近结果：</strong></div>
            <pre>{{ lastResult }}</pre>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="admin-table-card">
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
            <span class="current-pulse">当前：{{ currentPanPulse }}us</span>
          </div>
          <div class="calibration-row">
            <span class="calibration-row__label">最小</span>
            <el-input-number v-model="panMinPulse" :min="500" :max="2500" :step="10" />
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" @click="fillCurrentPanPulse('MIN')">设为当前</el-button>
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" type="primary" @click="setAxisCalibrationValue('PAN', 'MIN', panMinPulse)">确定</el-button>
          </div>
          <div class="calibration-row">
            <span class="calibration-row__label">中位</span>
            <el-input-number v-model="panCenterPulse" :min="500" :max="2500" :step="10" />
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" @click="fillCurrentPanPulse('CENTER')">设为当前</el-button>
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" type="primary" @click="setAxisCalibrationValue('PAN', 'CENTER', panCenterPulse)">确定</el-button>
          </div>
          <div class="calibration-row">
            <span class="calibration-row__label">最大</span>
            <el-input-number v-model="panMaxPulse" :min="500" :max="2500" :step="10" />
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" @click="fillCurrentPanPulse('MAX')">设为当前</el-button>
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" type="primary" @click="setAxisCalibrationValue('PAN', 'MAX', panMaxPulse)">确定</el-button>
          </div>

          <el-divider content-position="left">TILT 垂直校准</el-divider>
          <div class="pulse-row">
            <el-input-number v-model="tiltPulse" :min="500" :max="2500" :step="10" />
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" type="primary" @click="applyTiltPulse">
              应用 TILT 脉宽
            </el-button>
            <span class="current-pulse">当前：{{ currentTiltPulse }}us</span>
          </div>
          <div class="calibration-row">
            <span class="calibration-row__label">最小</span>
            <el-input-number v-model="tiltMinPulse" :min="500" :max="2500" :step="10" />
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" @click="fillCurrentTiltPulse('MIN')">设为当前</el-button>
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" type="primary" @click="setAxisCalibrationValue('TILT', 'MIN', tiltMinPulse)">确定</el-button>
          </div>
          <div class="calibration-row">
            <span class="calibration-row__label">中位</span>
            <el-input-number v-model="tiltCenterPulse" :min="500" :max="2500" :step="10" />
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" @click="fillCurrentTiltPulse('CENTER')">设为当前</el-button>
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" type="primary" @click="setAxisCalibrationValue('TILT', 'CENTER', tiltCenterPulse)">确定</el-button>
          </div>
          <div class="calibration-row">
            <span class="calibration-row__label">最大</span>
            <el-input-number v-model="tiltMaxPulse" :min="500" :max="2500" :step="10" />
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" @click="fillCurrentTiltPulse('MAX')">设为当前</el-button>
            <el-button :disabled="!hasDeviceIp || !isCalibrationMode" type="primary" @click="setAxisCalibrationValue('TILT', 'MAX', tiltMaxPulse)">确定</el-button>
          </div>
          <div class="calibration-tip">
            校准步骤：1. 进入校准。2. 用"应用脉宽"把舵机转到目标位置。3. 点击"设为当前"带入当前脉宽，再分别保存为最小/中位/最大。4. 保存校准。5. 退出校准。PAN 中位应为正前方，TILT 中位应为平视水平。
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
  controlMoveTo,
  controlNudge,
  exitCalibration,
  queryCalibrationData,
  queryPtzStatus,
  saveCalibration,
  setCalibrationPanPulse,
  setCalibrationTiltPulse,
  setCalibrationValue,
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
const currentPanPulse = ref(1500)
const currentTiltPulse = ref(1500)
const panMinPulse = ref(500)
const panCenterPulse = ref(1500)
const panMaxPulse = ref(2500)
const tiltMinPulse = ref(500)
const tiltCenterPulse = ref(1500)
const tiltMaxPulse = ref(2500)
const targetPan = ref(90)
const targetTilt = ref(90)  // 默认90度（平视）
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

const syncCalibrationInputs = (snapshot: CalibrationSnapshot) => {
  panMinPulse.value = snapshot.panMinUs > 0 ? snapshot.panMinUs : 500
  panCenterPulse.value = snapshot.panCenterUs > 0 ? snapshot.panCenterUs : 1500
  panMaxPulse.value = snapshot.panMaxUs > 0 ? snapshot.panMaxUs : 2500
  tiltMinPulse.value = snapshot.tiltMinUs > 0 ? snapshot.tiltMinUs : 500
  tiltCenterPulse.value = snapshot.tiltCenterUs > 0 ? snapshot.tiltCenterUs : 1500
  tiltMaxPulse.value = snapshot.tiltMaxUs > 0 ? snapshot.tiltMaxUs : 2500
}

const updateStatusFromResponse = (result: PtzDeviceResponse) => {
  const mode = Number(result.value3 ?? -1)
  if (mode === 0 || mode === 1) {
    isCalibrationMode.value = mode === 1
  }
  if (result.command?.startsWith('CALIB') && typeof result.pan === 'number' && typeof result.tilt === 'number' && result.pan >= 0 && result.tilt >= 0) {
    currentPanPulse.value = result.pan
    currentTiltPulse.value = result.tilt
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
    syncCalibrationInputs(snapshot)
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

const fillCurrentPanPulse = (key: 'MIN' | 'CENTER' | 'MAX') => {
  if (key === 'MIN') {
    panMinPulse.value = currentPanPulse.value
    return
  }
  if (key === 'CENTER') {
    panCenterPulse.value = currentPanPulse.value
    return
  }
  panMaxPulse.value = currentPanPulse.value
}

const fillCurrentTiltPulse = (key: 'MIN' | 'CENTER' | 'MAX') => {
  if (key === 'MIN') {
    tiltMinPulse.value = currentTiltPulse.value
    return
  }
  if (key === 'CENTER') {
    tiltCenterPulse.value = currentTiltPulse.value
    return
  }
  tiltMaxPulse.value = currentTiltPulse.value
}

const setAxisCalibrationValue = async (axis: 'PAN' | 'TILT', key: 'MIN' | 'CENTER' | 'MAX', pulse: number) => {
  await runAction(async () => {
    if (!hasDeviceIp.value) {
      return
    }
    const result = await setCalibrationValue(deviceIp.value, axis, key, pulse)
    updateStatusFromResponse(result)
    await handleLoadCalibrationData()
    ElMessage.success(`${axis} ${key} 校准值已设置为 ${pulse}us`)
  })
}

const handleMoveTo = async () => {
  await runAction(async () => {
    if (!hasDeviceIp.value) {
      return
    }
    const result = await controlMoveTo(deviceIp.value, targetPan.value, targetTilt.value)
    updateStatusFromResponse(result)
    ElMessage.success(`已移动到 PAN:${targetPan.value}° TILT:${targetTilt.value}°`)
  })
}

const setMoveToPreset = async (pan: number, tilt: number) => {
  targetPan.value = pan
  targetTilt.value = tilt
  await handleMoveTo()
}

onMounted(() => {
  deviceIp.value = localStorage.getItem(LAST_DEVICE_IP_KEY) || ''
})
</script>

<style scoped>
.video-wrap {
  background: #000;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
}

.preview-image {
  width: 100%;
  max-height: 480px;
  object-fit: contain;
  background: #000;
}

.video-placeholder {
  color: #909399;
  padding: 60px 0;
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

.calibration-actions,
.pulse-row,
.calibration-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.pulse-row,
.calibration-row {
  align-items: center;
}

.calibration-row {
  margin-top: 10px;
}

.calibration-row__label {
  width: 36px;
  color: #606266;
}

.current-pulse {
  color: #606266;
  font-size: 13px;
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

.move-to-row {
  margin-top: 8px;
}

.move-to-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.move-to-form .el-form-item {
  margin-bottom: 0;
}

.move-to-presets {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
