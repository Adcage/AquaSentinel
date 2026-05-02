interface ApiEnvelope<T> {
  code?: number;
  data?: T;
  message?: string;
  requestId?: string;
}

interface ApiHttpResponse<T> {
  data?: ApiEnvelope<T>;
}

const SUCCESS_CODE = 0;
const RATE_LIMIT_CODE = 40301;
const RATE_LIMIT_TEXT = /请求过于频繁|操作过于频繁/;

const LOCAL_DATETIME_PATTERN =
  /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}:\d{2}$/;

const formatAsLocalDateTime = (date: Date) => {
  const pad = (num: number) => String(num).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

export const unwrapApiData = <T>(
  response: ApiHttpResponse<T>,
  fallbackMessage: string,
): T => {
  const payload = response?.data;
  if (!payload) {
    throw new Error(fallbackMessage);
  }
  if (payload.code !== SUCCESS_CODE) {
    if (isRateLimitError(payload.code, payload.message)) {
      throw new Error("操作过于频繁，请稍后再试");
    }
    throw new Error(payload.message || fallbackMessage);
  }
  return payload.data as T;
};

export const isRateLimitError = (code?: number, message?: string) => {
  if (code === RATE_LIMIT_CODE) {
    return true;
  }
  return RATE_LIMIT_TEXT.test((message || "").trim());
};

export const normalizeApiErrorMessage = (message?: string, code?: number) => {
  if (isRateLimitError(code, message)) {
    return "操作过于频繁，请稍后再试";
  }
  return message;
};

export const toLocalDateTimeString = (value: unknown) => {
  if (value == null || value === "") {
    return undefined;
  }
  const raw = String(value).trim();
  if (!raw) {
    return undefined;
  }
  if (LOCAL_DATETIME_PATTERN.test(raw)) {
    return raw.replace("T", " ");
  }
  const date = value instanceof Date ? value : new Date(raw);
  if (Number.isNaN(date.getTime())) {
    return undefined;
  }
  return formatAsLocalDateTime(date);
};

export const toApiDateTimeString = (value: unknown) => {
  if (value == null || value === "") {
    return undefined;
  }
  const raw = String(value).trim();
  if (!raw) {
    return undefined;
  }
  const normalized = LOCAL_DATETIME_PATTERN.test(raw)
    ? raw.replace(" ", "T")
    : raw;
  const date = value instanceof Date ? value : new Date(normalized);
  if (Number.isNaN(date.getTime())) {
    return undefined;
  }
  return date.toISOString();
};

export const normalizeDateTime = (value: unknown, fallback = "-") => {
  return toLocalDateTimeString(value) ?? fallback;
};

export const venueNameToId = (venue?: string) => {
  if (!venue) {
    return undefined;
  }
  const text = String(venue).trim();
  if (!text) {
    return undefined;
  }
  const numeric = Number(text);
  if (Number.isFinite(numeric) && numeric > 0) {
    return numeric;
  }
  const matched = text.match(/(\d+)/);
  if (matched) {
    const parsed = Number(matched[1]);
    if (Number.isFinite(parsed) && parsed > 0) {
      return parsed;
    }
  }
  return undefined;
};

export const venueIdToName = (venueId?: number | null) => {
  if (venueId == null) {
    return "未知场馆";
  }
  return `${venueId}号场馆`;
};
