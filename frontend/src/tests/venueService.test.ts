import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  createVenue,
  getVenuePage,
  removeVenue,
  updateVenueInfo,
} from '@/services/venueService'
import {
  addVenue,
  deleteVenue,
  editVenue,
  listVenueVoByPage,
} from '@/api/venueController'

vi.mock('@/api/venueController', () => ({
  listVenueVoByPage: vi.fn(),
  addVenue: vi.fn(),
  editVenue: vi.fn(),
  deleteVenue: vi.fn(),
}))

const mockedListVenueVoByPage = vi.mocked(listVenueVoByPage)
const mockedAddVenue = vi.mocked(addVenue)
const mockedEditVenue = vi.mocked(editVenue)
const mockedDeleteVenue = vi.mocked(deleteVenue)

describe('venueService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads venue page with normalized rows', async () => {
    mockedListVenueVoByPage.mockResolvedValueOnce({
      data: {
        code: 0,
        data: {
          records: [
            {
              id: 7,
              venueCode: 'SH-001',
              venueName: '东馆',
              address: '一层东侧',
              managerUserId: 1001,
            },
          ],
          total: 1,
          current: 1,
          size: 20,
        },
      },
    } as never)

    const page = await getVenuePage({ current: 1, pageSize: 20, keyword: '东' })

    expect(mockedListVenueVoByPage).toHaveBeenCalledWith(
      expect.objectContaining({
        current: 1,
        pageSize: 20,
        venueName: '东',
      }),
    )
    expect(page.total).toBe(1)
    expect(page.list[0]).toEqual(
      expect.objectContaining({
        id: 7,
        venueCode: 'SH-001',
        venueName: '东馆',
        location: '一层东侧',
      }),
    )
  })

  it('creates venue and returns created id', async () => {
    mockedAddVenue.mockResolvedValueOnce({
      data: {
        code: 0,
        data: 88,
      },
    } as never)

    const id = await createVenue({
      venueCode: 'SH-PD-001',
      venueName: '新馆',
      location: '南区',
      managerUserId: 9,
    })

    expect(mockedAddVenue).toHaveBeenCalledWith(
      expect.objectContaining({
        venueCode: 'SH-PD-001',
        venueName: '新馆',
        address: '南区',
        managerUserId: 9,
      }),
    )
    expect(id).toBe(88)
  })

  it('updates venue with edit api', async () => {
    mockedEditVenue.mockResolvedValueOnce({
      data: {
        code: 0,
        data: true,
      },
    } as never)

    await updateVenueInfo(12, {
      venueCode: 'SH-012',
      venueName: '东馆-改',
      location: 'B2',
    })

    expect(mockedEditVenue).toHaveBeenCalledWith(
      expect.objectContaining({
        id: 12,
        venueCode: 'SH-012',
        venueName: '东馆-改',
        address: 'B2',
      }),
    )
  })

  it('removes venue with delete request body', async () => {
    mockedDeleteVenue.mockResolvedValueOnce({
      data: {
        code: 0,
        data: true,
      },
    } as never)

    await removeVenue(35)

    expect(mockedDeleteVenue).toHaveBeenCalledWith({ id: 35 })
  })

  it('throws when delete api returns false', async () => {
    mockedDeleteVenue.mockResolvedValueOnce({
      data: {
        code: 0,
        data: false,
      },
    } as never)

    await expect(removeVenue(99)).rejects.toThrow('删除场馆失败')
  })
})
