import { beforeEach, describe, expect, it, vi } from 'vitest'

import { adminLogin, getCaptcha, logout, register } from '@/api/authController'
import {
  clearAuthSession,
  fetchCaptcha,
  getStoredAuthUser,
  loginAsAdmin,
  logoutCurrentUser,
  registerAccount,
} from '@/services/authService'

vi.mock('@/api/authController', () => ({
  adminLogin: vi.fn(),
  getCaptcha: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
}))

const mockedAdminLogin = vi.mocked(adminLogin)
const mockedGetCaptcha = vi.mocked(getCaptcha)
const mockedRegister = vi.mocked(register)
const mockedLogout = vi.mocked(logout)

describe('auth service', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    sessionStorage.clear()
    localStorage.clear()
  })

  it('fetches captcha image and id from backend', async () => {
    mockedGetCaptcha.mockResolvedValueOnce(
      {
        data: {
          code: 0,
          data: {
            captchaId: 'cpt-1001',
            captchaImageBase64: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB',
          },
        },
      } as never,
    )

    const captcha = await fetchCaptcha()

    expect(captcha.captchaId).toBe('cpt-1001')
    expect(captcha.imageDataUrl).toContain('data:image/png;base64,')
  })

  it('logs in admin and persists tokens/user in session storage', async () => {
    mockedAdminLogin.mockResolvedValueOnce(
      {
        data: {
          code: 0,
          data: {
            accessToken: 'access-token-1',
            refreshToken: 'refresh-token-1',
            user: {
              id: 11,
              username: 'admin_a',
              displayName: '系统管理员A',
              roles: ['SUPER_ADMIN'],
            },
          },
        },
      } as never,
    )

    const result = await loginAsAdmin({
      username: 'admin_a',
      password: '12345678',
      captchaId: 'cpt-1',
      captchaCode: 'ABCD',
    })

    expect(result.user?.username).toBe('admin_a')
    expect(sessionStorage.getItem('token')).toBe('access-token-1')
    expect(sessionStorage.getItem('refreshToken')).toBe('refresh-token-1')
    expect(getStoredAuthUser()?.displayName).toBe('系统管理员A')
  })

  it('registers account with backend role code mapping', async () => {
    mockedRegister.mockResolvedValueOnce(
      {
        data: {
          code: 0,
          data: 10001,
        },
      } as never,
    )

    const registerId = await registerAccount({
      displayName: '王五',
      username: 'wangwu',
      password: '12345678',
      role: 'venue_admin',
      captchaId: 'cpt-2',
      captchaCode: 'WXYZ',
    })

    expect(registerId).toBe(10001)
    expect(mockedRegister).toHaveBeenCalledWith(
      expect.objectContaining({
        roleCode: 'VENUE_ADMIN',
      }),
    )
  })

  it('logs out current user and clears local auth cache', async () => {
    sessionStorage.setItem('token', 'access-token-2')
    sessionStorage.setItem('refreshToken', 'refresh-token-2')
    sessionStorage.setItem('authUser', JSON.stringify({ username: 'admin_b' }))

    mockedLogout.mockResolvedValueOnce(
      {
        data: {
          code: 0,
          data: true,
        },
      } as never,
    )

    await logoutCurrentUser()

    expect(mockedLogout).toHaveBeenCalledWith(expect.objectContaining({ refreshToken: 'refresh-token-2' }))
    expect(sessionStorage.getItem('token')).toBeNull()
    expect(sessionStorage.getItem('authUser')).toBeNull()
  })

  it('clears auth session directly', () => {
    sessionStorage.setItem('token', 'access-token')
    sessionStorage.setItem('refreshToken', 'refresh-token')
    sessionStorage.setItem('authUser', JSON.stringify({ username: 'admin_c' }))

    clearAuthSession()

    expect(sessionStorage.getItem('token')).toBeNull()
    expect(sessionStorage.getItem('refreshToken')).toBeNull()
    expect(sessionStorage.getItem('authUser')).toBeNull()
  })
})
