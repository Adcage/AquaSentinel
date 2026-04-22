// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /alert-disposals/add */
export async function addAlertDisposal(
  body: API.AlertDisposalAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/alert-disposals/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /alert-disposals/delete */
export async function deleteAlertDisposal(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/alert-disposals/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /alert-disposals/edit */
export async function editAlertDisposal(
  body: API.AlertDisposalEditRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/alert-disposals/edit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /alert-disposals/get */
export async function getAlertDisposalById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getAlertDisposalByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAlertDisposal>("/alert-disposals/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /alert-disposals/get/vo */
export async function getAlertDisposalVoById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getAlertDisposalVOByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAlertDisposalVO>("/alert-disposals/get/vo", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /alert-disposals/list */
export async function listAlertDisposal(
  body: API.AlertDisposalQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListAlertDisposal>("/alert-disposals/list", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /alert-disposals/list/page */
export async function listAlertDisposalByPage(
  body: API.AlertDisposalQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAlertDisposal>(
    "/alert-disposals/list/page",
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

/** 此处后端没有提供注释 POST /alert-disposals/list/page/vo */
export async function listAlertDisposalVoByPage(
  body: API.AlertDisposalQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAlertDisposalVO>(
    "/alert-disposals/list/page/vo",
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

/** 此处后端没有提供注释 POST /alert-disposals/list/vo */
export async function listAlertDisposalVo(
  body: API.AlertDisposalQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListAlertDisposalVO>(
    "/alert-disposals/list/vo",
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

/** 此处后端没有提供注释 POST /alert-disposals/update */
export async function updateAlertDisposal(
  body: API.AlertDisposalUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/alert-disposals/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
