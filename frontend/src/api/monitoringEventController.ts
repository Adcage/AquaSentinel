// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /monitoring-events/add */
export async function addMonitoringEvent(
  body: API.MonitoringEventAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/monitoring-events/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /monitoring-events/delete */
export async function deleteMonitoringEvent(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/monitoring-events/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /monitoring-events/edit */
export async function editMonitoringEvent(
  body: API.MonitoringEventEditRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/monitoring-events/edit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /monitoring-events/get */
export async function getMonitoringEventById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getMonitoringEventByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMonitoringEvent>("/monitoring-events/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /monitoring-events/get/vo */
export async function getMonitoringEventVoById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getMonitoringEventVOByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMonitoringEventVO>(
    "/monitoring-events/get/vo",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /monitoring-events/list */
export async function listMonitoringEvent(
  body: API.MonitoringEventQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListMonitoringEvent>(
    "/monitoring-events/list",
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

/** 此处后端没有提供注释 POST /monitoring-events/list/page */
export async function listMonitoringEventByPage(
  body: API.MonitoringEventQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageMonitoringEvent>(
    "/monitoring-events/list/page",
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

/** 此处后端没有提供注释 POST /monitoring-events/list/page/vo */
export async function listMonitoringEventVoByPage(
  body: API.MonitoringEventQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageMonitoringEventVO>(
    "/monitoring-events/list/page/vo",
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

/** 此处后端没有提供注释 POST /monitoring-events/list/vo */
export async function listMonitoringEventVo(
  body: API.MonitoringEventQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListMonitoringEventVO>(
    "/monitoring-events/list/vo",
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

/** 此处后端没有提供注释 POST /monitoring-events/update */
export async function updateMonitoringEvent(
  body: API.MonitoringEventUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/monitoring-events/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
