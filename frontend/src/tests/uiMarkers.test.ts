import { describe, expect, it } from 'vitest'

import { API_PENDING_MARKER, MOCK_DATA_MARKER, createApiPendingText, createMockSectionTitle } from '@/constants/integrationMarkers'

describe('integration markers', () => {
  it('provides searchable markers for mock and pending api sections', () => {
    expect(MOCK_DATA_MARKER).toContain('TODO_MOCK_DATA')
    expect(API_PENDING_MARKER).toContain('TODO_REAL_API')
    expect(createMockSectionTitle('监控总览')).toContain('监控总览')
    expect(createApiPendingText('登录提交')).toContain('登录提交')
  })
})
