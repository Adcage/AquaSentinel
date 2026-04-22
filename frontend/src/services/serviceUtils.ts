interface ApiEnvelope<T> {
  code?: number;
  data?: T;
  message?: string;
}

interface ApiHttpResponse<T> {
  data?: ApiEnvelope<T>;
}

const SUCCESS_CODE = 0;

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
    throw new Error(payload.message || fallbackMessage);
  }
  return payload.data as T;
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
