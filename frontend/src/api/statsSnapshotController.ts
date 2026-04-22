// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /stats-snapshots/add */
export async function addStatsSnapshot(
  body: API.StatsSnapshotAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/stats-snapshots/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /stats-snapshots/delete */
export async function deleteStatsSnapshot(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/stats-snapshots/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /stats-snapshots/edit */
export async function editStatsSnapshot(
  body: API.StatsSnapshotEditRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/stats-snapshots/edit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /stats-snapshots/get */
export async function getStatsSnapshotById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getStatsSnapshotByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseStatsSnapshot>("/stats-snapshots/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /stats-snapshots/get/vo */
export async function getStatsSnapshotVoById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getStatsSnapshotVOByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseStatsSnapshotVO>("/stats-snapshots/get/vo", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /stats-snapshots/list */
export async function listStatsSnapshot(
  body: API.StatsSnapshotQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListStatsSnapshot>("/stats-snapshots/list", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /stats-snapshots/list/page */
export async function listStatsSnapshotByPage(
  body: API.StatsSnapshotQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageStatsSnapshot>(
    "/stats-snapshots/list/page",
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

/** 此处后端没有提供注释 POST /stats-snapshots/list/page/vo */
export async function listStatsSnapshotVoByPage(
  body: API.StatsSnapshotQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageStatsSnapshotVO>(
    "/stats-snapshots/list/page/vo",
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

/** 此处后端没有提供注释 POST /stats-snapshots/list/vo */
export async function listStatsSnapshotVo(
  body: API.StatsSnapshotQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListStatsSnapshotVO>(
    "/stats-snapshots/list/vo",
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

/** 此处后端没有提供注释 POST /stats-snapshots/update */
export async function updateStatsSnapshot(
  body: API.StatsSnapshotUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/stats-snapshots/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
