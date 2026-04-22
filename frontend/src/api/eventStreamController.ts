// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 GET /events */
export async function listEvents(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listEventsParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListMapStringObject>("/events", {
    method: "GET",
    params: {
      // limit has a default value: 20
      limit: "20",
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /events/annotated-stream */
export async function getAnnotatedStream(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getAnnotatedStreamParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/events/annotated-stream", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}
