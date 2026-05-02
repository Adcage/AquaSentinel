import { describe, expect, it } from 'vitest'

import {
  isRateLimitError,
  normalizeApiErrorMessage,
  normalizeDateTime,
  toLocalDateTimeString,
  unwrapApiData,
} from '@/services/serviceUtils'

const toExpectedLocalDateTime = (value: string | Date) => {
  const date = value instanceof Date ? value : new Date(value)
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

describe('serviceUtils time formatter', () => {
  it('converts UTC time string into local datetime string', () => {
    const utc = '2026-04-01T06:00:00.000Z'

    expect(normalizeDateTime(utc)).toBe(toExpectedLocalDateTime(utc))
  })

  it('keeps plain datetime string unchanged', () => {
    expect(normalizeDateTime('2026-04-01 14:00:00')).toBe('2026-04-01 14:00:00')
  })

  it('formats Date instance for backend query in local datetime format', () => {
    const value = new Date('2026-04-01T06:00:00.000Z')

    expect(toLocalDateTimeString(value)).toBe(toExpectedLocalDateTime(value))
  })
})

describe('serviceUtils rate limit error', () => {
  it('recognizes rate limit by business code', () => {
    expect(isRateLimitError(40301, '任意文案')).toBe(true)
  })

  it('normalizes rate limit message when unwrap envelope failed', () => {
    expect(() =>
      unwrapApiData(
        { data: { code: 40301, message: '设备操作请求过于频繁' } },
        '批量禁用失败',
      ),
    ).toThrow('操作过于频繁，请稍后再试')
  })

  it('keeps non-rate-limit messages unchanged', () => {
    expect(normalizeApiErrorMessage('普通业务异常', 50000)).toBe('普通业务异常')
  })
})
