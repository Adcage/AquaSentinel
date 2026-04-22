// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /venue-zones/add */
export async function addVenueZone(
  body: API.VenueZoneAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/venue-zones/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /venue-zones/delete */
export async function deleteVenueZone(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/venue-zones/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /venue-zones/edit */
export async function editVenueZone(
  body: API.VenueZoneEditRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/venue-zones/edit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /venue-zones/get */
export async function getVenueZoneById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getVenueZoneByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVenueZone>("/venue-zones/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /venue-zones/get/vo */
export async function getVenueZoneVoById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getVenueZoneVOByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVenueZoneVO>("/venue-zones/get/vo", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /venue-zones/list/page */
export async function listVenueZoneByPage(
  body: API.VenueZoneQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageVenueZone>("/venue-zones/list/page", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /venue-zones/list/page/vo */
export async function listVenueZoneVoByPage(
  body: API.VenueZoneQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageVenueZoneVO>("/venue-zones/list/page/vo", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /venue-zones/update */
export async function updateVenueZone(
  body: API.VenueZoneUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/venue-zones/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
