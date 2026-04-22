# PC管理端UI补齐与标记 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 按文档补齐 PC 管理端可见页面内容与样式，并为 mock/待接后端位置增加统一可检索标记。

**Architecture:** 以现有 Vue3 + Element Plus 页面为基础，不重做整体架构，优先补齐文档明确要求的页面结构、字段、按钮、筛选项、操作区和视觉状态。对仍依赖 mock 的页面保留当前交互，同时通过统一标记常量与显式文案让后续 API 替换可全局搜索。

**Tech Stack:** Vue 3、TypeScript、Element Plus、Vite、Vitest（新增用于 TDD）

---

### Task 1: 建立测试基线与标记规范

**Files:**
- Modify: `2701775749013023376/frontend/package.json`
- Modify: `2701775749013023376/frontend/src/constants/integrationMarkers.ts`
- Create: `2701775749013023376/frontend/vitest.config.ts`
- Create: `2701775749013023376/frontend/src/tests/uiMarkers.test.ts`

**Step 1: Write the failing test**
- 为统一标记常量和 mock/API 提示文案写失败测试。

**Step 2: Run test to verify it fails**
- 运行 Vitest 单测，确认测试因文件/导出缺失失败。

**Step 3: Write minimal implementation**
- 补充测试依赖、Vitest 配置、统一标记常量与帮助函数。

**Step 4: Run test to verify it passes**
- 再次运行对应测试，确认通过。

### Task 2: 补齐登录/注册认证界面 UI

**Files:**
- Modify: `2701775749013023376/frontend/src/views/LoginView.vue`
- Modify: `2701775749013023376/frontend/src/views/RegisterView.vue`
- Create: `2701775749013023376/frontend/src/tests/authViews.test.ts`

**Step 1: Write the failing test**
- 断言登录页存在验证码输入、验证码图片区、刷新按钮、连接说明。
- 断言注册页存在姓名、角色、验证码、说明标记。

**Step 2: Run test to verify it fails**

**Step 3: Write minimal implementation**
- 以后台商务风重做认证页细节，补齐文档要求字段和 mock/API 标记。

**Step 4: Run test to verify it passes**

### Task 3: 补齐后台整体框架与总览页

**Files:**
- Modify: `2701775749013023376/frontend/src/layouts/BackendLayout.vue`
- Modify: `2701775749013023376/frontend/src/router/admin.ts`
- Modify: `2701775749013023376/frontend/src/views/admin/dashboard/AdminDashboardView.vue`
- Modify: `2701775749013023376/frontend/src/components/business/CameraGridCard.vue`
- Modify: `2701775749013023376/frontend/src/components/business/MetricCard.vue`
- Create: `2701775749013023376/frontend/src/tests/dashboardLayout.test.ts`

**Step 1: Write the failing test**
- 断言总览页为 5 张统计卡、存在监控网格操作区、报警横幅与 mock 标记。

**Step 2: Run test to verify it fails**

**Step 3: Write minimal implementation**
- 补齐顶栏 logo 区、菜单层级、总览卡片与摄像头卡片信息层和操作区。

**Step 4: Run test to verify it passes**

### Task 4: 补齐业务列表与详情样式页

**Files:**
- Modify: `2701775749013023376/frontend/src/views/admin/device/DeviceManagementView.vue`
- Modify: `2701775749013023376/frontend/src/views/admin/lifeguard/LifeguardManagementView.vue`
- Modify: `2701775749013023376/frontend/src/views/admin/alarm/AlarmManagementView.vue`
- Modify: `2701775749013023376/frontend/src/views/admin/user/UserManagementView.vue`
- Create: `2701775749013023376/frontend/src/tests/adminPages.test.ts`

**Step 1: Write the failing test**
- 对关键页面断言新增的筛选项、工具栏、表格列、详情抽屉或地图占位存在。

**Step 2: Run test to verify it fails**

**Step 3: Write minimal implementation**
- 逐页补齐文档要求的字段、按钮、批量操作、详情展示和 mock/API 标记。

**Step 4: Run test to verify it passes**

### Task 5: 补齐统计分析、系统设置、个人中心 UI

**Files:**
- Modify: `2701775749013023376/frontend/src/views/admin/statistics/StatisticsView.vue`
- Modify: `2701775749013023376/frontend/src/views/admin/settings/SystemSettingsView.vue`
- Modify: `2701775749013023376/frontend/src/views/admin/profile/ProfileView.vue`
- Update: `2701775749013023376/frontend/src/tests/adminPages.test.ts`

**Step 1: Write the failing test**
- 断言统计页存在图表切换/下载、系统设置存在二级导航、个人中心存在资料编辑和修改密码表单。

**Step 2: Run test to verify it fails**

**Step 3: Write minimal implementation**
- 按文档补齐页面结构和说明区，并统一接入 mock/API 标记。

**Step 4: Run test to verify it passes**

### Task 6: 全量验证

**Files:**
- Verify only

**Step 1: Run tests**
- `npm run test -- --run`

**Step 2: Run type/build verification**
- `npm run build`

**Step 3: Record actual status**
- 根据命令输出整理 UI 已补齐内容与仍留给后端联调的标记位置。
