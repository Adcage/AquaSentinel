import type { RealtimeDetection, RealtimeRiskPoint } from "@/types/business";

export interface DetectionFrame {
  cameraId: number;
  frameWidth: number;
  frameHeight: number;
  timestamp: number;
  detections: RealtimeDetection[];
  headCount?: number;
  riskPoint?: RealtimeRiskPoint;
}
