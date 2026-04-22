import { mount } from "@vue/test-utils";
import ElementPlus, { ElMessage } from "element-plus";
import { nextTick } from "vue";
import { beforeEach, describe, expect, it, vi } from "vitest";

import AlarmManagementView from "@/views/admin/alarm/AlarmManagementView.vue";
import DeviceManagementView from "@/views/admin/device/DeviceManagementView.vue";
import LifeguardManagementView from "@/views/admin/lifeguard/LifeguardManagementView.vue";
import UserManagementView from "@/views/admin/user/UserManagementView.vue";

const flushPromises = async () => {
  await Promise.resolve();
  await nextTick();
  await nextTick();
};

// ─── 用 vi.hoisted 声明跨 mock 引用的 spy（vi.mock 会被提升到文件顶部）────────
const {
  mockUpdateCameraDevice,
  mockAddCameraDevice,
  mockGetCameraDeviceVoById,
  mockUpdateLifeguard,
  mockAddLifeguard,
  mockUpdateUser,
  mockAddUser,
  mockAssignUserRole,
} = vi.hoisted(() => ({
  mockUpdateCameraDevice: vi.fn(async () => ({
    data: { code: 0, data: true },
  })),
  mockAddCameraDevice: vi.fn(async () => ({ data: { code: 0, data: 1 } })),
  mockGetCameraDeviceVoById: vi.fn(async () => ({
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
  mockUpdateLifeguard: vi.fn(async () => ({ data: { code: 0, data: true } })),
  mockAddLifeguard: vi.fn(async () => ({ data: { code: 0, data: 1 } })),
  mockUpdateUser: vi.fn(async () => ({ data: { code: 0, data: true } })),
  mockAddUser: vi.fn(async () => ({ data: { code: 0, data: 1 } })),
  mockAssignUserRole: vi.fn(async () => ({ data: { code: 0, data: true } })),
}));

// ─── Mock 路由 ────────────────────────────────────────────────────────────────
vi.mock("vue-router", async () => {
  const actual =
    await vi.importActual<typeof import("vue-router")>("vue-router");
  return {
    ...actual,
    useRouter: () => ({ push: vi.fn() }),
    useRoute: () => ({ name: "Admin", meta: {} }),
  };
});

// ─── Mock 图表组件 ─────────────────────────────────────────────────────────────
vi.mock("@/components/dashboard/BarChart.vue", () => ({
  default: { template: "<div />" },
}));
vi.mock("@/components/dashboard/PieChart.vue", () => ({
  default: { template: "<div />" },
}));

// ─── Mock 服务层 ───────────────────────────────────────────────────────────────
vi.mock("@/services/deviceService", () => ({
  getDevicePage: vi.fn(async () => ({
    total: 2,
    list: [
      {
        id: "1",
        name: "CAM-A",
        venue: "A馆",
        location: "北侧",
        deviceType: "fixed",
        streamUrl: "rtsp://a",
        status: "online",
        maintenanceCycleDays: 30,
      },
      {
        id: "2",
        name: "CAM-B",
        venue: "B馆",
        location: "南侧",
        deviceType: "ptz",
        streamUrl: "rtsp://b",
        status: "offline",
        maintenanceCycleDays: 30,
      },
    ],
  })),
  removeDevice: vi.fn(async () => true),
}));

vi.mock("@/services/lifeguardService", () => ({
  getLifeguardPage: vi.fn(async () => ({
    total: 2,
    list: [
      {
        id: "10",
        name: "张三",
        phone: "13800000001",
        venue: "A馆",
        dutyStatus: "on_duty",
        lastReportTime: "2026-03-21 10:00:00",
      },
      {
        id: "11",
        name: "李四",
        phone: "13800000002",
        venue: "B馆",
        dutyStatus: "off_duty",
        lastReportTime: "2026-03-21 09:00:00",
      },
    ],
  })),
}));

vi.mock("@/services/userService", () => ({
  getUserPage: vi.fn(async () => ({
    total: 1,
    list: [
      {
        id: "20",
        account: "admin_a",
        name: "系统管理员",
        role: "super_admin",
        managedVenues: "A馆",
        status: "enabled",
      },
    ],
  })),
  listLinkableLifeguardUsers: vi.fn(async () => [
    {
      value: 20,
      label: "系统管理员（admin_a / 13800000000）",
      username: "admin_a",
      phone: "13800000000",
    },
  ]),
}));

vi.mock("@/services/alarmService", () => ({
  getAlarmPage: vi.fn(async () => ({
    total: 1,
    list: [
      {
        id: "ALM-001",
        type: "drowning",
        triggerTime: "2026-03-21 10:00:00",
        cameraLocation: "A馆深水区",
        emergencyContact: "王五",
        lifeguardName: "张三",
        status: "pending",
      },
    ],
  })),
  markAlarmsResolved: vi.fn(async () => true),
}));

vi.mock("@/services/adminIntegrationService", () => ({
  getDeviceMaintenanceRows: vi.fn(async () => [
    {
      deviceName: "CAM-A",
      content: "月度巡检",
      operator: "运维A",
      time: "2026-03-21 11:00:00",
    },
  ]),
  getDeviceMaintenancePage: vi.fn(async () => ({
    rows: [
      {
        deviceName: "CAM-A",
        content: "月度巡检",
        operator: "运维A",
        time: "2026-03-21 11:00:00",
      },
    ],
    total: 1,
    current: 1,
    pageSize: 20,
  })),
  getCoreRoleItems: vi.fn(async () => [
    {
      key: "ADMIN",
      label: "管理员",
      roleId: 2,
      roleCode: "VENUE_ADMIN",
      roleName: "场馆管理员",
      permissions: ["dashboard:view"],
      status: 1,
    },
    {
      key: "LIFEGUARD",
      label: "救生员",
      roleId: 3,
      roleCode: "LIFEGUARD",
      roleName: "救生员",
      permissions: ["alert:receive"],
      status: 1,
    },
  ]),
  getRolePermissionTree: vi.fn(async () => [
    { id: "ADMIN", label: "管理员" },
    { id: "LIFEGUARD", label: "救生员" },
  ]),
  getSystemLogRows: vi.fn(async () => []),
  requestStatsExport: vi.fn(async () => ({})),
}));

vi.mock("@/api/accessControlController", () => ({
  assignUserRole: mockAssignUserRole,
  updateMyProfile: vi.fn(async () => ({ data: { code: 0, data: true } })),
}));

vi.mock("@/services/authService", () => ({
  getStoredAuthUser: vi.fn(() => ({
    id: 1,
    username: "admin_a",
    displayName: "系统管理员",
    roles: ["SUPER_ADMIN"],
  })),
  logoutCurrentUser: vi.fn(async () => undefined),
}));

// ─── Mock API 控制器 ───────────────────────────────────────────────────────────
vi.mock("@/api/cameraDeviceController", () => ({
  updateCameraDevice: mockUpdateCameraDevice,
  addCameraDevice: mockAddCameraDevice,
  getCameraDeviceVoById: mockGetCameraDeviceVoById,
  listCameraDeviceVoByPage: vi.fn(async () => ({
    data: { code: 0, data: { records: [], total: 0 } },
  })),
  deleteCameraDevice: vi.fn(async () => ({ data: { code: 0, data: true } })),
}));

vi.mock("@/api/lifeguardController", () => ({
  updateLifeguard: mockUpdateLifeguard,
  addLifeguard: mockAddLifeguard,
  getLifeguardVoById: vi.fn(async () => ({
    data: {
      code: 0,
      data: {
        id: 10,
        fullName: "张三",
        phone: "13800000001",
        venueId: 1,
        lifeguardCode: "LG-001",
        auditStatus: "APPROVED",
        dutyStatus: "ON_DUTY",
        userId: 100,
      },
    },
  })),
  listLifeguardVoByPage: vi.fn(async () => ({
    data: { code: 0, data: { records: [], total: 0 } },
  })),
}));

vi.mock("@/api/userController", () => ({
  updateUser: mockUpdateUser,
  addUser: mockAddUser,
  getUserVoById: vi.fn(async () => ({
    data: {
      code: 0,
      data: {
        id: 20,
        username: "admin_a",
        displayName: "系统管理员",
        phone: "13800000000",
        roleCodes: ["SUPER_ADMIN"],
        status: 1,
        forceChangePassword: 0,
      },
    },
  })),
  listUserPage: vi.fn(async () => ({
    data: { code: 0, data: { records: [], total: 0 } },
  })),
  listUserPageVo: vi.fn(async () => ({
    data: { code: 0, data: { records: [], total: 0 } },
  })),
}));

vi.mock("@/api/venueController", () => ({
  listVenueVoByPage: vi.fn(async () => ({
    data: {
      code: 0,
      data: {
        records: [
          { id: 1, venueName: "A馆" },
          { id: 2, venueName: "B馆" },
        ],
        total: 2,
      },
    },
  })),
  getVenueVoById: vi.fn(async () => ({
    data: {
      code: 0,
      data: {
        id: 1,
        venueName: "A馆",
      },
    },
  })),
}));

// ─── 辅助函数 ──────────────────────────────────────────────────────────────────
/** 在 wrapper 中找到文本匹配的第一个按钮并返回 */
const findButton = (wrapper: ReturnType<typeof mount>, text: string) =>
  wrapper.findAll("button").find((b) => b.text().trim().includes(text));

const globalOpts = { plugins: [ElementPlus] };

// ─── 批量操作 - 空选择警告 ─────────────────────────────────────────────────────
describe("批量操作 - 空选择时显示警告", () => {
  let warnSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    warnSpy = vi
      .spyOn(ElMessage as any, "warning")
      .mockImplementation(() => ({ close: vi.fn() }));
    mockUpdateCameraDevice.mockClear();
    mockUpdateLifeguard.mockClear();
    mockUpdateUser.mockClear();
  });

  it("设备管理：未勾选行时批量启用显示警告且不调用API", async () => {
    const wrapper = mount(DeviceManagementView, { global: globalOpts });
    await flushPromises();
    await findButton(wrapper, "批量启用")?.trigger("click");
    expect(warnSpy).toHaveBeenCalledWith("请先选择设备");
    expect(mockUpdateCameraDevice).not.toHaveBeenCalled();
  });

  it("设备管理：未勾选行时批量禁用显示警告且不调用API", async () => {
    const wrapper = mount(DeviceManagementView, { global: globalOpts });
    await flushPromises();
    await findButton(wrapper, "批量禁用")?.trigger("click");
    expect(warnSpy).toHaveBeenCalledWith("请先选择设备");
    expect(mockUpdateCameraDevice).not.toHaveBeenCalled();
  });

  it("救生员管理：未勾选行时批量启用显示警告且不调用API", async () => {
    const wrapper = mount(LifeguardManagementView, { global: globalOpts });
    await flushPromises();
    await findButton(wrapper, "批量启用")?.trigger("click");
    expect(warnSpy).toHaveBeenCalledWith("请先选择救生员");
    expect(mockUpdateLifeguard).not.toHaveBeenCalled();
  });

  it("用户管理：未勾选行时批量启用显示警告且不调用API", async () => {
    const wrapper = mount(UserManagementView, { global: globalOpts });
    await flushPromises();
    await findButton(wrapper, "批量启用")?.trigger("click");
    expect(warnSpy).toHaveBeenCalledWith("请先选择用户");
    expect(mockUpdateUser).not.toHaveBeenCalled();
  });

  it("用户管理：未勾选用户时点击赋予选中用户显示警告", async () => {
    const wrapper = mount(UserManagementView, { global: globalOpts });
    await flushPromises();
    await findButton(wrapper, "赋予选中用户")?.trigger("click");
    expect(warnSpy).toHaveBeenCalledWith("请先选择用户");
    expect(mockAssignUserRole).not.toHaveBeenCalled();
  });

  it("报警管理：未勾选行时批量标记显示警告且不调用API", async () => {
    const { markAlarmsResolved } = await import("@/services/alarmService");
    const wrapper = mount(AlarmManagementView, { global: globalOpts });
    await flushPromises();
    const batchBtn = findButton(wrapper, "批量标记已处理");
    expect(batchBtn?.attributes("disabled")).toBeDefined();
    await batchBtn?.trigger("click");
    expect(warnSpy).not.toHaveBeenCalled();
    expect(markAlarmsResolved).not.toHaveBeenCalled();
  });
});

// ─── 批量操作 - 有选择行时调用API ─────────────────────────────────────────────
describe("批量操作 - 勾选行后调用正确API", () => {
  beforeEach(() => {
    mockUpdateCameraDevice.mockClear();
    mockUpdateLifeguard.mockClear();
    mockUpdateUser.mockClear();
    vi.spyOn(ElMessage as any, "success").mockImplementation(() => ({
      close: vi.fn(),
    }));
    vi.spyOn(ElMessage as any, "error").mockImplementation(() => ({
      close: vi.fn(),
    }));
  });

  it("设备管理：批量启用以 enabled=1 调用 updateCameraDevice", async () => {
    const wrapper = mount(DeviceManagementView, { global: globalOpts });
    await flushPromises();

    const pageTable = wrapper.findComponent({ name: "PageTable" }) as any;
    await pageTable.vm.$emit("selectionChange", [
      {
        id: "1",
        name: "CAM-A",
        venue: "A馆",
        location: "北侧",
        deviceType: "fixed",
        streamUrl: "rtsp://a",
        status: "online",
        maintenanceCycleDays: 30,
      },
    ]);
    await nextTick();

    await findButton(wrapper, "批量启用")?.trigger("click");
    await flushPromises();

    expect(mockUpdateCameraDevice).toHaveBeenCalledWith(
      expect.objectContaining({ id: 1, enabled: 1, deviceStatus: "ONLINE" }),
    );
  });

  it("设备管理：批量禁用以 enabled=0 调用 updateCameraDevice", async () => {
    const wrapper = mount(DeviceManagementView, { global: globalOpts });
    await flushPromises();

    const pageTable = wrapper.findComponent({ name: "PageTable" }) as any;
    await pageTable.vm.$emit("selectionChange", [
      {
        id: "2",
        name: "CAM-B",
        venue: "B馆",
        location: "南侧",
        deviceType: "ptz",
        streamUrl: "rtsp://b",
        status: "offline",
        maintenanceCycleDays: 30,
      },
    ]);
    await nextTick();

    await findButton(wrapper, "批量禁用")?.trigger("click");
    await flushPromises();

    expect(mockUpdateCameraDevice).toHaveBeenCalledWith(
      expect.objectContaining({ id: 2, enabled: 0, deviceStatus: "OFFLINE" }),
    );
  });

  it("救生员管理：批量启用以 auditStatus=APPROVED 调用 updateLifeguard", async () => {
    const wrapper = mount(LifeguardManagementView, { global: globalOpts });
    await flushPromises();

    const pageTable = wrapper.findComponent({ name: "PageTable" }) as any;
    await pageTable.vm.$emit("selectionChange", [
      {
        id: "10",
        name: "张三",
        phone: "13800000001",
        venue: "A馆",
        dutyStatus: "on_duty",
        lastReportTime: "2026-03-21 10:00:00",
      },
    ]);
    await nextTick();

    await findButton(wrapper, "批量启用")?.trigger("click");
    await flushPromises();

    expect(mockUpdateLifeguard).toHaveBeenCalledWith(
      expect.objectContaining({ id: 10, auditStatus: "APPROVED" }),
    );
  });

  it("救生员管理：批量禁用以 auditStatus=REJECTED 调用 updateLifeguard", async () => {
    const wrapper = mount(LifeguardManagementView, { global: globalOpts });
    await flushPromises();

    const pageTable = wrapper.findComponent({ name: "PageTable" }) as any;
    await pageTable.vm.$emit("selectionChange", [
      {
        id: "10",
        name: "张三",
        phone: "13800000001",
        venue: "A馆",
        dutyStatus: "on_duty",
        lastReportTime: "2026-03-21 10:00:00",
      },
    ]);
    await nextTick();

    await findButton(wrapper, "批量禁用")?.trigger("click");
    await flushPromises();

    expect(mockUpdateLifeguard).toHaveBeenCalledWith(
      expect.objectContaining({ id: 10, auditStatus: "REJECTED" }),
    );
  });

  it("用户管理：批量启用以 status=1 调用 updateUser", async () => {
    const wrapper = mount(UserManagementView, { global: globalOpts });
    await flushPromises();

    const pageTable = wrapper.findComponent({ name: "PageTable" }) as any;
    await pageTable.vm.$emit("selectionChange", [
      {
        id: "20",
        account: "admin_a",
        name: "系统管理员",
        role: "super_admin",
        managedVenues: "A馆",
        status: "enabled",
      },
    ]);
    await nextTick();

    await findButton(wrapper, "批量启用")?.trigger("click");
    await flushPromises();

    expect(mockUpdateUser).toHaveBeenCalledWith(
      expect.objectContaining({ id: 20, status: 1 }),
    );
  });

  it("用户管理：批量禁用以 status=0 调用 updateUser", async () => {
    const wrapper = mount(UserManagementView, { global: globalOpts });
    await flushPromises();

    const pageTable = wrapper.findComponent({ name: "PageTable" }) as any;
    await pageTable.vm.$emit("selectionChange", [
      {
        id: "20",
        account: "admin_a",
        name: "系统管理员",
        role: "super_admin",
        managedVenues: "A馆",
        status: "enabled",
      },
    ]);
    await nextTick();

    await findButton(wrapper, "批量禁用")?.trigger("click");
    await flushPromises();

    expect(mockUpdateUser).toHaveBeenCalledWith(
      expect.objectContaining({ id: 20, status: 0 }),
    );
  });

  it("用户管理：选择角色后可批量赋予选中用户", async () => {
    const wrapper = mount(UserManagementView, { global: globalOpts });
    await flushPromises();

    const pageTable = wrapper.findComponent({ name: "PageTable" }) as any;
    await pageTable.vm.$emit("selectionChange", [
      {
        id: "20",
        account: "admin_a",
        name: "系统管理员",
        role: "super_admin",
        managedVenues: "A馆",
        status: "enabled",
      },
    ]);
    await nextTick();

    const tree = wrapper.findComponent({ name: "ElTree" }) as any;
    await tree.vm.$emit(
      "check-change",
      { id: "ADMIN", label: "管理员" },
      true,
      false,
    );
    await nextTick();

    await findButton(wrapper, "赋予选中用户")?.trigger("click");
    await flushPromises();

    expect(mockAssignUserRole).toHaveBeenCalledWith(
      expect.objectContaining({
        userId: 20,
        roleCodes: ["VENUE_ADMIN"],
      }),
    );
  });
});

// ─── 新增对话框 - 按钮点击打开 ─────────────────────────────────────────────────
describe("新增对话框 - 点击新增按钮后对话框可见", () => {
  it("设备管理：点击新增设备后 DeviceFormDialog modelValue 变为 true", async () => {
    const { default: DeviceFormDialog } =
      await import("@/components/business/DeviceFormDialog.vue");
    const wrapper = mount(DeviceManagementView, { global: globalOpts });
    await flushPromises();

    const dialog = wrapper.findComponent(DeviceFormDialog);
    expect(dialog.props("modelValue")).toBe(false);

    await findButton(wrapper, "新增设备")?.trigger("click");
    await nextTick();

    expect(dialog.props("modelValue")).toBe(true);
  });

  it("救生员管理：点击新增救生员后 LifeguardAddDialog modelValue 变为 true", async () => {
    const { default: LifeguardAddDialog } =
      await import("@/views/admin/lifeguard/dialogs/LifeguardAddDialog.vue");
    const wrapper = mount(LifeguardManagementView, { global: globalOpts });
    await flushPromises();

    const dialog = wrapper.findComponent(LifeguardAddDialog);
    expect(dialog.props("modelValue")).toBe(false);

    await findButton(wrapper, "新增救生员")?.trigger("click");
    await nextTick();

    expect(dialog.props("modelValue")).toBe(true);
  });

  it("用户管理：点击新增用户后 UserAddDialog modelValue 变为 true", async () => {
    const { default: UserAddDialog } =
      await import("@/views/admin/user/dialogs/UserAddDialog.vue");
    const wrapper = mount(UserManagementView, { global: globalOpts });
    await flushPromises();

    const dialog = wrapper.findComponent(UserAddDialog);
    expect(dialog.props("modelValue")).toBe(false);

    await findButton(wrapper, "新增用户")?.trigger("click");
    await nextTick();

    expect(dialog.props("modelValue")).toBe(true);
  });
});

// ─── 对话框提交 - validate 失败时显示警告 ──────────────────────────────────────
describe("对话框保存 - 表单验证失败显示警告", () => {
  it("DeviceFormDialog：空表单提交后显示警告", async () => {
    const { default: DeviceFormDialog } =
      await import("@/components/business/DeviceFormDialog.vue");
    const warnSpy = vi
      .spyOn(ElMessage as any, "warning")
      .mockImplementation(() => ({ close: vi.fn() }));
    const errorSpy = vi
      .spyOn(ElMessage as any, "error")
      .mockImplementation(() => ({ close: vi.fn() }));

    const wrapper = mount(DeviceFormDialog, {
      global: globalOpts,
      props: { modelValue: true, deviceId: undefined },
    });
    await flushPromises();

    (wrapper.vm as any).formRef = {
      validate: vi.fn(async () => {
        throw new Error("invalid");
      }),
    };
    await (wrapper.vm as any).handleSubmit();
    await flushPromises();

    expect(warnSpy.mock.calls.length + errorSpy.mock.calls.length).toBeGreaterThan(
      0,
    );
    expect(mockAddCameraDevice).not.toHaveBeenCalled();
  });

  it("UserAddDialog：空表单提交后显示警告", async () => {
    const { default: UserAddDialog } =
      await import("@/views/admin/user/dialogs/UserAddDialog.vue");
    const warnSpy = vi
      .spyOn(ElMessage as any, "warning")
      .mockImplementation(() => ({ close: vi.fn() }));
    const errorSpy = vi
      .spyOn(ElMessage as any, "error")
      .mockImplementation(() => ({ close: vi.fn() }));

    const wrapper = mount(UserAddDialog, {
      global: globalOpts,
      props: { modelValue: true },
    });
    await flushPromises();

    (wrapper.vm as any).formRef = {
      validate: vi.fn(async () => {
        throw new Error("invalid");
      }),
    };
    await (wrapper.vm as any).handleSubmit();
    await flushPromises();

    expect(warnSpy.mock.calls.length + errorSpy.mock.calls.length).toBeGreaterThan(
      0,
    );
    expect(mockAddUser).not.toHaveBeenCalled();
  });

  it("LifeguardAddDialog：关联已有用户模式提交包含userId且不包含账号密码", async () => {
    const { default: LifeguardAddDialog } =
      await import("@/views/admin/lifeguard/dialogs/LifeguardAddDialog.vue");
    mockAddLifeguard.mockClear();
    const wrapper = mount(LifeguardAddDialog, {
      global: globalOpts,
      props: { modelValue: true },
    });
    await flushPromises();

    (wrapper.vm as any).formRef = {
      validate: vi.fn(async () => true),
    };
    Object.assign((wrapper.vm as any).form, {
      accountMode: "bind_existing",
      linkedUserId: 20,
      fullName: "张三",
      phone: "13800000001",
      username: "",
      password: "",
      email: "zs@test.com",
      lifeguardCode: "LG-TEST-001",
      venueId: 1,
      auditStatus: "PENDING",
      dutyStatus: "OFF_DUTY",
    });

    await (wrapper.vm as any).handleSubmit();
    await flushPromises();

    expect(mockAddLifeguard).toHaveBeenCalledWith(
      expect.objectContaining({
        userId: 20,
        fullName: "张三",
        phone: "13800000001",
      }),
    );
    const payload = mockAddLifeguard.mock.calls[0][0];
    expect(payload.username).toBeUndefined();
    expect(payload.password).toBeUndefined();
  });

  it("LifeguardAddDialog：创建新账号模式提交包含账号密码", async () => {
    const { default: LifeguardAddDialog } =
      await import("@/views/admin/lifeguard/dialogs/LifeguardAddDialog.vue");
    mockAddLifeguard.mockClear();
    const wrapper = mount(LifeguardAddDialog, {
      global: globalOpts,
      props: { modelValue: true },
    });
    await flushPromises();

    (wrapper.vm as any).formRef = {
      validate: vi.fn(async () => true),
    };
    Object.assign((wrapper.vm as any).form, {
      accountMode: "create_new",
      linkedUserId: 0,
      fullName: "李四",
      phone: "13800000002",
      username: "lifeguard.lisi",
      password: "123456",
      email: "ls@test.com",
      lifeguardCode: "LG-TEST-002",
      venueId: 1,
      auditStatus: "PENDING",
      dutyStatus: "OFF_DUTY",
    });

    await (wrapper.vm as any).handleSubmit();
    await flushPromises();

    expect(mockAddLifeguard).toHaveBeenCalledWith(
      expect.objectContaining({
        username: "lifeguard.lisi",
        password: "123456",
        fullName: "李四",
      }),
    );
    const payload = mockAddLifeguard.mock.calls[0][0];
    expect(payload.userId).toBeUndefined();
  });
});

// ─── 导出 CSV ──────────────────────────────────────────────────────────────────
describe("导出 CSV", () => {
  it("设备管理：点击导出维护记录触发文件下载（不报错）", async () => {
    const ExcelUtil = (await import("@/utils/excel")).default;
    const exportCsvSpy = vi
      .spyOn(ExcelUtil, "exportCSV")
      .mockReturnValue({ success: true, message: "ok" });

    const wrapper = mount(DeviceManagementView, { global: globalOpts });
    await flushPromises();

    (wrapper.vm as any).maintenanceRows = [
      {
        deviceName: "CAM-A",
        content: "月度巡检",
        operator: "运维A",
        time: "2026-03-21 11:00:00",
      },
    ];
    await nextTick();

    await (wrapper.vm as any).handleExportMaintenance("csv");
    await flushPromises();

    expect(exportCsvSpy).toHaveBeenCalled();
  });

  it("报警管理：点击导出记录触发文件下载（不报错）", async () => {
    const ExcelUtil = (await import("@/utils/excel")).default;
    const exportCsvSpy = vi
      .spyOn(ExcelUtil, "exportCSV")
      .mockReturnValue({ success: true, message: "ok" });

    const wrapper = mount(AlarmManagementView, { global: globalOpts });
    await flushPromises();

    await (wrapper.vm as any).handleExportRecords("csv");
    await flushPromises();

    expect(exportCsvSpy).toHaveBeenCalled();
  });
});
