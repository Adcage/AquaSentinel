// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /internal/ai/events */
export async function receiveEvent(
  body: string,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/internal/ai/events", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
