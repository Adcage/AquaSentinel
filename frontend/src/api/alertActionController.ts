// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 GET /alerts/${param0} */
export async function getAlertById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getAlertByIdParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseAlertRecordVO>(`/alerts/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /alerts/${param0}/actions */
export async function actions(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.actionsParams,
  body: API.AlertActionRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseMapStringObject>(`/alerts/${param0}/actions`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /alerts/${param0}/assign */
export async function assign(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.assignParams,
  body: API.AlertActionRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseMapStringObject>(`/alerts/${param0}/assign`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /alerts/action */
export async function action(
  body: API.AlertActionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/alerts/action", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /alerts/batch/action */
export async function batchAction(
  body: API.AlertBatchActionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBatchOperateResultVO>("/alerts/batch/action", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /alerts/list/page */
export async function listByPage(
  body: API.AlertRecordQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAlertRecordVO>("/alerts/list/page", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
