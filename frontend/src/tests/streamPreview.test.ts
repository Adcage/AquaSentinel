import { describe, expect, it, vi } from "vitest";

import { resolveCameraPreviewTarget, replaceMdnsCandidates } from "@/utils/streamPreview";

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

  it("builds webrtc direct url without leaking token or source_url into query", () => {
    vi.stubEnv("VITE_CAMERA_PREVIEW_MODE", "webrtc");
    vi.stubEnv("VITE_WEBRTC_WHEP_BASE_URL", "http://localhost:5100");
    vi.stubEnv("VITE_WEBRTC_WHEP_PATH_TEMPLATE", "video-hub/cameras/{cameraId}/whip");
    vi.stubEnv("VITE_WEBRTC_APPEND_TOKEN_QUERY", "true");

    const target = resolveCameraPreviewTarget({
      cameraId: 5021,
      cameraCode: "CAM-5021",
      streamUrl: "http://192.168.137.173/stream",
      token: "abc123",
    });

    expect(target.protocol).toBe("webrtc");
    expect(target.url).toBe("http://localhost:5100/video-hub/cameras/5021/whip");
    expect(target.url).not.toContain("token=");
    expect(target.url).not.toContain("source_url=");

    vi.unstubAllEnvs();
  });
});

describe("replaceMdnsCandidates", () => {
  it("replaces .local addresses in candidate lines", () => {
    const sdp =
      "v=0\r\n" +
      "a=candidate:1 1 UDP 12345 acad7439-1bcf-4505.local 51000 typ host\r\n" +
      "a=candidate:2 1 UDP 12346 192.168.0.181 51001 typ host\r\n" +
      "a=end-of-candidates\r\n";
    const result = replaceMdnsCandidates(sdp, ["192.168.0.181"]);
    expect(result).toContain("192.168.0.181 51000 typ host");
    expect(result).toContain("192.168.0.181 51001 typ host");
    expect(result).not.toContain(".local");
  });

  it("does not replace when candidateIps is empty", () => {
    const sdp =
      "a=candidate:1 1 UDP 12345 acad7439.local 51000 typ host\r\n";
    const result = replaceMdnsCandidates(sdp, []);
    expect(result).toContain("acad7439.local");
  });

  it("does not replace non-.local addresses", () => {
    const sdp =
      "a=candidate:1 1 UDP 12345 192.168.0.181 51000 typ host\r\n";
    const result = replaceMdnsCandidates(sdp, ["10.0.0.1"]);
    expect(result).toContain("192.168.0.181 51000 typ host");
    expect(result).not.toContain("10.0.0.1");
  });

  it("handles multiple .local candidates with IP rotation", () => {
    const sdp =
      "a=candidate:1 1 UDP 12345 aaa.local 51000 typ host\r\n" +
      "a=candidate:2 1 UDP 12346 bbb.local 51001 typ host\r\n";
    const result = replaceMdnsCandidates(sdp, ["192.168.0.181", "192.168.137.1"]);
    expect(result).toContain("192.168.0.181 51000 typ host");
    expect(result).toContain("192.168.137.1 51001 typ host");
  });
});

