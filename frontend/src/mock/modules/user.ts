import type { UserRecord } from '@/types/business'

export const userRecordsMock: UserRecord[] = [
  {
    id: 'USR-001',
    account: 'admin_root',
    name: '系统管理员',
    role: 'super_admin',
    managedVenues: '全部场馆',
    status: 'enabled',
  },
  {
    id: 'USR-002',
    account: 'venue_a_mgr',
    name: 'A馆管理员',
    role: 'venue_admin',
    managedVenues: 'A馆',
    status: 'enabled',
  },
  {
    id: 'USR-003',
    account: 'venue_b_mgr',
    name: 'B馆管理员',
    role: 'venue_admin',
    managedVenues: 'B馆',
    status: 'disabled',
  },
  {
    id: 'USR-004',
    account: 'operator_view',
    name: '监控值班员',
    role: 'viewer',
    managedVenues: 'A馆,B馆',
    status: 'enabled',
  },
]
