import { listLifeguardVoByPage } from "@/api/lifeguardController";
import type {
  LifeguardRecord,
  LifeguardDutyStatus,
  LifeguardAuditStatus,
  PageQuery,
  PageResult,
} from "@/types/business";
import {
  normalizeDateTime,
  unwrapApiData,
  venueIdToName,
  venueNameToId,
} from "@/services/serviceUtils";

export interface LifeguardQuery extends PageQuery {
  venueId?: string;
  venue?: string;
  dutyStatus?: LifeguardDutyStatus | "";
}

const dutyStatusToApi = (status?: LifeguardDutyStatus | "") => {
  if (status === "on_duty") {
    return "ON_DUTY";
  }
  if (status === "off_duty") {
    return "OFF_DUTY";
  }
  if (status === "out_of_fence") {
    return "OUT_OF_FENCE";
  }
  return undefined;
};

const dutyStatusToBusiness = (status?: string): LifeguardDutyStatus => {
  const normalized = status?.toUpperCase();
  if (normalized === "OFF_DUTY" || normalized === "LEAVE") {
    return "off_duty";
  }
  if (normalized === "OUT_OF_FENCE") {
    return "out_of_fence";
  }
  return "on_duty";
};

const toAuditStatus = (status?: string): LifeguardAuditStatus => {
  if (status === "APPROVED") return "APPROVED";
  if (status === "REJECTED") return "REJECTED";
  return "PENDING";
};

const toLifeguardRecord = (item: API.LifeguardVO): LifeguardRecord => ({
  id: String(item.id ?? item.lifeguardCode ?? ""),
  name: item.fullName || item.lifeguardCode || "未命名救生员",
  phone: item.phone || "-",
  venueId: String(item.venueId ?? ""),
  venue: venueIdToName(item.venueId ?? null),
  dutyStatus: dutyStatusToBusiness(item.dutyStatus),
  auditStatus: toAuditStatus(item.auditStatus),
  lastReportTime: normalizeDateTime(item.updatedAt ?? item.lastLoginAt),
});

export const getLifeguardPage = async (
  query: LifeguardQuery,
): Promise<PageResult<LifeguardRecord>> => {
  const resolvedVenueId = query.venueId
    ? Number(query.venueId)
    : venueNameToId(query.venue);
  const response = await listLifeguardVoByPage({
    current: query.current,
    pageSize: query.pageSize,
    venueId: resolvedVenueId,
    dutyStatus: dutyStatusToApi(query.dutyStatus),
    sortField: "updated_at",
    sortOrder: "descend",
  });
  const pageData = unwrapApiData<API.PageLifeguardVO>(
    response,
    "获取救生员列表失败",
  );
  const records = (pageData?.records ?? []).map(toLifeguardRecord);

  return {
    list: records,
    total: Number(pageData?.total ?? records.length),
  };
};
