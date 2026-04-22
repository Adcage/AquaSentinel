// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 GET /monitor/tasks/${param0} */
export async function getTaskByPath(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getTaskByPathParams,
  options?: { [key: string]: any },
) {
  const { taskCode: param0, ...queryParams } = params;
  return request<API.BaseResponseMapStringObject>(`/monitor/tasks/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /monitor/tasks/realtime/by-camera */
export async function getTaskRealtimeByCamera(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: {
    cameraId: number;
  },
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseMapStringObject>(
    "/monitor/tasks/realtime/by-camera",
    {
      method: "GET",
      params: {
        cameraId: params.cameraId,
      },
      ...(options || {}),
    },
  );
}

/** 此处后端没有提供注释 GET /monitor/tasks/realtime/batch */
export async function getTaskRealtimeBatch(
  params: {
    cameraIds: number[];
  },
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseMapStringObject>(
    "/monitor/tasks/realtime/batch",
    {
      method: "GET",
      params: {
        cameraIds: params.cameraIds.join(","),
      },
      ...(options || {}),
    },
  );
}

/** 此处后端没有提供注释 GET /monitor/tasks/engine/health */
export async function getEngineHealth(options?: { [key: string]: any }) {
  return request<API.BaseResponseMapStringObject>(
    "/monitor/tasks/engine/health",
    {
      method: "GET",
      ...(options || {}),
    },
  );
}

/** 此处后端没有提供注释 GET /monitor/tasks/get */
export async function getTaskByCode(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getTaskByCodeParams,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseMapStringObject>("/monitor/tasks/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /monitor/tasks/model/switch */
export async function switchTaskModel(
  body: API.MonitorTaskModelSwitchRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseAiStreamTask>("/monitor/tasks/model/switch", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /monitor/tasks/start */
export async function startTask(
  body: API.StartMonitorTaskRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseAiStreamTask>("/monitor/tasks/start", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /monitor/tasks/stop */
export async function stopTask(
  body: API.MonitorTaskControlRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>("/monitor/tasks/stop", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
