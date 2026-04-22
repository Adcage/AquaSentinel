// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /ai-stream-tasks/add */
export async function addAiStreamTask(
  body: API.AiStreamTaskAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/ai-stream-tasks/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /ai-stream-tasks/code/get */
export async function getAiStreamTaskByCode(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getAiStreamTaskByCodeParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAiStreamTask>("/ai-stream-tasks/code/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /ai-stream-tasks/delete */
export async function deleteAiStreamTask(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/ai-stream-tasks/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /ai-stream-tasks/edit */
export async function editAiStreamTask(
  body: API.AiStreamTaskEditRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/ai-stream-tasks/edit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /ai-stream-tasks/get */
export async function getAiStreamTaskById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getAiStreamTaskByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAiStreamTask>("/ai-stream-tasks/get", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /ai-stream-tasks/get/vo */
export async function getAiStreamTaskVoById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getAiStreamTaskVOByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAiStreamTaskVO>("/ai-stream-tasks/get/vo", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /ai-stream-tasks/list */
export async function listAiStreamTask(
  body: API.AiStreamTaskQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListAiStreamTask>("/ai-stream-tasks/list", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /ai-stream-tasks/list/page */
export async function listAiStreamTaskByPage(
  body: API.AiStreamTaskQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAiStreamTask>(
    "/ai-stream-tasks/list/page",
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

/** 此处后端没有提供注释 POST /ai-stream-tasks/list/page/vo */
export async function listAiStreamTaskVoByPage(
  body: API.AiStreamTaskQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAiStreamTaskVO>(
    "/ai-stream-tasks/list/page/vo",
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

/** 此处后端没有提供注释 POST /ai-stream-tasks/list/vo */
export async function listAiStreamTaskVo(
  body: API.AiStreamTaskQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListAiStreamTaskVO>(
    "/ai-stream-tasks/list/vo",
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

/** 此处后端没有提供注释 POST /ai-stream-tasks/update */
export async function updateAiStreamTask(
  body: API.AiStreamTaskUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/ai-stream-tasks/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
