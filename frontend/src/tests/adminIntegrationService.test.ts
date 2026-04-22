import { beforeEach, describe, expect, it, vi } from 'vitest'

import { listCameraMaintenanceLogVoByPage } from '@/api/cameraMaintenanceController'
import { addRole, deleteRole, listRolePageVo } from '@/api/roleController'
import { exportCsv, exportExcel } from '@/api/statsController'
import { listSystemAuditLogVoByPage } from '@/api/systemAuditLogController'
import {
  copyCoreRole,
  deleteCoreRole,
  getDeviceMaintenancePage,
  getDeviceMaintenanceRows,
  getCoreRoleItems,
  getSystemLogPage,
  getRolePermissionTree,
  getSystemLogRows,
  requestStatsExport,
} from '@/services/adminIntegrationService'

vi.mock('@/api/cameraMaintenanceController', () => ({
  listCameraMaintenanceLogVoByPage: vi.fn(),
}))

vi.mock('@/api/roleController', () => ({
  addRole: vi.fn(),
  deleteRole: vi.fn(),
  listRolePageVo: vi.fn(),
}))

vi.mock('@/api/statsController', () => ({
  exportCsv: vi.fn(),
  exportExcel: vi.fn(),
}))

vi.mock('@/api/systemAuditLogController', () => ({
  listSystemAuditLogVoByPage: vi.fn(),
}))

const mockedListMaintenance = vi.mocked(listCameraMaintenanceLogVoByPage)
const mockedListRoles = vi.mocked(listRolePageVo)
const mockedAddRole = vi.mocked(addRole)
const mockedDeleteRole = vi.mocked(deleteRole)
const mockedExportCsv = vi.mocked(exportCsv)
const mockedExportExcel = vi.mocked(exportExcel)
const mockedListAuditLogs = vi.mocked(listSystemAuditLogVoByPage)

describe('admin integration service', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('maps maintenance logs from camera maintenance api', async () => {
    mockedListMaintenance.mockResolvedValueOnce(
      {
        data: {
          code: 0,
          data: {
            records: [
              {
                id: 11,
                cameraId: 1001,
                maintenanceContent: '清洁镜头',
                maintainedBy: '王工',
                maintainedAt: '2026-03-24T10:00:00',
              },
            ],
          },
        },
      } as never,
    )

    const rows = await getDeviceMaintenanceRows()

    expect(rows[0]).toEqual({
      deviceName: '设备#1001',
      content: '清洁镜头',
      operator: '王工',
      time: '2026-03-24 10:00:00',
    })
  })

  it('maps maintenance logs with paging metadata', async () => {
    mockedListMaintenance.mockResolvedValueOnce(
      {
        data: {
          code: 0,
          data: {
            current: 2,
            size: 10,
            total: 35,
            records: [
              {
                id: 21,
                cameraId: 2001,
                maintenanceContent: '更换电源模块',
                maintainedBy: '赵工',
                maintainedAt: '2026-03-24T11:00:00',
              },
            ],
          },
        },
      } as never,
    )

    const page = await getDeviceMaintenancePage({ current: 2, pageSize: 10 })

    expect(mockedListMaintenance).toHaveBeenCalledWith(
      expect.objectContaining({
        sortField: 'maintained_at',
        sortOrder: 'descend',
      }),
    )
    expect(page.total).toBe(35)
    expect(page.current).toBe(2)
    expect(page.pageSize).toBe(10)
    expect(page.rows[0]).toEqual({
      deviceName: '设备#2001',
      content: '更换电源模块',
      operator: '赵工',
      time: '2026-03-24 11:00:00',
    })
  })

  it('maps role permission tree from role vo list', async () => {
    mockedListRoles.mockResolvedValueOnce(
      {
        data: {
          code: 0,
          data: {
            records: [
              {
                id: 1,
                roleCode: 'SUPER_ADMIN',
                roleName: '超级管理员',
                permissionJson: {
                  permissions: ['user:read', 'user:update'],
                },
              },
              {
                id: 2,
                roleCode: 'REPORT_OPERATOR',
                roleName: '报表专员',
                permissionJson: {
                  permissions: ['stats:export'],
                },
              },
            ],
          },
        },
      } as never,
    )

    const tree = await getRolePermissionTree()

    expect(tree).toEqual([
      {
        id: 'ADMIN',
        label: '管理员',
      },
      {
        id: 'LIFEGUARD',
        label: '救生员',
      },
    ])
  })

  it('maps core role entries from role list', async () => {
    mockedListRoles.mockResolvedValueOnce(
      {
        data: {
          code: 0,
          data: {
            records: [
              {
                id: 2,
                roleCode: 'VENUE_ADMIN',
                roleName: '场馆管理员',
                permissionJson: ['dashboard:view', 'camera:list'],
                status: 1,
              },
              {
                id: 3,
                roleCode: 'LIFEGUARD',
                roleName: '救生员',
                permissionJson: ['alert:receive'],
                status: 1,
              },
            ],
          },
        },
      } as never,
    )

    const items = await getCoreRoleItems()

    expect(items).toEqual([
      {
        key: 'ADMIN',
        label: '管理员',
        roleId: 2,
        roleCode: 'VENUE_ADMIN',
        roleName: '场馆管理员',
        permissions: ['dashboard:view', 'camera:list'],
        status: 1,
      },
      {
        key: 'LIFEGUARD',
        label: '救生员',
        roleId: 3,
        roleCode: 'LIFEGUARD',
        roleName: '救生员',
        permissions: ['alert:receive'],
        status: 1,
      },
    ])
  })

  it('copies selected core role as new role', async () => {
    mockedListRoles.mockResolvedValueOnce(
      {
        data: {
          code: 0,
          data: {
            records: [
              {
                id: 2,
                roleCode: 'VENUE_ADMIN',
                roleName: '场馆管理员',
                permissionJson: ['dashboard:view', 'camera:list'],
                status: 1,
              },
            ],
          },
        },
      } as never,
    )
    mockedAddRole.mockResolvedValueOnce({ data: { code: 0, data: 101 } } as never)

    const createdId = await copyCoreRole('ADMIN', 'VENUE_ADMIN_COPY', '管理员副本')

    expect(createdId).toBe(101)
    expect(mockedAddRole).toHaveBeenCalledWith({
      roleCode: 'VENUE_ADMIN_COPY',
      roleName: '管理员副本',
      permissions: ['dashboard:view', 'camera:list'],
      status: 1,
    })
  })

  it('deletes selected core role by id', async () => {
    mockedListRoles.mockResolvedValueOnce(
      {
        data: {
          code: 0,
          data: {
            records: [
              {
                id: 3,
                roleCode: 'LIFEGUARD',
                roleName: '救生员',
                permissionJson: ['alert:receive'],
                status: 1,
              },
            ],
          },
        },
      } as never,
    )
    mockedDeleteRole.mockResolvedValueOnce({ data: { code: 0, data: true } } as never)

    const result = await deleteCoreRole('LIFEGUARD')

    expect(result).toBe(true)
    expect(mockedDeleteRole).toHaveBeenCalledWith({ id: 3 })
  })

  it('requests stats export and returns export record row', async () => {
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    const appendSpy = vi.spyOn(document.body, 'appendChild')
    const removeSpy = vi.spyOn(document.body, 'removeChild')
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      blob: async () => new Blob(['csv-content']),
    })
    vi.stubGlobal('fetch', fetchMock)
    const originalCreateObjectURL = URL.createObjectURL
    const originalRevokeObjectURL = URL.revokeObjectURL
    const createObjectURLMock = vi.fn(() => 'blob:mock-url')
    const revokeObjectURLMock = vi.fn()
    Object.defineProperty(URL, 'createObjectURL', {
      value: createObjectURLMock,
      configurable: true,
      writable: true,
    })
    Object.defineProperty(URL, 'revokeObjectURL', {
      value: revokeObjectURLMock,
      configurable: true,
      writable: true,
    })

    sessionStorage.setItem('token', 'test-token')

    mockedExportCsv.mockResolvedValueOnce(
      {
        data: {
          code: 0,
          data: {
            fileName: 'stats_export_1001.csv',
            downloadUrl: '/files/exports/stats/stats_export_1001.csv',
            requestedAt: '2026-03-24T11:00:00',
          },
        },
      } as never,
    )

    mockedExportExcel.mockResolvedValueOnce(
      {
        data: {
          code: 0,
          data: {
            fileName: 'stats_export_1002.xlsx',
            downloadUrl: '/files/exports/stats/stats_export_1002.xlsx',
            requestedAt: '2026-03-24T11:10:00',
          },
        },
      } as never,
    )

    const csvRow = await requestStatsExport('csv', { metricType: 'ALERT' }, '系统管理员')
    const excelRow = await requestStatsExport('excel', { metricType: 'ALERT' }, '系统管理员')

    expect(csvRow).toEqual({
      name: 'stats_export_1001.csv',
      type: 'CSV',
      operator: '系统管理员',
      createdAt: '2026-03-24 11:00:00',
    })

    expect(excelRow).toEqual({
      name: 'stats_export_1002.xlsx',
      type: 'Excel',
      operator: '系统管理员',
      createdAt: '2026-03-24 11:10:00',
    })

    expect(clickSpy).toHaveBeenCalledTimes(2)
    expect(appendSpy).toHaveBeenCalledTimes(2)
    expect(removeSpy).toHaveBeenCalledTimes(2)
    expect(createObjectURLMock).toHaveBeenCalledTimes(2)
    expect(revokeObjectURLMock).toHaveBeenCalledTimes(2)

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      '/api/files/exports/stats/stats_export_1001.csv',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer test-token' }),
      }),
    )
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      '/api/files/exports/stats/stats_export_1002.xlsx',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer test-token' }),
      }),
    )

    const firstAnchor = appendSpy.mock.calls[0][0] as HTMLAnchorElement
    const secondAnchor = appendSpy.mock.calls[1][0] as HTMLAnchorElement
    expect(firstAnchor.download).toBe('stats_export_1001.csv')
    expect(secondAnchor.download).toBe('stats_export_1002.xlsx')

    clickSpy.mockRestore()
    appendSpy.mockRestore()
    removeSpy.mockRestore()
    Object.defineProperty(URL, 'createObjectURL', {
      value: originalCreateObjectURL,
      configurable: true,
      writable: true,
    })
    Object.defineProperty(URL, 'revokeObjectURL', {
      value: originalRevokeObjectURL,
      configurable: true,
      writable: true,
    })
  })

  it('maps system logs for setting page table', async () => {
    mockedListAuditLogs.mockResolvedValueOnce(
      {
        data: {
          code: 0,
          data: {
            records: [
              {
                id: 99,
                createdAt: '2026-03-24T08:11:22',
                operatorName: '系统管理员',
                requestUri: '/users/update/my',
                responseCode: 200,
              },
            ],
          },
        },
      } as never,
    )

    const rows = await getSystemLogRows({})

    expect(rows[0]).toEqual({
      time: '2026-03-24 08:11:22',
      operator: '系统管理员',
      action: '/users/update/my',
      result: '成功',
    })
  })

  it('maps paged system logs with total and current info', async () => {
    mockedListAuditLogs.mockResolvedValueOnce(
      {
        data: {
          code: 0,
          data: {
            current: 2,
            size: 20,
            total: 45,
            records: [
              {
                id: 101,
                createdAt: '2026-03-24 14:11:22',
                operatorName: '系统管理员',
                requestUri: '/api/alerts/action',
                responseCode: 0,
              },
            ],
          },
        },
      } as never,
    )

    const page = await getSystemLogPage({ current: 2, pageSize: 20 })

    expect(mockedListAuditLogs).toHaveBeenCalledWith(
      expect.objectContaining({
        sortField: 'created_at',
        sortOrder: 'descend',
      }),
    )
    expect(page.total).toBe(45)
    expect(page.current).toBe(2)
    expect(page.pageSize).toBe(20)
    expect(page.rows).toEqual([
      {
        time: '2026-03-24 14:11:22',
        operator: '系统管理员',
        action: '/api/alerts/action',
        result: '成功',
      },
    ])
  })

  it('converts system log date filters to ISO datetime for backend parsing', async () => {
    mockedListAuditLogs.mockResolvedValueOnce(
      {
        data: {
          code: 0,
          data: {
            current: 1,
            size: 20,
            total: 0,
            records: [],
          },
        },
      } as never,
    )

    await getSystemLogPage({
      current: 1,
      pageSize: 20,
      startCreatedAt: '2026-04-01 00:00:00',
      endCreatedAt: '2026-04-02 23:59:59',
    })

    expect(mockedListAuditLogs).toHaveBeenCalledWith(
      expect.objectContaining({
        startCreatedAt: expect.stringMatching(/T/),
        endCreatedAt: expect.stringMatching(/T/),
      }),
    )
  })
})
