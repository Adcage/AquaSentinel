import { describe, expect, it } from "vitest";

import { resolveCameraPreviewTarget } from "@/utils/streamPreview";

describe("stream preview url", () => {
  it("prepends api base url and token to backend preview path", () => {
    const target = resolveCameraPreviewTarget({
      cameraId: 1001,
      cameraCode: "CAM-1001",
      streamUrl: "http://192.168.1.88/stream",
      previewUrl: "/streams/cameras/1001/preview",
      token: "abc123",
    });

    expect(target.protocol).toBe("mjpeg");
    expect(target.url).toBe(
      "/api/streams/cameras/1001/preview?token=abc123",
    );
  });

  it("uses absolute preview url as-is with token appended", () => {
    const target = resolveCameraPreviewTarget({
      cameraId: 1001,
      cameraCode: "CAM-1001",
      streamUrl: "http://192.168.1.88/stream",
      previewUrl: "http://yolo:5000/video-hub/cameras/1001/stream",
      token: "abc123",
    });

    expect(target.protocol).toBe("mjpeg");
    expect(target.url).toBe(
      "http://yolo:5000/video-hub/cameras/1001/stream?token=abc123",
    );
  });

  it("builds backend proxy preview url with provider and token", () => {
    const target = resolveCameraPreviewTarget({
      cameraId: 1001,
      cameraCode: "CAM-1001",
      streamUrl: "rtsp://camera/live",
      token: "abc123",
    });

    expect(target.protocol).toBe("mjpeg");
    expect(target.url).toContain("/api/streams/cameras/1001/preview");
    expect(target.url).toContain("provider=auto");
    expect(target.url).toContain("token=abc123");
  });
});
