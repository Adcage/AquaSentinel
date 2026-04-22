# Element Plus 全量替换 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将前端项目从 Ant Design Vue 完整迁移到 Element Plus，并完全移除 `ant-design-vue` 与 `@ant-design/icons-vue` 依赖，保证现有页面功能与路由行为可用。

**Architecture:** 采用“先全量改代码、后一次性验证”的分层迁移方式：先替换入口与全局配置，再替换基础页面、表单、后台布局、复杂演示页，最后清理依赖与文档。迁移期间不执行 Git 操作，所有代码任务完成后统一执行编译验证。

**Tech Stack:** Vue 3 + TypeScript + Vite + Element Plus + @element-plus/icons-vue + ECharts

---

## 约束与执行规则

1. 全程禁止执行任何 Git 操作（`git status`、`git add`、`git commit` 等均不执行）。
2. 必须先完成全部代码修改，再统一执行一次验证命令，不允许“每改一文件就编译一次”。
3. 验证仅在最后执行：`npm run build`。

### Task 1: 迁移入口与全局配置

**Files:**
- Modify: `src/main.ts`
- Modify: `src/App.vue`

**Step 1: 替换 UI 库注册**

在 `src/main.ts` 中移除 Antd 注册与样式导入，改为 Element Plus：
- 删除 `import Antd from 'ant-design-vue'`
- 删除 `import 'ant-design-vue/dist/reset.css'`
- 新增 `import ElementPlus from 'element-plus'`
- 新增 `import 'element-plus/dist/index.css'`
- `app.use(Antd)` 改为 `app.use(ElementPlus)`

**Step 2: 替换全局 ConfigProvider**

在 `src/App.vue` 中：
- `a-config-provider` 替换为 `el-config-provider`
- locale 从 Ant 的 `zhCN` 改为 Element Plus 的 `zhCn`

**Step 3: 保留现有主题变量**

保留 `src/styles/theme.css`，后续页面样式继续复用已有商务色板，避免视觉回退。

### Task 2: 替换基础页面组件（低风险）

**Files:**
- Modify: `src/views/HomeView.vue`
- Modify: `src/views/PlaceholderView.vue`
- Modify: `src/components/NavBar.vue`
- Modify: `src/components/common/StatusTag.vue`
- Modify: `src/components/common/EmptyState.vue`

**Step 1: 替换 Home 页组件**

在 `src/views/HomeView.vue` 将以下组件替换：
- `a-space` -> `el-space`
- `a-button` -> `el-button`
- `a-row`/`a-col` -> `el-row`/`el-col`
- `a-card` -> `el-card`

**Step 2: 替换 404 页面结果组件**

在 `src/views/PlaceholderView.vue`：
- `a-result` 改为 Element 结构（`el-result`）
- `#extra` 插槽改为 `#extra` 对应 `el-result`

**Step 3: 替换导航栏按钮/间距组件**

在 `src/components/NavBar.vue`：
- `a-space` -> `el-space`
- `a-button` -> `el-button`

**Step 4: 替换通用状态组件**

在 `src/components/common/StatusTag.vue`：
- `a-tag` -> `el-tag`
- 校正 `type` 与 `color` 的兼容映射（Element 的 `type` 语义与 Ant 不完全一致）

**Step 5: 替换空状态组件**

在 `src/components/common/EmptyState.vue`：
- `a-empty` -> `el-empty`
- 图标由 `InboxOutlined` 改为 `@element-plus/icons-vue` 对应图标组件

### Task 3: 替换登录与注册表单（中风险）

**Files:**
- Modify: `src/views/LoginView.vue`
- Modify: `src/views/RegisterView.vue`

**Step 1: 替换表单容器与校验模型**

将 `a-form` / `a-form-item` 改为 `el-form` / `el-form-item`，并引入：
- `FormInstance`
- `FormRules`

将 `@finish` 提交流程调整为 `formRef.validate()` 后再提交。

**Step 2: 替换输入框与前缀图标**

- `a-input` -> `el-input`
- `a-input-password` -> `el-input type="password" show-password`
- 图标插槽按 Element 方式改为 `#prefix`

**Step 3: 替换消息提示 API**

- `message.success/error` -> `ElMessage.success/error`

**Step 4: 修正样式深度选择器**

- `.ant-input-affix-wrapper` 替换为 `.el-input__wrapper`

### Task 4: 替换后台布局与菜单系统（高风险）

**Files:**
- Modify: `src/layouts/BackendLayout.vue`
- Modify: `src/router/admin.ts`

**Step 1: 迁移布局容器**

在 `src/layouts/BackendLayout.vue` 替换：
- `a-layout` 系列 -> `el-container` / `el-header` / `el-aside` / `el-main`

**Step 2: 迁移菜单组件与路由联动**

- `a-menu` / `a-sub-menu` / `a-menu-item` -> `el-menu` / `el-sub-menu` / `el-menu-item`
- 保持 `route.name` 与 `menu index` 的映射，确保高亮与跳转行为一致

**Step 3: 替换路由图标来源**

在 `src/router/admin.ts`：
- 移除 `DashboardOutlined`
- 改为 `@element-plus/icons-vue` 中的 `DataBoard`（或等价图标）

### Task 5: 替换仪表盘卡片组件

**Files:**
- Modify: `src/components/dashboard/StatCard.vue`

**Step 1: 替换卡片、提示与进度条**

- `a-card` -> `el-card`
- `a-tooltip` -> `el-tooltip`
- `a-progress` -> `el-progress`

**Step 2: 替换图标组件**

- `QuestionCircleOutlined` / `CaretUpOutlined` 替换为 Element 图标

### Task 6: 替换 Dashboard 页面组件

**Files:**
- Modify: `src/views/DashboardView.vue`

**Step 1: 替换栅格与卡片**

- `a-row` / `a-col` -> `el-row` / `el-col`
- `a-card` -> `el-card`

**Step 2: 替换步骤条与按钮区**

- `a-steps` / `a-step` -> `el-steps` / `el-step`
- `a-button` -> `el-button`

**Step 3: 替换页头组件**

- `a-page-header` 改为 Element 可替代结构（`el-page-header` 或自定义标题区）

### Task 7: 替换 DateUtilDemo 页面组件

**Files:**
- Modify: `src/views/DateUtilDemoView.vue`

**Step 1: 替换 Tabs / Card / Space**

- `a-tabs` / `a-tab-pane` -> `el-tabs` / `el-tab-pane`
- `a-card` -> `el-card`
- `a-space` -> `el-space`

**Step 2: 替换描述列表与标签**

- `a-descriptions` / `a-descriptions-item` -> `el-descriptions` / `el-descriptions-item`
- `a-tag` -> `el-tag`

### Task 8: 替换 UtilsDemo 页面组件（最高风险）

**Files:**
- Modify: `src/views/UtilsDemoView.vue`

**Step 1: 替换消息与加载**

- `message.*` -> `ElMessage.*`
- `message.loading(..., 0)` + `message.destroy()` -> `ElLoading.service(...)` + `loading.close()`

**Step 2: 替换上传组件逻辑**

- `a-upload` -> `el-upload`
- `before-upload` 迁移为 Element 回调签名（必要时使用 `UploadRawFile` 类型）

**Step 3: 替换表格与分页**

- `a-table :columns` 方案改为 `el-table` + 多个 `el-table-column`
- 统一处理 `key/dataIndex` 到 `prop/label` 的映射

**Step 4: 替换 Tabs / Alert / Input / Button 等基础控件**

- `a-tabs`、`a-alert`、`a-input`、`a-button`、`a-card`、`a-space` 全量替换为 Element 对应组件

### Task 9: 清理依赖与更新文档

**Files:**
- Modify: `package.json`
- Modify: `package-lock.json`
- Modify: `README.md`
- Search/Verify: `src/**/*.{vue,ts}`

**Step 1: 移除 Ant 相关依赖**

从 `package.json` 删除：
- `ant-design-vue`
- `@ant-design/icons-vue`

新增（若未安装）：
- `element-plus`
- `@element-plus/icons-vue`

**Step 2: 清理未使用图标库**

确认 `@fortawesome/*` 是否仍被引用；若完全无引用，移除依赖与 `main.ts` 注册代码。

**Step 3: 更新 README 技术栈描述**

将 `README.md` 中 Ant 相关描述改为 Element Plus，保持商务风规范说明不变。

### Task 10: 统一验证（仅最后执行一次）

**Files:**
- Verify: 全项目

**Step 1: 安装/更新依赖（如有变更）**

Run: `npm install`
Expected: 依赖安装成功，无 `ant-design-vue` 安装项。

**Step 2: 一次性执行构建验证**

Run: `npm run build`
Expected: Vite 构建成功，无 TypeScript 报错，无 `ant-design-vue` 或 `@ant-design/icons-vue` 模块解析错误。

**Step 3: 扫描残留引用（验证项）**

检查目标：
- `src` 中不再出现 `ant-design-vue` 与 `@ant-design/icons-vue` import
- 模板中不再出现 `a-` 前缀组件标签

Expected: 无残留引用。
