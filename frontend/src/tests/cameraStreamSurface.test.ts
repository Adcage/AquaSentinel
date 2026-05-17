import { mount } from '@vue/test-utils'
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'

import CameraStreamSurface from '@/components/business/CameraStreamSurface.vue'

describe('CameraStreamSurface', () => {
  const originalCreateObjectUrl = URL.createObjectURL
  const originalRevokeObjectUrl = URL.revokeObjectURL

  beforeEach(() => {
    URL.createObjectURL = vi.fn(() => 'blob:mock-frame')
    URL.revokeObjectURL = vi.fn()
  })

  afterEach(() => {
    URL.createObjectURL = originalCreateObjectUrl
    URL.revokeObjectURL = originalRevokeObjectUrl
  })

  it('renders mjpeg stream image when protocol is mjpeg', () => {
    const wrapper = mount(CameraStreamSurface, {
      props: {
        protocol: 'mjpeg',
        streamUrl: 'http://192.168.137.228/stream',
      },
    })

    const image = wrapper.get('img.camera-stream')
    expect(image.attributes('src')).toBe('http://192.168.137.228/stream')
  })

  it('switches to frame image after ws_jpeg frame update', async () => {
    const wrapper = mount(CameraStreamSurface, {
      props: {
        protocol: 'ws_jpeg',
        streamUrl: '',
      },
    })

    expect(wrapper.text()).toContain('视频流占位')

    const blob = new Blob(['frame'], { type: 'image/jpeg' })
    ;(wrapper.vm as unknown as { updateVideoFrame: (value: Blob) => void }).updateVideoFrame(blob)
    await wrapper.vm.$nextTick()

    const image = wrapper.get('img.camera-stream')
    expect(image.attributes('src')).toBe('blob:mock-frame')
  })
})
