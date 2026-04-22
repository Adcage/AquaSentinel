// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /stats/export/csv */
export async function exportCsv(
  body: API.StatsExportRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/stats/export/csv", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /stats/export/excel */
export async function exportExcel(
  body: API.StatsExportRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/stats/export/excel", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /stats/overview */
export async function getOverview(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getOverviewParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/stats/overview", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /stats/ranking */
export async function ranking(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.rankingParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/stats/ranking", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /stats/trend */
export async function trend(
  body: API.StatsTrendRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/stats/trend", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
