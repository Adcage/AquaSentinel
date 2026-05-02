import { batchAction, listByPage } from "@/api/alertActionController";
import type {
  AlarmStatus,
  PageQuery,
  PageResult,
  AlarmRecord,
} from "@/types/business";
import { normalizeDateTime, unwrapApiData } from "@/services/serviceUtils";

export interface AlarmQuery extends PageQuery {
  venueId?: string;
  keyword?: string;
  status?: AlarmStatus | "";
  type?: AlarmRecord["type"] | "";
}

const resolveVenueId = (value?: string) => {
  if (!value) {
    return undefined;
  }
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return undefined;
  }
  return parsed;
};

const alarmTypeToApi = (type?: AlarmRecord["type"] | "") => {
  if (type === "drowning") {
    return "DROWNING";
  }
  if (type === "cross_border") {
    return "CROSS_BORDER";
  }
  if (type === "over_capacity") {
    return "OVER_CAPACITY";
  }
  return undefined;
};

const apiAlertTypeToBusiness = (type?: string): AlarmRecord["type"] => {
  const normalized = type?.toUpperCase();
  if (normalized === "CROSS_BORDER") {
    return "cross_border";
  }
  if (normalized === "OVER_CAPACITY") {
    return "over_capacity";
  }
  return "drowning";
};

const alarmStatusToApi = (status?: AlarmStatus | "") => {
  if (status === "pending") {
    return "PENDING";
  }
  if (status === "processing") {
    return "ASSIGNED";
  }
  if (status === "resolved") {
    return "DONE";
  }
  if (status === "false_alarm") {
    return "FALSE_ALARM";
  }
  return undefined;
};

const apiAlertStatusToBusiness = (status?: string): AlarmStatus => {
  const normalized = status?.toUpperCase();
  if (normalized === "DONE" || normalized === "RESOLVED") {
    return "resolved";
  }
  if (normalized === "FALSE_ALARM") {
    return "false_alarm";
  }
  if (
    normalized === "ASSIGNED" ||
    normalized === "CONFIRMED" ||
    normalized === "PROCESSING"
  ) {
    return "processing";
  }
  return "pending";
};

const toAlarmRecord = (item: API.AlertRecordVO): AlarmRecord => {
  const contactName = item.emergencyContactName || "";
  const contactPhone = item.emergencyContactPhone || "";
  const emergencyContact =
    contactName && contactPhone
      ? `${contactName} ${contactPhone}`
      : contactName || contactPhone || "-";

  return {
    id: item.alertUid || String(item.id ?? ""),
    dbId: item.id ?? undefined,
    type: apiAlertTypeToBusiness(item.alertType),
    triggerTime: normalizeDateTime(item.createdAt),
    cameraLocation: item.incidentLocation || `摄像头#${item.cameraId ?? "-"}`,
    emergencyContact,
    lifeguardName: item.lifeguardId ? `救生员#${item.lifeguardId}` : "-",
    status: apiAlertStatusToBusiness(item.alertStatus),
  };
};

export const getAlarmPage = async (
  query: AlarmQuery,
): Promise<PageResult<AlarmRecord>> => {
  const response = await listByPage({
    current: query.current,
    pageSize: query.pageSize,
    venueId: resolveVenueId(query.venueId),
    keyword: query.keyword,
    alertStatus: alarmStatusToApi(query.status),
    alertType: alarmTypeToApi(query.type),
    sortField: "created_at",
    sortOrder: "descend",
  });
  const pageData = unwrapApiData<API.PageAlertRecordVO>(
    response,
    "获取报警列表失败",
  );
  const records = pageData?.records ?? [];

  return {
    list: records.map(toAlarmRecord),
    total: Number(pageData?.total ?? 0),
  };
};

export const markAlarmsResolved = async (records: AlarmRecord[]) => {
  const alertIds = records
    .map((record) => Number(record.dbId ?? 0))
    .filter((id) => Number.isFinite(id) && id > 0);
  if (!alertIds.length) {
    return { successCount: 0, failedCount: 0 };
  }
  const response = await batchAction({
    alertIds,
    actionType: "DONE",
  });
  const result = unwrapApiData<API.BatchOperateResultVO>(response, "批量处理失败");
  return {
    successCount: Number(result.successCount ?? 0),
    failedCount: Number(result.failedCount ?? 0),
  };
};
