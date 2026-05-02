import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  action,
  listByPage,
} from '@/api/alertActionController'
import {
  addCameraDevice,
  deleteCameraDevice,
  listCameraDeviceVoByPage,
  updateCameraDevice,
} from '@/api/cameraDeviceController'
import { listLifeguardVoByPage } from '@/api/lifeguardController'
import { getOverview, ranking } from '@/api/statsController'
import { listUserPageVo } from '@/api/userController'
import { listVenueVoByPage } from '@/api/venueController'
import { getAlarmPage, markAlarmsResolved } from '@/services/alarmService'
import { getCameraGrid, getDashboardMetrics } from '@/services/dashboardService'
import { createDevice, getDevicePage, removeDevice, updateDevice } from '@/services/deviceService'
import { getLifeguardPage } from '@/services/lifeguardService'
import {
  getAlarmTrend,
  getAlarmTypeDistribution,
  getStatisticsKpi,
  getVenueRanking,
} from '@/services/statisticsService'
import { getUserPage } from '@/services/userService'

vi.mock('@/api/statsController', () => ({
  getOverview: vi.fn(),
  trend: vi.fn(),
  ranking: vi.fn(),
}))

vi.mock('@/api/cameraDeviceController', () => ({
  listCameraDeviceVoByPage: vi.fn(),
  addCameraDevice: vi.fn(),
  updateCameraDevice: vi.fn(),
  deleteCameraDevice: vi.fn(),
}))

vi.mock('@/api/alertActionController', () => ({
  listByPage: vi.fn(),
  action: vi.fn(),
}))

vi.mock('@/api/lifeguardController', () => ({
  listLifeguardVoByPage: vi.fn(),
}))

vi.mock('@/api/userController', () => ({
  listUserPageVo: vi.fn(),
}))

vi.mock('@/api/venueController', () => ({
  listVenueVoByPage: vi.fn(),
}))

const mockedGetOverview = vi.mocked(getOverview)
const mockedRanking = vi.mocked(ranking)
const mockedListCameraDeviceVoByPage = vi.mocked(listCameraDeviceVoByPage)
const mockedAddCameraDevice = vi.mocked(addCameraDevice)
const mockedUpdateCameraDevice = vi.mocked(updateCameraDevice)
const mockedDeleteCameraDevice = vi.mocked(deleteCameraDevice)
const mockedListByPage = vi.mocked(listByPage)
const mockedAction = vi.mocked(action)
const mockedListLifeguardVoByPage = vi.mocked(listLifeguardVoByPage)
const mockedListUserPageVo = vi.mocked(listUserPageVo)
const mockedListVenueVoByPage = vi.mocked(listVenueVoByPage)

const ok = <T>(data: T) => ({
  data: {
    code: 0,
    data,
    message: 'ok',
  },
})

describe('services real api integration', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('maps dashboard metrics and camera grid from backend response', async () => {
    mockedGetOverview.mockResolvedValueOnce(
      ok({
        onlineDeviceCount: 20,
        todayAlertCount: 9,
        pendingAlertCount: 3,
        onDutyLifeguardCount: 12,
        currentPoolHeadCount: 88,
      }) as never,
    )

    mockedListCameraDeviceVoByPage.mockResolvedValueOnce(
      ok({
        records: [
          {
            id: 1001,
            cameraName: 'A馆东北角',
            streamUrl: 'rtsp://cam-1001',
            venueId: 1,
            zoneId: 3,
            deviceStatus: 'ONLINE',
          },
        ],
        total: 1,
      }) as never,
    )

    const metrics = await getDashboardMetrics()
    const cameraGrid = await getCameraGrid()

    expect(metrics).toMatchObject({
      onlineDeviceCount: 20,
      todayAlarmCount: 9,
      pendingAlarmCount: 3,
      onDutyLifeguardCount: 12,
      realtimeSwimmerCount: 88,
    })

    expect(cameraGrid[0]).toMatchObject({
      id: '1001',
      name: 'A馆东北角',
      location: '1号场馆-区域3',
      peopleCount: 0,
      isAlarming: false,
    })
  })

  it('maps alarm list and resolves alarms via backend action api', async () => {
    mockedListByPage.mockResolvedValueOnce(
      ok({
        records: [
          {
            id: 301,
            alertUid: 'ALERT-301',
            alertType: 'DROWNING',
            alertStatus: 'PENDING',
            incidentLocation: 'A馆-深水区',
            emergencyContactName: '值班室',
            emergencyContactPhone: '13800000000',
            createdAt: '2026-03-24T09:30:00',
          },
        ],
        total: 1,
      }) as never,
    )

    mockedAction.mockResolvedValue(ok({ status: 'DONE' }) as never)

    const page = await getAlarmPage({
      current: 1,
      pageSize: 20,
      keyword: 'ALERT',
      status: 'pending',
      type: 'drowning',
    })

    expect(mockedListByPage).toHaveBeenCalledWith(
      expect.objectContaining({
        sortField: 'created_at',
        sortOrder: 'descend',
      }),
    )

    expect(page.total).toBe(1)
    expect(page.list[0]).toMatchObject({
      id: 'ALERT-301',
      type: 'drowning',
      status: 'pending',
      cameraLocation: 'A馆-深水区',
    })

    await markAlarmsResolved([
      page.list[0],
      {
        ...page.list[0],
        id: 'ALERT-302',
        dbId: 302,
      },
    ])
    expect(mockedAction).toHaveBeenCalledTimes(2)
  })

  it('maps device list and CRUD payloads to camera api', async () => {
    mockedListCameraDeviceVoByPage.mockResolvedValueOnce(
      ok({
        records: [
          {
            id: 501,
            cameraName: 'B馆跳台区',
            streamUrl: 'rtsp://cam-501',
            venueId: 2,
            zoneId: 6,
            protocol: 'PTZ',
            deviceStatus: 'OFFLINE',
          },
        ],
        total: 1,
      }) as never,
    )
    mockedAddCameraDevice.mockResolvedValue(ok(9001) as never)
    mockedUpdateCameraDevice.mockResolvedValue(ok(true) as never)
    mockedDeleteCameraDevice.mockResolvedValue(ok(true) as never)

    const page = await getDevicePage({
      current: 1,
      pageSize: 20,
      venue: 'B馆',
      status: 'offline',
      deviceType: 'ptz',
    })

    expect(page.list[0]).toMatchObject({
      id: '501',
      name: 'B馆跳台区',
      status: 'offline',
      deviceType: 'ptz',
    })

    await createDevice({
      name: '新设备',
      location: 'C馆东侧',
      venue: 'C馆',
      deviceType: 'fixed',
      streamUrl: 'rtsp://new',
      status: 'online',
      maintenanceCycleDays: 30,
      enabled: 1,
    })
    await updateDevice('501', {
      name: '新设备2',
      location: 'C馆西侧',
      venue: 'C馆',
      deviceType: 'ptz',
      streamUrl: 'rtsp://new2',
      status: 'error',
      maintenanceCycleDays: 7,
      enabled: 1,
    })
    await removeDevice('501')

    expect(mockedAddCameraDevice).toHaveBeenCalledTimes(1)
    expect(mockedUpdateCameraDevice).toHaveBeenCalledTimes(1)
    expect(mockedDeleteCameraDevice).toHaveBeenCalledTimes(1)
  })

  it('maps lifeguard and user pages from backend response', async () => {
    mockedListLifeguardVoByPage.mockResolvedValueOnce(
      ok({
        records: [
          {
            id: 701,
            fullName: '李娜',
            phone: '13800001111',
            venueId: 2,
            dutyStatus: 'ON_DUTY',
            updatedAt: '2026-03-24T10:00:00',
          },
        ],
        total: 1,
      }) as never,
    )

    mockedListUserPageVo.mockResolvedValueOnce(
      ok({
        records: [
          {
            id: 801,
            username: 'admin_root',
            displayName: '系统管理员',
            status: 1,
            roleCodes: ['SUPER_ADMIN'],
          },
        ],
        total: 1,
      }) as never,
    )

    const lifeguardPage = await getLifeguardPage({
      current: 1,
      pageSize: 20,
      venue: 'B馆',
      dutyStatus: 'on_duty',
    })

    expect(mockedListLifeguardVoByPage).toHaveBeenCalledWith(
      expect.objectContaining({
        sortField: 'updated_at',
        sortOrder: 'descend',
      }),
    )

    const userPage = await getUserPage({
      current: 1,
      pageSize: 20,
      role: 'super_admin',
      status: 'enabled',
    })

    expect(lifeguardPage.list[0]).toMatchObject({
      name: '李娜',
      dutyStatus: 'on_duty',
    })

    expect(userPage.list[0]).toMatchObject({
      account: 'admin_root',
      name: '系统管理员',
      role: 'super_admin',
      status: 'enabled',
    })
  })

  it('maps LIFEGUARD role to lifeguard and sends lifeguard query role code', async () => {
    mockedListUserPageVo.mockResolvedValueOnce(
      ok({
        records: [
          {
            id: 901,
            username: 'lg.demo',
            displayName: '救生员演示',
            status: 1,
            roleCodes: ['LIFEGUARD'],
          },
        ],
        total: 1,
      }) as never,
    )

    const userPage = await getUserPage({
      current: 1,
      pageSize: 20,
      role: 'lifeguard',
      status: 'enabled',
    })

    expect(mockedListUserPageVo).toHaveBeenCalledWith(
      expect.objectContaining({ roleCode: 'LIFEGUARD' }),
    )
    expect(userPage.list[0]).toMatchObject({
      account: 'lg.demo',
      name: '救生员演示',
      role: 'lifeguard',
      status: 'enabled',
    })
  })

  it('maps statistics datasets from backend responses', async () => {
    mockedGetOverview.mockResolvedValueOnce(
      ok({
        todayAlertCount: 16,
        pendingAlertCount: 4,
      }) as never,
    )

    mockedRanking.mockResolvedValueOnce(
      ok({
        items: [
          { venueId: 1, alertCount: 12 },
          { venueId: 2, alertCount: 8 },
        ],
      }) as never,
    )

    mockedListVenueVoByPage.mockResolvedValueOnce(
      ok({
        records: [
          { id: 1, venueName: 'A馆' },
          { id: 2, venueName: 'B馆' },
        ],
        total: 2,
      }) as never,
    )

    mockedListByPage
      .mockResolvedValueOnce(
        ok({
          records: [
            { createdAt: '2026-03-21T12:00:00', alertType: 'DROWNING' },
            { createdAt: '2026-03-21T13:00:00', alertType: 'DROWNING' },
            { createdAt: '2026-03-22T09:00:00', alertType: 'CROSS_BORDER' },
          ],
          total: 3,
        }) as never,
      )
      .mockResolvedValueOnce(
        ok({
          records: [
            { createdAt: '2026-03-21T12:00:00', alertType: 'DROWNING' },
            { createdAt: '2026-03-21T13:00:00', alertType: 'DROWNING' },
            { createdAt: '2026-03-22T09:00:00', alertType: 'CROSS_BORDER' },
          ],
          total: 3,
        }) as never,
      )

    const kpi = await getStatisticsKpi()
    const alarmTrend = await getAlarmTrend()
    const venueRanking = await getVenueRanking()
    const distribution = await getAlarmTypeDistribution()

    expect(kpi).toEqual({
      alarmTotal: 16,
      resolvedRate: 75,
      avgResponseSeconds: 0,
      highRiskVenueCount: 0,
    })
    expect(alarmTrend).toEqual([
      { month: '03-21', value: 2 },
      { month: '03-22', value: 1 },
    ])
    expect(venueRanking).toEqual([
      { month: 'A馆', value: 12 },
      { month: 'B馆', value: 8 },
    ])
    expect(distribution).toEqual([
      { name: '溺水', value: 2 },
      { name: '越界', value: 1 },
    ])
  })

  it('throws when backend response code is not success', async () => {
    mockedGetOverview.mockResolvedValueOnce(
      {
        data: {
          code: 50000,
          message: '服务异常',
        },
      } as never,
    )

    await expect(getStatisticsKpi()).rejects.toThrow('服务异常')
  })
})
