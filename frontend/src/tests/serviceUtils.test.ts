import { describe, expect, it } from 'vitest'

import { normalizeDateTime, toLocalDateTimeString } from '@/services/serviceUtils'

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
