<template>
  <video
    ref="videoRef"
    class="webrtc-player"
    :muted="muted"
    autoplay
    playsinline
  />
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue";

interface Props {
  src: string;
  muted?: boolean;
  active?: boolean;
}

const emit = defineEmits<{
  (event: "playing"): void;
  (event: "error", message: string): void;
}>();

const props = withDefaults(defineProps<Props>(), {
  muted: true,
  active: true,
});

const videoRef = ref<HTMLVideoElement | null>(null);
let peerConnection: RTCPeerConnection | null = null;
let sessionUrl = "";
let startSeq = 0;

const closeConnection = async () => {
  if (peerConnection) {
    peerConnection.ontrack = null;
    peerConnection.close();
    peerConnection = null;
  }
  if (sessionUrl) {
    const deletingUrl = sessionUrl;
    sessionUrl = "";
    await fetch(deletingUrl, {
      method: "DELETE",
    }).catch(() => undefined);
  }
  if (videoRef.value) {
    videoRef.value.srcObject = null;
  }
};

const waitIceGatheringDone = async (pc: RTCPeerConnection): Promise<void> => {
  if (pc.iceGatheringState === "complete") {
    return;
  }
  await new Promise<void>((resolve) => {
    const onStateChange = () => {
      if (pc.iceGatheringState === "complete") {
        pc.removeEventListener("icegatheringstatechange", onStateChange);
        resolve();
      }
    };
    pc.addEventListener("icegatheringstatechange", onStateChange);
    setTimeout(() => {
      pc.removeEventListener("icegatheringstatechange", onStateChange);
      resolve();
    }, 1800);
  });
};

const startConnection = async () => {
  const currentSeq = ++startSeq;
  const src = props.src.trim();
  if (!src || !props.active) {
    await closeConnection();
    return;
  }

  await closeConnection();

  const pc = new RTCPeerConnection({
    iceServers: [],
  });
  peerConnection = pc;

  pc.addTransceiver("video", { direction: "recvonly" });
  pc.ontrack = (event) => {
    const [stream] = event.streams;
    if (!videoRef.value || !stream) {
      return;
    }
    videoRef.value.srcObject = stream;
  };

  const offer = await pc.createOffer({
    offerToReceiveVideo: true,
    offerToReceiveAudio: false,
  });
  await pc.setLocalDescription(offer);
  await waitIceGatheringDone(pc);

  if (!pc.localDescription?.sdp) {
    throw new Error("WebRTC offer 为空");
  }

  const response = await fetch(src, {
    method: "POST",
    headers: {
      "Content-Type": "application/sdp",
      Accept: "application/sdp",
    },
    body: pc.localDescription.sdp,
  });

  if (!response.ok) {
    throw new Error(`WHEP 建连失败: ${response.status}`);
  }

  const answerSdp = await response.text();
  const locationHeader = response.headers.get("location") || "";
  if (locationHeader) {
    sessionUrl = new URL(locationHeader, src).toString();
  }

  if (startSeq !== currentSeq) {
    return;
  }

  await pc.setRemoteDescription({
    type: "answer",
    sdp: answerSdp,
  });
  emit("playing");
};

const startConnectionSafely = async () => {
  try {
    await startConnection();
  } catch (error) {
    const message = error instanceof Error ? error.message : "WebRTC 播放失败";
    emit("error", message);
  }
};

watch(
  () => [props.src, props.active],
  () => {
    void startConnectionSafely();
  },
);

watch(
  () => props.muted,
  (value) => {
    if (videoRef.value) {
      videoRef.value.muted = value;
    }
  },
);

onMounted(() => {
  void startConnectionSafely();
});

onBeforeUnmount(() => {
  void closeConnection();
});
</script>

<style scoped>
.webrtc-player {
  width: 100%;
  height: 100%;
  object-fit: cover;
  position: absolute;
  top: 0;
  left: 0;
}
</style>
