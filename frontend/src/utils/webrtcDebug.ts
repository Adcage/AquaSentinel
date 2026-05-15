type StatsLike = Record<string, unknown>;

const VERBOSE_ONLY_PREFIXES = [
  "local candidate",
  "remoteDescription actual SDP",
  "offer candidates",
  "answer candidates",
  "[stats] local-candidate",
  "[stats] remote-candidate",
  "[stats] candidate-pair",
  "[stats] transport",
  "stats",
];

export const isVerboseWebrtcDebugEnabled = (): boolean =>
  String(import.meta.env.VITE_WEBRTC_DEBUG_VERBOSE || "false").toLowerCase() === "true";

export const shouldLogWebrtcEvent = (label: string, verbose: boolean): boolean => {
  if (verbose) {
    return true;
  }
  return !VERBOSE_ONLY_PREFIXES.some((prefix) => label.startsWith(prefix));
};

export const summarizeStatsSnapshot = (reports: StatsLike[]) => {
  const selectedPair = reports.find(
    (report) =>
      report.type === "candidate-pair" &&
      (report.selected === true || report.nominated === true),
  );
  const transport = reports.find((report) => report.type === "transport");
  return {
    selectedPair: selectedPair
      ? {
          state: selectedPair.state,
          nominated: selectedPair.nominated,
          requestsSent: selectedPair.requestsSent,
          responsesReceived: selectedPair.responsesReceived,
          bytesSent: selectedPair.bytesSent,
          bytesReceived: selectedPair.bytesReceived,
        }
      : null,
    transport: transport
      ? {
          dtlsState: transport.dtlsState,
          iceRole: transport.iceRole,
          iceState: transport.iceState,
        }
      : null,
  };
};
