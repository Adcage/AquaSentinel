// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /roles/add */
export async function addRole(
  body: API.RoleAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/roles/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /roles/delete */
export async function deleteRole(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/roles/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /roles/get */
export async function getRoleById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getRoleByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseSysRole>("/roles/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /roles/get/vo */
export async function getRoleVoById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getRoleVOByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseRoleVO>("/roles/get/vo", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /roles/list/page */
export async function listRolePage(
  body: API.RoleQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageSysRole>("/roles/list/page", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /roles/list/page/vo */
export async function listRolePageVo(
  body: API.RoleQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageRoleVO>("/roles/list/page/vo", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /roles/update */
export async function updateRole(
  body: API.RoleUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/roles/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
