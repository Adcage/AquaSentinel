import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { createPinia } from 'pinia'
import { nextTick } from 'vue'
import { describe, expect, it, vi } from 'vitest'

import SystemSettingsView from '@/views/admin/settings/SystemSettingsView.vue'
import { createVenue, getVenuePage, removeVenue, updateVenueInfo } from '@/services/venueService'

const flushPromises = async () => {
  await Promise.resolve()
  await nextTick()
  await nextTick()
}

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
        action: '/venues/add',
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

describe('system settings venue management', () => {
  const getPlugins = () => [createPinia(), ElementPlus]

  it('renders venue management tab and loads venue list', async () => {
    const wrapper = mount(SystemSettingsView, {
      global: {
        plugins: getPlugins(),
        stubs: {
          teleport: true,
        },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('场馆管理')

    const navItems = wrapper.findAll('.settings-nav__item')
    await navItems[2].trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('新增场馆')
    expect(wrapper.text()).toContain('A馆')
    expect(mockedGetVenuePage).toHaveBeenCalled()
  })

  it('creates venue from venue dialog', async () => {
    const wrapper = mount(SystemSettingsView, {
      global: {
        plugins: getPlugins(),
        stubs: {
          teleport: true,
        },
      },
    })
    await flushPromises()

    const navItems = wrapper.findAll('.settings-nav__item')
    await navItems[2].trigger('click')
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      openVenueCreateDialog: () => void
      venueForm: { venueCode: string; venueName: string; location: string }
      handleVenueSubmit: () => Promise<void>
    }

    vm.openVenueCreateDialog()
    vm.venueForm.venueCode = 'SH-002'
    vm.venueForm.venueName = 'B馆'
    vm.venueForm.location = '西区'
    await vm.handleVenueSubmit()
    await flushPromises()

    expect(mockedCreateVenue).toHaveBeenCalledWith(
      expect.objectContaining({
        venueCode: 'SH-002',
        venueName: 'B馆',
        location: '西区',
      }),
    )
  })

  it('edits and deletes venue from action column', async () => {
    const wrapper = mount(SystemSettingsView, {
      global: {
        plugins: getPlugins(),
        stubs: {
          teleport: true,
        },
      },
    })
    await flushPromises()

    const navItems = wrapper.findAll('.settings-nav__item')
    await navItems[2].trigger('click')
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      openVenueEditDialog: (row: { id: number; venueName: string; location: string }) => void
      handleVenueSubmit: () => Promise<void>
      handleVenueDelete: (row: { id: number; venueName: string; location: string }) => Promise<void>
      venueForm: { venueCode: string; venueName: string; location: string }
    }

    vm.openVenueEditDialog({ id: 1, venueName: 'A馆', location: '东区' })
    vm.venueForm.venueCode = 'SH-001'
    vm.venueForm.venueName = 'A馆-更新'
    await vm.handleVenueSubmit()
    await flushPromises()

    expect(mockedUpdateVenueInfo).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ venueCode: 'SH-001', venueName: 'A馆-更新' }),
    )

    await vm.handleVenueDelete({ id: 1, venueName: 'A馆', location: '东区' })
    await flushPromises()

    expect(mockedRemoveVenue).toHaveBeenCalledWith(1)
  })
})
