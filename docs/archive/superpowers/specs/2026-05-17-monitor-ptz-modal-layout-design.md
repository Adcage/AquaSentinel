# 监控页面优化设计：布局调整、模态框 PTZ 控制、云台页面重构

## 概述

对前端监控总览页面和云台控制测试页面进行三项优化：
1. 3x3 网格画面太窄 — 优化卡片内部空间分配
2. 点击画面弹出模态框 — 集成 PTZ 云台控制（后端代理链路）
3. 云台控制页面布局割裂 — 改为上视频下操作的垂直布局

---

## 问题 1：3x3 网格画面优化

### 现状

- `AdminDashboardView` 使用 Element Plus 24 栅格，3x3 时 `span=8`（每卡 1/3 宽度）
- `CameraGridCard` 内部 padding 偏大，信息区域占据垂直空间，视频画面被压缩变扁
- 卡片无明确宽高比约束，视频区域高度随内容撑开

### 方案

- **视频区域设固定宽高比**：`aspect-ratio: 16/9`，确保视频等比撑满卡片宽度
- **压缩卡片 padding**：从 12px 减至 8px
- **信息栏精简**：检测人数和风险等级用行内标签，减少垂直占用
- 保持三列布局不变，通过内部空间优化让视频区域更高

### 涉及文件

- `frontend/src/components/business/CameraGridCard.vue` — 调整内部布局和样式

---

## 问题 2：点击画面弹出模态框 + PTZ 控制

### 现状

- `CameraGridItem` 转换时丢弃了 `protocol` 字段，无法判断设备是否可控
- PTZ 控制链路两条并存：前端直连（ptzControlService）和后端代理（controlCameraPtz），仅直连被使用
- 后端代理链路已完整实现：`POST /cameras/control/ptz` → `Esp32PtzControlService` → 转发到 ESP32

### 方案

#### 2.1 数据层补全

- `CameraGridItem` 类型新增 `protocol?: string` 字段
- `dashboardService.toCameraGridItem()` 补充传递 `protocol` 字段

#### 2.2 新建 CameraDetailModal 组件

**位置**：`frontend/src/components/business/CameraDetailModal.vue`

**布局结构**（上视频下信息）：

```
┌─────────────────────────────────┐
│  ✕                              │  ← 关闭按钮
│                                 │
│       视频预览（放大）            │  ← 复用 WebRtcWhepPlayer / img
│       aspect-ratio: 16/9        │
│                                 │
├─────────────────────────────────┤
│  设备名称  位置  检测人数  风险   │  ← 基本信息
├─────────────────────────────────┤
│  [PTZ 控制面板]                  │  ← 仅 protocol === 'PTZ' 时显示
│  方向盘(上下左右)  回中  状态    │
└─────────────────────────────────┘
```

**PTZ 控制面板功能**：
- 方向控制：上/下/左/右微调（NUDGE），每个方向按钮触发 `controlCameraPtz({ cameraId, action: 'NUDGE', direction, step: 5 })`
- 回中按钮：`action: 'HOME'`
- 状态查询按钮：`action: 'STATUS'`，展示返回的 pan/tilt 角度
- 所有控制走后端代理链路 `cameraDeviceController.controlCameraPtz`

**非 PTZ 设备**：`protocol !== 'PTZ'` 时，控制面板区域完全不渲染，模态框仅显示视频预览 + 基本信息。

#### 2.3 事件流

1. `CameraGridCard` 添加点击事件 `@click`，emit `camera-click` 事件携带 `CameraGridItem`
2. `AdminDashboardView` 监听事件，设置 `selectedCamera` 并打开模态框
3. 模态框接收 `CameraGridItem` 作为 prop，根据 `protocol` 决定是否显示 PTZ 面板

### 涉及文件

- `frontend/src/types/business.ts` — `CameraGridItem` 新增 `protocol` 字段
- `frontend/src/services/dashboardService.ts` — `toCameraGridItem()` 传递 `protocol`
- `frontend/src/components/business/CameraGridCard.vue` — 添加点击事件
- `frontend/src/components/business/CameraDetailModal.vue` — 新建模态框组件
- `frontend/src/views/admin/dashboard/AdminDashboardView.vue` — 集成模态框

---

## 问题 3：云台控制测试页布局重构

### 现状

- `PtzControlTestView` 左右分栏：左边短视频预览，右边一长串控制按钮
- 视频区域窄小，操作区域过长，割裂感强

### 方案

改为**上视频下操作**的垂直布局：

```
┌──────────────────────────────────────────┐
│  设备 IP 输入 + 连接按钮                   │  ← 顶部工具栏
├──────────────────────────────────────────┤
│                                          │
│         视频预览（撑满宽度）               │  ← aspect-ratio: 16/9
│                                          │
├──────────────────────────────────────────┤
│  ┌─────────┐  ┌──────┐  ┌──────┐        │
│  │ 方向盘   │  │ 回中  │  │ 状态  │       │  ← 主控制行
│  │ ↑       │  │      │  │      │        │
│  ← ↓ →    │  └──────┘  └──────┘        │
│  └─────────┘                             │
├──────────────────────────────────────────┤
│  校准控制（可折叠）                        │  ← 折叠面板
│  开始校准 | PAN 脉宽 | TILT 脉宽 |        │
│  保存 | 退出 | 重置                       │
└──────────────────────────────────────────┘
```

- 视频预览撑满宽度，高度用 `aspect-ratio: 16/9` 或 max-height 约束
- 主控制区横向排列：方向盘居左，功能按钮组居右
- 校准控制放在可折叠面板（`el-collapse`）中，默认收起，需要时展开

### 涉及文件

- `frontend/src/views/admin/device/PtzControlTestView.vue` — 重构布局

---

## 技术约束

- PTZ 控制走后端代理链路 `controlCameraPtz`，需要 `cameraId`
- 后端 `Esp32PtzControlService` 从 `stream_url` 解析设备 IP（仅支持 HTTP 开头的 URL）
- 后端 PTZ 端点要求 `VENUE_ADMIN` 角色权限
- `CameraDeviceVO.protocol` 字段已有，值为 `"PTZ"` 表示可控设备，其他值（如 `"RTSP"`）表示固定枪机
- 视频播放需兼容三种协议：`webrtc`、`mjpeg`、`ws_jpeg`
- 禁用 Emoji，专业商务风格，纯色中性色

## 不在范围内

- 后端新增字段或接口
- 后端 PTZ 端点的 protocol 校验/限流/软删除检查（可作为后续优化）
- 校准功能的高级 UI（脉宽滑块等，保持现有功能即可）
