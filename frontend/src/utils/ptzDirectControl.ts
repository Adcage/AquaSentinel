const trimTrailingSlash = (value: string) => value.replace(/\/+$/, '')

export const normalizeDeviceBaseUrl = (input: string): string => {
  const raw = String(input || '').trim()
  if (!raw) {
    return ''
  }
  const withScheme = /^https?:\/\//i.test(raw) ? raw : `http://${raw}`
  return trimTrailingSlash(withScheme)
}

export const buildDeviceStreamUrl = (input: string): string => {
  const baseUrl = normalizeDeviceBaseUrl(input)
  return baseUrl ? `${baseUrl}/stream` : ''
}

export const buildDeviceApiUrl = (input: string, path: string): string => {
  const baseUrl = normalizeDeviceBaseUrl(input)
  if (!baseUrl) {
    return ''
  }
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${baseUrl}${normalizedPath}`
}
