<template>
  <el-dialog
    :model-value="visible"
    :title="item?.name ?? '设备详情'"
    width="720px"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
    @close="handleClose"
  >
    <div class="camera-detail">
      <div class="camera-detail__video">
        <canvas
          v-if="sourceElement"
          ref="mirrorCanvasRef"
          class="camera-detail__mirror"
        />
        <CameraStreamSurface
          v-else
          ref="previewRef"
          :protocol="effectiveProtocol"
          :stream-url="effectiveStreamUrl"
          :muted="true"
          :visible="visible"
        />
      </div>

      <div class="camera-detail__info">
        <div class="camera-detail__info-row">
          <span class="camera-detail__label">位置</span>
          <span>{{ item?.location ?? '-' }}</span>
        </div>
        <div class="camera-detail__info-row">
          <span class="camera-detail__label">人数</span>
          <span>{{ item?.peopleCount ?? 0 }} 人</span>
        </div>
        <div class="camera-detail__info-row">
          <span class="camera-detail__label">风险等级</span>
          <StatusTag
            :label="riskMeta.label"
            :type="riskMeta.type"
          />
        </div>
      </div>

      <template v-if="supportsPtzControls">
        <el-divider content-position="left">云台控制</el-divider>
        <div class="ptz-panel">
          <div class="ptz-panel__toolbar">
            <div class="ptz-panel__angles">
              <span>当前 PAN {{ displayPan }}°</span>
              <span>TILT {{ displayTilt }}°</span>
            </div>
            <el-button
              type="primary"
              :loading="loadingHome"
              :disabled="isControlBusy"
              class="ptz-panel__home-btn"
              @click="handleHome"
            >
              回中
            </el-button>
          </div>

          <div class="ptz-panel__board-wrap">
            <div class="ptz-panel__top-actions">
              <el-button
                class="limit-btn"
                :disabled="isControlBusy"
                @click="setMovePreset(90, 0)"
              >
                仰视
              </el-button>
              <el-button
                :loading="activeDirection === 'UP'"
                :disabled="isDirectionBusy"
                class="direction-btn"
                @mousedown.prevent="startDirectionPress('UP')"
                @mouseup.prevent="stopDirectionPress()"
                @mouseleave.prevent="stopDirectionPress()"
                @touchstart.prevent="startDirectionPress('UP')"
                @touchend.prevent="stopDirectionPress()"
              >
                上
              </el-button>
            </div>

            <div class="ptz-panel__middle-actions">
              <div class="ptz-panel__side ptz-panel__side--left">
                <el-button
                  class="limit-btn"
                  :disabled="isControlBusy"
                  @click="setMovePreset(0, 90)"
                >
                  最左
                </el-button>
                <el-button
                  :loading="activeDirection === 'LEFT'"
                  :disabled="isDirectionBusy"
                  class="direction-btn"
                  @mousedown.prevent="startDirectionPress('LEFT')"
                  @mouseup.prevent="stopDirectionPress()"
                  @mouseleave.prevent="stopDirectionPress()"
                  @touchstart.prevent="startDirectionPress('LEFT')"
                  @touchend.prevent="stopDirectionPress()"
                >
                  左
                </el-button>
              </div>

              <div class="ptz-panel__board-main">
                <div class="ptz-panel__board-header">直接定位</div>
                <div
                  ref="squarePadRef"
                  class="ptz-square-pad"
                  @pointerdown.prevent="handlePadPointerDown"
                  @pointermove.prevent="handlePadPointerMove"
                  @pointerup.prevent="handlePadPointerUp"
                  @pointercancel.prevent="handlePadPointerUp"
                >
                  <div class="ptz-square-pad__grid"></div>
                  <div class="ptz-square-pad__center-cross"></div>
                  <div class="ptz-square-pad__label ptz-square-pad__label--top">TILT 0°</div>
                  <div class="ptz-square-pad__label ptz-square-pad__label--bottom">TILT 180°</div>
                  <div class="ptz-square-pad__label ptz-square-pad__label--left">PAN 0°</div>
                  <div class="ptz-square-pad__label ptz-square-pad__label--right">PAN 180°</div>
                  <div class="ptz-square-pad__cursor" :style="padCursorStyle">
                    <span class="ptz-square-pad__cursor-cross"></span>
                  </div>
                </div>
                <el-form :inline="true" class="ptz-panel__move-form">
                  <el-form-item label="PAN">
                    <el-input-number
                      v-model="targetPan"
                      :min="0"
                      :max="180"
                      :step="1"
                      controls-position="right"
                    />
                  </el-form-item>
                  <el-form-item label="TILT">
                    <el-input-number
                      v-model="targetTilt"
                      :min="0"
                      :max="180"
                      :step="1"
                      controls-position="right"
                    />
                  </el-form-item>
                  <el-form-item>
                    <el-button
                      type="primary"
                      :loading="loadingMove"
                      :disabled="isControlBusy"
                      @click="handleMoveTo"
                    >
                      移动到
                    </el-button>
                  </el-form-item>
                </el-form>
              </div>

              <div class="ptz-panel__side ptz-panel__side--right">
                <el-button
                  :loading="activeDirection === 'RIGHT'"
                  :disabled="isDirectionBusy"
                  class="direction-btn"
                  @mousedown.prevent="startDirectionPress('RIGHT')"
                  @mouseup.prevent="stopDirectionPress()"
                  @mouseleave.prevent="stopDirectionPress()"
                  @touchstart.prevent="startDirectionPress('RIGHT')"
                  @touchend.prevent="stopDirectionPress()"
                >
                  右
                </el-button>
                <el-button
                  class="limit-btn"
                  :disabled="isControlBusy"
                  @click="setMovePreset(180, 90)"
                >
                  最右
                </el-button>
              </div>
            </div>

            <div class="ptz-panel__bottom-actions">
              <el-button
                :loading="activeDirection === 'DOWN'"
                :disabled="isDirectionBusy"
                class="direction-btn"
                @mousedown.prevent="startDirectionPress('DOWN')"
                @mouseup.prevent="stopDirectionPress()"
                @mouseleave.prevent="stopDirectionPress()"
                @touchstart.prevent="startDirectionPress('DOWN')"
                @touchend.prevent="stopDirectionPress()"
              >
                下
              </el-button>
              <el-button
                class="limit-btn"
                :disabled="isControlBusy"
                @click="setMovePreset(90, 180)"
              >
                俯视
              </el-button>
              <el-button
                :loading="loadingStatus"
                :disabled="isControlBusy"
                @click="handleStatus"
              >
                查询状态
              </el-button>
              <el-button
                class="limit-btn"
                :disabled="isControlBusy"
                @click="setMovePreset(90, 90)"
              >
                平视
              </el-button>
            </div>
          </div>
          <div v-if="statusText" class="ptz-panel__status">
            <pre>{{ statusText }}</pre>
          </div>
        </div>
      </template>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import CameraStreamSurface from "@/components/business/CameraStreamSurface.vue";
import StatusTag from "@/components/common/StatusTag.vue";
import { controlCameraPtz } from "@/api/cameraDeviceController";
import { unwrapApiData } from "@/services/serviceUtils";
import type { CameraGridItem } from "@/types/business";

interface Props {
  visible: boolean;
  item: CameraGridItem | null;
  sourceElement?: HTMLImageElement | HTMLVideoElement | null;
}

const props = defineProps<Props>();
const previewRef = ref<InstanceType<typeof CameraStreamSurface> | null>(null);
const mirrorCanvasRef = ref<HTMLCanvasElement | null>(null);
const squarePadRef = ref<HTMLDivElement | null>(null);

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
}>();

const activeDirection = ref<"LEFT" | "RIGHT" | "UP" | "DOWN" | "">("");
const loadingHome = ref(false);
const loadingStatus = ref(false);
const loadingMove = ref(false);
const statusText = ref("");
const targetPan = ref(90)
const targetTilt = ref(90)
const displayPan = ref(90)
const displayTilt = ref(90)
const isPadDragging = ref(false)
const isDirectionBusy = computed(() => activeDirection.value !== '')
const isControlBusy = computed(
  () => isDirectionBusy.value || loadingHome.value || loadingStatus.value || loadingMove.value,
)

const SHORT_NUDGE_STEP = 5
const CONTINUOUS_NUDGE_STEP = 10
const LONG_PRESS_DELAY_MS = 260
const LONG_PRESS_INTERVAL_MS = 260

const effectiveProtocol = computed(() => props.item?.previewProtocol || "mjpeg");
const effectiveStreamUrl = computed(() => props.item?.previewUrl || props.item?.streamUrl || "");

const isPtzDevice = computed(() => props.item?.protocol?.toUpperCase() === "PTZ");
const hasDirectDeviceStream = computed(() => {
  const streamUrl = String(props.item?.streamUrl || '').trim()
  return /^https?:\/\/.+\/stream(?:\?|$)/i.test(streamUrl)
})
const supportsPtzControls = computed(() =>
  isPtzDevice.value || hasDirectDeviceStream.value,
)

const riskMeta = computed(() => {
  if (!props.item) return { label: "正常", type: "success" as const };
  if (props.item.riskLevel === "danger") return { label: "危险", type: "danger" as const };
  if (props.item.riskLevel === "warning") return { label: "预警", type: "warning" as const };
  return { label: "正常", type: "success" as const };
});

const clampAngle = (value: number) => {
  if (!Number.isFinite(value)) {
    return 90
  }
  return Math.max(0, Math.min(180, Math.round(value)))
}

const syncDisplayAngles = (pan: number, tilt: number) => {
  const safePan = clampAngle(pan)
  const safeTilt = clampAngle(tilt)
  displayPan.value = safePan
  displayTilt.value = safeTilt
  targetPan.value = safePan
  targetTilt.value = safeTilt
}

const padCursorStyle = computed(() => ({
  left: `${(displayPan.value / 180) * 100}%`,
  top: `${(displayTilt.value / 180) * 100}%`,
}))

const sendNudge = async (
  direction: "LEFT" | "RIGHT" | "UP" | "DOWN",
  step: number,
) => {
  if (!props.item) return;
  try {
    const response = await controlCameraPtz({
      cameraId: props.item.cameraId,
      action: "NUDGE",
      direction,
      step,
    });
    unwrapApiData(response, "控制失败");
    if (direction === 'LEFT') {
      syncDisplayAngles(displayPan.value - step, displayTilt.value)
    } else if (direction === 'RIGHT') {
      syncDisplayAngles(displayPan.value + step, displayTilt.value)
    } else if (direction === 'UP') {
      syncDisplayAngles(displayPan.value, displayTilt.value - step)
    } else {
      syncDisplayAngles(displayPan.value, displayTilt.value + step)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "控制失败");
  }
};

let directionPressTimer: ReturnType<typeof setTimeout> | null = null
let directionRepeatTimer: ReturnType<typeof setInterval> | null = null
let longPressTriggered = false
let directionRequestInFlight = false

const clearDirectionTimers = () => {
  if (directionPressTimer) {
    clearTimeout(directionPressTimer)
    directionPressTimer = null
  }
  if (directionRepeatTimer) {
    clearInterval(directionRepeatTimer)
    directionRepeatTimer = null
  }
}

const runContinuousNudge = async (direction: "LEFT" | "RIGHT" | "UP" | "DOWN") => {
  if (directionRequestInFlight) {
    return
  }
  directionRequestInFlight = true
  try {
    await sendNudge(direction, CONTINUOUS_NUDGE_STEP)
  } finally {
    directionRequestInFlight = false
  }
}

const startDirectionPress = (direction: "LEFT" | "RIGHT" | "UP" | "DOWN") => {
  if (!props.item || activeDirection.value) {
    return
  }
  activeDirection.value = direction
  longPressTriggered = false
  directionRequestInFlight = false
  clearDirectionTimers()
  directionPressTimer = setTimeout(() => {
    longPressTriggered = true
    void runContinuousNudge(direction)
    directionRepeatTimer = setInterval(() => {
      void runContinuousNudge(direction)
    }, LONG_PRESS_INTERVAL_MS)
  }, LONG_PRESS_DELAY_MS)
}

const stopDirectionPress = () => {
  const direction = activeDirection.value
  if (!direction) {
    return
  }
  clearDirectionTimers()
  activeDirection.value = ''
  if (!longPressTriggered) {
    void sendNudge(direction, SHORT_NUDGE_STEP)
  }
  longPressTriggered = false
}

const handleHome = async () => {
  if (!props.item) return;
  loadingHome.value = true;
  try {
    const response = await controlCameraPtz({
      cameraId: props.item.cameraId,
      action: "HOME",
    });
    unwrapApiData(response, "回中失败");
    syncDisplayAngles(90, 90)
    ElMessage.success("已发送回中指令");
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "回中失败");
  } finally {
    loadingHome.value = false;
  }
};

const handleStatus = async () => {
  if (!props.item) return;
  loadingStatus.value = true;
  try {
    const response = await controlCameraPtz({
      cameraId: props.item.cameraId,
      action: "STATUS",
    });
    const data = unwrapApiData<Record<string, unknown>>(response, "状态查询失败");
    statusText.value = JSON.stringify(data, null, 2);
    const deviceResponse = data.deviceResponse as Record<string, unknown> | undefined
    const pan = Number(deviceResponse?.pan)
    const tilt = Number(deviceResponse?.tilt)
    if (Number.isFinite(pan) && Number.isFinite(tilt)) {
      syncDisplayAngles(pan, tilt)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "状态查询失败");
  } finally {
    loadingStatus.value = false;
  }
};

let moveRequestInFlight = false
let queuedMoveTarget: { pan: number; tilt: number } | null = null
let lastSuccessfulMoveKey = ''

const toMoveKey = (pan: number, tilt: number) => `${pan}:${tilt}`

const executeMoveTo = async (pan: number, tilt: number) => {
  if (!props.item) return;
  loadingMove.value = true;
  moveRequestInFlight = true
  try {
    const response = await controlCameraPtz({
      cameraId: props.item.cameraId,
      action: "MOVE",
      pan,
      tilt,
    });
    unwrapApiData(response, "移动到指定角度失败");
    syncDisplayAngles(pan, tilt)
    lastSuccessfulMoveKey = toMoveKey(pan, tilt)
    ElMessage.success(`已移动到 PAN:${pan}° TILT:${tilt}°`);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "移动到指定角度失败");
  } finally {
    moveRequestInFlight = false
    loadingMove.value = false;
    if (queuedMoveTarget) {
      const nextTarget = queuedMoveTarget
      queuedMoveTarget = null
      await executeMoveTo(nextTarget.pan, nextTarget.tilt)
    }
  }
};

const handleMoveTo = async (force = false) => {
  const pan = clampAngle(targetPan.value)
  const tilt = clampAngle(targetTilt.value)
  const moveKey = toMoveKey(pan, tilt)
  if (!force && !moveRequestInFlight && moveKey === lastSuccessfulMoveKey) {
    return
  }
  if (moveRequestInFlight) {
    if (!queuedMoveTarget || toMoveKey(queuedMoveTarget.pan, queuedMoveTarget.tilt) !== moveKey) {
      queuedMoveTarget = { pan, tilt }
    }
    return
  }
  await executeMoveTo(pan, tilt)
}

const setMovePreset = async (pan: number, tilt: number) => {
  targetPan.value = clampAngle(pan)
  targetTilt.value = clampAngle(tilt)
  await handleMoveTo(true)
}

const resolvePadAnglesFromPoint = (clientX: number, clientY: number) => {
  const pad = squarePadRef.value
  if (!pad) {
    return null
  }
  const rect = pad.getBoundingClientRect()
  if (!rect.width || !rect.height) {
    return null
  }
  const xRatio = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width))
  const yRatio = Math.max(0, Math.min(1, (clientY - rect.top) / rect.height))
  return {
    pan: clampAngle(xRatio * 180),
    tilt: clampAngle(yRatio * 180),
  }
}

let padPointerId: number | null = null
let padMoveTimer: ReturnType<typeof setTimeout> | null = null

const clearPadMoveTimer = () => {
  if (padMoveTimer) {
    clearTimeout(padMoveTimer)
    padMoveTimer = null
  }
}

const queuePadMove = () => {
  clearPadMoveTimer()
  padMoveTimer = setTimeout(() => {
    if (isPadDragging.value) {
      void handleMoveTo()
    }
  }, 180)
}

const handlePadPointerDown = (event: PointerEvent) => {
  if (isControlBusy.value && !isPadDragging.value) {
    return
  }
  const nextAngles = resolvePadAnglesFromPoint(event.clientX, event.clientY)
  if (!nextAngles) {
    return
  }
  padPointerId = event.pointerId
  isPadDragging.value = true
  squarePadRef.value?.setPointerCapture?.(event.pointerId)
  syncDisplayAngles(nextAngles.pan, nextAngles.tilt)
  void handleMoveTo(true)
}

const handlePadPointerMove = (event: PointerEvent) => {
  if (!isPadDragging.value || padPointerId !== event.pointerId) {
    return
  }
  const nextAngles = resolvePadAnglesFromPoint(event.clientX, event.clientY)
  if (!nextAngles) {
    return
  }
  syncDisplayAngles(nextAngles.pan, nextAngles.tilt)
  queuePadMove()
}

const handlePadPointerUp = (event: PointerEvent) => {
  if (!isPadDragging.value || (padPointerId !== null && padPointerId !== event.pointerId)) {
    return
  }
  squarePadRef.value?.releasePointerCapture?.(event.pointerId)
  padPointerId = null
  isPadDragging.value = false
  clearPadMoveTimer()
  void handleMoveTo()
}

const handleClose = () => {
  statusText.value = "";
  stopDirectionPress()
  clearPadMoveTimer()
  isPadDragging.value = false
};

let mirrorAnimationFrame = 0;

const stopMirrorLoop = () => {
  if (mirrorAnimationFrame) {
    cancelAnimationFrame(mirrorAnimationFrame);
    mirrorAnimationFrame = 0;
  }
};

const syncMirrorFrame = () => {
  const canvas = mirrorCanvasRef.value;
  const source = props.sourceElement;
  if (!canvas || !source || !props.visible) {
    stopMirrorLoop();
    return;
  }

  const width = source instanceof HTMLVideoElement
    ? Math.max(source.videoWidth, source.clientWidth)
    : Math.max(source.naturalWidth, source.clientWidth);
  const height = source instanceof HTMLVideoElement
    ? Math.max(source.videoHeight, source.clientHeight)
    : Math.max(source.naturalHeight, source.clientHeight);

  if (width > 0 && height > 0) {
    if (canvas.width !== width || canvas.height !== height) {
      canvas.width = width;
      canvas.height = height;
    }
    const context = canvas.getContext('2d');
    if (context) {
      context.drawImage(source, 0, 0, canvas.width, canvas.height);
    }
  }

  mirrorAnimationFrame = requestAnimationFrame(syncMirrorFrame);
};

watch(
  () => [props.visible, props.sourceElement],
  () => {
    stopMirrorLoop();
    if (props.visible && props.sourceElement) {
      mirrorAnimationFrame = requestAnimationFrame(syncMirrorFrame);
    }
  },
  { immediate: true },
);

const updateVideoFrame = (blob: Blob) => {
  previewRef.value?.updateVideoFrame(blob);
};

defineExpose({
  updateVideoFrame,
  cameraId: computed(() => props.item?.cameraId ?? 0),
  startDirectionPress,
  stopDirectionPress,
  handlePadPointerDown,
  handlePadPointerMove,
  handlePadPointerUp,
});

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('mouseup', stopDirectionPress)
    window.removeEventListener('touchend', stopDirectionPress)
  }
  clearDirectionTimers()
  clearPadMoveTimer()
  stopMirrorLoop();
});

onMounted(() => {
  if (typeof window !== 'undefined') {
    window.addEventListener('mouseup', stopDirectionPress)
    window.addEventListener('touchend', stopDirectionPress)
  }
})
</script>

<style scoped>
.camera-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.camera-detail__video {
  position: relative;
  aspect-ratio: 16 / 9;
  background: #000;
  border-radius: var(--radius-sm);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.camera-detail__mirror {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: contain;
}

.camera-detail__info {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.camera-detail__info-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.camera-detail__label {
  color: var(--color-text-tertiary);
  min-width: 56px;
}

.ptz-panel {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 16px;
}

.ptz-panel__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.ptz-panel__angles {
  display: flex;
  gap: 16px;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.ptz-panel__board-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.ptz-panel__top-actions,
.ptz-panel__bottom-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.ptz-panel__middle-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  width: 100%;
}

.ptz-panel__side {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.ptz-panel__board-main {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.ptz-panel__board-header {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.ptz-square-pad {
  position: relative;
  width: 240px;
  height: 240px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: linear-gradient(180deg, #f8fafc 0%, #eef3f8 100%);
  overflow: hidden;
  touch-action: none;
  cursor: crosshair;
}

.ptz-square-pad__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(to right, rgba(27, 79, 155, 0.12) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(27, 79, 155, 0.12) 1px, transparent 1px);
  background-size: 25% 25%;
}

.ptz-square-pad__center-cross {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 1px;
  height: 100%;
  background: rgba(27, 79, 155, 0.2);
  transform: translateX(-50%);
}

.ptz-square-pad__center-cross::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: 100%;
  height: 1px;
  background: rgba(27, 79, 155, 0.2);
  transform: translate(-50%, -50%);
}

.ptz-square-pad__label {
  position: absolute;
  font-size: 11px;
  color: var(--color-text-tertiary);
  background: rgba(255, 255, 255, 0.8);
  padding: 2px 6px;
  border-radius: 999px;
}

.ptz-square-pad__label--top {
  top: 8px;
  left: 50%;
  transform: translateX(-50%);
}

.ptz-square-pad__label--bottom {
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
}

.ptz-square-pad__label--left {
  left: 8px;
  top: 50%;
  transform: translateY(-50%);
}

.ptz-square-pad__label--right {
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
}

.ptz-square-pad__cursor {
  position: absolute;
  width: 18px;
  height: 18px;
  border: 2px solid var(--color-primary);
  border-radius: 50%;
  background: rgba(27, 79, 155, 0.12);
  transform: translate(-50%, -50%);
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.9);
}

.ptz-square-pad__cursor-cross {
  position: absolute;
  inset: 0;
}

.ptz-square-pad__cursor-cross::before,
.ptz-square-pad__cursor-cross::after {
  content: '';
  position: absolute;
  background: var(--color-primary);
}

.ptz-square-pad__cursor-cross::before {
  width: 2px;
  height: 100%;
  left: 50%;
  top: 0;
  transform: translateX(-50%);
}

.ptz-square-pad__cursor-cross::after {
  height: 2px;
  width: 100%;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
}

.ptz-panel__move-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: center;
}

.ptz-panel__move-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.direction-btn,
.limit-btn,
.ptz-panel__home-btn {
  min-width: 68px;
}

@media (max-width: 900px) {
  .ptz-panel__middle-actions {
    flex-direction: column;
  }

  .ptz-panel__side {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
  }
}

.ptz-panel__status {
  width: 100%;
  max-height: 160px;
  overflow-y: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg-page);
  padding: 10px;
}

.ptz-panel__status pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  color: var(--color-text-secondary);
}
</style>
