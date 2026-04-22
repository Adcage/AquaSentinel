import type { LifeguardRecord } from "@/types/business";

export const lifeguardRecordsMock: LifeguardRecord[] = [
  {
    id: "LG-001",
    name: "张伟",
    phone: "13800001111",
    venueId: "2001",
    venue: "A馆",
    dutyStatus: "on_duty",
    auditStatus: "APPROVED",
    lastReportTime: "2026-03-21 11:28:10",
  },
  {
    id: "LG-002",
    name: "李娜",
    phone: "13800002222",
    venueId: "2002",
    venue: "B馆",
    dutyStatus: "on_duty",
    auditStatus: "APPROVED",
    lastReportTime: "2026-03-21 11:27:48",
  },
  {
    id: "LG-003",
    name: "赵鹏",
    phone: "13800003333",
    venueId: "2001",
    venue: "A馆",
    dutyStatus: "off_duty",
    auditStatus: "PENDING",
    lastReportTime: "2026-03-21 11:11:25",
  },
  {
    id: "LG-004",
    name: "王敏",
    phone: "13800004444",
    venueId: "2003",
    venue: "C馆",
    dutyStatus: "out_of_fence",
    auditStatus: "APPROVED",
    lastReportTime: "2026-03-21 11:05:36",
  },
];
