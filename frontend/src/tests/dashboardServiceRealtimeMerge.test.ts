import { describe, expect, it } from "vitest";
import { mergeRealtimeBatchIntoGrid } from "@/services/dashboardService";
import type { CameraGridItem } from "@/types/business";

const createBaseItem = (): CameraGridItem => ({
  id: "CAM-1",
  cameraId: 1,
  cameraCode: "CAM-1",
  name: "1号摄像头",
  location: "1号场馆-区域A",
  peopleCount: 0,
  riskLevel: "normal",
  isAlarming: false,
  streamUrl: "http://example.com/stream",
  previewProtocol: "mjpeg",
  previewUrl: "http://example.com/preview",
  detections: [],
});

describe("mergeRealtimeBatchIntoGrid", () => {
  it("合并WS批量实时数据后更新检测框与风险状态", () => {
    const base = [createBaseItem()];
    const merged = mergeRealtimeBatchIntoGrid(base, {
      "1": {
        engine: {
          realtime: {
            frame_ts: 1710000000,
            detections: [
              {
                track_id: "T-1",
                label: "drowning",
                confidence: 0.92,
                bbox_norm: {
                  x_min: 0.1,
                  y_min: 0.2,
                  x_max: 0.4,
                  y_max: 0.7,
                },
                extra_json: {
                  risk_level: "HIGH",
                  duration_sec: 3.2,
                  rule_hits: ["duration_abnormal"],
                },
              },
            ],
            risk_point: {
              cameraId: 1,
              trackId: "T-1",
              riskScore: 0.88,
              triggered: true,
            },
          },
        },
      },
    });

    expect(merged[0].frameTs).toBe(1710000000);
    expect(merged[0].detections).toHaveLength(1);
    expect(merged[0].peopleCount).toBe(1);
    expect(merged[0].riskLevel).toBe("danger");
    expect(merged[0].isAlarming).toBe(true);
    expect(merged[0].riskPoint?.cameraId).toBe(1);
  });

  it("当frameTs未变化时保持原对象引用", () => {
    const baseItem = {
      ...createBaseItem(),
      frameTs: 1710000000,
    };
    const base = [baseItem];
    const merged = mergeRealtimeBatchIntoGrid(base, {
      "1": {
        engine: {
          realtime: {
            frame_ts: 1710000000,
            detections: [],
          },
        },
      },
    });

    expect(merged[0]).toBe(baseItem);
  });
});
