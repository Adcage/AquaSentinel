import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ElementPlus from 'element-plus'

import CameraDetailModal from '@/components/business/CameraDetailModal.vue'
import { controlCameraPtz } from '@/api/cameraDeviceController'
import type { CameraGridItem } from '@/types/business'

type CameraDetailModalVm = {
  startDirectionPress: (direction: 'LEFT' | 'RIGHT' | 'UP' | 'DOWN') => void
  stopDirectionPress: () => void
}

const flushMicrotasks = async () => {
  await Promise.resolve()
  await Promise.resolve()
}

vi.mock('@/api/cameraDeviceController', () => ({
  controlCameraPtz: vi.fn(async () => ({
    data: {
      code: 0,
      data: {},
    },
  })),
}))

const createItem = (): CameraGridItem => ({
  id: 'CAM-001',
  cameraId: 1,
  cameraCode: 'ESP32-CAM-1001',
  name: 'ESP32-CAM-1001',
  location: '2001号场馆-区域一',
  peopleCount: 0,
  riskLevel: 'normal',
  isAlarming: false,
  streamUrl: 'http://192.168.137.228/stream',
  previewProtocol: 'mjpeg',
  previewUrl: 'http://192.168.137.228/stream',
  protocol: 'PTZ',
  detections: [],
})

describe('CameraDetailModal', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.mocked(controlCameraPtz).mockClear()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('prefers mirroring the selected grid preview when source element is provided', () => {
    const sourceElement = document.createElement('img')
    sourceElement.src = 'http://192.168.137.228/stream'

    const wrapper = mount(CameraDetailModal, {
      props: {
        visible: true,
        item: createItem(),
        sourceElement,
      },
      global: {
        stubs: {
          CameraStreamSurface: {
            template: '<div class="camera-stream-surface-stub" />',
          },
          StatusTag: {
            template: '<span class="status-tag-stub" />',
          },
          ElButton: {
            template: '<button v-bind="$attrs"><slot /></button>',
          },
          ElDivider: {
            template: '<div><slot /></div>',
          },
          ElDialog: {
            props: ['modelValue', 'title'],
            template: '<div class="dialog-stub"><slot /></div>',
          },
        },
      },
    })

    expect(wrapper.find('canvas.camera-detail__mirror').exists()).toBe(true)
    expect(wrapper.find('.camera-stream-surface-stub').exists()).toBe(false)
  })

  it('shows ptz controls for direct device stream even when protocol field is missing', () => {
    const wrapper = mount(CameraDetailModal, {
      props: {
        visible: true,
        item: {
          ...createItem(),
          protocol: '',
          streamUrl: 'http://192.168.137.228/stream',
          previewUrl: 'http://192.168.137.228/stream',
        },
      },
      global: {
        stubs: {
          CameraStreamSurface: {
            template: '<div class="camera-stream-surface-stub" />',
          },
          StatusTag: {
            template: '<span class="status-tag-stub" />',
          },
          ElButton: {
            template: '<button v-bind="$attrs"><slot /></button>',
          },
          ElDivider: {
            template: '<div><slot /></div>',
          },
          ElDialog: {
            props: ['modelValue', 'title'],
            template: '<div class="dialog-stub"><slot /></div>',
          },
        },
      },
    })

    expect(wrapper.text()).toContain('云台控制')
    expect(wrapper.text()).toContain('回中')
    expect(wrapper.text()).toContain('查询状态')
  })

  it('renders direct move controls for ptz device', () => {
    const wrapper = mount(CameraDetailModal, {
      props: {
        visible: true,
        item: createItem(),
      },
      global: {
        plugins: [ElementPlus],
        stubs: {
          CameraStreamSurface: {
            template: '<div class="camera-stream-surface-stub" />',
          },
          StatusTag: {
            template: '<span class="status-tag-stub" />',
          },
          ElDivider: {
            template: '<div><slot /></div>',
          },
          ElDialog: {
            props: ['modelValue', 'title'],
            template: '<div class="dialog-stub"><slot /></div>',
          },
        },
      },
    })

    expect(wrapper.text()).toContain('直接定位')
    expect(wrapper.text()).toContain('移动到')
    expect(wrapper.text()).toContain('平视')
  })

  it('uses single small step on short press and repeated large step on long press', async () => {
    const wrapper = mount(CameraDetailModal, {
      props: {
        visible: true,
        item: createItem(),
      },
      global: {
        plugins: [ElementPlus],
        stubs: {
          CameraStreamSurface: {
            template: '<div class="camera-stream-surface-stub" />',
          },
          StatusTag: {
            template: '<span class="status-tag-stub" />',
          },
          ElDivider: {
            template: '<div><slot /></div>',
          },
          ElDialog: {
            props: ['modelValue', 'title'],
            template: '<div class="dialog-stub"><slot /></div>',
          },
        },
      },
    })

    const vm = wrapper.vm as unknown as CameraDetailModalVm

    vm.startDirectionPress('UP')
    await vi.advanceTimersByTimeAsync(120)
    vm.stopDirectionPress()
    await flushMicrotasks()

    expect(controlCameraPtz).toHaveBeenCalledTimes(1)
    expect(controlCameraPtz).toHaveBeenLastCalledWith({
      cameraId: 1,
      action: 'NUDGE',
      direction: 'UP',
      step: 5,
    })

    vi.mocked(controlCameraPtz).mockClear()

    vm.startDirectionPress('RIGHT')
    await vi.advanceTimersByTimeAsync(320)
    await vi.advanceTimersByTimeAsync(360)
    vm.stopDirectionPress()
    await flushMicrotasks()

    expect(controlCameraPtz.mock.calls.length).toBeGreaterThanOrEqual(2)
    for (const [payload] of vi.mocked(controlCameraPtz).mock.calls) {
      expect(payload).toMatchObject({
        cameraId: 1,
        action: 'NUDGE',
        direction: 'RIGHT',
        step: 10,
      })
    }

    const callCountAfterRelease = vi.mocked(controlCameraPtz).mock.calls.length
    await vi.advanceTimersByTimeAsync(400)
    expect(controlCameraPtz).toHaveBeenCalledTimes(callCountAfterRelease)
  })

  it('debounces square pad dragging and avoids duplicate final move for same target', async () => {
    const wrapper = mount(CameraDetailModal, {
      props: {
        visible: true,
        item: createItem(),
      },
      global: {
        plugins: [ElementPlus],
        stubs: {
          CameraStreamSurface: {
            template: '<div class="camera-stream-surface-stub" />',
          },
          StatusTag: {
            template: '<span class="status-tag-stub" />',
          },
          ElDivider: {
            template: '<div><slot /></div>',
          },
          ElDialog: {
            props: ['modelValue', 'title'],
            template: '<div class="dialog-stub"><slot /></div>',
          },
        },
      },
      attachTo: document.body,
    })

    const pad = wrapper.get('.ptz-square-pad')
    Object.defineProperty(pad.element, 'getBoundingClientRect', {
      value: () => ({
        left: 0,
        top: 0,
        width: 200,
        height: 200,
        right: 200,
        bottom: 200,
      }),
    })

    await pad.trigger('pointerdown', { clientX: 100, clientY: 100, pointerId: 1 })
    await flushMicrotasks()

    await pad.trigger('pointermove', { clientX: 190, clientY: 170, pointerId: 1 })
    await pad.trigger('pointermove', { clientX: 200, clientY: 180, pointerId: 1 })
    await vi.advanceTimersByTimeAsync(100)
    expect(controlCameraPtz).toHaveBeenCalledTimes(1)

    await vi.advanceTimersByTimeAsync(120)
    await flushMicrotasks()
    expect(controlCameraPtz).toHaveBeenCalledTimes(2)

    await pad.trigger('pointerup', { clientX: 200, clientY: 180, pointerId: 1 })
    await flushMicrotasks()
    expect(controlCameraPtz).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('当前 PAN 180°')
    expect(wrapper.text()).toContain('TILT 162°')
  })

  it('syncs current angles from status response', async () => {
    vi.mocked(controlCameraPtz).mockResolvedValueOnce({
      data: {
        code: 0,
        data: {
          deviceResponse: {
            pan: 33,
            tilt: 144,
          },
        },
      },
    } as never)

    const wrapper = mount(CameraDetailModal, {
      props: {
        visible: true,
        item: createItem(),
      },
      global: {
        plugins: [ElementPlus],
        stubs: {
          CameraStreamSurface: {
            template: '<div class="camera-stream-surface-stub" />',
          },
          StatusTag: {
            template: '<span class="status-tag-stub" />',
          },
          ElDivider: {
            template: '<div><slot /></div>',
          },
          ElDialog: {
            props: ['modelValue', 'title'],
            template: '<div class="dialog-stub"><slot /></div>',
          },
        },
      },
    })

    const statusButton = wrapper.findAll('button').find((button) => button.text().includes('查询状态'))
    expect(statusButton).toBeDefined()
    await statusButton!.trigger('click')
    await flushMicrotasks()

    expect(wrapper.text()).toContain('当前 PAN 33°')
    expect(wrapper.text()).toContain('TILT 144°')
  })
})
