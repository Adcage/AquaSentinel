// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /auth/admin/login */
export async function adminLogin(
  body: API.AdminLoginRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLoginResultVO>("/auth/admin/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /auth/captcha */
export async function getCaptcha(options?: { [key: string]: any }) {
  return request<API.BaseResponseCaptchaVO>("/auth/captcha", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /auth/login */
export async function login(
  body: API.LoginRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLoginResultVO>("/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /auth/logout */
export async function logout(
  body: API.LogoutRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/auth/logout", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /auth/refresh */
export async function refresh(
  body: API.RefreshTokenRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLoginResultVO>("/auth/refresh", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /auth/register */
export async function register(
  body: API.RegisterRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/auth/register", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
