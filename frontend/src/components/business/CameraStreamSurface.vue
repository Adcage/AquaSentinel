<template>
  <WebRtcWhepPlayer
    v-if="protocol === 'webrtc' && streamUrl && !paused"
    ref="webrtcPlayerRef"
    :src="streamUrl"
    :muted="muted"
    :active="visible"
    @playing="handleStreamPlaying"
    @error="handleStreamError"
  />
  <img
    v-else-if="protocol === 'ws_jpeg' && videoFrameBlobUrl && !paused && visible"
    ref="frameImageRef"
    :src="videoFrameBlobUrl"
    class="camera-stream"
    alt="视频流"
    @load="handleStreamPlaying"
  />
  <img
    v-else-if="protocol === 'mjpeg' && streamUrl && !paused && visible"
    ref="streamImageRef"
    :src="streamUrl"
    class="camera-stream"
    alt="视频流"
    @load="handleStreamPlaying"
    @error="handleMjpegError"
  />
  <div v-else-if="streamError" class="camera-stream__placeholder is-error">
    {{ streamError }}
  </div>
  <div v-else-if="paused" class="camera-stream__placeholder">已暂停</div>
  <div v-else-if="!visible" class="camera-stream__placeholder">
    已自动暂停（不在可视区）
  </div>
  <div v-else class="camera-stream__placeholder">视频流占位</div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'

import WebRtcWhepPlayer from '@/components/business/WebRtcWhepPlayer.vue'
import type { CameraPreviewProtocol } from '@/utils/streamPreview'

interface Props {
  protocol: CameraPreviewProtocol
  streamUrl: string
  paused?: boolean
  muted?: boolean
  visible?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  paused: false,
  muted: false,
  visible: true,
})

const videoFrameBlobUrl = ref('')
const streamError = ref('')
const frameImageRef = ref<HTMLImageElement | null>(null)
const streamImageRef = ref<HTMLImageElement | null>(null)
const webrtcPlayerRef = ref<InstanceType<typeof WebRtcWhepPlayer> | null>(null)

const handleStreamPlaying = () => {
  streamError.value = ''
}

const handleStreamError = (message: string) => {
  streamError.value = message || '视频流加载失败'
}

const handleMjpegError = () => {
  streamError.value = '视频流加载失败'
}

watch(
  () => [props.protocol, props.streamUrl, props.paused, props.visible],
  () => {
    if (!props.paused && props.visible && props.streamUrl) {
      streamError.value = ''
    }
  },
)

const updateVideoFrame = (blob: Blob) => {
  if (props.protocol !== 'ws_jpeg' || props.paused) {
    return
  }
  if (videoFrameBlobUrl.value) {
    URL.revokeObjectURL(videoFrameBlobUrl.value)
  }
  videoFrameBlobUrl.value = URL.createObjectURL(blob)
}

defineExpose({
  updateVideoFrame,
  getRenderableElement: () => {
    if (props.protocol === 'webrtc') {
      return webrtcPlayerRef.value?.getVideoElement?.() ?? null
    }
    if (props.protocol === 'ws_jpeg') {
      return frameImageRef.value
    }
    if (props.protocol === 'mjpeg') {
      return streamImageRef.value
    }
    return null
  },
})

onBeforeUnmount(() => {
  if (videoFrameBlobUrl.value) {
    URL.revokeObjectURL(videoFrameBlobUrl.value)
    videoFrameBlobUrl.value = ''
  }
})
</script>

<style scoped>
.camera-stream {
  width: 100%;
  height: 100%;
  object-fit: cover;
  position: absolute;
  top: 0;
  left: 0;
}

.camera-stream__placeholder {
  color: rgba(255, 255, 255, 0.65);
  font-size: 12px;
  letter-spacing: 1px;
}

.camera-stream__placeholder.is-error {
  color: #ff7875;
}
</style>
