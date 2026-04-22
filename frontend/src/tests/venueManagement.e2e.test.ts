import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { createPinia } from 'pinia'
import { nextTick } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import App from '@/App.vue'
import router from '@/router'
import SystemSettingsView from '@/views/admin/settings/SystemSettingsView.vue'
import { createVenue, getVenuePage, removeVenue, updateVenueInfo } from '@/services/venueService'

const flushPromises = async () => {
  await Promise.resolve()
  await nextTick()
  await nextTick()
}

vi.mock('@/services/dashboardService', () => ({
  getDashboardMetrics: vi.fn(async () => ({
    onlineDeviceCount: 0,
    todayAlarmCount: 0,
    pendingAlarmCount: 0,
    onDutyLifeguardCount: 0,
    realtimeSwimmerCount: 0,
  })),
}))

vi.mock('@/services/alertWsService', () => ({
  alertWsService: {
    connect: vi.fn(),
    disconnect: vi.fn(),
    send: vi.fn(),
    getStatus: vi.fn(() => 'connected'),
  },
}))

vi.mock('@/services/authService', () => ({
  getStoredAuthUser: vi.fn(() => ({
    id: 1,
    username: 'admin_a',
    displayName: '系统管理员',
    roles: ['SUPER_ADMIN'],
  })),
  logoutCurrentUser: vi.fn(async () => undefined),
}))

vi.mock('@/services/systemNoticeSettingsService', () => ({
  getNoticeSettings: vi.fn(async () => ({
    offDutyThreshold: 60,
    deviceOfflineThreshold: 180,
    drowningAlertThreshold: 3,
  })),
  saveNoticeSettings: vi.fn(async (payload: unknown) => payload),
}))

vi.mock('@/services/adminIntegrationService', () => ({
  getSystemLogPage: vi.fn(async () => ({
    rows: [
      {
        time: '2026-04-02 10:00:00',
        operator: '系统管理员',
        action: '/venues/list/page/vo',
        result: '成功',
      },
    ],
    total: 1,
    current: 1,
    pageSize: 20,
  })),
}))

vi.mock('@/services/venueService', () => ({
  getVenuePage: vi.fn(async () => ({
    list: [
      {
        id: 1,
        venueName: 'A馆',
        location: '东区',
      },
    ],
    total: 1,
    current: 1,
    pageSize: 10,
  })),
  createVenue: vi.fn(async () => 2),
  updateVenueInfo: vi.fn(async () => true),
  removeVenue: vi.fn(async () => true),
}))

const mockedGetVenuePage = vi.mocked(getVenuePage)
const mockedCreateVenue = vi.mocked(createVenue)
const mockedUpdateVenueInfo = vi.mocked(updateVenueInfo)
const mockedRemoveVenue = vi.mocked(removeVenue)

describe('venue management e2e flow', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    sessionStorage.setItem('token', 'e2e-token')
    await router.push('/admin/settings')
    await router.isReady()
  })

  const mountApp = async () => {
    const wrapper = mount(App, {
      global: {
        plugins: [createPinia(), router, ElementPlus],
        stubs: {
          teleport: true,
          transition: false,
        },
      },
    })
    await flushPromises()
    return wrapper
  }

  const getSettingsVm = (wrapper: ReturnType<typeof mount>) => {
    const settings = wrapper.findComponent(SystemSettingsView)
    expect(settings.exists()).toBe(true)

    return settings.vm as unknown as {
      activeTab: string
      venueFilters: { keyword: string }
      venueForm: { venueCode: string; venueName: string; location: string }
      venuePagination: { current: number; pageSize: number }
      handleVenueSearch: () => void
      handleVenueReset: () => void
      handleVenuePageChange: (page: number) => void
      handleVenuePageSizeChange: (size: number) => void
      openVenueCreateDialog: () => void
      openVenueEditDialog: (row: { id: number; venueName: string; location: string }) => void
      handleVenueSubmit: () => Promise<void>
      handleVenueDelete: (row: { id: number; venueName: string; location: string }) => Promise<void>
    }
  }

  it('covers list search reset and pagination flow', async () => {
    const wrapper = await mountApp()

    expect(wrapper.text()).toContain('系统设置')
    expect(wrapper.text()).toContain('场馆管理')

    const vm = getSettingsVm(wrapper)
    vm.activeTab = 'venue'
    await flushPromises()

    vm.venueFilters.keyword = 'A馆'
    vm.handleVenueSearch()
    await flushPromises()

    vm.handleVenuePageChange(2)
    await flushPromises()

    vm.handleVenuePageSizeChange(20)
    await flushPromises()

    vm.handleVenueReset()
    await flushPromises()

    expect(mockedGetVenuePage).toHaveBeenCalled()
    expect(vm.venuePagination.current).toBe(1)
    expect(mockedGetVenuePage).toHaveBeenCalledWith(
      expect.objectContaining({ current: 1, pageSize: 20 }),
    )
  })

  it('covers create and edit venue flow', async () => {
    const wrapper = await mountApp()

    const vm = getSettingsVm(wrapper)
    vm.activeTab = 'venue'
    await flushPromises()

    vm.openVenueCreateDialog()
    vm.venueForm.venueCode = 'SH-002'
    vm.venueForm.venueName = 'B馆'
    vm.venueForm.location = '西区'
    await vm.handleVenueSubmit()
    await flushPromises()
    expect(mockedCreateVenue).toHaveBeenCalledWith(
      expect.objectContaining({ venueCode: 'SH-002', venueName: 'B馆', location: '西区' }),
    )

    vm.openVenueEditDialog({ id: 1, venueName: 'A馆', location: '东区' })
    vm.venueForm.venueCode = 'SH-001'
    vm.venueForm.venueName = 'A馆-更新'
    await vm.handleVenueSubmit()
    await flushPromises()
    expect(mockedUpdateVenueInfo).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ venueCode: 'SH-001', venueName: 'A馆-更新' }),
    )
  })

  it('covers delete success and delete-failed-by-reference flow', async () => {
    const wrapper = await mountApp()

    const vm = getSettingsVm(wrapper)
    vm.activeTab = 'venue'
    await flushPromises()

    await vm.handleVenueDelete({ id: 1, venueName: 'A馆', location: '东区' })
    await flushPromises()
    expect(mockedRemoveVenue).toHaveBeenCalledWith(1)

    const reloadCountAfterSuccess = mockedGetVenuePage.mock.calls.length
    mockedRemoveVenue.mockRejectedValueOnce(
      new Error('场馆已被报警记录或其他业务数据引用，无法删除'),
    )
    await vm.handleVenueDelete({ id: 2, venueName: 'B馆', location: '西区' })
    await flushPromises()

    expect(mockedRemoveVenue).toHaveBeenCalledWith(2)
    expect(mockedGetVenuePage.mock.calls.length).toBe(reloadCountAfterSuccess)
    expect(mockedGetVenuePage).toHaveBeenCalled()
  })
})
