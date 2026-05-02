import { mount } from '@vue/test-utils'
import ElementPlus, { ElMessage } from 'element-plus'
import { createPinia } from 'pinia'
import { nextTick } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import DeviceManagementView from '@/views/admin/device/DeviceManagementView.vue'

const flushPromises = async () => {
  await Promise.resolve()
  await nextTick()
  await nextTick()
}

const { mockBatchDisableCameraDevices } = vi.hoisted(() => ({
  mockBatchDisableCameraDevices: vi.fn(async () => ({
    data: {
      code: 0,
      data: {
        successIds: [1],
        failed: [{ id: 2, reason: '设备操作请求过于频繁' }],
        successCount: 1,
        failedCount: 1,
      },
    },
  })),
}))

vi.mock('@/services/deviceService', () => ({
  getDevicePage: vi.fn(async () => ({
    total: 2,
    list: [
      {
        id: '1',
        name: 'CAM-A',
        venue: 'A馆',
        location: '北侧',
        deviceType: 'fixed',
        streamUrl: 'rtsp://a',
        status: 'online',
        maintenanceCycleDays: 30,
      },
      {
        id: '2',
        name: 'CAM-B',
        venue: 'A馆',
        location: '南侧',
        deviceType: 'fixed',
        streamUrl: 'rtsp://b',
        status: 'offline',
        maintenanceCycleDays: 30,
      },
    ],
  })),
  removeDevice: vi.fn(async () => true),
}))

vi.mock('@/services/adminIntegrationService', () => ({
  getDeviceMaintenancePage: vi.fn(async () => ({ rows: [], total: 0, current: 1, pageSize: 20 })),
}))

vi.mock('@/api/venueController', () => ({
  listVenueVoByPage: vi.fn(async () => ({ data: { code: 0, data: { records: [], total: 0 } } })),
  getVenueVoById: vi.fn(async () => ({ data: { code: 0, data: { id: 1, venueName: 'A馆' } } })),
}))

vi.mock('@/api/cameraDeviceController', () => ({
  updateCameraDevice: vi.fn(async () => ({ data: { code: 0, data: true } })),
  batchDisableCameraDevices: mockBatchDisableCameraDevices,
}))

describe('device batch disable e2e flow', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.spyOn(ElMessage as any, 'success').mockImplementation(() => ({ close: vi.fn() }))
    vi.spyOn(ElMessage as any, 'error').mockImplementation(() => ({ close: vi.fn() }))
  })

  it('shows batch summary and failure reasons', async () => {
    const wrapper = mount(DeviceManagementView, {
      global: {
        plugins: [createPinia(), ElementPlus],
        stubs: {
          teleport: true,
          transition: false,
        },
      },
    })
    await flushPromises()

    const pageTable = wrapper.findComponent({ name: 'PageTable' }) as any
    await pageTable.vm.$emit('selectionChange', [
      {
        id: '1',
        name: 'CAM-A',
        venue: 'A馆',
        location: '北侧',
        deviceType: 'fixed',
        streamUrl: 'rtsp://a',
        status: 'online',
        maintenanceCycleDays: 30,
      },
      {
        id: '2',
        name: 'CAM-B',
        venue: 'A馆',
        location: '南侧',
        deviceType: 'fixed',
        streamUrl: 'rtsp://b',
        status: 'offline',
        maintenanceCycleDays: 30,
      },
    ])
    await nextTick()

    const buttons = wrapper.findAll('button')
    const disableButton = buttons.find((btn) => btn.text().includes('批量禁用'))
    await disableButton?.trigger('click')
    await flushPromises()

    expect(mockBatchDisableCameraDevices).toHaveBeenCalledWith({ cameraIds: [1, 2] })
    expect(ElMessage.success).toHaveBeenCalledWith('已成功禁用 1 台设备')
    expect(ElMessage.error).toHaveBeenCalledWith('设备 2：设备操作请求过于频繁')
  })
})
