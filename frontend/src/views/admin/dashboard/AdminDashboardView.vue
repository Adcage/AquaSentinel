<template>
  <div class="admin-dashboard-view admin-page">
    <div class="page-header admin-page-header">
      <h1>监控总览</h1>
      <p>展示设备状态、报警态势与重点监控画面</p>
    </div>

    <el-alert
      v-if="engineUnavailable"
      class="engine-alert"
      title="AI引擎当前不可用，已暂停实时识别"
      :description="engineUnavailableMessage"
      type="warning"
      :closable="false"
      show-icon
    />

    <div class="metric-grid">
      <div class="metric-grid__item">
        <MetricCard
          title="在线设备数"
          :value="metrics.onlineDeviceCount"
          :icon="VideoCamera"
          :footer="onlineDeviceFooter"
        />
      </div>
      <div class="metric-grid__item">
        <MetricCard
          title="今日报警数"
          :value="metrics.todayAlarmCount"
          :icon="Warning"
          :footer="todayAlarmFooter"
        />
      </div>
      <div class="metric-grid__item">
        <MetricCard
          title="未处理报警"
          :value="metrics.pendingAlarmCount"
          :danger="metrics.pendingAlarmCount > 0"
          :icon="Bell"
          :footer="pendingAlarmFooter"
        />
      </div>
      <div class="metric-grid__item">
        <MetricCard
          title="在岗救生员"
          :value="metrics.onDutyLifeguardCount"
          :icon="User"
          :footer="lifeguardFooter"
        />
      </div>
      <div class="metric-grid__item">
        <MetricCard
          title="泳池实时总人数"
          :value="displayRealtimeSwimmerCount"
          :icon="Histogram"
          :footer="swimmerFooter"
        />
      </div>
    </div>

    <el-card shadow="never" class="camera-section admin-table-card">
      <template #header>
        <div class="card-header">
          <span>摄像头监控网格</span>
          <div class="card-header__right">
            <!-- <div class="realtime-source" :class="realtimeSourceClass">
              <span class="realtime-source__label">实时来源</span>
              <span class="realtime-source__value">{{
                realtimeSourceText
              }}</span>
              <span
                v-if="realtimeSourceReasonText"
                class="realtime-source__reason"
              >
                {{ realtimeSourceReasonText }}
              </span>
            </div> -->
            <el-radio-group v-model="layoutMode" size="small">
              <el-radio-button value="2x2">2x2</el-radio-button>
              <el-radio-button value="3x3">3x3</el-radio-button>
              <el-radio-button value="4x3">4x3</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>

      <el-row :gutter="12">
        <el-col
          v-for="item in cameraGrid"
          :id="`camera-card-${item.cameraId}`"
          :key="item.id"
          :span="cameraSpan"
        >
          <CameraGridCard
            :ref="(el) => setCameraCardRef(item.cameraId, el)"
            :item="item"
            @camera-click="handleCameraClick"
          />
        </el-col>
      </el-row>

      <div class="camera-pagination">
        <el-pagination
          v-model:current-page="cameraPageCurrent"
          v-model:page-size="cameraPageSize"
          :page-sizes="[9, 12, 18, 24]"
          :total="cameraTotal"
          background
          layout="total, sizes, prev, pager, next"
          @current-change="handleCameraPageChange"
          @size-change="handleCameraSizeChange"
        />
      </div>
    </el-card>

    <CameraDetailModal
      ref="detailModalRef"
      v-model:visible="detailModalVisible"
      :item="selectedCamera"
      :source-element="selectedPreviewElement"
    />
  </div>
</template>

<script setup lang="ts">
import {
  Bell,
  Histogram,
  User,
  VideoCamera,
  Warning,
} from "@element-plus/icons-vue";
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import CameraGridCard from "@/components/business/CameraGridCard.vue";
import CameraDetailModal from "@/components/business/CameraDetailModal.vue";
import MetricCard from "@/components/business/MetricCard.vue";
import {
  checkEngineAvailability,
  enrichCameraGridRealtimeBatch,
  fetchCameraGridBase,
  getDashboardMetrics,
  mergeRealtimeBatchIntoGrid,
} from "@/services/dashboardService";
import { alertWsService } from "@/services/alertWsService";
import type { WsVideoFramePayload } from "@/services/alertWsService";
import type { CameraGridItem, DashboardMetrics } from "@/types/business";

type RealtimeSourceMode = "connecting" | "ws" | "recovering";

type CameraCardInstance = InstanceType<typeof CameraGridCard> & {
  updateVideoFrame?: (blob: Blob) => void;
  cameraId?: number;
  getPreviewElement?: () => HTMLImageElement | HTMLVideoElement | null;
};

type CameraDetailModalInstance = InstanceType<typeof CameraDetailModal> & {
  updateVideoFrame?: (blob: Blob) => void;
  cameraId?: number;
};

const WS_WATCH_INTERVAL_MS = 500;
const METRICS_REFRESH_INTERVAL_MS = Math.max(
  3_000,
  Number(import.meta.env.VITE_DASHBOARD_METRICS_REFRESH_INTERVAL_MS ?? 10_000),
);
const VIDEO_RENDER_MIN_INTERVAL_MS = Math.max(
  30,
  Number(import.meta.env.VITE_MONITOR_RENDER_MIN_INTERVAL_MS ?? 120),
);

const metrics = ref<DashboardMetrics>({
  onlineDeviceCount: 0,
  todayAlarmCount: 0,
  pendingAlarmCount: 0,
  onDutyLifeguardCount: 0,
  realtimeSwimmerCount: 0,
});

const cameraGrid = ref<CameraGridItem[]>([]);
const cameraPageCurrent = ref(1);
const cameraPageSize = ref(9);
const cameraTotal = ref(0);
const layoutMode = ref<"2x2" | "3x3" | "4x3">("3x3");
const detailModalVisible = ref(false);
const selectedCameraId = ref(0);
const selectedPreviewElement = ref<HTMLImageElement | HTMLVideoElement | null>(null);
let wsWatchdogTimer: ReturnType<typeof setInterval> | null = null;
let metricsRefreshTimer: ReturnType<typeof setInterval> | null = null;
let metricsRefreshing = false;
const engineUnavailable = ref(false);
const engineUnavailableMessage = ref("AI引擎不可用，请启动Python服务");

const realtimeSourceMode = ref<RealtimeSourceMode>("connecting");
const realtimeSourceReason = ref("等待实时数据");
const wsConnected = ref(false);
const wsLastBatchAt = ref(0);
const isDev = import.meta.env.DEV;
const isTest = import.meta.env.MODE === "test";
const enableDevLogs = isDev && !isTest;

const cameraCardRefs = ref<Map<number, CameraCardInstance>>(new Map());
const cameraLastRenderAt = ref<Map<number, number>>(new Map());
const detailModalRef = ref<CameraDetailModalInstance | null>(null);
const route = useRoute();

const selectedCamera = computed(() =>
  cameraGrid.value.find((item) => item.cameraId === selectedCameraId.value) ?? null,
);

const onlineDeviceFooter = computed(() => {
  const diff = metrics.value.onlineDeviceDiff ?? 0;
  if (diff === 0) return "较昨日持平";
  return `较昨日 ${diff > 0 ? "+" : ""}${diff}`;
});

const todayAlarmFooter = computed(() => {
  const diff = metrics.value.todayAlarmDiff ?? 0;
  if (diff === 0) return "较昨日持平";
  return `较昨日 ${diff > 0 ? "+" : ""}${diff}`;
});

const pendingAlarmFooter = computed(() => {
  if (metrics.value.pendingAlarmCount > 0) return "需优先响应";
  return "暂无待处理报警";
});

const lifeguardFooter = computed(() => {
  if (metrics.value.onDutyLifeguardCount > 0) return "全员状态正常";
  return "暂无救生员在岗";
});

/** 当前页摄像头网格上识别人数之和（与画面角标一致） */
const gridRealtimeSwimmerSum = computed(() =>
  cameraGrid.value.reduce((sum, item) => {
    const n = item.peopleCount;
    return sum + (Number.isFinite(n) ? n : 0);
  }, 0),
);

/**
 * 概览接口人数与网格识别人数取较大值：接口未汇总引擎结果时常为 0，此时用网格合计对齐画面。
 * 若后端已返回全馆总数且大于当前页合计，仍以接口为准（多页场景）。
 */
const displayRealtimeSwimmerCount = computed(() =>
  Math.max(metrics.value.realtimeSwimmerCount, gridRealtimeSwimmerSum.value),
);

const swimmerFooter = computed(() => {
  return "实时数据监控中";
});

const setCameraCardRef = (cameraId: number, el: unknown) => {
  const card = el as CameraCardInstance | null;
  if (card) {
    if (!cameraCardRefs.value.has(cameraId)) {
      cameraCardRefs.value.set(cameraId, card);
    }
  } else {
    cameraCardRefs.value.delete(cameraId);
    cameraLastRenderAt.value.delete(cameraId);
  }
};

const mergeFrameDetections = (
  cameraId: number,
  frameHeader: WsVideoFramePayload,
) => {
  if (!frameHeader.detections) {
    return;
  }
  const idx = cameraGrid.value.findIndex((item) => item.cameraId === cameraId);
  if (idx < 0) {
    return;
  }
  const fakeBatch: Record<string, unknown> = {
    [String(cameraId)]: {
      engine: {
        available: true,
        realtime: {
          detections: frameHeader.detections,
          head_count: frameHeader.headCount ?? frameHeader.detections.length,
          risk_point: frameHeader.riskPoint,
        },
      },
    },
  };
  const updated = mergeRealtimeBatchIntoGrid(
    [cameraGrid.value[idx]],
    fakeBatch,
  );
  if (updated.length > 0) {
    cameraGrid.value[idx] = updated[0];
  }
};

const handleVideoFrame = (
  cameraId: number,
  blob: Blob,
  frameHeader?: WsVideoFramePayload,
) => {
  const now = Date.now();
  const lastRenderAt = cameraLastRenderAt.value.get(cameraId) || 0;
  if (now - lastRenderAt < VIDEO_RENDER_MIN_INTERVAL_MS) {
    return;
  }
  cameraLastRenderAt.value.set(cameraId, now);
  const card = cameraCardRefs.value.get(cameraId);
  if (card?.updateVideoFrame) {
    card.updateVideoFrame(blob);
  }
  if (
    detailModalVisible.value &&
    detailModalRef.value?.cameraId === cameraId &&
    detailModalRef.value.updateVideoFrame
  ) {
    detailModalRef.value.updateVideoFrame(blob);
  }
  if (frameHeader) {
    mergeFrameDetections(cameraId, frameHeader);
  }
  wsLastBatchAt.value = Date.now();
  switchToWsMode("收到视频帧");
};

const handleVideoFrameEvent = (event: Event) => {
  const customEvent = event as CustomEvent<{
    cameraId?: number;
    blob?: Blob;
    frameHeader?: WsVideoFramePayload;
  }>;
  const cameraId = Number(customEvent.detail?.cameraId ?? 0);
  const blob = customEvent.detail?.blob;
  if (!cameraId || cameraId <= 0 || !(blob instanceof Blob)) {
    return;
  }
  handleVideoFrame(cameraId, blob, customEvent.detail?.frameHeader);
};

const setRealtimeSource = (mode: RealtimeSourceMode, reason: string) => {
  const from = realtimeSourceMode.value;
  if (from === mode && realtimeSourceReason.value === reason) {
    return;
  }
  realtimeSourceMode.value = mode;
  realtimeSourceReason.value = reason;
  if (enableDevLogs) {
    console.info("[monitor-realtime-channel]", {
      from,
      to: mode,
      reason,
      switchedAt: new Date().toISOString(),
    });
  }
};

const refreshCameraGridBase = async () => {
  const pageResult = await fetchCameraGridBase({
    current: cameraPageCurrent.value,
    pageSize: cameraPageSize.value,
  });
  cameraGrid.value = pageResult.records;
  cameraTotal.value = pageResult.total;
  cameraPageCurrent.value = pageResult.current;
  cameraPageSize.value = pageResult.pageSize;
};

const handleCameraPageChange = async (nextPage: number) => {
  if (!Number.isFinite(nextPage) || nextPage <= 0) {
    return;
  }
  if (nextPage === cameraPageCurrent.value) {
    return;
  }
  unsubscribeRealtimeBatch();
  cameraPageCurrent.value = Math.floor(nextPage);
  await refreshCameraGridBase();
  if (engineUnavailable.value) {
    return;
  }
  subscribeRealtimeBatch();
  await refreshRealtimeByPolling();
  wsLastBatchAt.value = Date.now();
  if (wsConnected.value) {
    setRealtimeSource("recovering", "分页切换后等待WS首帧确认");
  }
};

const handleCameraSizeChange = async (newSize: number) => {
  if (!Number.isFinite(newSize) || newSize <= 0) {
    return;
  }
  cameraPageSize.value = Math.floor(newSize);
  cameraPageCurrent.value = 1;
  unsubscribeRealtimeBatch();
  await refreshCameraGridBase();
  if (engineUnavailable.value) {
    return;
  }
  subscribeRealtimeBatch();
  await refreshRealtimeByPolling();
  wsLastBatchAt.value = Date.now();
  if (wsConnected.value) {
    setRealtimeSource("recovering", "每页数量切换后等待WS首帧确认");
  }
};

const refreshRealtimeByPolling = async () => {
  if (!cameraGrid.value.length || engineUnavailable.value) {
    return;
  }
  cameraGrid.value = await enrichCameraGridRealtimeBatch(cameraGrid.value);
};

const refreshMetrics = async () => {
  if (metricsRefreshing) {
    return;
  }
  metricsRefreshing = true;
  try {
    metrics.value = await getDashboardMetrics();
  } finally {
    metricsRefreshing = false;
  }
};

const startMetricsAutoRefresh = () => {
  if (metricsRefreshTimer) {
    return;
  }
  metricsRefreshTimer = setInterval(() => {
    void refreshMetrics();
  }, METRICS_REFRESH_INTERVAL_MS);
};

const switchToWsMode = (reason: string) => {
  setRealtimeSource("ws", reason);
};

const getCurrentCameraIds = (): number[] =>
  cameraGrid.value
    .map((item) => item.cameraId)
    .filter((cameraId) => Number.isFinite(cameraId) && cameraId > 0);

const subscribeRealtimeBatch = () => {
  const cameraIds = getCurrentCameraIds();
  if (!cameraIds.length) {
    return;
  }
  alertWsService.send({
    action: "SUBSCRIBE_MONITOR_REALTIME",
    cameraIds,
  });
};

const unsubscribeRealtimeBatch = () => {
  const cameraIds = getCurrentCameraIds();
  alertWsService.send({
    action: "UNSUBSCRIBE_MONITOR_REALTIME",
    cameraIds,
  });
};

const ensureWsWatchdog = () => {
  if (isTest) {
    return;
  }
  if (wsWatchdogTimer) {
    return;
  }
  wsWatchdogTimer = setInterval(() => {
    if (engineUnavailable.value || !cameraGrid.value.length) {
      return;
    }
    if (!wsConnected.value) {
      if (realtimeSourceMode.value !== "connecting") {
        setRealtimeSource("connecting", "WS连接断开，等待重连");
      }
      return;
    }
    if (realtimeSourceMode.value === "connecting") {
      setRealtimeSource("recovering", "WS已连接，等待首帧");
    }
  }, WS_WATCH_INTERVAL_MS);
};

const focusCameraCard = (cameraId: number) => {
  if (!cameraId || cameraId <= 0) {
    return;
  }
  const target = document.getElementById(`camera-card-${cameraId}`);
  if (!target) {
    return;
  }
  target.scrollIntoView({ behavior: "smooth", block: "center" });
};

const requestCameraFullscreen = async (cameraId: number) => {
  if (!cameraId || cameraId <= 0) {
    return;
  }
  await nextTick();
  const target = document.getElementById(`camera-card-${cameraId}`);
  if (!target) {
    return;
  }
  const screen = target.querySelector(".camera-screen") as HTMLElement | null;
  if (!screen) {
    return;
  }
  if (document.fullscreenElement === screen) {
    return;
  }
  if (!document.fullscreenElement) {
    await screen.requestFullscreen().catch(() => undefined);
  }
};

const handleRouteAlertFocus = async () => {
  const query = route.query ?? {};
  const cameraId = Number(query.focusCameraId ?? 0);
  if (!cameraId || cameraId <= 0) {
    return;
  }
  focusCameraCard(cameraId);
  const fullscreenCameraId = Number(query.fullscreenCameraId ?? 0);
  if (fullscreenCameraId > 0) {
    await requestCameraFullscreen(fullscreenCameraId);
  }
};

const handleCameraFocusEvent = (event: Event) => {
  const customEvent = event as CustomEvent<{ cameraId?: number }>;
  const cameraId = Number(customEvent.detail?.cameraId ?? 0);
  focusCameraCard(cameraId);
};

const handleWsBatchEvent = (event: Event) => {
  if (engineUnavailable.value || !cameraGrid.value.length) {
    return;
  }
  const customEvent = event as CustomEvent<Record<string, unknown>>;
  const payload = customEvent.detail || {};
  cameraGrid.value = mergeRealtimeBatchIntoGrid(cameraGrid.value, payload);
  wsLastBatchAt.value = Date.now();
  switchToWsMode("收到WS实时批次");
};

const handleWsStatusEvent = (event: Event) => {
  const customEvent = event as CustomEvent<{ status?: string }>;
  const status = String(customEvent.detail?.status || "disconnected");
  wsConnected.value = status === "connected";
  if (wsConnected.value) {
    subscribeRealtimeBatch();
    if (realtimeSourceMode.value !== "ws") {
      setRealtimeSource("recovering", "WS已恢复，等待首帧确认");
    }
    return;
  }
  setRealtimeSource("connecting", "WS连接断开，等待重连");
};

const cameraSpan = computed(() => {
  if (layoutMode.value === "2x2") return 12;
  if (layoutMode.value === "4x3") return 6;
  return 8;
});

const handleCameraClick = (item: CameraGridItem) => {
  selectedCameraId.value = item.cameraId;
  selectedPreviewElement.value = cameraCardRefs.value.get(item.cameraId)?.getPreviewElement?.() ?? null;
  detailModalVisible.value = true;
};

onMounted(async () => {
  window.addEventListener(
    "drowning-alert-camera-focus",
    handleCameraFocusEvent,
  );
  window.addEventListener("monitor-realtime-ws-batch", handleWsBatchEvent);
  window.addEventListener("alert-ws-status-changed", handleWsStatusEvent);
  window.addEventListener("monitor-video-frame", handleVideoFrameEvent);

  await refreshMetrics();
  startMetricsAutoRefresh();
  const health = await checkEngineAvailability();
  engineUnavailable.value = !health.available;
  engineUnavailableMessage.value = health.message;
  wsConnected.value = alertWsService.getStatus() === "connected";

  await refreshCameraGridBase();

  if (engineUnavailable.value) {
    setRealtimeSource("connecting", "AI引擎不可用");
    return;
  }

  if (wsConnected.value) {
    setRealtimeSource("recovering", "检测到已连接WS，等待首帧确认");
  } else {
    setRealtimeSource("connecting", "优先建立WS实时通道");
  }
  subscribeRealtimeBatch();
  await refreshRealtimeByPolling();
  ensureWsWatchdog();
  await handleRouteAlertFocus();
});

watch(
  () => {
    const query = route.query ?? {};
    return [query.focusCameraId, query.fullscreenCameraId];
  },
  () => {
    void handleRouteAlertFocus();
  },
);

onUnmounted(() => {
  window.removeEventListener(
    "drowning-alert-camera-focus",
    handleCameraFocusEvent,
  );
  window.removeEventListener("monitor-realtime-ws-batch", handleWsBatchEvent);
  window.removeEventListener("alert-ws-status-changed", handleWsStatusEvent);
  window.removeEventListener("monitor-video-frame", handleVideoFrameEvent);
  unsubscribeRealtimeBatch();
  if (wsWatchdogTimer) {
    clearInterval(wsWatchdogTimer);
    wsWatchdogTimer = null;
  }
  if (metricsRefreshTimer) {
    clearInterval(metricsRefreshTimer);
    metricsRefreshTimer = null;
  }
});
</script>

<style scoped>
.admin-dashboard-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.engine-alert {
  margin-top: -4px;
}

.page-header h1 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 20px;
  line-height: 28px;
}

.page-header p {
  margin: 8px 0 0;
  color: var(--color-text-secondary);
}

.camera-section {
  border: 1px solid var(--color-border);
}

.camera-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.metric-grid__item {
  min-width: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-header__right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.realtime-source {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  border-radius: 16px;
  border: 1px solid var(--color-border);
  padding: 0 10px;
  font-size: 12px;
  line-height: 20px;
  background: #fff;
}

.realtime-source__label {
  color: var(--color-text-tertiary);
}

.realtime-source__value {
  font-weight: 600;
}

.realtime-source__reason {
  color: var(--color-text-secondary);
  max-width: 180px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.realtime-source.is-ws {
  border-color: #95de64;
  background: #f6ffed;
  color: #389e0d;
}

.realtime-source.is-polling {
  border-color: #ffd666;
  background: #fffbe6;
  color: #ad6800;
}

.realtime-source.is-recovering,
.realtime-source.is-connecting {
  border-color: #91caff;
  background: #e6f4ff;
  color: #0958d9;
}

@media (max-width: 1440px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 900px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .card-header__right {
    flex-wrap: wrap;
  }

  .realtime-source {
    width: 100%;
    justify-content: flex-start;
  }

  .realtime-source__reason {
    max-width: 100%;
  }
}
</style>
