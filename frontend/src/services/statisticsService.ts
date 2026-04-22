import { getOverview, ranking } from "@/api/statsController";
import { listByPage } from "@/api/alertActionController";
import { listVenueVoByPage } from "@/api/venueController";
import { toApiDateTimeString, unwrapApiData } from "@/services/serviceUtils";

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

const dateToLabel = (value: unknown) => {
  if (!value) {
    return "-";
  }
  const text = String(value);
  if (/^\d{4}-\d{2}-\d{2}/.test(text)) {
    return text.slice(5, 10);
  }
  return text;
};

const metricKeyToName = (metricKey?: string) => {
  const normalized = metricKey?.toUpperCase();
  if (normalized === "DROWNING") {
    return "溺水";
  }
  if (normalized === "CROSS_BORDER") {
    return "越界";
  }
  if (normalized === "OVER_CAPACITY") {
    return "超员";
  }
  return "其他";
};

const alarmTypeToApi = (alarmType?: string) => {
  if (alarmType === "drowning") {
    return "DROWNING";
  }
  if (alarmType === "cross_border") {
    return "CROSS_BORDER";
  }
  if (alarmType === "over_capacity") {
    return "OVER_CAPACITY";
  }
  return undefined;
};

const dateToLocalStart = (date?: string) => {
  if (!date) {
    return undefined;
  }
  return toApiDateTimeString(`${date} 00:00:00`);
};

const dateToLocalEnd = (date?: string) => {
  if (!date) {
    return undefined;
  }
  return toApiDateTimeString(`${date} 23:59:59`);
};

export interface StatisticsQueryParams {
  venueId?: string;
  startDate?: string;
  endDate?: string;
  alarmType?: string;
}

interface AlertPageQuery {
  venueId?: number;
  alertType?: string;
  startCreatedAt?: string;
  endCreatedAt?: string;
}

const toLocalDateKey = (value: unknown) => {
  if (!value) {
    return "";
  }
  const date = value instanceof Date ? value : new Date(String(value));
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const listAllAlertRecords = async (query: AlertPageQuery) => {
  const pageSize = 100;
  let current = 1;
  let total = 0;
  const allRecords: API.AlertRecordVO[] = [];

  do {
    const response = await listByPage({
      current,
      pageSize,
      venueId: query.venueId,
      alertType: query.alertType,
      startCreatedAt: query.startCreatedAt,
      endCreatedAt: query.endCreatedAt,
    });
    const data =
      unwrapApiData<API.PageAlertRecordVO>(response, "获取报警列表失败") || {};
    const records = Array.isArray(data.records) ? data.records : [];
    total = Number(data.total ?? 0);
    allRecords.push(...records);
    current += 1;
  } while (allRecords.length < total);

  return allRecords;
};

const listAllVenues = async () => {
  const pageSize = 100;
  let current = 1;
  let total = 0;
  const allVenues: API.VenueVO[] = [];

  do {
    const response = await listVenueVoByPage({
      current,
      pageSize,
    });
    const data =
      unwrapApiData<API.PageVenueVO>(response, "获取场馆列表失败") || {};
    const records = Array.isArray(data.records) ? data.records : [];
    total = Number(data.total ?? 0);
    allVenues.push(...records);
    current += 1;
  } while (allVenues.length < total);

  return allVenues;
};

export const getStatisticsKpi = async (params?: StatisticsQueryParams) => {
  const response = await getOverview({
    venueId: resolveVenueId(params?.venueId),
    date: params?.endDate,
  });
  const data =
    unwrapApiData<Record<string, unknown>>(response, "获取统计指标失败") || {};
  const total = Number(data.todayAlertCount ?? data.todayAlarmCount ?? 0);
  const pending = Number(data.pendingAlertCount ?? data.pendingAlarmCount ?? 0);

  return {
    alarmTotal: total,
    resolvedRate:
      total > 0 ? Math.round(((total - pending) / total) * 100) : 100,
    avgResponseSeconds: Number(data.avgResponseSeconds ?? 0),
    highRiskVenueCount: Number(data.highRiskVenueCount ?? 0),
  };
};

export const getAlarmTrend = async (params?: StatisticsQueryParams) => {
  const records = await listAllAlertRecords({
    venueId: resolveVenueId(params?.venueId),
    alertType: alarmTypeToApi(params?.alarmType),
    startCreatedAt: dateToLocalStart(params?.startDate),
    endCreatedAt: dateToLocalEnd(params?.endDate),
  });

  const dayMap = new Map<string, number>();
  records.forEach((item) => {
    const day = toLocalDateKey(item.createdAt);
    if (!day) {
      return;
    }
    const current = dayMap.get(day) ?? 0;
    dayMap.set(day, current + 1);
  });

  if (params?.startDate && params?.endDate) {
    const start = new Date(`${params.startDate}T00:00:00`);
    const end = new Date(`${params.endDate}T00:00:00`);
    if (!Number.isNaN(start.getTime()) && !Number.isNaN(end.getTime()) && start <= end) {
      const cursor = new Date(start);
      while (cursor <= end) {
        const day = toLocalDateKey(cursor);
        if (day && !dayMap.has(day)) {
          dayMap.set(day, 0);
        }
        cursor.setDate(cursor.getDate() + 1);
      }
    }
  }

  return Array.from(dayMap.entries())
    .sort((a, b) => a[0].localeCompare(b[0]))
    .map(([day, value]) => ({
      month: dateToLabel(day),
      value,
    }));
};

export const getVenueRanking = async (params?: StatisticsQueryParams) => {
  const [rankingResp, venues] = await Promise.all([
    ranking({
      startDate: params?.startDate,
      endDate: params?.endDate,
    }),
    listAllVenues(),
  ]);

  const data =
    unwrapApiData<Record<string, unknown>>(rankingResp, "获取场馆排名失败") || {};
  const items = Array.isArray(data.items) ? data.items : [];

  const rankMap = new Map<number, number>();
  items.forEach((item) => {
    const rankItem = item as Record<string, unknown>;
    const venueId = Number(rankItem.venueId);
    if (!Number.isFinite(venueId) || venueId <= 0) {
      return;
    }
    rankMap.set(venueId, Number(rankItem.alertCount ?? 0));
  });

  const selectedVenueId = Number(params?.venueId);
  const venueRows = venues
    .filter((venue) => venue.id != null)
    .map((venue) => {
      const venueId = Number(venue.id);
      return {
        venueId,
        month: String(venue.venueName ?? `${venueId}号场馆`),
        value: rankMap.get(venueId) ?? 0,
      };
    });

  const filteredRows = Number.isFinite(selectedVenueId) && selectedVenueId > 0
    ? venueRows.filter((item) => item.venueId === selectedVenueId)
    : venueRows;

  return filteredRows
    .sort((a, b) => b.value - a.value)
    .map((item) => ({ month: item.month, value: item.value }));
};

export const getAlarmTypeDistribution = async (params?: StatisticsQueryParams) => {
  const records = await listAllAlertRecords({
    venueId: resolveVenueId(params?.venueId),
    alertType: alarmTypeToApi(params?.alarmType),
    startCreatedAt: dateToLocalStart(params?.startDate),
    endCreatedAt: dateToLocalEnd(params?.endDate),
  });

  const statsMap = new Map<string, number>();
  records.forEach((item) => {
    const key = metricKeyToName(item.alertType);
    const current = statsMap.get(key) ?? 0;
    statsMap.set(key, current + 1);
  });

  return Array.from(statsMap.entries()).map(([name, value]) => ({
    name,
    value,
  }));
};
