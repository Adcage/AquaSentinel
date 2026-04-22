import {
  addVenue,
  deleteVenue,
  editVenue,
  listVenueVoByPage,
} from "@/api/venueController";
import { unwrapApiData } from "@/services/serviceUtils";

export interface VenueRecord {
  id: number;
  venueCode: string;
  venueName: string;
  location: string;
  managerUserId?: number;
}

export interface VenueQuery {
  current: number;
  pageSize: number;
  keyword?: string;
}

export interface VenuePage {
  list: VenueRecord[];
  total: number;
  current: number;
  pageSize: number;
}

export interface VenueUpsertPayload {
  venueCode?: string;
  venueName: string;
  location?: string;
  managerUserId?: number;
}

const toVenueRecord = (item: API.VenueVO): VenueRecord => {
  const extra = item as Record<string, unknown>;
  return {
    id: Number(item.id ?? 0),
    venueCode: item.venueCode || `VENUE-${item.id ?? ""}`,
    venueName: item.venueName || "未命名场馆",
    location: String(item.address ?? "-"),
    managerUserId:
      typeof extra.managerUserId === "number"
        ? extra.managerUserId
        : undefined,
  };
};

const toUpsertPayload = (payload: VenueUpsertPayload) => {
  const body: Record<string, unknown> = {
    venueName: payload.venueName,
  };
  if (payload.venueCode) {
    body.venueCode = payload.venueCode;
  }
  if (payload.location) {
    body.address = payload.location;
  }
  if (typeof payload.managerUserId === "number") {
    body.managerUserId = payload.managerUserId;
  }
  return body;
};

export const getVenuePage = async (query: VenueQuery): Promise<VenuePage> => {
  const response = await listVenueVoByPage({
    current: query.current,
    pageSize: query.pageSize,
    venueName: query.keyword || undefined,
  });
  const pageData = unwrapApiData<API.PageVenueVO>(response, "获取场馆列表失败");
  const list = (pageData?.records ?? [])
    .filter((item) => item.id != null)
    .map(toVenueRecord);
  return {
    list,
    total: Number(pageData?.total ?? list.length),
    current: Number(pageData?.current ?? query.current),
    pageSize: Number(pageData?.size ?? query.pageSize),
  };
};

export const createVenue = async (payload: VenueUpsertPayload) => {
  const response = await addVenue(toUpsertPayload(payload) as API.VenueAddRequest);
  return unwrapApiData<number>(response, "新增场馆失败");
};

export const updateVenueInfo = async (
  id: number,
  payload: VenueUpsertPayload,
) => {
  const response = await editVenue({
    id,
    ...(toUpsertPayload(payload) as API.VenueEditRequest),
  });
  unwrapApiData<boolean>(response, "更新场馆失败");
  return true;
};

export const removeVenue = async (id: number) => {
  const response = await deleteVenue({ id });
  const result = unwrapApiData<boolean>(response, "删除场馆失败");
  if (!result) {
    throw new Error("删除场馆失败，请确认场馆未被业务数据引用");
  }
  return true;
};
