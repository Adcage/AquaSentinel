# 监控页面优化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 优化监控总览页面的 3x3 网格画面空间、新增点击画面弹出模态框集成 PTZ 控制、重构云台控制测试页面为上视频下操作布局。

**Architecture:** 前端 Vue 3 Composition API + Element Plus，PTZ 控制走后端代理链路 `controlCameraPtz`，模态框根据 `protocol` 字段判断是否显示控制面板，云台测试页面从左右分栏改为垂直布局。

**Tech Stack:** Vue 3, TypeScript, Element Plus, Pinia

---

## 文件结构

| 操作 | 文件路径 | 职责 |
|------|---------|------|
| 修改 | `frontend/src/types/business.ts` | `CameraGridItem` 新增 `protocol` 字段 |
| 修改 | `frontend/src/services/dashboardService.ts` | `toCameraGridItem()` 传递 `protocol` |
| 修改 | `frontend/src/components/business/CameraGridCard.vue` | 优化画面空间 + 添加点击事件 |
| 新建 | `frontend/src/components/business/CameraDetailModal.vue` | 设备详情模态框（含 PTZ 控制） |
| 修改 | `frontend/src/views/admin/dashboard/AdminDashboardView.vue` | 集成模态框 |
| 修改 | `frontend/src/views/admin/device/PtzControlTestView.vue` | 重构为上视频下操作布局 |

---

### Task 1: CameraGridItem 类型补全 protocol 字段

**Files:**
- Modify: `frontend/src/types/business.ts:52-69`

- [ ] **Step 1: 在 CameraGridItem 接口中新增 protocol 字段**

在 `CameraGridItem` 接口的 `previewUrl` 之后、`detections` 之前添加 `protocol` 字段：

```ts
export interface CameraGridItem {
  id: string;
  cameraId: number;
  cameraCode?: string;
  name: string;
  location: string;
  peopleCount: number;
  riskLevel: "normal" | "warning" | "danger";
  isAlarming: boolean;
  streamUrl: string;
  previewProtocol?: "webrtc" | "mjpeg" | "ws_jpeg";
  previewUrl?: string;
  protocol?: string;
  detections: RealtimeDetection[];
  frameWidth?: number;
  frameHeight?: number;
  frameTs?: number;
  riskPoint?: RealtimeRiskPoint;
}
```

- [ ] **Step 2: 在 dashboardService.toCameraGridItem() 中传递 protocol**

在 `frontend/src/services/dashboardService.ts` 的 `toCameraGridItem` 函数的 return 对象中，在 `previewUrl` 之后添加：

```ts
protocol: item.protocol || "",
```

完整的 return 对象变为：
```ts
return {
  id: String(item.id ?? item.cameraCode ?? ""),
  cameraId: Number(item.id ?? 0),
  cameraCode: item.cameraCode || "",
  name: item.cameraName || item.cameraCode || "未命名摄像头",
  location: `${venueIdToName(item.venueId ?? null)}-区域${item.zoneId ?? "-"}`,
  peopleCount: 0,
  riskLevel,
  isAlarming: riskLevel === "danger",
  streamUrl: item.streamUrl || "",
  previewProtocol: previewTarget.protocol,
  previewUrl: previewTarget.url,
  protocol: item.protocol || "",
  detections: [],
  frameTs: undefined,
  riskPoint: undefined,
};
```

---

### Task 2: CameraGridCard 画面空间优化 + 点击事件

**Files:**
- Modify: `frontend/src/components/business/CameraGridCard.vue`

- [ ] **Step 1: 优化 camera-screen 高度和视频比例**

将 `.camera-screen` 的固定高度 `height: 220px` 改为基于宽高比自适应：

修改 `<style scoped>` 中的 `.camera-screen` 样式：
```css
.camera-screen {
  position: relative;
  aspect-ratio: 16 / 9;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(
    180deg,
    rgba(14, 24, 42, 0.3),
    rgba(0, 0, 0, 0.85)
  );
  overflow: hidden;
  cursor: pointer;
}
```

变更点：`height: 220px` → `aspect-ratio: 16 / 9`，新增 `cursor: pointer`。

- [ ] **Step 2: 压缩 camera-overlay 内边距**

修改 `.camera-overlay` 的 padding：
```css
.camera-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 10px 8px;
  background: linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.85) 100%);
}
```

变更点：`padding: 16px 12px 12px` → `padding: 12px 10px 8px`。

- [ ] **Step 3: 精简 camera-footer**

修改 `.camera-footer` 使其更紧凑：
```css
.camera-footer {
  border-top: 1px solid rgba(255, 255, 255, 0.18);
  padding: 6px 10px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
  text-align: center;
}
```

变更点：`padding: 8px 10px` → `padding: 6px 10px`，新增 `text-align: center`。

- [ ] **Step 4: 添加点击事件 emit**

在 `<script setup lang="ts">` 中添加 emit 定义：

在 `const props = defineProps<Props>();` 之后添加：
```ts
const emit = defineEmits<{
  (event: "camera-click", item: CameraGridItem): void;
}>();
```

- [ ] **Step 5: 绑定点击事件到 camera-screen**

在模板的 `<div ref="screenRef" class="camera-screen">` 上添加 `@click` 事件：
```html
<div ref="screenRef" class="camera-screen" @click="emit('camera-click', props.item)">
```

- [ ] **Step 6: 更新 camera-footer 文本**

将 `<div class="camera-footer">点击可查看报警详情</div>` 改为：
```html
<div class="camera-footer">点击查看详情</div>
```

---

### Task 3: 新建 CameraDetailModal 模态框组件

**Files:**
- Create: `frontend/src/components/business/CameraDetailModal.vue`

- [ ] **Step 1: 创建 CameraDetailModal.vue 完整代码**

创建文件 `frontend/src/components/business/CameraDetailModal.vue`，内容如下：

```vue
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
        <WebRtcWhepPlayer
          v-if="item && effectiveProtocol === 'webrtc' && effectiveStreamUrl"
          :src="effectiveStreamUrl"
          :muted="true"
          :active="visible"
          @playing="() => {}"
          @error="handleStreamError"
        />
        <img
          v-else-if="item && effectiveProtocol === 'mjpeg' && effectiveStreamUrl"
          :src="effectiveStreamUrl"
          class="camera-detail__stream-img"
          alt="视频预览"
        />
        <div v-else class="camera-detail__video-placeholder">
          视频流加载中
        </div>
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

      <template v-if="isPtzDevice">
        <el-divider content-position="left">云台控制</el-divider>
        <div class="ptz-panel">
          <div class="ptz-panel__direction">
            <div class="direction-row">
              <div class="direction-empty"></div>
              <el-button
                :loading="loadingDir === 'UP'"
                :disabled="!!loadingDir"
                class="direction-btn"
                @click="handleNudge('UP')"
              >
                上
              </el-button>
              <div class="direction-empty"></div>
            </div>
            <div class="direction-row">
              <el-button
                :loading="loadingDir === 'LEFT'"
                :disabled="!!loadingDir"
                class="direction-btn"
                @click="handleNudge('LEFT')"
              >
                左
              </el-button>
              <el-button
                :loading="loadingHome"
                :disabled="!!loadingDir || loadingHome"
                class="direction-btn direction-btn--home"
                @click="handleHome"
              >
                回中
              </el-button>
              <el-button
                :loading="loadingDir === 'RIGHT'"
                :disabled="!!loadingDir"
                class="direction-btn"
                @click="handleNudge('RIGHT')"
              >
                右
              </el-button>
            </div>
            <div class="direction-row">
              <div class="direction-empty"></div>
              <el-button
                :loading="loadingDir === 'DOWN'"
                :disabled="!!loadingDir"
                class="direction-btn"
                @click="handleNudge('DOWN')"
              >
                下
              </el-button>
              <div class="direction-empty"></div>
            </div>
          </div>
          <div class="ptz-panel__actions">
            <el-button
              :loading="loadingStatus"
              :disabled="!!loadingDir || loadingHome || loadingStatus"
              @click="handleStatus"
            >
              查询状态
            </el-button>
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
import { computed, ref } from "vue";
import { ElMessage } from "element-plus";
import StatusTag from "@/components/common/StatusTag.vue";
import WebRtcWhepPlayer from "@/components/business/WebRtcWhepPlayer.vue";
import { controlCameraPtz } from "@/api/cameraDeviceController";
import { unwrapApiData } from "@/services/serviceUtils";
import type { CameraGridItem } from "@/types/business";

interface Props {
  visible: boolean;
  item: CameraGridItem | null;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
}>();

const loadingDir = ref<"LEFT" | "RIGHT" | "UP" | "DOWN" | "">("");
const loadingHome = ref(false);
const loadingStatus = ref(false);
const statusText = ref("");
const streamError = ref("");

const effectiveProtocol = computed(() => props.item?.previewProtocol || "mjpeg");
const effectiveStreamUrl = computed(() => props.item?.previewUrl || props.item?.streamUrl || "");

const isPtzDevice = computed(() => props.item?.protocol?.toUpperCase() === "PTZ");

const riskMeta = computed(() => {
  if (!props.item) return { label: "正常", type: "success" as const };
  if (props.item.riskLevel === "danger") return { label: "危险", type: "danger" as const };
  if (props.item.riskLevel === "warning") return { label: "预警", type: "warning" as const };
  return { label: "正常", type: "success" as const };
});

const handleStreamError = (message: string) => {
  streamError.value = message;
};

const handleNudge = async (direction: "LEFT" | "RIGHT" | "UP" | "DOWN") => {
  if (!props.item) return;
  loadingDir.value = direction;
  try {
    const response = await controlCameraPtz({
      cameraId: props.item.cameraId,
      action: "NUDGE",
      direction,
      step: 5,
    });
    unwrapApiData(response, "控制失败");
    ElMessage.success(`已发送${direction === "UP" ? "上" : direction === "DOWN" ? "下" : direction === "LEFT" ? "左" : "右"}转指令`);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "控制失败");
  } finally {
    loadingDir.value = "";
  }
};

const handleHome = async () => {
  if (!props.item) return;
  loadingHome.value = true;
  try {
    const response = await controlCameraPtz({
      cameraId: props.item.cameraId,
      action: "HOME",
    });
    unwrapApiData(response, "回中失败");
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
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "状态查询失败");
  } finally {
    loadingStatus.value = false;
  }
};

const handleClose = () => {
  statusText.value = "";
  streamError.value = "";
};
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

.camera-detail__stream-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.camera-detail__video-placeholder {
  color: rgba(255, 255, 255, 0.65);
  font-size: 14px;
  letter-spacing: 1px;
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
  align-items: center;
  gap: 16px;
}

.ptz-panel__direction {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.direction-row {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.direction-empty {
  width: 56px;
}

.direction-btn {
  width: 56px;
}

.direction-btn--home {
  width: 56px;
}

.ptz-panel__actions {
  display: flex;
  gap: 12px;
  justify-content: center;
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
```

关键设计点：
- 视频区域 `aspect-ratio: 16/9` 黑底圆角
- 信息栏横向排列（位置、人数、风险等级）
- PTZ 控制面板仅在 `protocol === 'PTZ'` 时渲染
- 方向盘十字布局 + 回中按钮在中心
- 所有 PTZ 操作走 `controlCameraPtz` 后端代理
- 每个按钮独立 loading 状态，防止并发操作
- 关闭模态框时清空状态

---

### Task 4: AdminDashboardView 集成模态框

**Files:**
- Modify: `frontend/src/views/admin/dashboard/AdminDashboardView.vue`

- [ ] **Step 1: 导入 CameraDetailModal 组件**

在 `<script setup lang="ts">` 的 import 区域添加：
```ts
import CameraDetailModal from "@/components/business/CameraDetailModal.vue";
```

- [ ] **Step 2: 添加模态框状态变量**

在 `const layoutMode = ref<"2x2" | "3x3" | "4x3">("3x3");` 之后添加：
```ts
const detailModalVisible = ref(false);
const selectedCamera = ref<CameraGridItem | null>(null);
```

- [ ] **Step 3: 添加打开模态框的处理函数**

在 `const cameraSpan = computed(...)` 之后添加：
```ts
const handleCameraClick = (item: CameraGridItem) => {
  selectedCamera.value = item;
  detailModalVisible.value = true;
};
```

- [ ] **Step 4: 在 CameraGridCard 上绑定事件**

修改模板中的 `<CameraGridCard>` 标签，添加 `@camera-click` 事件：
```html
<CameraGridCard
  :ref="(el) => setCameraCardRef(item.cameraId, el)"
  :item="item"
  @camera-click="handleCameraClick"
/>
```

- [ ] **Step 5: 在模板末尾添加模态框组件**

在 `</div>` (`.admin-dashboard-view` 的闭合标签) 之前添加：
```html
<CameraDetailModal
  v-model:visible="detailModalVisible"
  :item="selectedCamera"
/>
```

---

### Task 5: 云台控制测试页面布局重构

**Files:**
- Modify: `frontend/src/views/admin/device/PtzControlTestView.vue`

- [ ] **Step 1: 重构模板为上视频下操作布局**

将整个 `<template>` 替换为：

```html
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
```

变更点：
- 视频预览从 `el-row/el-col` 左右分栏中的左侧移出，变为独立的全宽卡片
- 视频预览区域不再嵌套在 `el-col :md="16"` 中，而是占据整行
- 下方控制区域从 `el-col :md="8"` 的单列改为 `el-row` 包含两个 `el-col :md="12"` 并排：左侧方向控制+直接定位+状态，右侧校准操作
- script 部分完全不变，只改模板和样式

- [ ] **Step 2: 更新样式**

将 `<style scoped>` 部分替换为：

```css
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
```

变更点：
- `.video-wrap` 移除 `min-height: 360px`，改为黑底背景
- `.preview-image` 的 `max-height` 从 `540px` 降为 `480px`，适配上视频下操作布局
- `.video-placeholder` 添加 `padding: 60px 0` 增加垂直空间
- 移除不再需要的 `.calibration-card` 和 `.pad-grid` 样式

---

### Task 6: 构建验证

- [ ] **Step 1: 运行前端构建**

Run: `cd frontend && npm run build`

Expected: 构建成功，无 TypeScript 错误

- [ ] **Step 2: 运行类型检查**

Run: `cd frontend && npx vue-tsc --noEmit`

Expected: 无类型错误
