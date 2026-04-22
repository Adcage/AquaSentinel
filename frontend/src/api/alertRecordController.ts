// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /alert-records/add */
export async function addAlertRecord(
  body: API.AlertRecordAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/alert-records/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /alert-records/delete */
export async function deleteAlertRecord(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/alert-records/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /alert-records/edit */
export async function editAlertRecord(
  body: API.AlertRecordEditRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/alert-records/edit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /alert-records/get */
export async function getAlertRecordById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getAlertRecordByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAlertRecord>("/alert-records/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /alert-records/get/vo */
export async function getAlertRecordVoById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getAlertRecordVOByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAlertRecordVO>("/alert-records/get/vo", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /alert-records/list */
export async function listAlertRecord(
  body: API.AlertRecordQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListAlertRecord>("/alert-records/list", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /alert-records/list/page */
export async function listAlertRecordByPage(
  body: API.AlertRecordQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAlertRecord>("/alert-records/list/page", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /alert-records/list/page/vo */
export async function listAlertRecordVoByPage(
  body: API.AlertRecordQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAlertRecordVO>(
    "/alert-records/list/page/vo",
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

/** 此处后端没有提供注释 POST /alert-records/list/vo */
export async function listAlertRecordVo(
  body: API.AlertRecordQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListAlertRecordVO>("/alert-records/list/vo", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /alert-records/update */
export async function updateAlertRecord(
  body: API.AlertRecordUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/alert-records/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
