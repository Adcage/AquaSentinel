import type {
  AlarmStatus,
  DeviceStatus,
  LifeguardAuditStatus,
  LifeguardDutyStatus,
} from "@/types/business";

export interface StatusMeta {
  label: string;
  type: "success" | "warning" | "danger" | "processing" | "default";
  emphasized?: boolean;
}

const deviceStatusMap: Record<DeviceStatus, StatusMeta> = {
  online: { label: "在线", type: "success" },
  offline: { label: "离线", type: "warning" },
  error: { label: "异常", type: "danger", emphasized: true },
};

const alarmStatusMap: Record<AlarmStatus, StatusMeta> = {
  pending: { label: "未处理", type: "danger", emphasized: true },
  processing: { label: "处理中", type: "warning" },
  resolved: { label: "已处理", type: "success" },
  false_alarm: { label: "误报", type: "default" },
};

const lifeguardStatusMap: Record<LifeguardDutyStatus, StatusMeta> = {
  on_duty: { label: "在岗", type: "success" },
  off_duty: { label: "离岗", type: "warning" },
  out_of_fence: { label: "围栏外", type: "danger", emphasized: true },
};

const lifeguardAuditStatusMap: Record<LifeguardAuditStatus, StatusMeta> = {
  PENDING: { label: "待审核", type: "warning" },
  APPROVED: { label: "已通过", type: "success" },
  REJECTED: { label: "已拒绝", type: "danger" },
};

const fallbackStatus: StatusMeta = { label: "未知状态", type: "default" };

export const getDeviceStatusMeta = (status: DeviceStatus): StatusMeta =>
  deviceStatusMap[status] ?? fallbackStatus;

export const getAlarmStatusMeta = (status: AlarmStatus): StatusMeta =>
  alarmStatusMap[status] ?? fallbackStatus;

export const getLifeguardStatusMeta = (
  status: LifeguardDutyStatus,
): StatusMeta => lifeguardStatusMap[status] ?? fallbackStatus;

export const getLifeguardAuditStatusMeta = (
  status: LifeguardAuditStatus,
): StatusMeta => lifeguardAuditStatusMap[status] ?? fallbackStatus;

export const getAlarmTypeMeta = (
  type: "drowning" | "cross_border" | "over_capacity",
) => {
  if (type === "drowning") return { label: "溺水", type: "danger" as const };
  if (type === "cross_border")
    return { label: "越界", type: "warning" as const };
  return { label: "超员", type: "processing" as const };
};
