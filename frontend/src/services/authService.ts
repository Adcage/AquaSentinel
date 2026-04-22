import { adminLogin, getCaptcha, logout, register } from '@/api/authController'
import { unwrapApiData } from '@/services/serviceUtils'

const TOKEN_KEY = 'token'
const REFRESH_TOKEN_KEY = 'refreshToken'
const AUTH_USER_KEY = 'authUser'

type RegisterRole = 'super_admin' | 'venue_admin' | 'viewer'

interface LoginParams {
  username: string
  password: string
  captchaId: string
  captchaCode: string
}

interface RegisterParams {
  displayName: string
  username: string
  password: string
  role: RegisterRole
  captchaId: string
  captchaCode: string
}

interface CaptchaResult {
  captchaId: string
  imageDataUrl: string
}

const roleCodeMap: Record<RegisterRole, string> = {
  super_admin: 'SUPER_ADMIN',
  venue_admin: 'VENUE_ADMIN',
  viewer: 'USER',
}

const buildCaptchaImage = (imageBase64?: string) => {
  if (!imageBase64) {
    return ''
  }
  if (imageBase64.startsWith('data:image')) {
    return imageBase64
  }
  return `data:image/png;base64,${imageBase64}`
}

const getDeviceId = () => {
  const raw = `${navigator.userAgent}-${navigator.language}`
  return raw.slice(0, 120)
}

export const clearAuthSession = () => {
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(REFRESH_TOKEN_KEY)
  sessionStorage.removeItem(AUTH_USER_KEY)
}

export const getToken = (): string => {
  return sessionStorage.getItem(TOKEN_KEY) || ""
}

export const getStoredAuthUser = (): API.UserInfo | null => {
  const raw = sessionStorage.getItem(AUTH_USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as API.UserInfo
  } catch {
    return null
  }
}

export const fetchCaptcha = async (): Promise<CaptchaResult> => {
  const response = await getCaptcha()
  const data = unwrapApiData<API.CaptchaVO>(response, '获取验证码失败')

  return {
    captchaId: data?.captchaId || '',
    imageDataUrl: buildCaptchaImage(data?.captchaImageBase64),
  }
}

export const loginAsAdmin = async (params: LoginParams): Promise<API.LoginResultVO> => {
  const response = await adminLogin({
    username: params.username,
    password: params.password,
    captchaId: params.captchaId,
    captchaCode: params.captchaCode,
    deviceId: getDeviceId(),
    clientType: 'WEB',
    clientVersion: '1.0.0',
  })

  const data = unwrapApiData<API.LoginResultVO>(response, '登录失败')
  sessionStorage.setItem(TOKEN_KEY, data?.accessToken || '')
  sessionStorage.setItem(REFRESH_TOKEN_KEY, data?.refreshToken || '')
  sessionStorage.setItem(AUTH_USER_KEY, JSON.stringify(data?.user || null))
  return data
}

export const registerAccount = async (params: RegisterParams): Promise<number> => {
  const response = await register({
    displayName: params.displayName,
    username: params.username,
    password: params.password,
    roleCode: roleCodeMap[params.role],
    captchaId: params.captchaId,
    captchaCode: params.captchaCode,
  })
  return unwrapApiData<number>(response, '注册失败')
}

export const logoutCurrentUser = async () => {
  const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY) || undefined
  await logout({
    refreshToken,
    deviceId: getDeviceId(),
  }).catch(() => undefined)
  clearAuthSession()
}
