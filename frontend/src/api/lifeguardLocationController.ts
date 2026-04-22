// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /lifeguards/location-logs/add */
export async function addLifeguardLocationLog(
  body: API.LifeguardLocationLogAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/lifeguards/location-logs/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /lifeguards/location-logs/delete */
export async function deleteLifeguardLocationLog(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/lifeguards/location-logs/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /lifeguards/location-logs/edit */
export async function editLifeguardLocationLog(
  body: API.LifeguardLocationLogEditRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/lifeguards/location-logs/edit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /lifeguards/location-logs/get */
export async function getLifeguardLocationLogById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getLifeguardLocationLogByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLifeguardLocationLog>(
    "/lifeguards/location-logs/get",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 GET /lifeguards/location-logs/get/vo */
export async function getLifeguardLocationLogVoById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getLifeguardLocationLogVOByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLifeguardLocationLogVO>(
    "/lifeguards/location-logs/get/vo",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /lifeguards/location-logs/list/page */
export async function listLifeguardLocationLogByPage(
  body: API.LifeguardLocationLogQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageLifeguardLocationLog>(
    "/lifeguards/location-logs/list/page",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /lifeguards/location-logs/list/page/vo */
export async function listLifeguardLocationLogVoByPage(
  body: API.LifeguardLocationLogQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageLifeguardLocationLogVO>(
    "/lifeguards/location-logs/list/page/vo",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /lifeguards/location-logs/update */
export async function updateLifeguardLocationLog(
  body: API.LifeguardLocationLogUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/lifeguards/location-logs/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
