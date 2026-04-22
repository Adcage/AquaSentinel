# 校内学习资料交流共享系统 - 前端

本项目是基于 Vue 3 + Vite + Element Plus 开发的校内学习资料交流共享系统前端。系统采用双布局架构，分别为学生端（前台）和管理员端（后台）。

## 🌟 系统核心功能

### 1. 学生端 (Student Client)
- **资料广场**：支持多维筛选（类型、专业、成色）、关键词搜索、分页浏览。
- **发布资料**：支持图片上传、成色标注、价格设置（免费/收费）、交接方式选择。
- **交换管理**：支持发起交换申请、在线议价、确认交接、评价反馈。
- **个人中心**：管理个人信息、查询历史评价、提交校园身份认证。

### 2. 管理员端 (Admin Dashboard)
- **数据总览**：可视化展示资料曝光、点击、订单及成交总额（GMV）等统计数据。
- **审核管理**：对学生发布的资料进行人工审核，确保内容合规。
- **交易监管**：监控全站交换订单，强制干预异常交易。
- **纠纷处理**：人工调解用户投诉，判定责任归属。
- **用户管理**：管理用户权限、审核校园认证申请。

## 🎨 视觉规范
- **商务风定位**：克制、清晰、高对比度。
- **纯色体系**：主色调使用企业蓝 (#1f5fae)，全面替代渐变色。
- **图标标准化**：统一使用 Element Plus 图标库或组件化 SVG。
- **无 Emoji 约束**：保持界面严肃商务感。

## 🚀 技术实现
- **架构**：Vue 3 Composition API + TypeScript 严格模式。
- **UI 组件**：Element Plus + `@element-plus/icons-vue`。
- **布局**：BasicLayout (前台) + BackendLayout (后台)。
- **接口**：严格复用 `src/api` 自动生成的 OpenAPI 请求函数。
- **状态**：Pinia 管理登录态与权限。
- **工具**：集成 DateUtil, ExcelUtil, PDFUtil 等高效开发工具。

## 🛠 开发命令

```bash
# 安装依赖
npm install

# 启动开发环境
npm run dev

# 构建生产版本
npm run build

# 单元测试
npm run test -- --run

# 端到端流程测试
npm run test:e2e

# 同步后端接口 (OpenAPI)
npm run openapi
```

## 📂 核心目录
- `src/api/`: 自动生成的后端接口定义。
- `src/views/material/`: 资料相关业务页面。
- `src/views/admin/`: 后台管理业务页面。
- `src/layouts/`: 双布局组件实现。
- `src/styles/theme.css`: 全局商务风 CSS 变量。

## License
MIT
