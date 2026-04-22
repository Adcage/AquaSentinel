// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /data/analysis/report/list */
export async function listAnalysisReport(
  body: API.DataAnalysisReportQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListMapStringObject>(
    "/data/analysis/report/list",
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

/** 此处后端没有提供注释 GET /data/collect/status */
export async function getCollectStatus(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getCollectStatusParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/data/collect/status", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /data/preprocess/list/page */
export async function listPreprocessByPage(
  body: API.DataPreprocessQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>(
    "/data/preprocess/list/page",
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
