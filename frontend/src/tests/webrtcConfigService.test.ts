import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { fetchWebrtcCandidateIps, resolveWebrtcCandidateIps, resetWebrtcConfigCache } from '@/services/webrtcConfigService'

describe('webrtcConfigService', () => {
  beforeEach(() => {
    resetWebrtcConfigCache()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.unstubAllEnvs()
  })

  describe('fetchWebrtcCandidateIps', () => {
    it('returns IPs from API when preferredIp is set', async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ code: 0, data: { preferredIp: '192.168.0.221' } }),
      })
      vi.stubGlobal('fetch', mockFetch)
      vi.stubEnv('VITE_WEBRTC_CANDIDATE_IPS', '')

      const ips = await fetchWebrtcCandidateIps()

      expect(ips).toEqual(['192.168.0.221'])
      expect(mockFetch).toHaveBeenCalledWith('/api/video-hub/webrtc-config')
    })

    it('falls back to VITE env when API returns empty preferredIp', async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ code: 0, data: { preferredIp: '' } }),
      })
      vi.stubGlobal('fetch', mockFetch)
      vi.stubEnv('VITE_WEBRTC_CANDIDATE_IPS', '10.0.0.1,10.0.0.2')

      const ips = await fetchWebrtcCandidateIps()

      expect(ips).toEqual(['10.0.0.1', '10.0.0.2'])
    })

    it('falls back to VITE env when API request fails', async () => {
      const mockFetch = vi.fn().mockRejectedValue(new Error('network error'))
      vi.stubGlobal('fetch', mockFetch)
      vi.stubEnv('VITE_WEBRTC_CANDIDATE_IPS', '192.168.0.181')

      const ips = await fetchWebrtcCandidateIps()

      expect(ips).toEqual(['192.168.0.181'])
    })

    it('returns empty array when both API and VITE env are unavailable', async () => {
      const mockFetch = vi.fn().mockRejectedValue(new Error('network error'))
      vi.stubGlobal('fetch', mockFetch)
      vi.stubEnv('VITE_WEBRTC_CANDIDATE_IPS', '')

      const ips = await fetchWebrtcCandidateIps()

      expect(ips).toEqual([])
    })

    it('caches result after first fetch', async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ code: 0, data: { preferredIp: '192.168.0.221' } }),
      })
      vi.stubGlobal('fetch', mockFetch)
      vi.stubEnv('VITE_WEBRTC_CANDIDATE_IPS', '')

      await fetchWebrtcCandidateIps()
      await fetchWebrtcCandidateIps()

      expect(mockFetch).toHaveBeenCalledTimes(1)
    })
  })

  describe('resolveWebrtcCandidateIps', () => {
    it('returns cached IPs after fetch', async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ code: 0, data: { preferredIp: '192.168.0.100' } }),
      })
      vi.stubGlobal('fetch', mockFetch)
      vi.stubEnv('VITE_WEBRTC_CANDIDATE_IPS', '')

      await fetchWebrtcCandidateIps()
      const ips = resolveWebrtcCandidateIps()

      expect(ips).toEqual(['192.168.0.100'])
    })

    it('returns empty array before fetch', () => {
      const ips = resolveWebrtcCandidateIps()
      expect(ips).toEqual([])
    })
  })
})
