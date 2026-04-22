// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /venues/add */
export async function addVenue(
  body: API.VenueAddRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseLong>("/venues/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /venues/delete */
export async function deleteVenue(
  body: API.DeleteRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>("/venues/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /venues/edit */
export async function editVenue(
  body: API.VenueEditRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>("/venues/edit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /venues/get */
export async function getVenueById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getVenueByIdParams,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseVenue>("/venues/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /venues/get/vo */
export async function getVenueVoById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getVenueVOByIdParams,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseVenueVO>("/venues/get/vo", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /venues/list/page */
export async function listVenueByPage(
  body: API.VenueQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageVenue>("/venues/list/page", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /venues/list/page/vo */
export async function listVenueVoByPage(
  body: API.VenueQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageVenueVO>("/venues/list/page/vo", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /venues/list/fence/bounds */
export async function listVenueFenceByBounds(
  body: API.VenueFenceBoundsRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageVenueVO>("/venues/list/fence/bounds", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /venues/update */
export async function updateVenue(
  body: API.VenueUpdateRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>("/venues/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
