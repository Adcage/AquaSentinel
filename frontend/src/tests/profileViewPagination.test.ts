import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { nextTick } from 'vue'
import { describe, expect, it, vi } from 'vitest'

import { listSystemAuditLogVoByPage } from '@/api/systemAuditLogController'
import ProfileView from '@/views/admin/profile/ProfileView.vue'

const flushPromises = async () => {
  await Promise.resolve()
  await nextTick()
  await nextTick()
}

vi.mock('@/services/authService', () => ({
  getStoredAuthUser: vi.fn(() => ({
    id: 1,
    username: 'admin_a',
    displayName: '系统管理员',
    roles: ['SUPER_ADMIN'],
  })),
  logoutCurrentUser: vi.fn(async () => undefined),
}))

vi.mock('@/api/accessControlController', () => ({
  updateMyProfile: vi.fn(async () => ({ data: { code: 0, data: true } })),
}))

vi.mock('@/api/userController', () => ({
  getUserVoById: vi.fn(async () => ({
    data: {
      code: 0,
      data: {
        id: 1,
        username: 'admin_a',
        displayName: '系统管理员',
        phone: '13800000000',
      },
    },
  })),
}))

vi.mock('@/api/systemAuditLogController', () => ({
  listSystemAuditLogVoByPage: vi.fn(async (payload: API.SystemAuditLogQueryRequest) => ({
    data: {
      code: 0,
      data: {
        current: payload.current,
        size: payload.pageSize,
        total: 25,
        records: [
          {
            createdAt: '2026-03-21T10:11:22',
            clientIp: '10.10.1.20',
            requestMethod: 'POST',
            requestUri: '/auth/login',
          },
        ],
      },
    },
  })),
}))

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return {
    ...actual,
  }
})

vi.mock('@/router', () => ({
  default: {
    push: vi.fn(),
  },
}))

describe('profile view login records pagination', () => {
  it('requests paged login records when page changes', async () => {
    const mockedListLogs = vi.mocked(listSystemAuditLogVoByPage)
    mockedListLogs.mockClear()

    const wrapper = mount(ProfileView, {
      global: { plugins: [ElementPlus] },
    })
    await flushPromises()

    expect(wrapper.find('.el-pagination').exists()).toBe(true)
    expect(mockedListLogs).toHaveBeenCalledWith(
      expect.objectContaining({ current: 1, pageSize: 10, operatorId: 1 }),
    )

    const pagination = wrapper.findComponent({ name: 'ElPagination' })
    pagination.vm.$emit('current-change', 2)
    await flushPromises()

    expect(mockedListLogs).toHaveBeenLastCalledWith(
      expect.objectContaining({ current: 2, pageSize: 10, operatorId: 1 }),
    )
  })
})
