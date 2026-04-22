import { describe, expect, it } from "vitest";

import { resolveCameraPreviewTarget } from "@/utils/streamPreview";

describe("stream preview url", () => {
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
