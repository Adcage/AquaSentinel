# 全局临时切换到 MJPEG 预览设计

## 背景

当前监控页 WebRTC 预览链路已经完成后端代理、WHIP 信令和上游拉流接入，但浏览器侧 ICE/DTLS 在本机多网卡与 mDNS 候选环境下仍无法完成连接，导致画面持续黑屏。

与此同时，图片抓拍与单帧导出链路工作正常，说明问题集中在 WebRTC 网络协商层，而不是 ESP32-CAM 上游视频源、video-hub-service 拉流能力或 JPEG 帧内容本身。

为了先恢复监控画面可用性，决定临时放弃 WebRTC 预览，统一切换回已经存在且稳定的 MJPEG 后端代理链路。

## 目标

1. 全局预览模式改为 `backend_proxy`
2. 前端监控页不再发起 `/api/video-hub/cameras/{id}/whip` 请求
3. 前端统一走 `/api/streams/cameras/{id}/preview` 预览地址
4. 不删除现有 WebRTC 代码，仅停止默认启用，保留后续继续排查与恢复的空间

## 方案

### 方案选择

采用“全局临时切换到 MJPEG/后端代理”方案。

### 具体改动

1. 修改 `frontend/.env.development`
   - 将 `VITE_CAMERA_PREVIEW_MODE` 从 `webrtc` 改为 `backend_proxy`

2. 修改 `frontend/.env.production`
   - 将 `VITE_CAMERA_PREVIEW_MODE` 从 `webrtc` 改为 `backend_proxy`

### 与当前实现的差异

- 修改前：`resolveCameraPreviewTarget()` 在开发和生产环境下优先解析 WebRTC 地址，`CameraGridCard` 会进入 `WebRtcWhepPlayer` 分支，触发 WHIP 建连。
- 修改后：同一套组件会直接进入 MJPEG 分支，使用 `<img>` 加载后端代理流地址，不再触发 WebRTC 协商。

## 影响范围

### 会变化的行为

- 所有监控预览默认改为 MJPEG
- 浏览器网络面板中将不再出现 `/api/video-hub/cameras/{id}/whip`
- 会出现 `/api/streams/cameras/{id}/preview` 长连接请求

### 不变的行为

- backend 到 video-hub-service 的代理结构保持不变
- yolo-service 的 `snapshot` 取帧逻辑保持不变
- 已写好的 WebRTC 调试代码与链路保持在仓库中，不做删除

## 风险与取舍

1. MJPEG 带宽与后端转发压力高于 WebRTC
2. 但它实现简单、可观测性更强，且当前项目里已有稳定链路
3. 对当前目标“先恢复监控画面”来说，收益显著高于继续卡在 WebRTC 本地网络环境问题上

## 验证标准

1. 监控页刷新后可直接出图
2. 前端不再输出 `WebRtcWhepPlayer` 的建连日志
3. 浏览器网络面板中能看到 `/api/streams/cameras/{id}/preview`
4. 黑屏问题消失，即使帧率较低也应能稳定看到画面
