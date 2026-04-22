// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /cameras/add */
export async function addCameraDevice(
  body: API.CameraDeviceAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/cameras/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /cameras/delete */
export async function deleteCameraDevice(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/cameras/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /cameras/edit */
export async function editCameraDevice(
  body: API.CameraDeviceEditRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/cameras/edit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /cameras/get */
export async function getCameraDeviceById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getCameraDeviceByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseCameraDevice>("/cameras/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /cameras/get/vo */
export async function getCameraDeviceVoById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getCameraDeviceVOByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseCameraDeviceVO>("/cameras/get/vo", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /cameras/list/page */
export async function listCameraDeviceByPage(
  body: API.CameraDeviceQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCameraDevice>("/cameras/list/page", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /cameras/list/page/vo */
export async function listCameraDeviceVoByPage(
  body: API.CameraDeviceQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCameraDeviceVO>("/cameras/list/page/vo", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /cameras/update */
export async function updateCameraDevice(
  body: API.CameraDeviceUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/cameras/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
