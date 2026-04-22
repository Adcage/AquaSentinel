import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { describe, expect, it, vi } from 'vitest'

import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
  }),
}))

vi.mock('@/services/authService', () => ({
  fetchCaptcha: vi.fn(async () => ({
    captchaId: 'cpt-test',
    imageDataUrl: 'data:image/png;base64,abcd',
  })),
  loginAsAdmin: vi.fn(),
  registerAccount: vi.fn(),
}))

describe('auth views ui', () => {
  it('renders captcha on login page without exposing integration markers', () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [ElementPlus],
      },
    })

    expect(wrapper.text()).toContain('图片验证码')
    expect(wrapper.text()).toContain('点击图片切换验证码')
    expect(wrapper.text()).not.toContain('刷新验证码')
    expect(wrapper.text()).not.toContain('TODO_REAL_API')
    expect(wrapper.text()).not.toContain('待接后端')
  })

  it('renders name role and captcha fields on register page without exposing integration markers', () => {
    const wrapper = mount(RegisterView, {
      global: {
        plugins: [ElementPlus],
      },
    })

    expect(wrapper.text()).toContain('姓名')
    expect(wrapper.text()).toContain('角色')
    expect(wrapper.text()).toContain('图片验证码')
    expect(wrapper.text()).toContain('用于创建后台账号')
    expect(wrapper.text()).not.toContain('TODO_REAL_API')
  })
})
