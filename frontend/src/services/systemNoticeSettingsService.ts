import request from "@/request";
import { unwrapApiData } from "@/services/serviceUtils";

export interface NoticeSettings {
  offDutyThreshold: number;
  deviceOfflineThreshold: number;
  drowningAlertThreshold: number;
}

export const getNoticeSettings = async (): Promise<NoticeSettings> => {
  const response = await request<any>("/system-settings/notice", {
    method: "GET",
  });
  const data = unwrapApiData<Record<string, unknown>>(response, "获取通知配置失败");
  return {
    offDutyThreshold: Number(data?.offDutyThreshold ?? 60),
    deviceOfflineThreshold: Number(data?.deviceOfflineThreshold ?? 180),
    drowningAlertThreshold: Number(data?.drowningAlertThreshold ?? 3),
  };
};

export const saveNoticeSettings = async (payload: NoticeSettings): Promise<NoticeSettings> => {
  const response = await request<any>("/system-settings/notice", {
    method: "POST",
    data: {
      offDutyThreshold: payload.offDutyThreshold,
      deviceOfflineThreshold: payload.deviceOfflineThreshold,
      drowningAlertThreshold: payload.drowningAlertThreshold,
    },
  });
  const data = unwrapApiData<Record<string, unknown>>(response, "保存通知配置失败");
  return {
    offDutyThreshold: Number(data?.offDutyThreshold ?? payload.offDutyThreshold),
    deviceOfflineThreshold: Number(data?.deviceOfflineThreshold ?? payload.deviceOfflineThreshold),
    drowningAlertThreshold: Number(data?.drowningAlertThreshold ?? payload.drowningAlertThreshold),
  };
};
