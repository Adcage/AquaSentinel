import { beforeEach, describe, expect, it, vi } from 'vitest'

import { listByPage } from '@/api/alertActionController'
import { getAlarmTrend } from '@/services/statisticsService'

vi.mock('@/api/statsController', () => ({
  getOverview: vi.fn(),
  ranking: vi.fn(),
}))

vi.mock('@/api/venueController', () => ({
  listVenueVoByPage: vi.fn(),
}))

vi.mock('@/api/alertActionController', () => ({
  listByPage: vi.fn(),
}))

const mockedListByPage = vi.mocked(listByPage)

describe('statistics service time range', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('sends local datetime range when querying alert trend', async () => {
    mockedListByPage.mockResolvedValueOnce({
      data: {
        code: 0,
        data: {
          records: [],
          total: 0,
        },
      },
    } as never)

    await getAlarmTrend({
      startDate: '2026-04-01',
      endDate: '2026-04-01',
    })

    expect(mockedListByPage).toHaveBeenCalledWith(
      expect.objectContaining({
        startCreatedAt: expect.stringMatching(/T/),
        endCreatedAt: expect.stringMatching(/T/),
      }),
    )
  })
})
