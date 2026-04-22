// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 GET /cameras/maintenance/${param0} */
export async function listByCamera(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listByCameraParams,
  options?: { [key: string]: any }
) {
  const { cameraId: param0, ...queryParams } = params;
  return request<API.BaseResponsePageCameraMaintenanceLogVO>(
    `/cameras/maintenance/${param0}`,
    {
      method: "GET",
      params: {
        // current has a default value: 1
        current: "1",
        // pageSize has a default value: 20
        pageSize: "20",
        ...queryParams,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /cameras/maintenance/${param0} */
export async function addCameraMaintenanceLogByCamera(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.addCameraMaintenanceLogByCameraParams,
  body: API.CameraMaintenanceLogAddRequest,
  options?: { [key: string]: any }
) {
  const { cameraId: param0, ...queryParams } = params;
  return request<API.BaseResponseLong>(`/cameras/maintenance/${param0}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /cameras/maintenance/add */
export async function addCameraMaintenanceLog(
  body: API.CameraMaintenanceLogAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/cameras/maintenance/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /cameras/maintenance/delete */
export async function deleteCameraMaintenanceLog(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/cameras/maintenance/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /cameras/maintenance/edit */
export async function editCameraMaintenanceLog(
  body: API.CameraMaintenanceLogEditRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/cameras/maintenance/edit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /cameras/maintenance/get */
export async function getCameraMaintenanceLogById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getCameraMaintenanceLogByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseCameraMaintenanceLog>(
    "/cameras/maintenance/get",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 GET /cameras/maintenance/get/vo */
export async function getCameraMaintenanceLogVoById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getCameraMaintenanceLogVOByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseCameraMaintenanceLogVO>(
    "/cameras/maintenance/get/vo",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /cameras/maintenance/list/page */
export async function listCameraMaintenanceLogByPage(
  body: API.CameraMaintenanceLogQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCameraMaintenanceLog>(
    "/cameras/maintenance/list/page",
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

/** 此处后端没有提供注释 POST /cameras/maintenance/list/page/vo */
export async function listCameraMaintenanceLogVoByPage(
  body: API.CameraMaintenanceLogQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCameraMaintenanceLogVO>(
    "/cameras/maintenance/list/page/vo",
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

/** 此处后端没有提供注释 POST /cameras/maintenance/update */
export async function updateCameraMaintenanceLog(
  body: API.CameraMaintenanceLogUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/cameras/maintenance/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
