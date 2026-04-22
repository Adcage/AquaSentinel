export type DeviceStatus = "online" | "offline" | "error";

export type AlarmStatus = "pending" | "processing" | "resolved" | "false_alarm";

export type LifeguardDutyStatus = "on_duty" | "off_duty" | "out_of_fence";

export interface DashboardMetrics {
  onlineDeviceCount: number;
  onlineDeviceDiff?: number;
  todayAlarmCount: number;
  todayAlarmDiff?: number;
  pendingAlarmCount: number;
  onDutyLifeguardCount: number;
  realtimeSwimmerCount: number;
}

export interface RealtimeBBox {
  xMin: number;
  yMin: number;
  xMax: number;
  yMax: number;
}

export interface RealtimeDetection {
  trackId: string;
  label: string;
  confidence: number;
  bbox: RealtimeBBox;
  bboxNorm?: RealtimeBBox;
  riskScore?: number;
  riskLevel?: string;
  durationSec?: number;
  triggered?: boolean;
  ruleHits?: string[];
}

export interface RealtimeRiskPoint {
  cameraId?: number;
  trackId?: string;
  riskScore?: number;
  riskLevel?: string;
  durationSec?: number;
  triggered?: boolean;
  ruleHits?: string[];
  bboxCenterNorm?: {
    x: number;
    y: number;
  };
}

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
  detections: RealtimeDetection[];
  frameTs?: number;
  riskPoint?: RealtimeRiskPoint;
}

export interface AlarmRecord {
  id: string;
  dbId?: number;
  type: "drowning" | "cross_border" | "over_capacity";
  triggerTime: string;
  cameraLocation: string;
  emergencyContact: string;
  lifeguardName: string;
  status: AlarmStatus;
}

export interface DeviceRecord {
  id: string;
  name: string;
  location: string;
  venue: string;
  deviceType: "fixed" | "ptz";
  streamUrl: string;
  status: DeviceStatus;
  maintenanceCycleDays: number;
  enabled: number;
}

export type LifeguardAuditStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface LifeguardRecord {
  id: string;
  name: string;
  phone: string;
  venueId: string;
  venue: string;
  dutyStatus: LifeguardDutyStatus;
  auditStatus: LifeguardAuditStatus;
  lastReportTime: string;
}

export interface UserRecord {
  id: string;
  account: string;
  name: string;
  role: "super_admin" | "venue_admin" | "lifeguard" | "viewer";
  managedVenues: string;
  status: "enabled" | "disabled";
}

export interface PageQuery {
  current: number;
  pageSize: number;
}

export interface PageResult<T> {
  list: T[];
  total: number;
}
