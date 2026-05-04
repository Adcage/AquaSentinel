import { describe, expect, it } from 'vitest'

import { normalizeDeviceBaseUrl, buildDeviceStreamUrl } from '@/utils/ptzDirectControl'

describe('ptz direct control url', () => {
  it('normalizes bare ip to http base url', () => {
    expect(normalizeDeviceBaseUrl('192.168.137.175')).toBe('http://192.168.137.175')
  })

  it('keeps explicit scheme and port', () => {
    expect(normalizeDeviceBaseUrl('http://192.168.137.175:8080/')).toBe('http://192.168.137.175:8080')
  })

  it('builds stream endpoint from input ip', () => {
    expect(buildDeviceStreamUrl('192.168.137.175')).toBe('http://192.168.137.175/stream')
  })
})
