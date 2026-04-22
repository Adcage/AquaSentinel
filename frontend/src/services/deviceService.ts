import {
  addCameraDevice,
  deleteCameraDevice,
  listCameraDeviceVoByPage,
  updateCameraDevice,
} from "@/api/cameraDeviceController";
import type {
  DeviceRecord,
  DeviceStatus,
  PageQuery,
  PageResult,
} from "@/types/business";
import { unwrapApiData, venueIdToName } from "@/services/serviceUtils";

export interface DeviceQuery extends PageQuery {
  venueId?: string;
  venue?: string;
  status?: DeviceStatus | "";
  deviceType?: DeviceRecord["deviceType"] | "";
}

const businessStatusToApi = (status?: DeviceStatus | "") => {
  if (status === "online") {
    return "ONLINE";
  }
  if (status === "offline") {
    return "OFFLINE";
  }
  if (status === "error") {
    return "ERROR";
  }
  return undefined;
};

const apiStatusToBusiness = (status?: string): DeviceStatus => {
  const normalized = status?.toUpperCase();
  if (normalized === "OFFLINE") {
    return "offline";
  }
  if (normalized === "ERROR") {
    return "error";
  }
  return "online";
};

const protocolToDeviceType = (
  protocol?: string,
): DeviceRecord["deviceType"] => {
  if (protocol?.toUpperCase() === "PTZ") {
    return "ptz";
  }
  return "fixed";
};

const deviceTypeToProtocol = (deviceType?: DeviceRecord["deviceType"]) => {
  if (deviceType === "ptz") {
    return "PTZ";
  }
  return "RTSP";
};

const toDeviceRecord = (item: API.CameraDeviceVO): DeviceRecord => ({
  id: String(item.id ?? item.cameraCode ?? ""),
  name: item.cameraName || item.cameraCode || "未命名设备",
  location: item.zoneId != null ? `区域${item.zoneId}` : "未配置区域",
  venue: venueIdToName(item.venueId ?? null),
  deviceType: protocolToDeviceType(item.protocol),
  streamUrl: item.streamUrl || "",
  status: apiStatusToBusiness(item.deviceStatus),
  maintenanceCycleDays: 30,
  enabled: item.enabled ?? 1,
});

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

const createCameraCode = (venue: string) => {
  const prefix = venue
    ? String(venue).trim().slice(0, 1).toUpperCase() || "V"
    : "V";
  return `${prefix}-${Date.now()}`;
};

const toAddPayload = (
  payload: Omit<DeviceRecord, "id">,
): API.CameraDeviceAddRequest => ({
  venueId: resolveVenueId(payload.venue),
  cameraCode: createCameraCode(payload.venue),
  cameraName: payload.name,
  streamUrl: payload.streamUrl,
  protocol: deviceTypeToProtocol(payload.deviceType),
  deviceStatus: businessStatusToApi(payload.status),
});

const toUpdatePayload = (
  id: string,
  payload: Omit<DeviceRecord, "id">,
): API.CameraDeviceUpdateRequest => ({
  id: Number(id),
  venueId: resolveVenueId(payload.venue),
  cameraName: payload.name,
  streamUrl: payload.streamUrl,
  protocol: deviceTypeToProtocol(payload.deviceType),
  deviceStatus: businessStatusToApi(payload.status),
});

export const getDevicePage = async (
  query: DeviceQuery,
): Promise<PageResult<DeviceRecord>> => {
  const response = await listCameraDeviceVoByPage({
    current: query.current,
    pageSize: query.pageSize,
    venueId: resolveVenueId(query.venueId),
    deviceStatus: businessStatusToApi(query.status),
  });
  const pageData = unwrapApiData<API.PageCameraDeviceVO>(
    response,
    "获取设备列表失败",
  );
  const records = (pageData?.records ?? []).map(toDeviceRecord);

  const filtered = records.filter((item) => {
    const matchType = query.deviceType
      ? item.deviceType === query.deviceType
      : true;
    return matchType;
  });

  return {
    list: filtered,
    total: Number(pageData?.total ?? filtered.length),
  };
};

export const createDevice = async (payload: Omit<DeviceRecord, "id">) => {
  const response = await addCameraDevice(toAddPayload(payload));
  unwrapApiData<number>(response, "新增设备失败");
  return true;
};

export const updateDevice = async (
  id: string,
  payload: Omit<DeviceRecord, "id">,
) => {
  const response = await updateCameraDevice(toUpdatePayload(id, payload));
  unwrapApiData<boolean>(response, "更新设备失败");
  return true;
};

export const removeDevice = async (id: string) => {
  const response = await deleteCameraDevice({
    id: Number(id),
  });
  unwrapApiData<boolean>(response, "删除设备失败");
  return true;
};
