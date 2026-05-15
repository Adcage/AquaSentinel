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

import {
  isVerboseWebrtcDebugEnabled,
  shouldLogWebrtcEvent,
  summarizeStatsSnapshot,
} from "@/utils/webrtcDebug";

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
const verboseDebug = isVerboseWebrtcDebugEnabled();

const debugLog = (label: string, detail?: unknown) => {
  if (!shouldLogWebrtcEvent(label, verboseDebug)) {
    return;
  }
  console.info(`[WebRtcWhepPlayer] ${label}`, detail ?? "");
};

const summarizeSdp = (sdp: string) => {
  const lines = sdp.split(/\r?\n/);
  return {
    mLines: lines.filter((line) => line.startsWith("m=")),
    mids: lines.filter((line) => line.startsWith("a=mid:")),
    bundle: lines.find((line) => line.startsWith("a=group:BUNDLE")) || "",
    setup: lines.filter((line) => line.startsWith("a=setup:")),
    endOfCandidates: lines.filter((line) => line === "a=end-of-candidates").length,
    candidates: lines.filter((line) => line.startsWith("a=candidate:")),
  };
};

const logSdpSummary = (label: string, sdp: string) => {
  const summary = summarizeSdp(sdp);
  debugLog(`${label} summary`, summary);
  if (verboseDebug) {
    debugLog(`${label} candidates`, summary.candidates.join(" || "));
  }
};

const dumpStats = async (pc: RTCPeerConnection) => {
  try {
    const stats = await pc.getStats();
    const selectedPair: Record<string, unknown> | undefined = Array.from(stats.values()).find(
      (report) =>
        report.type === "candidate-pair" &&
        (("selected" in report && report.selected) ||
          ("nominated" in report && report.nominated)),
    ) as Record<string, unknown> | undefined;
    const inboundVideo: Record<string, unknown> | undefined = Array.from(stats.values()).find(
      (report) => report.type === "inbound-rtp" && report.kind === "video",
    ) as Record<string, unknown> | undefined;
    const transport: Record<string, unknown> | undefined = Array.from(stats.values()).find(
      (report) => report.type === "transport",
    ) as Record<string, unknown> | undefined;
    debugLog("stats", {
      connectionState: pc.connectionState,
      iceConnectionState: pc.iceConnectionState,
      selectedPair: selectedPair
        ? {
            state: selectedPair.state,
            nominated: selectedPair.nominated,
            selected: selectedPair.selected,
            bytesReceived: selectedPair.bytesReceived,
            bytesSent: selectedPair.bytesSent,
            currentRoundTripTime: selectedPair.currentRoundTripTime,
          }
        : null,
      inboundVideo: inboundVideo
        ? {
            packetsReceived: inboundVideo.packetsReceived,
            bytesReceived: inboundVideo.bytesReceived,
            framesDecoded: inboundVideo.framesDecoded,
            framesReceived: inboundVideo.framesReceived,
          }
        : null,
      transport: transport
        ? {
            dtlsState: transport.dtlsState,
            iceRole: transport.iceRole,
          }
        : null,
    });
  } catch (error) {
    debugLog("stats error", error);
  }
};

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
    bundlePolicy: "max-bundle",
    rtcpMuxPolicy: "require",
  });
  peerConnection = pc;

  pc.addEventListener("icecandidate", (event) => {
    debugLog("local candidate", event.candidate?.candidate || "end-of-candidates");
  });
  pc.addEventListener("connectionstatechange", () => {
    debugLog("connectionState", pc.connectionState);
  });
  pc.addEventListener("iceconnectionstatechange", () => {
    debugLog("iceConnectionState", pc.iceConnectionState);
  });
  pc.addEventListener("signalingstatechange", () => {
    debugLog("signalingState", pc.signalingState);
  });
  pc.addEventListener("icegatheringstatechange", () => {
    debugLog("iceGatheringState", pc.iceGatheringState);
  });
  pc.addEventListener("icecandidateerror", (event) => {
    debugLog("iceCandidateError", {
      address: event.address,
      port: event.port,
      url: event.url,
      errorCode: event.errorCode,
      errorText: event.errorText,
    });
  });

  pc.addTransceiver("video", { direction: "recvonly" });
  pc.ontrack = (event) => {
    const [stream] = event.streams;
    debugLog("ontrack", {
      kind: event.track.kind,
      muted: event.track.muted,
      streamCount: event.streams.length,
    });
    event.track.onunmute = () => {
      debugLog("track unmute", event.track.kind);
    };
    event.track.onmute = () => {
      debugLog("track mute", event.track.kind);
    };
    if (!videoRef.value || !stream) {
      return;
    }
    videoRef.value.srcObject = stream;
    void videoRef.value.play().then(() => {
      debugLog("video.play resolved");
    }).catch((error) => {
      debugLog("video.play rejected", error);
    });
  };

  const offer = await pc.createOffer();
  await pc.setLocalDescription(offer);
  await waitIceGatheringDone(pc);

  if (!pc.localDescription?.sdp) {
    throw new Error("WebRTC offer 为空");
  }

  logSdpSummary("offer", pc.localDescription.sdp);

  const response = await fetch(src, {
    method: "POST",
    headers: {
      "Content-Type": "application/sdp",
      Accept: "application/sdp",
      ...(sessionStorage.getItem("token")
        ? { Authorization: `Bearer ${sessionStorage.getItem("token")}` }
        : {}),
    },
    body: pc.localDescription.sdp,
  });

  if (!response.ok) {
    throw new Error(`WHEP 建连失败: ${response.status}`);
  }

  const answerSdp = await response.text();
  const locationHeader = response.headers.get("location") || "";
  debugLog("WHIP answer received", {
    locationHeader,
    answerLength: answerSdp.length,
    candidateCount: (answerSdp.match(/^a=candidate:/gm) || []).length,
  });
  logSdpSummary("answer", answerSdp);
  if (locationHeader) {
    sessionUrl = new URL(locationHeader, src).toString();
  }

  if (startSeq !== currentSeq) {
    return;
  }

  debugLog("setRemoteDescription start");
  await pc.setRemoteDescription({
    type: "answer",
    sdp: answerSdp,
  });
  debugLog("setRemoteDescription resolved");
  if (verboseDebug) {
    debugLog("remoteDescription actual SDP", pc.remoteDescription?.sdp);
  }
  let lastStatsKey = "";
  const statsInterval = setInterval(async () => {
    if (peerConnection !== pc) {
      clearInterval(statsInterval);
      return;
    }
    try {
      const stats = await pc.getStats();
      if (verboseDebug) {
        for (const report of stats.values()) {
          if (
            report.type === "candidate-pair" ||
            report.type === "local-candidate" ||
            report.type === "remote-candidate"
          ) {
            debugLog(`[stats] ${report.type}`, {
              id: report.id,
              ...(report.type === "candidate-pair"
                ? {
                    state: (report as RTCIceCandidatePairStats).state,
                    nominated: (report as RTCIceCandidatePairStats).nominated,
                    requestsSent: (report as RTCIceCandidatePairStats).requestsSent,
                    responsesReceived: (report as RTCIceCandidatePairStats).responsesReceived,
                    bytesSent: (report as RTCIceCandidatePairStats).bytesSent,
                    bytesReceived: (report as RTCIceCandidatePairStats).bytesReceived,
                  }
                : {
                    candidateType: (report as RTCIceCandidateStats).candidateType,
                    ip: (report as RTCIceCandidateStats).ip,
                    port: (report as RTCIceCandidateStats).port,
                    protocol: (report as RTCIceCandidateStats).protocol,
                  }),
            });
          }
        }
      } else {
        const summary = summarizeStatsSnapshot(Array.from(stats.values()) as Record<string, unknown>[]);
        const nextKey = JSON.stringify(summary);
        if (summary.selectedPair && nextKey !== lastStatsKey) {
          lastStatsKey = nextKey;
          debugLog("stats summary", summary);
        }
      }
    } catch {
      clearInterval(statsInterval);
    }
  }, 2000);
  setTimeout(() => {
    if (peerConnection === pc) {
      void dumpStats(pc);
    }
  }, 3000);
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
