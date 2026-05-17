import { listCameraDeviceVoByPage } from "@/api/cameraDeviceController";
import {
  getEngineHealth,
  getTaskRealtimeBatch,
} from "@/api/monitorTaskController";
import { getOverview } from "@/api/statsController";
import type {
  CameraGridItem,
  DashboardMetrics,
  RealtimeDetection,
  RealtimeRiskPoint,
} from "@/types/business";
import { unwrapApiData, venueIdToName } from "@/services/serviceUtils";
import { resolveCameraPreviewTarget } from "@/utils/streamPreview";

type CameraDeviceVoWithPreview = API.CameraDeviceVO & {
  previewUrl?: string;
};

const toRiskLevel = (status?: string): CameraGridItem["riskLevel"] => {
  const normalized = status?.toUpperCase();
  if (normalized === "ERROR") {
    return "danger";
  }
  if (normalized === "OFFLINE") {
    return "warning";
  }
  return "normal";
};

const toCameraGridItem = (item: CameraDeviceVoWithPreview): CameraGridItem => {
  const riskLevel = toRiskLevel(item.deviceStatus);
  const token = sessionStorage.getItem("token") || "";
  const previewTarget = resolveCameraPreviewTarget({
    streamUrl: item.streamUrl,
    previewUrl: item.previewUrl,
    cameraCode: item.cameraCode,
    cameraId: Number(item.id ?? 0),
    token,
  });
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
};

const toSafeNumber = (value: unknown): number => {
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return 0;
  }
  return num;
};

const normalizeRatio = (value: number): number => {
  if (!Number.isFinite(value)) {
    return 0;
  }
  if (value > 1 && value <= 100) {
    return value / 100;
  }
  return value;
};

const normalizeDetectionLabel = (value: string): string =>
  value.trim().toLowerCase();

const pickNumber = (
  source: Record<string, unknown>,
  keys: string[],
): number => {
  for (const key of keys) {
    if (key in source) {
      const value = Number(source[key]);
      if (Number.isFinite(value)) {
        return value;
      }
    }
  }
  return 0;
};

const pickString = (
  source: Record<string, unknown>,
  keys: string[],
): string => {
  for (const key of keys) {
    const value = source[key];
    if (value !== undefined && value !== null) {
      const text = String(value).trim();
      if (text.length > 0) {
        return text;
      }
    }
  }
  return "";
};

const pickBoolean = (
  source: Record<string, unknown>,
  keys: string[],
): boolean | undefined => {
  for (const key of keys) {
    if (!(key in source)) {
      continue;
    }
    const value = source[key];
    if (typeof value === "boolean") {
      return value;
    }
    if (value === null || value === undefined) {
      continue;
    }
    const text = String(value).trim().toLowerCase();
    if (["1", "true", "yes", "on"].includes(text)) {
      return true;
    }
    if (["0", "false", "no", "off"].includes(text)) {
      return false;
    }
  }
  return undefined;
};

const pickStringList = (
  source: Record<string, unknown>,
  keys: string[],
): string[] | undefined => {
  for (const key of keys) {
    const value = source[key];
    if (!Array.isArray(value)) {
      continue;
    }
    const list = value
      .map((item) => String(item ?? "").trim())
      .filter((item) => item.length > 0);
    if (list.length > 0) {
      return list;
    }
  }
  return undefined;
};

const isDrowningLabel = (value: string): boolean => {
  const normalized = normalizeDetectionLabel(value);
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

const normalizeBBox = (bbox: {
  xMin: number;
  yMin: number;
  xMax: number;
  yMax: number;
}) => {
  const xMin = Math.max(0, Math.min(1, normalizeRatio(bbox.xMin)));
  const yMin = Math.max(0, Math.min(1, normalizeRatio(bbox.yMin)));
  const xMax = Math.max(0, Math.min(1, normalizeRatio(bbox.xMax)));
  const yMax = Math.max(0, Math.min(1, normalizeRatio(bbox.yMax)));
  const left = Math.min(xMin, xMax);
  const right = Math.max(xMin, xMax);
  const top = Math.min(yMin, yMax);
  const bottom = Math.max(yMin, yMax);
  if (right - left < 0.001 || bottom - top < 0.001) {
    return null;
  }
  return {
    xMin: left,
    yMin: top,
    xMax: right,
    yMax: bottom,
  };
};

const calcIou = (a: RealtimeDetection, b: RealtimeDetection): number => {
  const aBox = a.bboxNorm;
  const bBox = b.bboxNorm;
  if (!aBox || !bBox) {
    return 0;
  }
  const interLeft = Math.max(aBox.xMin, bBox.xMin);
  const interTop = Math.max(aBox.yMin, bBox.yMin);
  const interRight = Math.min(aBox.xMax, bBox.xMax);
  const interBottom = Math.min(aBox.yMax, bBox.yMax);
  const interWidth = Math.max(0, interRight - interLeft);
  const interHeight = Math.max(0, interBottom - interTop);
  const interArea = interWidth * interHeight;
  if (interArea <= 0) {
    return 0;
  }
  const areaA =
    Math.max(0, aBox.xMax - aBox.xMin) * Math.max(0, aBox.yMax - aBox.yMin);
  const areaB =
    Math.max(0, bBox.xMax - bBox.xMin) * Math.max(0, bBox.yMax - bBox.yMin);
  const union = areaA + areaB - interArea;
  if (union <= 0) {
    return 0;
  }
  return interArea / union;
};

const dedupeDetections = (
  detections: RealtimeDetection[],
): RealtimeDetection[] => {
  const sorted = [...detections].sort((a, b) => b.confidence - a.confidence);
  const picked: RealtimeDetection[] = [];
  for (const detection of sorted) {
    if (picked.length >= 20) {
      break;
    }
    const overlap = picked.some(
      (exist) =>
        normalizeDetectionLabel(exist.label) ===
          normalizeDetectionLabel(detection.label) &&
        calcIou(exist, detection) >= 0.75,
    );
    if (!overlap) {
      picked.push(detection);
    }
  }
  return picked;
};

const toRealtimeDetections = (raw: unknown): RealtimeDetection[] => {
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw
    .map((item) => {
      if (!item || typeof item !== "object") {
        return null;
      }
      const payload = item as Record<string, unknown>;
      const bboxRaw =
        payload.bbox && typeof payload.bbox === "object"
          ? (payload.bbox as Record<string, unknown>)
          : {};
      const bboxNormRaw =
        payload.bbox_norm && typeof payload.bbox_norm === "object"
          ? (payload.bbox_norm as Record<string, unknown>)
          : payload.bboxNorm && typeof payload.bboxNorm === "object"
            ? (payload.bboxNorm as Record<string, unknown>)
            : {};
      const rawNormBox = {
        xMin: pickNumber(bboxNormRaw, ["x_min", "xMin"]),
        yMin: pickNumber(bboxNormRaw, ["y_min", "yMin"]),
        xMax: pickNumber(bboxNormRaw, ["x_max", "xMax"]),
        yMax: pickNumber(bboxNormRaw, ["y_max", "yMax"]),
      };
      const normalized = normalizeBBox(rawNormBox);
      if (!normalized) {
        return null;
      }
      const confidenceRaw = pickNumber(payload, ["confidence", "score"]);
      const confidence =
        confidenceRaw > 1 ? confidenceRaw / 100 : confidenceRaw;
      const extraRaw =
        payload.extra_json && typeof payload.extra_json === "object"
          ? (payload.extra_json as Record<string, unknown>)
          : payload.extraJson && typeof payload.extraJson === "object"
            ? (payload.extraJson as Record<string, unknown>)
            : {};
      return {
        trackId: pickString(payload, ["track_id", "trackId", "id"]),
        label: pickString(payload, ["label", "class_name", "className"]),
        confidence,
        bbox: {
          xMin: pickNumber(bboxRaw, ["x_min", "xMin"]),
          yMin: pickNumber(bboxRaw, ["y_min", "yMin"]),
          xMax: pickNumber(bboxRaw, ["x_max", "xMax"]),
          yMax: pickNumber(bboxRaw, ["y_max", "yMax"]),
        },
        bboxNorm: normalized,
        riskScore: pickNumber(extraRaw, ["risk_score", "riskScore"]),
        riskLevel: pickString(extraRaw, ["risk_level", "riskLevel"]),
        durationSec: pickNumber(extraRaw, ["duration_sec", "durationSec"]),
        triggered: pickBoolean(extraRaw, ["triggered"]),
        ruleHits: pickStringList(extraRaw, ["rule_hits", "ruleHits"]),
      } as RealtimeDetection;
    })
    .filter((item): item is RealtimeDetection => Boolean(item && item.trackId));
};

const toRealtimeRiskPoint = (raw: unknown): RealtimeRiskPoint | undefined => {
  if (!raw || typeof raw !== "object") {
    return undefined;
  }
  const payload = raw as Record<string, unknown>;
  const bboxCenterNorm =
    payload.bboxCenterNorm && typeof payload.bboxCenterNorm === "object"
      ? (payload.bboxCenterNorm as Record<string, unknown>)
      : undefined;
  return {
    cameraId:
      payload.cameraId === undefined
        ? undefined
        : toSafeNumber(payload.cameraId),
    trackId:
      payload.trackId === undefined ? undefined : String(payload.trackId),
    riskScore: pickNumber(payload, ["riskScore", "risk_score"]),
    riskLevel: pickString(payload, ["riskLevel", "risk_level"]),
    durationSec: pickNumber(payload, ["durationSec", "duration_sec"]),
    triggered: pickBoolean(payload, ["triggered"]),
    ruleHits: pickStringList(payload, ["ruleHits", "rule_hits"]),
    bboxCenterNorm: bboxCenterNorm
      ? {
          x: toSafeNumber(bboxCenterNorm.x),
          y: toSafeNumber(bboxCenterNorm.y),
        }
      : undefined,
  };
};

const mergeRealtimeIntoItem = (
  item: CameraGridItem,
  rawRealtimeItem?: unknown,
): CameraGridItem => {
  if (!rawRealtimeItem || typeof rawRealtimeItem !== "object") {
    return item;
  }
  const payload = rawRealtimeItem as Record<string, unknown>;
  const engine =
    payload.engine && typeof payload.engine === "object"
      ? (payload.engine as Record<string, unknown>)
      : undefined;
  const realtime =
    engine?.realtime && typeof engine.realtime === "object"
      ? (engine.realtime as Record<string, unknown>)
      : undefined;
  const frameTs = realtime ? toSafeNumber(realtime.frame_ts) : undefined;
  if (frameTs && item.frameTs && frameTs === item.frameTs) {
    return item;
  }
  const detections = dedupeDetections(
    toRealtimeDetections(realtime?.detections),
  );
  const confirmedDrowningCount = detections.filter((detection) =>
    isConfirmedDrowning(detection),
  ).length;
  const warningDrowningCount = detections.filter((detection) =>
    isWarningDrowning(detection),
  ).length;
  return {
    ...item,
    detections,
    peopleCount: detections.length,
    frameTs,
    riskPoint: toRealtimeRiskPoint(realtime?.risk_point),
    isAlarming: confirmedDrowningCount > 0 || item.isAlarming,
    riskLevel: confirmedDrowningCount > 0 ? "danger" : warningDrowningCount > 0 ? "warning" : item.riskLevel,
  };
};

export const mergeRealtimeBatchIntoGrid = (
  baseItems: CameraGridItem[],
  payload: Record<string, unknown> | undefined,
): CameraGridItem[] => {
  const batchPayload = payload || {};
  return baseItems.map((item) =>
    mergeRealtimeIntoItem(item, batchPayload[String(item.cameraId)]),
  );
};

export const enrichCameraGridRealtimeBatch = async (
  baseItems: CameraGridItem[],
): Promise<CameraGridItem[]> => {
  const cameraIds = baseItems
    .map((item) => item.cameraId)
    .filter((cameraId) => Number.isFinite(cameraId) && cameraId > 0);
  if (cameraIds.length === 0) {
    return baseItems;
  }
  try {
    const response = await getTaskRealtimeBatch({ cameraIds });
    const payload =
      unwrapApiData<Record<string, unknown>>(
        response,
        "批量获取摄像头实时任务失败",
      ) || {};
    return mergeRealtimeBatchIntoGrid(baseItems, payload);
  } catch {
    return baseItems;
  }
};

export const checkEngineAvailability = async (): Promise<{
  available: boolean;
  message: string;
}> => {
  try {
    const response = await getEngineHealth();
    const payload =
      unwrapApiData<Record<string, unknown>>(response, "AI引擎健康检查失败") ||
      {};
    const available = Boolean(payload.available);
    const message =
      typeof payload.message === "string" && payload.message.trim().length > 0
        ? payload.message
        : available
          ? "AI引擎运行正常"
          : "AI引擎不可用，请启动Python服务";
    return {
      available,
      message,
    };
  } catch {
    return {
      available: false,
      message: "AI引擎不可用，请启动Python服务",
    };
  }
};

export const getDashboardMetrics = async (): Promise<DashboardMetrics> => {
  const response = await getOverview({});
  const data =
    unwrapApiData<Record<string, unknown>>(response, "获取监控总览指标失败") ||
    {};
  const todayAlertCount = Number(
    data.todayAlertCount ?? data.todayAlarmCount ?? 0,
  );
  const pendingAlertCount = Number(
    data.pendingAlertCount ?? data.pendingAlarmCount ?? 0,
  );

  return {
    onlineDeviceCount: Number(data.onlineDeviceCount ?? 0),
    onlineDeviceDiff: Number(data.onlineDeviceDiff ?? 0),
    todayAlarmCount: todayAlertCount,
    todayAlarmDiff: Number(data.todayAlertDiff ?? 0),
    pendingAlarmCount: pendingAlertCount,
    onDutyLifeguardCount: Number(data.onDutyLifeguardCount ?? 0),
    realtimeSwimmerCount: Number(
      data.currentPoolHeadCount ?? data.realtimeSwimmerCount ?? 0,
    ),
  };
};

export type CameraGridPageQuery = {
  current?: number;
  pageSize?: number;
};

export type CameraGridPageResult = {
  records: CameraGridItem[];
  total: number;
  current: number;
  pageSize: number;
};

export const fetchCameraGridBase = async (
  query: CameraGridPageQuery = {},
): Promise<CameraGridPageResult> => {
  const current =
    typeof query.current === "number" && query.current > 0
      ? Math.floor(query.current)
      : 1;
  const pageSize =
    typeof query.pageSize === "number" && query.pageSize > 0
      ? Math.floor(query.pageSize)
      : 9;
  const response = await listCameraDeviceVoByPage({
    current,
    pageSize,
  });
  const pageData = unwrapApiData<API.PageCameraDeviceVO>(
    response,
    "获取摄像头网格失败",
  );
  const records = pageData?.records ?? [];
  return {
    records: records.map(toCameraGridItem),
    total: Number(pageData?.total ?? records.length),
    current: Number(pageData?.current ?? current),
    pageSize: Number(pageData?.size ?? pageSize),
  };
};

export const getCameraGrid = async (
  options: { includeRealtime?: boolean } = {},
): Promise<CameraGridItem[]> => {
  const pageResult = await fetchCameraGridBase();
  const baseItems = pageResult.records;
  if (options.includeRealtime === false) {
    return baseItems;
  }
  return enrichCameraGridRealtimeBatch(baseItems);
};
