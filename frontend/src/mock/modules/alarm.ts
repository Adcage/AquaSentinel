import type { AlarmRecord } from '@/types/business'

export const alarmRecordsMock: AlarmRecord[] = [
  {
    id: 'ALM-20260321-001',
    type: 'drowning',
    triggerTime: '2026-03-21 09:12:10',
    cameraLocation: 'A馆-东北角',
    emergencyContact: '值班室 13800001234',
    lifeguardName: '张伟',
    status: 'pending',
  },
  {
    id: 'ALM-20260321-002',
    type: 'cross_border',
    triggerTime: '2026-03-21 09:28:40',
    cameraLocation: 'B馆-跳台区',
    emergencyContact: '值班室 13800001234',
    lifeguardName: '-',
    status: 'processing',
  },
  {
    id: 'ALM-20260321-003',
    type: 'over_capacity',
    triggerTime: '2026-03-21 10:05:05',
    cameraLocation: 'C馆-中央',
    emergencyContact: '值班室 13800001234',
    lifeguardName: '李娜',
    status: 'resolved',
  },
  {
    id: 'ALM-20260321-004',
    type: 'drowning',
    triggerTime: '2026-03-21 10:24:31',
    cameraLocation: 'A馆-西南角',
    emergencyContact: '值班室 13800001234',
    lifeguardName: '赵鹏',
    status: 'false_alarm',
  },
  {
    id: 'ALM-20260321-005',
    type: 'cross_border',
    triggerTime: '2026-03-21 10:55:42',
    cameraLocation: 'B馆-看台区',
    emergencyContact: '值班室 13800001234',
    lifeguardName: '-',
    status: 'pending',
  },
]
