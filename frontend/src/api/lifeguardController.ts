// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /lifeguards/${param0}/audit */
export async function auditLifeguardByPath(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.auditLifeguardByPathParams,
  body: API.LifeguardAuditRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/lifeguards/${param0}/audit`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /lifeguards/${param0}/duty */
export async function updateDutyStatusByPath(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.updateDutyStatusByPathParams,
  body: API.LifeguardDutyUpdateRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/lifeguards/${param0}/duty`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /lifeguards/${param0}/leave-report */
export async function submitLeaveReportByPath(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.submitLeaveReportByPathParams,
  body: API.LifeguardLeaveReportRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(
    `/lifeguards/${param0}/leave-report`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      params: { ...queryParams },
      data: body,
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /lifeguards/${param0}/locations */
export async function reportLocationByPath(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.reportLocationByPathParams,
  body: API.LifeguardLocationReportRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseMapStringObject>(
    `/lifeguards/${param0}/locations`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      params: { ...queryParams },
      data: body,
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /lifeguards/add */
export async function addLifeguard(
  body: API.LifeguardAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/lifeguards/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /lifeguards/audit */
export async function auditLifeguard(
  body: API.LifeguardAuditRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/lifeguards/audit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /lifeguards/delete */
export async function deleteLifeguard(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/lifeguards/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /lifeguards/duty/update */
export async function updateDutyStatus(
  body: API.LifeguardDutyUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/lifeguards/duty/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /lifeguards/edit */
export async function editLifeguard(
  body: API.LifeguardEditRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/lifeguards/edit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /lifeguards/get */
export async function getLifeguardById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getLifeguardByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLifeguard>("/lifeguards/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /lifeguards/get/vo */
export async function getLifeguardVoById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getLifeguardVOByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLifeguardVO>("/lifeguards/get/vo", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /lifeguards/leave-report */
export async function submitLeaveReport(
  body: API.LifeguardLeaveReportRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/lifeguards/leave-report", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /lifeguards/list/page */
export async function listLifeguardByPage(
  body: API.LifeguardQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageLifeguard>("/lifeguards/list/page", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /lifeguards/list/page/vo */
export async function listLifeguardVoByPage(
  body: API.LifeguardQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageLifeguardVO>("/lifeguards/list/page/vo", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /lifeguards/location/recent */
export async function recentLocations(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.recentLocationsParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListLifeguardLocationLogVO>(
    "/lifeguards/location/recent",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /lifeguards/location/report */
export async function reportLocation(
  body: API.LifeguardLocationReportRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>(
    "/lifeguards/location/report",
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

/** 此处后端没有提供注释 POST /lifeguards/login */
export async function lifeguardLogin(
  body: API.LoginRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLoginResultVO>("/lifeguards/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /lifeguards/offpost/check */
export async function offPostCheck(
  body: API.LifeguardOffPostCheckRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/lifeguards/offpost/check", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /lifeguards/update */
export async function updateLifeguard(
  body: API.LifeguardUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/lifeguards/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
