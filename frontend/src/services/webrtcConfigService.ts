interface WebrtcConfigResponse {
  code?: number
  data?: { preferredIp?: string }
  message?: string
}

const IPV4_PATTERN = /^\d+\.\d+\.\d+\.\d+$/

const parseIpList = (raw: string): string[] => {
  if (!raw.trim()) {
    return []
  }
  return raw
    .split(',')
    .map((ip) => ip.trim())
    .filter((ip) => IPV4_PATTERN.test(ip))
}

let cachedIps: string[] | null = null
let fetchPromise: Promise<string[]> | null = null

const getFallbackIps = (): string[] => {
  const raw = String(import.meta.env.VITE_WEBRTC_CANDIDATE_IPS || '').trim()
  return parseIpList(raw)
}

export const fetchWebrtcCandidateIps = async (): Promise<string[]> => {
  if (cachedIps !== null) {
    return cachedIps
  }
  if (fetchPromise) {
    return fetchPromise
  }
  fetchPromise = (async () => {
    try {
      const response = await fetch('/api/video-hub/webrtc-config')
      if (response.ok) {
        const payload: WebrtcConfigResponse = await response.json()
        if (payload.code === 0 && payload.data?.preferredIp) {
          const ips = parseIpList(payload.data.preferredIp)
          if (ips.length > 0) {
            cachedIps = ips
            return cachedIps
          }
        }
      }
    } catch {}
    const fallback = getFallbackIps()
    cachedIps = fallback
    return cachedIps
  })()
  fetchPromise.finally(() => {
    fetchPromise = null
  })
  return fetchPromise
}

export const resolveWebrtcCandidateIps = (): string[] => {
  return cachedIps ?? []
}

export const resetWebrtcConfigCache = () => {
  cachedIps = null
  fetchPromise = null
}
