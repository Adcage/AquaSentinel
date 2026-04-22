# 前端与安卓 Mock 数据使用现状全面审计（2026-03-24）

## 1. 文档目的

本审计文档用于完整记录当前项目中“仍在使用 Mock 数据或占位逻辑”的位置、影响范围、证据路径，并明确每一块业务功能后续应接入的后端接口（精确到 `src/api` 中的控制器函数）。

该文档的目标是：

1. 作为后续联调改造的唯一事实清单，避免遗漏；
2. 让“页面功能 -> 服务层 -> 目标接口”的替换关系一目了然；
3. 区分“真实 Mock 数据源”和“页面静态占位数据/假逻辑”；
4. 同时覆盖 PC 管理端（Vue）与安卓端（Jetpack Compose）。

---

## 2. 审计范围与方法

### 2.1 审计范围

- PC 管理端源码：`2701775749013023376/frontend/src`
- 安卓端源码：`2701775749013023376/android/app/src`
- 已生成接口代码：`2701775749013023376/frontend/src/api`

### 2.2 关键词与线索

本次排查使用以下线索进行交叉验证：

- 关键词：`mock`、`Mock`、`TODO_MOCK_DATA`、`TODO_REAL_API`、`MOCK_PHASE`、`API_TODO`
- 代码结构：`src/mock/modules`、`src/services`、页面中的 `ref([...])` 静态数组
- 依赖关系：页面是否 import `@/services/*`，服务是否 import `@/mock/modules/*`
- 接口接入情况：业务代码是否存在 `from '@/api'` 引用

### 2.3 排除项

以下内容不纳入“业务 Mock 使用”结论：

- `node_modules`、`dist`、`build` 下的第三方/产物命中；
- 前端单测中的 `vi.mock(...)`（会单列说明，不算生产逻辑）；
- 一般意义上的 UI 视觉占位（但与业务数据耦合的占位会记录）。

---

## 3. 总结结论（先看）

### 3.1 前端（PC 管理端）

1. `src/api` 虽已生成完整接口代码，但业务层当前 **0 引用** `@/api`；
2. 管理端核心数据全部经过 `src/services/*`，而服务层目前全部直接读取 `src/mock/modules/*`；
3. 管理端多个页面还存在“本地静态数组/演示流程”，即使不在 `src/mock` 目录，也属于假数据阶段；
4. 登录、注册仍是演示提交逻辑，未接入真实鉴权与验证码接口。

### 3.2 安卓端

1. 当前安卓端数据层只有 `data/mock/MockRepositories.kt`，没有远程数据层；
2. 主要页面（首页、报警、定位、记录、我的）全部直接读取 `Mock*Repository`；
3. 关键交互（刷新、视频重连、报警处置提交、登录）均为占位逻辑；
4. 构建依赖中未引入 Retrofit/OkHttp/Ktor 等网络库，说明尚未开始真实 API 接入。

---

## 4. 前端 Mock 使用明细（生产代码）

## 4.1 Mock 数据源文件（6 个）

以下文件是当前管理端业务数据的原始 Mock 源：

1. `2701775749013023376/frontend/src/mock/modules/dashboard.ts`
2. `2701775749013023376/frontend/src/mock/modules/alarm.ts`
3. `2701775749013023376/frontend/src/mock/modules/device.ts`
4. `2701775749013023376/frontend/src/mock/modules/lifeguard.ts`
5. `2701775749013023376/frontend/src/mock/modules/statistics.ts`
6. `2701775749013023376/frontend/src/mock/modules/user.ts`

数据特征：

- 包含完整业务列表（报警/设备/用户/救生员等）；
- 含典型 mock 字段，如设备流地址 `mock://camera/...`（`device.ts`）；
- 被服务层直接 import 并用于过滤、分页、返回。

## 4.2 服务层（全部仍为 Mock 读法）

### 4.2.1 `dashboardService.ts`

文件：`2701775749013023376/frontend/src/services/dashboardService.ts`

- 当前：
  - `getDashboardMetrics()` 返回 `dashboardMetricsMock`
  - `getCameraGrid()` 返回 `cameraGridMock`
  - 含模拟延迟 `sleep()`
- 目标：改为调用 `statsController` + `cameraDeviceController` +（可选）`eventStreamController`

### 4.2.2 `alarmService.ts`

文件：`2701775749013023376/frontend/src/services/alarmService.ts`

- 当前：
  - `getAlarmPage(query)`：在 `alarmRecordsMock` 本地过滤并分页
  - `markAlarmsResolved(ids)`：仅返回 `true`
  - 含 `sleep()`
- 目标：改为 `alertActionController.listByPage` + `alertActionController.action/actions`

### 4.2.3 `deviceService.ts`

文件：`2701775749013023376/frontend/src/services/deviceService.ts`

- 当前：
  - `getDevicePage(query)`：本地过滤 `deviceRecordsMock`
  - `createDevice/updateDevice/removeDevice`：仅返回 `true`
  - 含 `sleep()`
- 目标：改为 `cameraDeviceController.*` + `cameraMaintenanceController.*`

### 4.2.4 `lifeguardService.ts`

文件：`2701775749013023376/frontend/src/services/lifeguardService.ts`

- 当前：
  - `getLifeguardPage(query)`：本地过滤 `lifeguardRecordsMock`
  - 含 `sleep()`
- 目标：改为 `lifeguardController.listLifeguardVoByPage`（轨迹/定位再接 `lifeguardLocationController`）

### 4.2.5 `statisticsService.ts`

文件：`2701775749013023376/frontend/src/services/statisticsService.ts`

- 当前：
  - `getStatisticsKpi/getAlarmTrend/getVenueRanking/getAlarmTypeDistribution` 全部读本地 mock
  - 含 `sleep()`
- 目标：改为 `statsController.getOverview/trend/ranking`

### 4.2.6 `userService.ts`

文件：`2701775749013023376/frontend/src/services/userService.ts`

- 当前：
  - `getUserPage(query)`：本地过滤 `userRecordsMock`
  - 含 `sleep()`
- 目标：改为 `userController.listUserPageVo`

## 4.3 页面层的“非 src/mock 目录”假数据/占位

这些不是 `src/mock/modules`，但本质仍是非真实后端数据：

1. `DeviceManagementView.vue`
   - 维护记录 `maintenanceRows` 为本地数组
   - 文件：`2701775749013023376/frontend/src/views/admin/device/DeviceManagementView.vue`

2. `StatisticsView.vue`
   - 导出记录 `exportRows` 为本地数组
   - 文件：`2701775749013023376/frontend/src/views/admin/statistics/StatisticsView.vue`

3. `UserManagementView.vue`
   - 角色权限树 `roleTree` 本地写死
   - 文件：`2701775749013023376/frontend/src/views/admin/user/UserManagementView.vue`

4. `ProfileView.vue`
   - `profile` 与 `loginRows` 本地写死
   - 文件：`2701775749013023376/frontend/src/views/admin/profile/ProfileView.vue`

5. `SystemSettingsView.vue`
   - `form` 与 `logs` 本地写死
   - 文件：`2701775749013023376/frontend/src/views/admin/settings/SystemSettingsView.vue`

6. `LoginView.vue`
   - 登录为演示流程（`Promise.resolve(formState)`）
   - 验证码刷新仅本地随机串
   - 文件：`2701775749013023376/frontend/src/views/LoginView.vue`

7. `RegisterView.vue`
   - 注册为演示流程（`Promise.resolve(formState)`）
   - 文件：`2701775749013023376/frontend/src/views/RegisterView.vue`

8. `CameraGridCard.vue`
   - 画面区域是“视频流占位”文案，交互未接路由/真实流
   - 文件：`2701775749013023376/frontend/src/components/business/CameraGridCard.vue`

## 4.4 前端业务代码未接入 `src/api` 的证据

审计结果：`frontend/src` 内未检索到 `from '@/api'` 或 `from "@/api"` 的业务引用。

结论：

- 你已生成的接口层代码是完整可用的；
- 但目前仍处于“接口层存在、业务层未接线”的阶段。

---

## 5. 安卓端 Mock 使用明细（生产代码）

## 5.1 唯一数据层：`data/mock/MockRepositories.kt`

文件：

- `2701775749013023376/android/app/src/main/java/com/vision/swimsafe/data/mock/MockRepositories.kt`

包含对象：

- `MockHomeRepository`
- `MockAlarmRepository`
- `MockLocationRepository`
- `MockRecordRepository`
- `MockProfileRepository`

说明：

- 文件内明确标记 `MOCK_PHASE`、`API_TODO`；
- 报警详情、定位、个人页数据均来源于该文件内本地对象；
- 没有仓储抽象（Repository 接口）和远程实现并存架构，属于单一路径 mock 数据。

## 5.2 页面直接依赖 MockRepository 的位置

1. 首页
   - 文件：`2701775749013023376/android/app/src/main/java/com/vision/swimsafe/ui/screens/home/HomeScreen.kt`
   - `MockHomeRepository.getHomeUiState()`

2. 报警中心
   - 文件：`2701775749013023376/android/app/src/main/java/com/vision/swimsafe/ui/screens/alarm/AlarmCenterScreen.kt`
   - `MockAlarmRepository.getAlarmCenterUiState()`

3. 报警详情
   - 文件：`2701775749013023376/android/app/src/main/java/com/vision/swimsafe/ui/screens/alarm/AlarmDetailScreen.kt`
   - `MockAlarmRepository.getAlarmDetailUiState(alarmId)`

4. 定位页
   - 文件：`2701775749013023376/android/app/src/main/java/com/vision/swimsafe/ui/screens/location/LocationScreen.kt`
   - `MockLocationRepository.getLocationUiState()`

5. 报警记录页
   - 文件：`2701775749013023376/android/app/src/main/java/com/vision/swimsafe/ui/screens/record/RecordScreen.kt`
   - `MockRecordRepository.getAlarmRecordUiState()`

6. 我的页
   - 文件：`2701775749013023376/android/app/src/main/java/com/vision/swimsafe/ui/screens/profile/ProfileScreen.kt`
   - `MockProfileRepository.getProfileUiState()`

## 5.3 安卓端关键占位交互

1. 首页“刷新”按钮无真实行为
   - `HomeScreen.kt` 内 `MOCK_PHASE`

2. 报警详情“重新加载视频”无真实行为
   - `AlarmDetailScreen.kt` 内 `MOCK_PHASE`

3. 报警详情“确认提交”无真实处置接口提交
   - `AlarmDetailScreen.kt` 内 `MOCK_PHASE`

4. 登录页默认账号/密码预填
   - `LoginScreen.kt` 内 `MOCK_PHASE`

## 5.4 安卓网络能力现状

- `android/app/build.gradle.kts` 依赖中未见 Retrofit/OkHttp/Ktor；
- `android/.../data` 目录仅有 `mock` 子目录；
- 说明：安卓端尚未建立远程 API 基础设施。

---

## 6. 前端已生成接口总览（可直接接线）

接口入口：

- `2701775749013023376/frontend/src/api/index.ts`

当前可用于本次改造的核心控制器：

1. 认证：`authController`
2. 报警：`alertActionController`、`alertRecordController`、`alertDisposalController`
3. 设备：`cameraDeviceController`、`cameraMaintenanceController`
4. 救生员：`lifeguardController`、`lifeguardLocationController`、`lifeguardDutyController`
5. 用户与权限：`userController`、`roleController`、`accessControlController`
6. 统计：`statsController`、`statsSnapshotController`
7. 实时/流相关：`eventStreamController`、`monitorTaskController`
8. 运营支撑：`systemAuditLogController`、`venueController`、`venueZoneController`

---

## 7. 逐页面“Mock -> 接口”详细映射（重点）

说明：本节按照“页面功能点”给出建议接线接口，优先复用你当前已生成函数名。

## 7.1 管理端登录页 `LoginView.vue`

当前状态：

- 验证码本地随机串；
- 登录提交为演示 `Promise.resolve`。

建议接口：

1. 获取验证码：
   - 函数：`authController.getCaptcha()`
   - 路径：`GET /auth/captcha`
   - 返回：`API.BaseResponseCaptchaVO`
   - 关键字段：`captchaId`、`captchaImageBase64`

2. 管理员登录：
   - 函数：`authController.adminLogin(body)`
   - 路径：`POST /auth/admin/login`
   - 请求类型：`API.AdminLoginRequest`
   - 返回：`API.BaseResponseLoginResultVO`
   - 关键字段：`accessToken`、`refreshToken`、`user`

## 7.2 注册页 `RegisterView.vue`

当前状态：

- 注册为演示提交。

建议接口：

1. 验证码：`authController.getCaptcha()`
2. 注册提交：
   - 函数：`authController.register(body)`
   - 路径：`POST /auth/register`
   - 请求类型：`API.RegisterRequest`

## 7.3 监控总览页 `AdminDashboardView.vue`

当前状态：

- KPI 与摄像头网格来自 `dashboardService` 的 mock。

建议接口组合：

1. KPI 总览：
   - 函数：`statsController.getOverview(params)`
   - 路径：`GET /stats/overview`
   - 请求类型：`API.getOverviewParams`

2. 摄像头列表（基础信息）：
   - 函数：`cameraDeviceController.listCameraDeviceVoByPage(body)`
   - 路径：`POST /cameras/list/page/vo`
   - 请求类型：`API.CameraDeviceQueryRequest`

3. 每路视频的标注流地址（可选增强）：
   - 函数：`eventStreamController.getAnnotatedStream(params)`
   - 路径：`GET /events/annotated-stream`
   - 请求类型：`API.getAnnotatedStreamParams`

4. 事件告警热度（可选）：
   - 函数：`eventStreamController.listEvents(params)`
   - 路径：`GET /events`
   - 请求类型：`API.listEventsParams`

## 7.4 报警管理页 `AlarmManagementView.vue`

当前状态：

- 列表本地过滤分页；
- 批量已处理按钮不触达真实后端。

建议接口：

1. 报警分页列表：
   - 函数：`alertActionController.listByPage(body)`
   - 路径：`POST /alerts/list/page`
   - 请求类型：`API.AlertRecordQueryRequest`
   - 返回：`API.BaseResponsePageAlertRecordVO`

2. 指派处理：
   - 函数：`alertActionController.assign(params, body)`
   - 路径：`POST /alerts/{id}/assign`

3. 状态操作（已处理/误报等）：
   - 函数：`alertActionController.actions(params, body)` 或 `alertActionController.action(body)`
   - 路径：`POST /alerts/{id}/actions` 或 `POST /alerts/action`

4. 查看详情：
   - 函数：`alertActionController.getAlertById(params)`
   - 路径：`GET /alerts/{id}`

5. 处置记录（时间线/日志）：
   - 函数：`alertDisposalController.listAlertDisposalVoByPage(body)`
   - 路径：`POST /alert-disposals/list/page/vo`

## 7.5 设备管理页 `DeviceManagementView.vue`

当前状态：

- 列表、增删改均 mock；
- 维护记录为页面静态数组。

建议接口：

1. 设备分页：
   - 函数：`cameraDeviceController.listCameraDeviceVoByPage(body)`
   - 路径：`POST /cameras/list/page/vo`

2. 新增设备：`cameraDeviceController.addCameraDevice(body)`（`POST /cameras/add`）
3. 编辑设备：`cameraDeviceController.updateCameraDevice(body)`（`POST /cameras/update`）
4. 删除设备：`cameraDeviceController.deleteCameraDevice(body)`（`POST /cameras/delete`）

5. 维护记录分页：
   - 函数：`cameraMaintenanceController.listByCamera(params)`
   - 路径：`GET /cameras/maintenance/{cameraId}`
   - 或 `cameraMaintenanceController.listCameraMaintenanceLogVoByPage(body)`

6. 新增维护记录：
   - 函数：`cameraMaintenanceController.addCameraMaintenanceLogByCamera(params, body)`
   - 路径：`POST /cameras/maintenance/{cameraId}`

## 7.6 救生员管理页 `LifeguardManagementView.vue`

当前状态：

- 列表 mock；
- 地图与轨迹按钮占位。

建议接口：

1. 救生员分页：
   - 函数：`lifeguardController.listLifeguardVoByPage(body)`
   - 路径：`POST /lifeguards/list/page/vo`

2. 在岗状态更新：
   - 函数：`lifeguardController.updateDutyStatus(body)` 或 `updateDutyStatusByPath(params, body)`

3. 最近定位轨迹：
   - 函数：`lifeguardController.recentLocations(params)`
   - 路径：`GET /lifeguards/location/recent`

4. 详细定位日志分页：
   - 函数：`lifeguardLocationController.listLifeguardLocationLogVoByPage(body)`

5. 离岗报备（如需在该页处理）：
   - 函数：`lifeguardController.submitLeaveReport(body)`

## 7.7 用户管理页 `UserManagementView.vue`

当前状态：

- 用户列表 mock；
- 角色树本地常量；
- 批量启用/禁用为占位按钮。

建议接口：

1. 用户分页：
   - 函数：`userController.listUserPageVo(body)`
   - 路径：`POST /users/list/page/vo`

2. 新增用户：`userController.addUser(body)`
3. 编辑用户：`userController.updateUser(body)`
4. 删除用户：`userController.deleteUser(body)`

5. 用户角色分配：
   - 函数：`accessControlController.assignUserRole(body)`
   - 路径：`POST /users/assign/role`

6. 角色分页（填充角色树来源）：
   - 函数：`roleController.listRolePageVo(body)`

7. 权限树更新：
   - 函数：`accessControlController.updateRolePermission(body)`

## 7.8 统计分析页 `StatisticsView.vue`

当前状态：

- KPI/趋势/分布/排名全部 mock；
- 导出记录静态数组。

建议接口：

1. KPI：`statsController.getOverview(params)`
2. 趋势：`statsController.trend(body)`
3. 排名：`statsController.ranking(params)`
4. 导出 CSV：`statsController.exportCsv(body)`
5. 导出 Excel：`statsController.exportExcel(body)`
6. 历史快照（可用于导出记录页）：
   - `statsSnapshotController.listStatsSnapshotVoByPage(body)`

## 7.9 个人中心页 `ProfileView.vue`

当前状态：

- 资料、登录记录都是静态数据。

建议接口：

1. 更新个人资料/密码：
   - 函数：`accessControlController.updateMyProfile(body)`
   - 路径：`POST /users/update/my`

2. 退出登录：
   - 函数：`authController.logout(body)`
   - 路径：`POST /auth/logout`

3. 登录记录来源（若后端暂未提供专门登录日志 API）：
   - 临时可从 `systemAuditLogController.listSystemAuditLogVoByPage(body)` 按类别筛选

## 7.10 系统设置页 `SystemSettingsView.vue`

当前状态：

- 基础设置、阈值、日志都为本地常量。

可接入接口（现有可复用）：

1. 日志管理：
   - `systemAuditLogController.listSystemAuditLogVoByPage(body)`

2. 基础场馆信息（可作为“基础设置”一部分）：
   - `venueController.getVenueVoById(params)` / `venueController.updateVenue(body)`

3. 阈值配置：
   - 当前 `src/api` 未看到明确“系统参数配置”专用接口；
   - 可选策略：
     - A：先通过后端补充 `system-config` 相关接口；
     - B：临时复用现有实体不推荐（语义不清，维护成本高）。

---

## 8. 安卓端“页面功能 -> 目标接口”映射建议

注意：安卓端尚未生成接口代码，以下使用前端已生成控制器作为“后端契约参考”。

## 8.1 登录页

- 验证码：`authController.getCaptcha()`
- 登录：`authController.login(body)` 或救生员专用 `lifeguardController.lifeguardLogin(body)`

## 8.2 首页（在岗状态 + 今日报警）

- 在岗状态：`lifeguardController.getLifeguardVoById(params)` + `lifeguardController.offPostCheck(body)`
- 今日报警摘要：`statsController.getOverview(params)`（按场馆和日期）

## 8.3 报警中心/报警详情/处置

- 列表：`alertActionController.listByPage(body)`
- 详情：`alertActionController.getAlertById(params)`
- 指派：`alertActionController.assign(params, body)`
- 状态更新：`alertActionController.actions(params, body)`
- 处置日志：`alertDisposalController.listAlertDisposalVoByPage(body)`

## 8.4 定位页

- 定位上报：`lifeguardController.reportLocation(body)` 或 `reportLocationByPath(params, body)`
- 最近轨迹：`lifeguardController.recentLocations(params)`
- 定位日志：`lifeguardLocationController.listLifeguardLocationLogVoByPage(body)`

## 8.5 报警记录页

- 报警分页：`alertActionController.listByPage(body)`（可按时间、状态、关键词）

## 8.6 我的页

- 资料更新：`accessControlController.updateMyProfile(body)`
- 退出登录：`authController.logout(body)`

---

## 9. 现有测试中的 Mock 绑定（后续要同步改）

## 9.1 前端测试

文件：`2701775749013023376/frontend/src/tests/adminPages.test.ts`

- 对 `@/services/*` 做了 `vi.mock(...)`；
- 现阶段用于 UI 完整性测试合理；
- 若服务层改为真实接口后，建议：
  - 保留组件测试但改成 mock `@/api/*`；
  - 或新增服务层单测验证映射与转换逻辑。

## 9.2 安卓测试

文件：`2701775749013023376/android/app/src/test/java/com/vision/swimsafe/NavigationAndMockDataTest.kt`

- 测试名即 `NavigationAndMockDataTest`，直接依赖 `Mock*Repository`；
- 引入真实接口后建议拆分：
  - 导航与 UI 状态测试；
  - Repository 单元测试（本地 fake + 网络 fake）；
  - 废弃对具体 Mock 仓库的强耦合断言。

---

## 10. 风险清单与注意事项

1. 类型映射风险：
   - 现有 `src/types/business.ts` 与 `src/api/typings.d.ts` 字段命名并不一致（camelCase 与业务别名混用）；
   - 接入时必须在服务层做 DTO -> ViewModel 转换，不能直接把 API 类型暴露给页面。

2. 统计口径风险：
   - `统计页` 的趋势/分布/排名可能需要多个接口聚合；
   - 需先确定后端返回结构是否与当前图表组件输入一致。

3. 系统设置接口缺口：
   - 目前未看到明确“系统阈值配置”专用接口；
   - 这块不是前端改造能闭环的问题，需后端补齐。

4. 安卓改造量较大：
   - 当前无网络层依赖与基础架构；
   - 需要先补基础设施，再逐屏替换。

---

## 11. 建议的改造优先级（供执行阶段使用）

P0（必须先做）

1. 前端 `services/*` 全部去 mock，接入 `src/api/*`；
2. 登录/注册接入 `authController`。

P1（高优先）

1. 报警、设备、救生员、用户、统计 5 个核心管理页完成真实数据联通；
2. 清理页面静态假数据（维护记录、导出记录、角色树等）。

P2（中优先）

1. 个人中心、系统设置细节能力完善；
2. 监控大盘“实时流与事件”联动增强。

P3（安卓专项）

1. 建立安卓网络层与仓储层；
2. 逐屏替换 MockRepository；
3. 同步更新单测。

---

## 12. 核心证据路径索引（便于复核）

### 12.1 前端

- Mock 源目录：`2701775749013023376/frontend/src/mock/modules`
- Mock 服务目录：`2701775749013023376/frontend/src/services`
- 管理页目录：`2701775749013023376/frontend/src/views/admin`
- 登录页：`2701775749013023376/frontend/src/views/LoginView.vue`
- 注册页：`2701775749013023376/frontend/src/views/RegisterView.vue`
- 摄像头卡片：`2701775749013023376/frontend/src/components/business/CameraGridCard.vue`
- 已生成接口：`2701775749013023376/frontend/src/api`

### 12.2 安卓

- Mock 数据层：`2701775749013023376/android/app/src/main/java/com/vision/swimsafe/data/mock/MockRepositories.kt`
- 页面目录：`2701775749013023376/android/app/src/main/java/com/vision/swimsafe/ui/screens`
- 导航：`2701775749013023376/android/app/src/main/java/com/vision/swimsafe/ui/navigation/AppNavGraph.kt`
- 构建依赖：`2701775749013023376/android/app/build.gradle.kts`

---

## 13. 最终结论

你当前判断完全正确：

1. 管理端虽然已经生成了后端接口代码，但业务逻辑仍在使用 mock；
2. 安卓端尚未生成/接入接口，仍是全量 mock 状态；
3. 项目已具备“前端先快速接线”的条件，因为 `src/api` 已较完整；
4. 安卓端需要先完成网络基础设施再改业务页面，不建议直接硬改 UI 文件。

本文件可直接作为后续改造执行清单的依据。
