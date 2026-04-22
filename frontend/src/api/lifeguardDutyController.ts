// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /lifeguards/duty-logs/add */
export async function addLifeguardDutyLog(
  body: API.LifeguardDutyLogAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/lifeguards/duty-logs/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /lifeguards/duty-logs/delete */
export async function deleteLifeguardDutyLog(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/lifeguards/duty-logs/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /lifeguards/duty-logs/edit */
export async function editLifeguardDutyLog(
  body: API.LifeguardDutyLogEditRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/lifeguards/duty-logs/edit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /lifeguards/duty-logs/get */
export async function getLifeguardDutyLogById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getLifeguardDutyLogByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLifeguardDutyLog>(
    "/lifeguards/duty-logs/get",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 GET /lifeguards/duty-logs/get/vo */
export async function getLifeguardDutyLogVoById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getLifeguardDutyLogVOByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLifeguardDutyLogVO>(
    "/lifeguards/duty-logs/get/vo",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /lifeguards/duty-logs/list/page */
export async function listLifeguardDutyLogByPage(
  body: API.LifeguardDutyLogQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageLifeguardDutyLog>(
    "/lifeguards/duty-logs/list/page",
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

/** 此处后端没有提供注释 POST /lifeguards/duty-logs/list/page/vo */
export async function listLifeguardDutyLogVoByPage(
  body: API.LifeguardDutyLogQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageLifeguardDutyLogVO>(
    "/lifeguards/duty-logs/list/page/vo",
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

/** 此处后端没有提供注释 POST /lifeguards/duty-logs/update */
export async function updateLifeguardDutyLog(
  body: API.LifeguardDutyLogUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/lifeguards/duty-logs/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
