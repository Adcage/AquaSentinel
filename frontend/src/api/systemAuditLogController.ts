// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /system-audit-logs/add */
export async function addSystemAuditLog(
  body: API.SystemAuditLogAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/system-audit-logs/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /system-audit-logs/delete */
export async function deleteSystemAuditLog(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/system-audit-logs/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /system-audit-logs/edit */
export async function editSystemAuditLog(
  body: API.SystemAuditLogEditRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/system-audit-logs/edit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /system-audit-logs/get */
export async function getSystemAuditLogById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getSystemAuditLogByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseSystemAuditLog>("/system-audit-logs/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /system-audit-logs/get/vo */
export async function getSystemAuditLogVoById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getSystemAuditLogVOByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseSystemAuditLogVO>(
    "/system-audit-logs/get/vo",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /system-audit-logs/list */
export async function listSystemAuditLog(
  body: API.SystemAuditLogQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListSystemAuditLog>(
    "/system-audit-logs/list",
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

/** 此处后端没有提供注释 POST /system-audit-logs/list/page */
export async function listSystemAuditLogByPage(
  body: API.SystemAuditLogQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageSystemAuditLog>(
    "/system-audit-logs/list/page",
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

/** 此处后端没有提供注释 POST /system-audit-logs/list/page/vo */
export async function listSystemAuditLogVoByPage(
  body: API.SystemAuditLogQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageSystemAuditLogVO>(
    "/system-audit-logs/list/page/vo",
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

/** 此处后端没有提供注释 POST /system-audit-logs/list/vo */
export async function listSystemAuditLogVo(
  body: API.SystemAuditLogQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListSystemAuditLogVO>(
    "/system-audit-logs/list/vo",
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

/** 此处后端没有提供注释 POST /system-audit-logs/update */
export async function updateSystemAuditLog(
  body: API.SystemAuditLogUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/system-audit-logs/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
