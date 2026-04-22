export type CameraPreviewProtocol = "webrtc" | "mjpeg" | "ws_jpeg";

interface ResolvePreviewOptions {
  streamUrl?: string;
  cameraCode?: string;
  cameraId?: number;
  token?: string;
}

export interface CameraPreviewTarget {
  protocol: CameraPreviewProtocol;
  url: string;
}

const trimTrailingSlash = (value: string): string => value.replace(/\/+$/, "");

const trimLeadingSlash = (value: string): string => value.replace(/^\/+/, "");

const appendQuery = (url: string, key: string, value: string): string => {
  if (!url || !key || !value) {
    return url;
  }
  const separator = url.includes("?") ? "&" : "?";
  return `${url}${separator}${encodeURIComponent(key)}=${encodeURIComponent(value)}`;
};

const resolveWhepByTemplate = (
  cameraCode?: string,
  cameraId?: number,
): string => {
  const baseUrl = trimTrailingSlash(
    String(import.meta.env.VITE_WEBRTC_WHEP_BASE_URL || "").trim(),
  );
  if (!baseUrl) {
    return "";
  }
  const pathTemplate =
    String(import.meta.env.VITE_WEBRTC_WHEP_PATH_TEMPLATE || "{cameraCode}/whep").trim() ||
    "{cameraCode}/whep";

  const resolvedPath = trimLeadingSlash(
    pathTemplate
      .replaceAll("{cameraCode}", encodeURIComponent(cameraCode || ""))
      .replaceAll("{cameraId}", encodeURIComponent(String(cameraId || ""))),
  );

  if (!resolvedPath || resolvedPath.includes("%7B") || resolvedPath.includes("%7D")) {
    return "";
  }

  return `${baseUrl}/${resolvedPath}`;
};

const resolveWebrtcUrl = (
  streamUrl?: string,
  cameraCode?: string,
  cameraId?: number,
): string => {
  const normalized = (streamUrl || "").trim();
  if (/^https?:\/\/.*\/whep(\?|$)/i.test(normalized)) {
    return normalized;
  }
  return resolveWhepByTemplate(cameraCode, cameraId);
};

const resolveMjpegUrl = (streamUrl?: string, token?: string): string => {
  const normalized = (streamUrl || "").trim();
  if (!normalized) {
    return "";
  }
  const shouldAppendToken =
    String(import.meta.env.VITE_STREAM_TOKEN_PARAM_ENABLED || "false").toLowerCase() ===
    "true";
  if (!shouldAppendToken || !token) {
    return normalized;
  }
  const paramName =
    String(import.meta.env.VITE_STREAM_TOKEN_PARAM_NAME || "token").trim() || "token";
  return appendQuery(normalized, paramName, token);
};

const resolveBackendProxyUrl = (
  cameraId?: number,
  token?: string,
): string => {
  const validCameraId = Number(cameraId || 0);
  if (!Number.isFinite(validCameraId) || validCameraId <= 0) {
    return "";
  }
  const baseUrl =
    trimTrailingSlash(String(import.meta.env.VITE_API_BASE_URL || "/api").trim()) ||
    "/api";
  const provider =
    String(import.meta.env.VITE_STREAM_PROVIDER || "auto").trim() || "auto";
  const tokenParamName =
    String(import.meta.env.VITE_STREAM_TOKEN_PARAM_NAME || "token").trim() || "token";
  let previewUrl = `${baseUrl}/streams/cameras/${validCameraId}/preview?provider=${encodeURIComponent(provider)}`;
  if (token) {
    previewUrl = appendQuery(previewUrl, tokenParamName, token);
  }
  return previewUrl;
};

export const resolveCameraPreviewTarget = (
  options: ResolvePreviewOptions,
): CameraPreviewTarget => {
  const token = options.token || "";
  const previewMode = String(import.meta.env.VITE_CAMERA_PREVIEW_MODE || "backend_proxy")
    .trim()
    .toLowerCase();

  if (previewMode === "backend_proxy") {
    return {
      protocol: "mjpeg",
      url: resolveBackendProxyUrl(options.cameraId, token),
    };
  }

  if (previewMode === "webrtc") {
    const whepUrl = resolveWebrtcUrl(
      options.streamUrl,
      options.cameraCode,
      options.cameraId,
    );
    if (whepUrl) {
      const shouldAppendToken =
        String(import.meta.env.VITE_WEBRTC_APPEND_TOKEN_QUERY || "false").toLowerCase() ===
        "true";
      const url = shouldAppendToken && token ? appendQuery(whepUrl, "token", token) : whepUrl;
      return {
        protocol: "webrtc",
        url,
      };
    }
  }

  if (previewMode === "ws_jpeg") {
    return {
      protocol: "ws_jpeg",
      url: "",
    };
  }

  return {
    protocol: "mjpeg",
    url: resolveMjpegUrl(options.streamUrl, token),
  };
};
