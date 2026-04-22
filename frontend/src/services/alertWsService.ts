export type WsConnectionStatus = "connected" | "disconnected";

export interface WsVideoFramePayload {
  cameraId: number;
  frameTs: number;
  seq: number;
  headCount?: number;
  detections?: unknown[];
  riskPoint?: unknown;
}

interface WsPayload {
  messageId?: string;
  messageType?: string;
  eventUid?: string;
  alertUid?: string;
  occurredAt?: number;
  data?: Record<string, unknown>;
}

const RETRY_DELAYS_MS = [1000, 2000, 5000, 10000];
const DEFAULT_FRAME_MAX_AGE_MS = 1200;

class AlertWsService {
  private socket: WebSocket | null = null;

  private currentStatus: WsConnectionStatus = "disconnected";

  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  private retryIndex = 0;

  private manualClosed = false;

  private currentToken = "";

  private messageCallback: ((payload: WsPayload) => void) | null = null;

  private statusCallback: ((status: WsConnectionStatus) => void) | null = null;

  private videoFrameCallback:
    | ((cameraId: number, blob: Blob, frameHeader: WsVideoFramePayload) => void)
    | null = null;

  private lastVideoFrameHeader: WsVideoFramePayload | null = null;

  private sendQueue: string[] = [];

  private frameMaxAgeMs = DEFAULT_FRAME_MAX_AGE_MS;

  private isFrameFresh(frameTs: number): boolean {
    if (!Number.isFinite(frameTs) || frameTs <= 0) {
      return true;
    }
    return Date.now() - frameTs <= this.frameMaxAgeMs;
  }

  private resolveFrameMaxAgeMs(): number {
    const raw = Number(
      import.meta.env.VITE_MONITOR_FRAME_MAX_AGE_MS ?? DEFAULT_FRAME_MAX_AGE_MS,
    );
    if (!Number.isFinite(raw) || raw <= 0) {
      return DEFAULT_FRAME_MAX_AGE_MS;
    }
    return Math.max(200, Math.floor(raw));
  }

  private buildWsUrl(token: string) {
    const baseURL = String(import.meta.env.VITE_API_BASE_URL || "/api");
    const wsProtocol = window.location.protocol === "https:" ? "wss" : "ws";
    const basePath = baseURL.startsWith("http")
      ? new URL(baseURL).pathname
      : baseURL;
    const prefix = basePath.startsWith("/") ? basePath : `/${basePath}`;
    const wsPath = `${prefix.replace(/\/$/, "")}/ws/alerts`;
    const url = `${wsProtocol}://${window.location.host}${wsPath}?token=${encodeURIComponent(token)}`;
    return url;
  }

  connect(
    token: string,
    onMessage: (payload: WsPayload) => void,
    onStatus: (status: WsConnectionStatus) => void,
    onVideoFrame?: (
      cameraId: number,
      blob: Blob,
      frameHeader: WsVideoFramePayload,
    ) => void,
  ) {
    if (!token) {
      return;
    }
    this.currentToken = token;
    this.frameMaxAgeMs = this.resolveFrameMaxAgeMs();
    this.messageCallback = onMessage;
    this.statusCallback = onStatus;
    this.videoFrameCallback = onVideoFrame ?? null;
    this.manualClosed = false;
    this.clearReconnectTimer();

    if (
      this.socket &&
      (this.socket.readyState === WebSocket.OPEN ||
        this.socket.readyState === WebSocket.CONNECTING)
    ) {
      return;
    }

    this.socket = new WebSocket(this.buildWsUrl(token));

    this.socket.onopen = () => {
      this.retryIndex = 0;
      this.currentStatus = "connected";
      this.statusCallback?.("connected");
      this.flushSendQueue();
    };

    this.socket.onmessage = (event) => {
      if (event.data instanceof Blob) {
        if (this.lastVideoFrameHeader && this.videoFrameCallback) {
          if (!this.isFrameFresh(this.lastVideoFrameHeader.frameTs)) {
            return;
          }
          this.videoFrameCallback(
            this.lastVideoFrameHeader.cameraId,
            event.data,
            this.lastVideoFrameHeader,
          );
        }
        return;
      }

      if (event.data instanceof ArrayBuffer) {
        if (this.lastVideoFrameHeader && this.videoFrameCallback) {
          if (!this.isFrameFresh(this.lastVideoFrameHeader.frameTs)) {
            return;
          }
          const blob = new Blob([event.data], { type: "image/jpeg" });
          this.videoFrameCallback(
            this.lastVideoFrameHeader.cameraId,
            blob,
            this.lastVideoFrameHeader,
          );
        }
        return;
      }

      try {
        const payload = JSON.parse(String(event.data)) as WsPayload;
        if (payload.messageType === "MONITOR_VIDEO_FRAME") {
          const data = payload.data as Record<string, unknown> | undefined;
          if (data) {
            this.lastVideoFrameHeader = {
              cameraId: Number(data.cameraId) || 0,
              frameTs: Number(data.frameTs) || 0,
              seq: Number(data.seq) || 0,
              headCount:
                data.headCount !== undefined
                  ? Number(data.headCount)
                  : undefined,
              detections: Array.isArray(data.detections)
                ? data.detections
                : undefined,
              riskPoint: data.riskPoint,
            };
          }
        } else {
          this.lastVideoFrameHeader = null;
        }
        this.messageCallback?.(payload);
      } catch {
        // ignore non-json payload
      }
    };

    this.socket.onerror = () => {
      this.currentStatus = "disconnected";
      this.statusCallback?.("disconnected");
    };

    this.socket.onclose = () => {
      this.currentStatus = "disconnected";
      this.statusCallback?.("disconnected");
      this.socket = null;
      if (!this.manualClosed) {
        this.scheduleReconnect();
      }
    };
  }

  send(payload: Record<string, unknown>) {
    if (!payload || typeof payload !== "object") {
      return;
    }
    const raw = JSON.stringify(payload);
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(raw);
      return;
    }
    this.sendQueue.push(raw);
    if (this.sendQueue.length > 50) {
      this.sendQueue.shift();
    }
  }

  disconnect() {
    this.manualClosed = true;
    this.clearReconnectTimer();
    this.sendQueue = [];
    this.currentStatus = "disconnected";
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
  }

  getStatus(): WsConnectionStatus {
    return this.currentStatus;
  }

  private flushSendQueue() {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      return;
    }
    while (this.sendQueue.length > 0) {
      const message = this.sendQueue.shift();
      if (!message) {
        continue;
      }
      this.socket.send(message);
    }
  }

  private scheduleReconnect() {
    if (!this.currentToken) {
      return;
    }
    this.clearReconnectTimer();
    const delay =
      RETRY_DELAYS_MS[Math.min(this.retryIndex, RETRY_DELAYS_MS.length - 1)];
    this.retryIndex += 1;
    this.reconnectTimer = setTimeout(() => {
      this.connect(
        this.currentToken,
        this.messageCallback ?? (() => undefined),
        this.statusCallback ?? (() => undefined),
        this.videoFrameCallback ?? undefined,
      );
    }, delay);
  }

  private clearReconnectTimer() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }
}

export const alertWsService = new AlertWsService();
