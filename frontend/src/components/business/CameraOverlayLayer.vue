<template>
  <div class="camera-overlay-layer">
    <div
      v-for="(detection, index) in visibleDetections"
      :key="`${detection.trackId}-${index}`"
      class="detection-box"
      :class="riskLevelClass(detection.riskLevel)"
      :style="detectionStyle(detection)"
    >
      <span class="detection-label">
        {{ detection.label }} {{ toPercent(detection.confidence) }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import type { RealtimeDetection } from "@/types/business";
import {
  computeOverlayStyle,
  computeContainOffset,
  filterVisibleDetections,
  riskLevelClass,
} from "@/utils/cameraOverlayUtils";

interface Props {
  detections: RealtimeDetection[];
  frameWidth: number;
  frameHeight: number;
  displayWidth: number;
  displayHeight: number;
  objectFit?: "contain" | "cover";
  maxAgeMs?: number;
}

const props = withDefaults(defineProps<Props>(), {
  objectFit: "contain",
  maxAgeMs: 2000,
});

const now = ref(Date.now());
let rafId: number | null = null;

const tick = () => {
  now.value = Date.now();
  rafId = requestAnimationFrame(tick);
};

onMounted(() => {
  rafId = requestAnimationFrame(tick);
});

onBeforeUnmount(() => {
  if (rafId != null) {
    cancelAnimationFrame(rafId);
    rafId = null;
  }
});

const visibleDetections = computed(() =>
  filterVisibleDetections(props.detections, props.maxAgeMs, now.value),
);

const containOffset = computed(() => {
  if (props.objectFit !== "contain") {
    return { offsetX: 0, offsetY: 0 };
  }
  return computeContainOffset(
    props.frameWidth,
    props.frameHeight,
    props.displayWidth,
    props.displayHeight,
  );
});

const renderedSize = computed(() => {
  const { offsetX, offsetY } = containOffset.value;
  return {
    width: props.displayWidth - 2 * offsetX,
    height: props.displayHeight - 2 * offsetY,
  };
});

const detectionStyle = (detection: RealtimeDetection) => {
  const box = detection.bboxNorm;
  if (!box) {
    return { left: "0px", top: "0px", width: "0px", height: "0px" };
  }

  if (props.objectFit === "contain") {
    const { offsetX, offsetY } = containOffset.value;
    const { width: rW, height: rH } = renderedSize.value;
    const style = computeOverlayStyle(box, rW, rH);
    const left = parseFloat(style.left) + offsetX;
    const top = parseFloat(style.top) + offsetY;
    return {
      left: `${left}px`,
      top: `${top}px`,
      width: style.width,
      height: style.height,
    };
  }

  if (props.objectFit === "cover" && props.frameWidth > 0 && props.frameHeight > 0) {
    const scaleW = props.displayWidth / props.frameWidth;
    const scaleH = props.displayHeight / props.frameHeight;
    const scale = Math.max(scaleW, scaleH);
    const renderedW = props.frameWidth * scale;
    const renderedH = props.frameHeight * scale;
    const cropX = (renderedW - props.displayWidth) / 2;
    const cropY = (renderedH - props.displayHeight) / 2;
    const left = box.xMin * renderedW - cropX;
    const top = box.yMin * renderedH - cropY;
    const width = (box.xMax - box.xMin) * renderedW;
    const height = (box.yMax - box.yMin) * renderedH;
    return {
      left: `${left}px`,
      top: `${top}px`,
      width: `${width}px`,
      height: `${height}px`,
    };
  }

  return computeOverlayStyle(box, props.displayWidth, props.displayHeight);
};

const toPercent = (value: number) => `${Math.round(value * 100)}%`;
</script>

<style scoped>
.camera-overlay-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 3;
  overflow: hidden;
}

.detection-box {
  position: absolute;
  box-sizing: border-box;
  border: 2px solid #1890ff;
  background: rgba(24, 144, 255, 0.08);
}

.detection-box.is-high {
  border: 3px solid #ff4d4f;
  background: rgba(255, 77, 79, 0.12);
}

.detection-box.is-medium {
  border: 2px solid #faad14;
  background: rgba(250, 173, 20, 0.1);
}

.detection-box.is-normal {
  border: 2px solid #1890ff;
  background: rgba(24, 144, 255, 0.08);
}

.detection-label {
  position: absolute;
  left: 2px;
  top: 2px;
  max-width: calc(100% - 4px);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 11px;
  line-height: 16px;
  background: rgba(0, 0, 0, 0.75);
  color: #fff;
}
</style>
