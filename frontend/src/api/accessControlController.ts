// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /roles/permissions/update */
export async function updateRolePermission(
  body: API.RolePermissionUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/roles/permissions/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /users/assign/role */
export async function assignUserRole(
  body: API.UserAssignRoleRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/users/assign/role", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /users/update/my */
export async function updateMyProfile(
  body: API.UserUpdateMyProfileRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/users/update/my", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
