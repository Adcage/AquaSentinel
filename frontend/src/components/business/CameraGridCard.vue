<template>
  <el-card
    shadow="never"
    class="camera-grid-card"
    :class="{ 'is-alarming': item.isAlarming }"
  >
    <div v-if="item.isAlarming" class="camera-banner">报警横幅 · 溺水预警</div>
    <div ref="screenRef" class="camera-screen">
      <WebRtcWhepPlayer
        v-if="effectiveProtocol === 'webrtc' && effectiveStreamUrl && !paused"
        :src="effectiveStreamUrl"
        :muted="muted"
        :active="isVisible"
        @playing="handleStreamPlaying"
        @error="handleStreamError"
      />
      <img
        v-else-if="
          effectiveProtocol === 'ws_jpeg' &&
          videoFrameBlobUrl &&
          !paused &&
          isVisible
        "
        :src="videoFrameBlobUrl"
        class="camera-stream"
        alt="视频流"
        @load="handleStreamPlaying"
      />
      <img
        v-else-if="
          effectiveProtocol === 'mjpeg' &&
          effectiveStreamUrl &&
          !paused &&
          isVisible
        "
        :src="effectiveStreamUrl"
        class="camera-stream"
        alt="视频流"
        @load="handleStreamPlaying"
        @error="handleMjpegError"
      />
      <div v-else-if="streamError" class="camera-screen__placeholder is-error">
        {{ streamError }}
      </div>
      <div v-else-if="paused" class="camera-screen__placeholder">已暂停</div>
      <div v-else-if="!isVisible" class="camera-screen__placeholder">
        已自动暂停（不在可视区）
      </div>
      <div v-else class="camera-screen__placeholder">视频流占位</div>
      <CameraOverlayLayer
        v-if="item.detections.length > 0 && !paused && isVisible"
        :detections="item.detections"
        :frame-width="item.frameWidth ?? 0"
        :frame-height="item.frameHeight ?? 0"
        :display-width="videoDisplayWidth"
        :display-height="videoDisplayHeight"
        object-fit="cover"
        :max-age-ms="2000"
      />
      <div class="camera-controls">
        <el-button size="small" text @click="handleFullscreen">{{
          isFullscreen ? "退出全屏" : "全屏"
        }}</el-button>
        <el-button size="small" text @click="handlePause">{{
          paused ? "继续" : "暂停"
        }}</el-button>
        <el-button size="small" text @click="handleMute">{{
          muted ? "取消静音" : "静音"
        }}</el-button>
      </div>
      <div class="camera-overlay">
        <div>
          <div class="camera-name">{{ item.name }}</div>
          <div class="camera-location">{{ item.location }}</div>
        </div>
        <div class="camera-meta-right">
          <span>{{ item.peopleCount }} 人</span>
          <StatusTag
            :label="riskMeta.label"
            :type="riskMeta.type"
            :emphasized="item.isAlarming"
          />
        </div>
      </div>
    </div>
    <div class="camera-footer">点击可查看报警详情</div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import StatusTag from "@/components/common/StatusTag.vue";
import WebRtcWhepPlayer from "@/components/business/WebRtcWhepPlayer.vue";
import CameraOverlayLayer from "./CameraOverlayLayer.vue";
import type { CameraGridItem, RealtimeDetection } from "@/types/business";

interface Props {
  item: CameraGridItem;
}

const props = defineProps<Props>();

const screenRef = ref<HTMLElement | null>(null);
const paused = ref(false);
const muted = ref(false);
const isFullscreen = ref(false);
const isVisible = ref(true);
const streamError = ref("");
const videoFrameBlobUrl = ref("");
const videoDisplayWidth = ref(0);
const videoDisplayHeight = ref(0);
let visibilityObserver: IntersectionObserver | null = null;
let resizeObserver: ResizeObserver | null = null;

const effectiveProtocol = computed(() => props.item.previewProtocol || "mjpeg");

const effectiveStreamUrl = computed(
  () => props.item.previewUrl || props.item.streamUrl || "",
);

const hasConfirmedDrowning = computed(() =>
  props.item.detections.some((d) => isConfirmedDrowning(d)),
);

const hasWarningDrowning = computed(() =>
  props.item.detections.some((d) => isWarningDrowning(d)),
);

const riskMeta = computed(() => {
  if (hasConfirmedDrowning.value) {
    return { label: "危险", type: "danger" as const };
  }
  if (hasWarningDrowning.value) {
    return { label: "预警", type: "warning" as const };
  }
  return { label: "正常", type: "success" as const };
});

const normalizeLabel = (value: string) => value.trim().toLowerCase();

const isDrowningLabel = (value: string) => {
  const normalized = normalizeLabel(value);
  return (
    normalized === "drowning" ||
    normalized === "drown" ||
    normalized.includes("drown") ||
    normalized.includes("溺")
  );
};

const isConfirmedDrowning = (detection: RealtimeDetection): boolean => {
  return detection.triggered === true || detection.riskLevel?.toUpperCase() === "HIGH";
};

const isWarningDrowning = (detection: RealtimeDetection): boolean => {
  const level = detection.riskLevel?.toUpperCase();
  return level === "MEDIUM" || (isDrowningLabel(detection.label) && !isConfirmedDrowning(detection));
};

const handleFullscreen = () => {
  const el = screenRef.value;
  if (!el) return;
  if (!document.fullscreenElement) {
    el.requestFullscreen()
      .then(() => {
        isFullscreen.value = true;
      })
      .catch(() => {});
  } else {
    document
      .exitFullscreen()
      .then(() => {
        isFullscreen.value = false;
      })
      .catch(() => {});
  }
};

const handlePause = () => {
  paused.value = !paused.value;
};

const handleMute = () => {
  muted.value = !muted.value;
};

const handleStreamPlaying = () => {
  streamError.value = "";
};

const handleStreamError = (message: string) => {
  streamError.value = message || "视频流加载失败";
};

const handleMjpegError = () => {
  streamError.value = "视频流加载失败";
};

watch(
  () => [
    effectiveProtocol.value,
    effectiveStreamUrl.value,
    paused.value,
    isVisible.value,
  ],
  () => {
    if (!paused.value && isVisible.value && effectiveStreamUrl.value) {
      streamError.value = "";
    }
  },
);

onMounted(() => {
  if (!screenRef.value || typeof IntersectionObserver === "undefined") {
    return;
  }
  visibilityObserver = new IntersectionObserver(
    (entries) => {
      const visible = entries.some((entry) => entry.isIntersecting);
      isVisible.value = visible;
    },
    {
      threshold: 0.1,
    },
  );
  visibilityObserver.observe(screenRef.value);

  if (typeof ResizeObserver !== "undefined") {
    resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect;
        videoDisplayWidth.value = Math.round(width);
        videoDisplayHeight.value = Math.round(height);
      }
    });
    resizeObserver.observe(screenRef.value);
  }
});

onBeforeUnmount(() => {
  if (visibilityObserver) {
    visibilityObserver.disconnect();
    visibilityObserver = null;
  }
  if (resizeObserver) {
    resizeObserver.disconnect();
    resizeObserver = null;
  }
  if (videoFrameBlobUrl.value) {
    URL.revokeObjectURL(videoFrameBlobUrl.value);
    videoFrameBlobUrl.value = "";
  }
});

const updateVideoFrame = (blob: Blob) => {
  if (effectiveProtocol.value !== "ws_jpeg") {
    return;
  }
  if (paused.value) {
    return;
  }
  if (videoFrameBlobUrl.value) {
    URL.revokeObjectURL(videoFrameBlobUrl.value);
  }
  videoFrameBlobUrl.value = URL.createObjectURL(blob);
};

defineExpose({
  updateVideoFrame,
  cameraId: computed(() => props.item.cameraId),
});
</script>

<style scoped>
.camera-grid-card {
  border: 1px solid #333;
  background: #000;
  color: #fff;
  border-radius: var(--radius-sm);
  overflow: hidden;
  position: relative;
}

.camera-grid-card.is-alarming {
  border: 2px solid var(--color-danger);
  animation: alarm-flash 1s infinite;
}

.camera-banner {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 2;
  padding: 4px 10px;
  border-radius: 4px;
  background: rgba(245, 34, 45, 0.9);
  font-size: 12px;
  font-weight: 600;
}

.camera-screen {
  position: relative;
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(
    180deg,
    rgba(14, 24, 42, 0.3),
    rgba(0, 0, 0, 0.85)
  );
  overflow: hidden;
}

.camera-stream {
  width: 100%;
  height: 100%;
  object-fit: cover;
  position: absolute;
  top: 0;
  left: 0;
}

.camera-screen__placeholder {
  color: rgba(255, 255, 255, 0.65);
  font-size: 12px;
  letter-spacing: 1px;
}

.camera-screen__placeholder.is-error {
  color: #ff7875;
}

.camera-controls {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  gap: 4px;
}

.camera-controls :deep(.el-button) {
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
  border-radius: 4px;
}

.camera-grid-card :deep(.el-card__body) {
  padding: 0;
  overflow: hidden;
}

.camera-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 12px 12px;
  background: linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.85) 100%);
}

.camera-name {
  font-size: 12px;
  margin-bottom: 4px;
}

.camera-location {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.camera-meta-right {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.camera-footer {
  border-top: 1px solid rgba(255, 255, 255, 0.18);
  padding: 8px 10px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

@keyframes alarm-flash {
  0%,
  100% {
    border-color: var(--color-danger);
  }

  50% {
    border-color: transparent;
  }
}
</style>
