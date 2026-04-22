# AI防溺水PC管理端（先样式+Mock） Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在 `frontend` 内先完成商务风管理端可视化与核心交互，统一使用 Mock 数据，并通过可检索标记实现后续快速替换真实后端接口。

**Architecture:** 采用“视图层（views）→ 服务层（services）→ 数据层（mock）”分层。页面只依赖 service，不直接引用 mock 数据，确保后续替换真实 API 只改 service。通过统一标记 `TODO_REAL_API` 和命名约定，提供一键全局检索替换入口。

**Tech Stack:** Vue 3 + TypeScript + Element Plus + Vue Router + Pinia + ECharts + Axios（现有项目栈）

---

### Task 1: 建立商务风主题变量与全局视觉基线

**Files:**
- Modify: `src/styles/theme.css`
- Modify: `src/main.ts`（若需补充全局样式导入）

**Step 1: 扩展主题变量**

- 在 `src/styles/theme.css` 中补齐主色、功能色、中性色、圆角、阴影、间距变量。
- 禁止新增任何渐变色变量与使用场景。

**Step 2: 统一基础样式基线**

- 规范 `body`、`#app`、全局背景色、文字色。
- 校验管理端页面在 1280+ 分辨率可正常显示。

**Step 3: 标注设计约束**

- 在主题文件内增加简短注释：禁止 Emoji、禁止渐变、优先图标库组件。

---

### Task 2: 重构管理端布局为目标商务风框架

**Files:**
- Modify: `src/layouts/BackendLayout.vue`

**Step 1: 顶栏结构对齐规范**

- 调整为 56px 顶栏，左侧系统标题，右侧连接状态/告警入口/用户区。
- 保留现有路由跳转逻辑，不引入后端鉴权依赖。

**Step 2: 侧栏风格与菜单交互**

- 侧栏使用深色背景，激活项高亮，支持展开/收起。
- 保持基于 `router/admin.ts` 自动生成菜单的机制。

**Step 3: 主内容区标准化**

- 主内容区背景与卡片留白统一（24px 间距）。
- 确保移动端/窄屏至少不破版（PC 主场景优先）。

---

### Task 3: 搭建 Mock 分层与可替换标记规范

**Files:**
- Create: `src/mock/modules/dashboard.ts`
- Create: `src/mock/modules/device.ts`
- Create: `src/mock/modules/alarm.ts`
- Create: `src/services/dashboardService.ts`
- Create: `src/services/deviceService.ts`
- Create: `src/services/alarmService.ts`
- Create: `src/types/business.ts`（如需统一业务类型）

**Step 1: 统一 Mock 出口结构**

- 每个模块导出类型化 mock 数据与查询函数（支持分页、筛选参数占位）。

**Step 2: 建立 service 抽象层**

- 页面仅调用 `services/*Service.ts`。
- 每个 service 方法都加统一标记：

```ts
// TODO_REAL_API: replace mock service with api.<module>.<method>
```

**Step 3: 定义替换约定文档片段**

- 在 service 文件顶部说明：真实接口接入时仅替换当前函数实现，不改函数签名。

---

### Task 4: 补齐管理端路由与菜单信息架构

**Files:**
- Modify: `src/router/admin.ts`
- Create: `src/views/admin/dashboard/AdminDashboardView.vue`
- Create: `src/views/admin/device/DeviceManagementView.vue`
- Create: `src/views/admin/lifeguard/LifeguardManagementView.vue`
- Create: `src/views/admin/alarm/AlarmManagementView.vue`
- Create: `src/views/admin/user/UserManagementView.vue`
- Create: `src/views/admin/statistics/StatisticsView.vue`
- Create: `src/views/admin/settings/SystemSettingsView.vue`
- Create: `src/views/admin/profile/ProfileView.vue`

**Step 1: 设计菜单分组**

- 监控总览、业务管理、统计分析、系统设置、个人中心。

**Step 2: 先创建页面骨架**

- 所有页面先输出统一页头、筛选区、卡片容器、空状态占位。

**Step 3: 接线路由与菜单图标**

- 使用 `@element-plus/icons-vue` 图标。
- 不允许在业务页面直接内联 `<svg>`。

---

### Task 5: 首页监控总览（样式优先 + Mock数据）

**Files:**
- Modify/Create: `src/views/admin/dashboard/AdminDashboardView.vue`
- Create: `src/components/business/MetricCard.vue`
- Create: `src/components/business/CameraGridCard.vue`

**Step 1: 统计卡片区（5卡）**

- 在线设备、今日报警、未处理报警、在岗救生员、实时人数。
- 数据来自 `dashboardService`。

**Step 2: 摄像头网格区**

- 使用 mock 视频占位块（非真实流）。
- 报警状态卡片边框闪烁（纯色动画，不使用渐变）。

**Step 3: 告警视觉优先级**

- 未处理和报警中的模块使用危险色和强调样式。

---

### Task 6: 报警管理页面（筛选 + 表格 + 批量操作占位）

**Files:**
- Modify/Create: `src/views/admin/alarm/AlarmManagementView.vue`
- Create: `src/components/business/PageTable.vue`（如现有无可复用表格壳）
- Reuse/Modify: `src/components/common/StatusTag.vue`

**Step 1: 筛选区组件化**

- 时间范围、场馆、报警类型、处理状态、关键词。

**Step 2: 列表区接 mock**

- 支持分页、多选、批量“标记已处理”（前端模拟成功反馈）。

**Step 3: 未处理高亮规则**

- 未处理行强化显示（红色标签、文字强调）。

---

### Task 7: 设备管理页面（筛选 + 列表 + 操作弹窗占位）

**Files:**
- Modify/Create: `src/views/admin/device/DeviceManagementView.vue`
- Create: `src/components/business/DeviceFormDialog.vue`

**Step 1: 列表与筛选**

- 场馆、设备状态、设备类型筛选；20条分页。

**Step 2: 新增/编辑/删除交互占位**

- 对话框表单与二次确认。
- 当前阶段仅 mock 提交，不接真实接口。

**Step 3: 状态颜色映射统一**

- 统一复用状态映射函数，不在页面散落硬编码颜色。

---

### Task 8: 统一业务状态映射与格式化工具

**Files:**
- Modify: `src/utils/businessFormatters.ts`
- Modify: `src/components/common/StatusTag.vue`

**Step 1: 扩展状态映射**

- 设备状态、报警状态、救生员在岗状态三类映射。

**Step 2: 对齐视觉规范**

- 返回 label + tag type + 可选强调样式字段。

**Step 3: 替换页面内临时映射**

- 统一调用 formatter，避免重复逻辑。

---

### Task 9: 预留真实后端接入缝位（不接入）

**Files:**
- Modify: `src/request.ts`
- Modify/Create: `src/api/index.ts`（仅在现有结构允许下补充导出）
- Create: `src/constants/integrationMarkers.ts`（可选）

**Step 1: 请求层增强最小化**

- 加入统一错误提示基础骨架（401/403/500）。

**Step 2: 约定 service 替换模板**

- 在每个 service 提供“mock实现”和“真实接口实现注释模板”。

**Step 3: 全局检索可达性验证**

- 确保搜索 `TODO_REAL_API` 可定位所有待替换点。

---

### Task 10: 一次性验证（所有代码完成后执行）

**Files:**
- 无新增文件（执行命令验证）

**Step 1: 类型与构建检查**

Run: `npm run build`

Expected: 构建成功，无阻塞错误。

**Step 2: 手工验证关键页面**

- 登录页、管理端首页、报警管理、设备管理可正常访问。
- 菜单跳转、筛选交互、mock数据渲染正常。

**Step 3: 验证替换标记完整性**

- 全局检索 `TODO_REAL_API`，确认覆盖全部 service 方法。

---

## 交付与约束说明

- 全程不执行 git 操作。
- 遵循“先完成全部代码，再统一构建验证”的执行顺序。
- 前端视觉遵循商务风：不使用 emoji、不使用渐变、图标优先使用 `@element-plus/icons-vue`。
- 自定义图标如需补充，统一放在 `src/components/icons/`，页面内禁止内联 SVG。
