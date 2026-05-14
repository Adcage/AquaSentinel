import type { RealtimeDetection } from "@/types/business";

export interface BBoxNorm {
  xMin: number;
  yMin: number;
  xMax: number;
  yMax: number;
}

export interface OverlayStyle {
  left: string;
  top: string;
  width: string;
  height: string;
}

export interface ContainOffset {
  offsetX: number;
  offsetY: number;
}

export function computeOverlayStyle(
  bboxNorm: BBoxNorm,
  displayWidth: number,
  displayHeight: number,
): OverlayStyle {
  const left = Math.round(bboxNorm.xMin * displayWidth);
  const top = Math.round(bboxNorm.yMin * displayHeight);
  const width = Math.round((bboxNorm.xMax - bboxNorm.xMin) * displayWidth);
  const height = Math.round((bboxNorm.yMax - bboxNorm.yMin) * displayHeight);
  return {
    left: `${left}px`,
    top: `${top}px`,
    width: `${width}px`,
    height: `${height}px`,
  };
}

export function computeContainOffset(
  frameW: number,
  frameH: number,
  displayW: number,
  displayH: number,
): ContainOffset {
  if (frameW <= 0 || frameH <= 0 || displayW <= 0 || displayH <= 0) {
    return { offsetX: 0, offsetY: 0 };
  }
  const scaleW = displayW / frameW;
  const scaleH = displayH / frameH;
  const scale = Math.min(scaleW, scaleH);
  const renderedW = frameW * scale;
  const renderedH = frameH * scale;
  return {
    offsetX: (displayW - renderedW) / 2,
    offsetY: (displayH - renderedH) / 2,
  };
}

export function filterVisibleDetections(
  detections: RealtimeDetection[],
  maxAgeMs: number,
  now: number,
): RealtimeDetection[] {
  return detections.filter((d) => {
    if (d.timestamp == null) return true;
    return now - d.timestamp <= maxAgeMs;
  });
}

export function riskLevelClass(riskLevel?: string): string {
  const level = riskLevel?.toUpperCase();
  if (level === "HIGH") return "is-high";
  if (level === "MEDIUM") return "is-medium";
  return "is-normal";
}
