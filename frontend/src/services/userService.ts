import { listUserPageVo } from '@/api/userController'
import type { PageQuery, PageResult, UserRecord } from '@/types/business'
import { unwrapApiData } from '@/services/serviceUtils'

export interface LinkableUserOption {
  value: number
  label: string
  username: string
  phone?: string
}

export interface UserQuery extends PageQuery {
  role?: UserRecord['role'] | ''
  status?: UserRecord['status'] | ''
}

const roleToApi = (role?: UserRecord['role'] | '') => {
  if (role === 'super_admin') {
    return 'SUPER_ADMIN'
  }
  if (role === 'venue_admin') {
    return 'VENUE_ADMIN'
  }
  if (role === 'lifeguard') {
    return 'LIFEGUARD'
  }
  if (role === 'viewer') {
    return 'USER'
  }
  return undefined
}

const roleToBusiness = (roleCodes?: string[]): UserRecord['role'] => {
  const normalized = (roleCodes ?? []).map((item) => item.toUpperCase())
  if (normalized.includes('SUPER_ADMIN')) {
    return 'super_admin'
  }
  if (normalized.includes('VENUE_ADMIN')) {
    return 'venue_admin'
  }
  if (normalized.includes('LIFEGUARD')) {
    return 'lifeguard'
  }
  return 'viewer'
}

const statusToApi = (status?: UserRecord['status'] | '') => {
  if (status === 'enabled') {
    return 1
  }
  if (status === 'disabled') {
    return 0
  }
  return undefined
}

const statusToBusiness = (status?: number) => {
  if (status === 0) {
    return 'disabled' as const
  }
  return 'enabled' as const
}

const toUserRecord = (item: API.UserVO): UserRecord => ({
  id: String(item.id ?? ''),
  account: item.username || '-',
  name: item.displayName || item.username || '-',
  role: roleToBusiness(item.roleCodes),
  managedVenues: roleToBusiness(item.roleCodes) === 'super_admin' ? '全部场馆' : '未配置',
  status: statusToBusiness(item.status),
})

export const getUserPage = async (query: UserQuery): Promise<PageResult<UserRecord>> => {
  const response = await listUserPageVo({
    current: query.current,
    pageSize: query.pageSize,
    roleCode: roleToApi(query.role),
    status: statusToApi(query.status),
  })
  const pageData = unwrapApiData<API.PageUserVO>(response, '获取用户列表失败')
  const records = (pageData?.records ?? []).map(toUserRecord)

  return {
    list: records,
    total: Number(pageData?.total ?? records.length),
  }
}

export const listLinkableLifeguardUsers = async (
  keyword = '',
): Promise<LinkableUserOption[]> => {
  const trimmedKeyword = keyword.trim()
  const normalizedPhoneKeyword = trimmedKeyword.replace(/\D/g, '')
  const baseQuery = {
    current: 1,
    pageSize: 50,
    status: 1,
  }

  const requests: Array<Promise<API.BaseResponsePageUserVO>> = []
  if (!trimmedKeyword) {
    requests.push(listUserPageVo(baseQuery))
  } else {
    requests.push(
      listUserPageVo({
        ...baseQuery,
        username: trimmedKeyword,
      }),
    )
    requests.push(
      listUserPageVo({
        ...baseQuery,
        displayName: trimmedKeyword,
      }),
    )
    if (normalizedPhoneKeyword.length >= 1) {
      requests.push(
        listUserPageVo({
          ...baseQuery,
          phone: normalizedPhoneKeyword,
        }),
      )
    }
  }

  const responses = await Promise.all(requests)
  const records: API.UserVO[] = []
  const seenUserIds = new Set<number>()
  for (const response of responses) {
    const pageData = unwrapApiData<API.PageUserVO>(response, '获取可关联用户失败')
    const currentRecords = pageData?.records ?? []
    for (const item of currentRecords) {
      const userId = Number(item.id ?? 0)
      if (!userId || seenUserIds.has(userId)) {
        continue
      }
      seenUserIds.add(userId)
      records.push(item)
    }
  }

  return records
    .filter((item) => !(item as API.UserVO & { linkedLifeguardId?: number }).linkedLifeguardId)
    .map((item) => ({
      value: Number(item.id ?? 0),
      label: `${item.displayName || item.username || '-'}（${item.username || '-'} / ${item.phone || '无手机号'}）`,
      username: item.username || '',
      phone: item.phone || undefined,
    }))
    .filter((item) => item.value > 0 && item.username)
}
