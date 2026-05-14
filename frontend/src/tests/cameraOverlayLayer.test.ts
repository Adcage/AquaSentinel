import { describe, expect, it } from "vitest";
import {
  computeOverlayStyle,
  computeContainOffset,
  filterVisibleDetections,
} from "@/utils/cameraOverlayUtils";

describe("cameraOverlayUtils", () => {
  describe("computeOverlayStyle", () => {
    it("scales normalized coordinates to display pixel dimensions", () => {
      const style = computeOverlayStyle(
        { xMin: 0.1, yMin: 0.2, xMax: 0.5, yMax: 0.8 },
        640,
        480,
      );
      expect(style.left).toBe("64px");
      expect(style.top).toBe("96px");
      expect(style.width).toBe("256px");
      expect(style.height).toBe("288px");
    });

    it("handles full-frame bbox", () => {
      const style = computeOverlayStyle(
        { xMin: 0, yMin: 0, xMax: 1, yMax: 1 },
        800,
        600,
      );
      expect(style.left).toBe("0px");
      expect(style.top).toBe("0px");
      expect(style.width).toBe("800px");
      expect(style.height).toBe("600px");
    });

    it("handles small bbox at origin", () => {
      const style = computeOverlayStyle(
        { xMin: 0, yMin: 0, xMax: 0.1, yMax: 0.1 },
        1000,
        500,
      );
      expect(style.left).toBe("0px");
      expect(style.top).toBe("0px");
      expect(style.width).toBe("100px");
      expect(style.height).toBe("50px");
    });
  });

  describe("computeContainOffset", () => {
    it("returns zero offset when aspect ratios match", () => {
      const offset = computeContainOffset(640, 480, 800, 600);
      expect(offset.offsetX).toBe(0);
      expect(offset.offsetY).toBe(0);
    });

    it("computes horizontal letterbox for wider display", () => {
      const offset = computeContainOffset(640, 480, 800, 480);
      expect(offset.offsetX).toBe(80);
      expect(offset.offsetY).toBe(0);
    });

    it("computes vertical pillarbox for taller display", () => {
      const offset = computeContainOffset(640, 480, 640, 600);
      expect(offset.offsetX).toBe(0);
      expect(offset.offsetY).toBe(60);
    });

    it("returns zero for zero frame dimensions", () => {
      const offset = computeContainOffset(0, 0, 800, 600);
      expect(offset.offsetX).toBe(0);
      expect(offset.offsetY).toBe(0);
    });

    it("returns zero for zero display dimensions", () => {
      const offset = computeContainOffset(640, 480, 0, 0);
      expect(offset.offsetX).toBe(0);
      expect(offset.offsetY).toBe(0);
    });

    it("handles same-size frame and display", () => {
      const offset = computeContainOffset(640, 480, 640, 480);
      expect(offset.offsetX).toBe(0);
      expect(offset.offsetY).toBe(0);
    });
  });

  describe("filterVisibleDetections", () => {
    it("keeps detections within maxAgeMs", () => {
      const now = 10000;
      const detections = [
        { trackId: "1", label: "person", confidence: 0.9, timestamp: now - 500 },
        { trackId: "2", label: "person", confidence: 0.8, timestamp: now - 1500 },
      ];
      const visible = filterVisibleDetections(detections, 2000, now);
      expect(visible).toHaveLength(2);
    });

    it("discards detections older than maxAgeMs", () => {
      const now = 10000;
      const detections = [
        { trackId: "1", label: "person", confidence: 0.9, timestamp: now - 500 },
        { trackId: "2", label: "person", confidence: 0.8, timestamp: now - 3000 },
      ];
      const visible = filterVisibleDetections(detections, 2000, now);
      expect(visible).toHaveLength(1);
      expect(visible[0].trackId).toBe("1");
    });

    it("keeps detections without timestamp", () => {
      const now = 10000;
      const detections = [
        { trackId: "1", label: "person", confidence: 0.9 },
      ];
      const visible = filterVisibleDetections(detections, 2000, now);
      expect(visible).toHaveLength(1);
    });

    it("returns empty array for empty input", () => {
      const visible = filterVisibleDetections([], 2000, Date.now());
      expect(visible).toHaveLength(0);
    });

    it("keeps detection at exact boundary", () => {
      const now = 10000;
      const detections = [
        { trackId: "1", label: "person", confidence: 0.9, timestamp: now - 2000 },
      ];
      const visible = filterVisibleDetections(detections, 2000, now);
      expect(visible).toHaveLength(1);
    });
  });
});
