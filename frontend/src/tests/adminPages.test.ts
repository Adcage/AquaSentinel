import { mount } from "@vue/test-utils";
import ElementPlus from "element-plus";
import { createPinia } from "pinia";
import { nextTick } from "vue";
import { describe, expect, it, vi } from "vitest";

import AlarmManagementView from "@/views/admin/alarm/AlarmManagementView.vue";
import AdminDashboardView from "@/views/admin/dashboard/AdminDashboardView.vue";
import DeviceManagementView from "@/views/admin/device/DeviceManagementView.vue";
import LifeguardManagementView from "@/views/admin/lifeguard/LifeguardManagementView.vue";
import ProfileView from "@/views/admin/profile/ProfileView.vue";
import SystemSettingsView from "@/views/admin/settings/SystemSettingsView.vue";
import StatisticsView from "@/views/admin/statistics/StatisticsView.vue";
import UserManagementView from "@/views/admin/user/UserManagementView.vue";
import { getDashboardMetrics } from "@/services/dashboardService";

const flushPromises = async () => {
  await Promise.resolve();
  await nextTick();
  await nextTick();
};

vi.mock("@/services/dashboardService", () => ({
  getDashboardMetrics: vi.fn(async () => ({
    onlineDeviceCount: 42,
    todayAlarmCount: 17,
    pendingAlarmCount: 5,
    onDutyLifeguardCount: 21,
    realtimeSwimmerCount: 168,
  })),
  fetchCameraGridBase: vi.fn(async () => ({
    records: [
      {
        id: "CAM-001",
        cameraId: 1,
        name: "1号泳池东北角",
        location: "A馆-东北角",
        peopleCount: 18,
        riskLevel: "danger",
        isAlarming: true,
        streamUrl: "/api/streams/cameras/1/preview",
        previewProtocol: "mjpeg",
        previewUrl: "/api/streams/cameras/1/preview?provider=auto&token=mock",
        detections: [],
      },
    ],
    total: 1,
    current: 1,
    pageSize: 9,
  })),
  enrichCameraGridRealtimeBatch: vi.fn(async (items: unknown[]) => items),
  mergeRealtimeBatchIntoGrid: vi.fn(
    (items: unknown[]) => items,
  ),
  checkEngineAvailability: vi.fn(async () => ({
    available: true,
    message: "AI引擎运行正常",
  })),
}));

vi.mock("@/services/deviceService", () => ({
  getDevicePage: vi.fn(async () => ({
    total: 1,
    list: [
      {
        id: "CAM-001",
        name: "1号摄像头",
        venue: "A馆",
        location: "深水区北侧",
        deviceType: "fixed",
        streamUrl: "http://mock",
        status: "online",
        maintenanceCycleDays: 30,
      },
    ],
  })),
  createDevice: vi.fn(async () => true),
  updateDevice: vi.fn(async () => true),
  removeDevice: vi.fn(async () => true),
}));

vi.mock("@/services/lifeguardService", () => ({
  getLifeguardPage: vi.fn(async () => ({
    total: 1,
    list: [
      {
        id: "LG-001",
        name: "张三",
        phone: "13800000000",
        venue: "A馆",
        dutyStatus: "on_duty",
        lastReportTime: "2026-03-21 10:00:00",
      },
    ],
  })),
}));

vi.mock("@/services/alarmService", () => ({
  getAlarmPage: vi.fn(async () => ({
    total: 1,
    list: [
      {
        id: "ALARM-001",
        type: "drowning",
        triggerTime: "2026-03-21 10:00:00",
        cameraLocation: "A馆-深水区",
        emergencyContact: "李四 13800000000",
        lifeguardName: "张三",
        status: "pending",
      },
    ],
  })),
  markAlarmsResolved: vi.fn(async () => true),
}));

vi.mock("@/services/userService", () => ({
  getUserPage: vi.fn(async () => ({
    total: 1,
    list: [
      {
        id: "USER-001",
        account: "admin_a",
        name: "系统管理员",
        role: "super_admin",
        managedVenues: "A馆,B馆",
        status: "enabled",
      },
    ],
  })),
  listLinkableLifeguardUsers: vi.fn(async () => []),
}));

vi.mock("@/services/statisticsService", () => ({
  getStatisticsKpi: vi.fn(async () => ({
    alarmTotal: 12,
    resolvedRate: 92,
    avgResponseSeconds: 35,
    highRiskVenueCount: 2,
  })),
  getAlarmTrend: vi.fn(async () => [{ month: "03-21", value: 12 }]),
  getVenueRanking: vi.fn(async () => [{ month: "A馆", value: 8 }]),
  getAlarmTypeDistribution: vi.fn(async () => [{ name: "溺水", value: 5 }]),
}));

vi.mock("@/services/adminIntegrationService", () => ({
  getDeviceMaintenancePage: vi.fn(async () => ({
    rows: [
      {
        deviceName: "A馆东北摄像头",
        content: "完成镜头清洁与角度校准",
        operator: "王工",
        time: "2026-03-21 09:20:00",
      },
    ],
    total: 25,
    current: 1,
    pageSize: 20,
  })),
  getDeviceMaintenanceRows: vi.fn(async () => [
    {
      deviceName: "A馆东北摄像头",
      content: "完成镜头清洁与角度校准",
      operator: "王工",
      time: "2026-03-21 09:20:00",
    },
  ]),
  getRolePermissionTree: vi.fn(async () => [
    {
      id: "SUPER_ADMIN",
      label: "超级管理员",
      children: [{ id: "SUPER_ADMIN-user:read", label: "user:read" }],
    },
  ]),
  getCoreRoleItems: vi.fn(async () => [
    {
      key: "ADMIN",
      label: "管理员",
      roleId: 1,
      roleCode: "VENUE_ADMIN",
      roleName: "场馆管理员",
      permissions: ["dashboard:view"],
      status: 1,
    },
  ]),
  getSystemLogPage: vi.fn(async () => ({
    rows: [
      {
        time: "2026-03-21 10:11:22",
        operator: "系统管理员",
        action: "/users/update/my",
        result: "成功",
      },
    ],
    total: 26,
    current: 1,
    pageSize: 20,
  })),
  getSystemLogRows: vi.fn(async () => [
    {
      time: "2026-03-21 10:11:22",
      operator: "系统管理员",
      action: "/users/update/my",
      result: "成功",
    },
  ]),
  requestStatsExport: vi.fn(async () => ({
    name: "stats_export_1001.csv",
    type: "CSV",
    operator: "系统管理员",
    createdAt: "2026-03-21 10:33:08",
  })),
}));

vi.mock("@/services/authService", () => ({
  getStoredAuthUser: vi.fn(() => ({
    id: 1,
    username: "admin_a",
    displayName: "系统管理员",
    roles: ["SUPER_ADMIN"],
  })),
  logoutCurrentUser: vi.fn(async () => undefined),
  fetchCaptcha: vi.fn(async () => ({
    captchaId: "cpt-test",
    imageDataUrl: "data:image/png;base64,abcd",
  })),
  loginAsAdmin: vi.fn(),
  registerAccount: vi.fn(),
}));

vi.mock("@/api/userController", () => ({
  getUserVoById: vi.fn(async () => ({
    data: {
      code: 0,
      data: {
        id: 1,
        username: "admin_a",
        displayName: "系统管理员",
        phone: "13800000000",
        roleCodes: ["SUPER_ADMIN"],
        status: 1,
        forceChangePassword: 0,
      },
    },
  })),
  addUser: vi.fn(async () => ({ data: { code: 0, data: 1 } })),
  updateUser: vi.fn(async () => ({ data: { code: 0, data: true } })),
}));

vi.mock("@/api/lifeguardController", () => ({
  addLifeguard: vi.fn(async () => ({ data: { code: 0, data: 1 } })),
  updateLifeguard: vi.fn(async () => ({ data: { code: 0, data: true } })),
  getLifeguardVoById: vi.fn(async () => ({
    data: {
      code: 0,
      data: {
        id: 1,
        fullName: "张三",
        phone: "13800000000",
        venueId: 1,
        auditStatus: "APPROVED",
        dutyStatus: "ON_DUTY",
      },
    },
  })),
  listLifeguardVoByPage: vi.fn(async () => ({
    data: { code: 0, data: { records: [], total: 0 } },
  })),
}));

vi.mock("@/api/cameraDeviceController", () => ({
  addCameraDevice: vi.fn(async () => ({ data: { code: 0, data: 1 } })),
  updateCameraDevice: vi.fn(async () => ({ data: { code: 0, data: true } })),
  getCameraDeviceVoById: vi.fn(async () => ({
    data: {
      code: 0,
      data: {
        id: 1,
        cameraName: "测试摄像头",
        venueId: 1,
        protocol: "RTSP",
        streamUrl: "rtsp://test",
        deviceStatus: "ONLINE",
        healthStatus: "NORMAL",
        enabled: 1,
      },
    },
  })),
  listCameraDeviceVoByPage: vi.fn(async () => ({
    data: { code: 0, data: { records: [], total: 0 } },
  })),
  deleteCameraDevice: vi.fn(async () => ({ data: { code: 0, data: true } })),
}));

vi.mock("@/api/systemAuditLogController", () => ({
  listSystemAuditLogVoByPage: vi.fn(async () => ({
    data: {
      code: 0,
      data: {
        records: [
          {
            createdAt: "2026-03-21T10:11:22",
            clientIp: "10.10.1.20",
            requestMethod: "POST",
            requestUri: "/auth/login",
          },
        ],
      },
    },
  })),
}));

vi.mock("@/api/accessControlController", () => ({
  updateMyProfile: vi.fn(async () => ({ data: { code: 0, data: true } })),
}));

vi.mock("@/api/venueController", () => ({
  listVenueVoByPage: vi.fn(async () => ({
    data: {
      code: 0,
      data: {
        records: [{ id: 1, venueName: "A馆" }],
      },
    },
  })),
  updateVenue: vi.fn(async () => ({ data: { code: 0, data: true } })),
}));

vi.mock("@/services/venueService", () => ({
  getVenuePage: vi.fn(async () => ({
    list: [{ id: 1, venueName: "A馆", location: "东区" }],
    total: 1,
    current: 1,
    pageSize: 10,
  })),
  createVenue: vi.fn(async () => 2),
  updateVenueInfo: vi.fn(async () => true),
  removeVenue: vi.fn(async () => true),
}));

vi.mock("@/components/dashboard/BarChart.vue", () => ({
  default: {
    template: "<div>BarChart Stub</div>",
  },
}));

vi.mock("@/components/dashboard/PieChart.vue", () => ({
  default: {
    template: "<div>PieChart Stub</div>",
  },
}));

vi.mock("vue-router", async () => {
  const actual =
    await vi.importActual<typeof import("vue-router")>("vue-router");
  return {
    ...actual,
    useRouter: () => ({ push: vi.fn(), getRoutes: vi.fn(() => []) }),
    useRoute: () => ({ name: "AdminDashboard", meta: { title: "监控总览" } }),
  };
});

describe("admin pages ui completion", () => {
  const getPlugins = () => [createPinia(), ElementPlus];

  it("renders dashboard alarm controls without exposing integration markers", async () => {
    const wrapper = mount(AdminDashboardView, {
      global: { plugins: getPlugins() },
    });
    await flushPromises();

    expect(wrapper.text()).toContain("泳池实时总人数");
    expect(wrapper.text()).toContain("实时来源");
    expect(wrapper.text()).not.toContain("TODO_REAL_API");
    expect(wrapper.text()).not.toContain("TODO_MOCK_DATA");
  });

  it("refreshes dashboard metrics on interval", async () => {
    vi.useFakeTimers();
    const mockedGetDashboardMetrics = vi.mocked(getDashboardMetrics);
    mockedGetDashboardMetrics.mockClear();

    const wrapper = mount(AdminDashboardView, {
      global: { plugins: getPlugins() },
    });
    await flushPromises();
    expect(mockedGetDashboardMetrics).toHaveBeenCalledTimes(1);

    await vi.advanceTimersByTimeAsync(10_000);
    await flushPromises();
    expect(mockedGetDashboardMetrics).toHaveBeenCalledTimes(2);

    wrapper.unmount();
    await vi.advanceTimersByTimeAsync(10_000);
    expect(mockedGetDashboardMetrics).toHaveBeenCalledTimes(2);
    vi.useRealTimers();
  });

  it("renders device page batch actions and maintenance area", async () => {
    const wrapper = mount(DeviceManagementView, {
      global: { plugins: getPlugins() },
    });
    await flushPromises();

    expect(wrapper.text()).toContain("批量启用");
    expect(wrapper.text()).toContain("维护记录");
    expect(wrapper.text()).toContain("导出维护记录");
    expect(wrapper.find(".table-header-toolbar").exists()).toBe(true);
    expect(wrapper.findAll(".el-pagination").length).toBeGreaterThan(1);
  });

  it("renders lifeguard page map and edit actions", async () => {
    const wrapper = mount(LifeguardManagementView, {
      global: { plugins: getPlugins() },
    });
    await flushPromises();

    expect(wrapper.text()).toContain("在岗地图监控");
    expect(wrapper.text()).toContain("新增救生员");
    expect(wrapper.text()).toContain("查看轨迹");
    expect(wrapper.find(".lifeguard-main-grid").exists()).toBe(true);
    expect(wrapper.find(".map-card").exists()).toBe(true);
    expect(wrapper.text()).toContain("在岗人数");
    expect(wrapper.find(".table-header-toolbar").exists()).toBe(true);
    expect(wrapper.find(".table-action-group").exists()).toBe(true);
  });

  it("renders alarm page full filters and detail actions", async () => {
    const wrapper = mount(AlarmManagementView, {
      global: { plugins: getPlugins() },
    });
    await flushPromises();

    expect(wrapper.text()).toContain("时间范围");
    expect(wrapper.text()).toContain("所属场馆");
    expect(wrapper.text()).toContain("导出记录");
    expect(wrapper.text()).toContain("查看详情");
    expect(wrapper.find(".table-header-toolbar").exists()).toBe(true);
    expect(wrapper.find(".table-action-group").exists()).toBe(true);
  });

  it("renders user page role permission management area", async () => {
    const wrapper = mount(UserManagementView, {
      global: { plugins: getPlugins() },
    });
    await flushPromises();

    expect(wrapper.text()).toContain("新增用户");
    expect(wrapper.text()).toContain("角色权限配置");
    expect(wrapper.text()).toContain("批量启用");
    expect(wrapper.find(".table-header-toolbar").exists()).toBe(true);
  });

  it("renders statistics settings and profile missing ui sections", async () => {
    const statsWrapper = mount(StatisticsView, {
      global: { plugins: getPlugins() },
    });
    await flushPromises();
    expect(statsWrapper.text()).toContain("报警类型");
    expect(statsWrapper.text()).toContain("图表类型");
    expect(statsWrapper.text()).toContain("下载PNG");

    const settingsWrapper = mount(SystemSettingsView, {
      global: { plugins: getPlugins() },
    });
    expect(settingsWrapper.text()).toContain("基础设置");
    expect(settingsWrapper.text()).toContain("通知设置");
    expect(settingsWrapper.text()).toContain("场馆管理");
    expect(settingsWrapper.text()).toContain("日志管理");
    expect(settingsWrapper.find(".settings-nav__item.is-active").exists()).toBe(
      true,
    );
    await settingsWrapper.findAll(".settings-nav__item")[3].trigger("click");
    await flushPromises();
    expect(settingsWrapper.find(".el-pagination").exists()).toBe(true);

    const profileWrapper = mount(ProfileView, {
      global: { plugins: getPlugins() },
    });
    expect(profileWrapper.text()).toContain("编辑资料");
    expect(profileWrapper.text()).toContain("原密码");
    expect(profileWrapper.text()).toContain("退出登录");
  });
});
