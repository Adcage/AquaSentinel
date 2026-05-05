import { afterEach, describe, expect, it, vi } from 'vitest'

import { controlHome, setCalibrationPanPulse } from '@/services/ptzControlService'

describe('ptzControlService error handling', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('throws device message when PTZ command returned ok false', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          ok: false,
          command: 'CALIB_EXIT',
          raw: 'ERR:BAD_CMD',
          message: '设备未成功退出校准模式，请确认固件版本后重试',
        }),
      }),
    )

    await expect(controlHome('192.168.137.175')).rejects.toThrow('设备未成功退出校准模式，请确认固件版本后重试')
  })

  it('throws limit message when calibration pulse exceeds safety range', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          ok: false,
          command: 'CALIB_PAN',
          raw: 'ERR:LIMIT',
          message: 'PAN 校准脉宽不能超过 2350us，请先回到安全位置后重试',
        }),
      }),
    )

    await expect(setCalibrationPanPulse('192.168.137.175', 2400)).rejects.toThrow(
      'PAN 校准脉宽不能超过 2350us，请先回到安全位置后重试',
    )
  })
})
