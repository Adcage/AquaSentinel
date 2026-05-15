import { describe, expect, it } from "vitest";

import {
  shouldLogWebrtcEvent,
  summarizeStatsSnapshot,
} from "@/utils/webrtcDebug";

describe("shouldLogWebrtcEvent", () => {
  it("keeps key state logs when verbose is disabled", () => {
    expect(shouldLogWebrtcEvent("connectionState", false)).toBe(true);
    expect(shouldLogWebrtcEvent("iceConnectionState", false)).toBe(true);
    expect(shouldLogWebrtcEvent("ontrack", false)).toBe(true);
  });

  it("suppresses noisy logs when verbose is disabled", () => {
    expect(shouldLogWebrtcEvent("local candidate", false)).toBe(false);
    expect(shouldLogWebrtcEvent("remoteDescription actual SDP", false)).toBe(false);
    expect(shouldLogWebrtcEvent("[stats] local-candidate", false)).toBe(false);
  });

  it("allows all logs when verbose is enabled", () => {
    expect(shouldLogWebrtcEvent("local candidate", true)).toBe(true);
    expect(shouldLogWebrtcEvent("[stats] local-candidate", true)).toBe(true);
  });
});

describe("summarizeStatsSnapshot", () => {
  it("returns only selected pair and transport summary", () => {
    const summary = summarizeStatsSnapshot([
      {
        type: "candidate-pair",
        nominated: true,
        state: "succeeded",
        requestsSent: 3,
        responsesReceived: 3,
        bytesSent: 10,
        bytesReceived: 20,
      },
      {
        type: "remote-candidate",
        ip: "192.168.0.1",
      },
      {
        type: "transport",
        dtlsState: "connected",
        iceRole: "controlling",
        iceState: "connected",
      },
    ]);

    expect(summary).toEqual({
      selectedPair: {
        state: "succeeded",
        nominated: true,
        requestsSent: 3,
        responsesReceived: 3,
        bytesSent: 10,
        bytesReceived: 20,
      },
      transport: {
        dtlsState: "connected",
        iceRole: "controlling",
        iceState: "connected",
      },
    });
  });
});
