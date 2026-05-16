# STM32 OLED 显示 ESP32 IP 地址 — 设计文档

**日期**：2026-05-16

**目标**：在 STM32 的 OLED 屏幕上新增"网络"页面，显示 ESP32-CAM 的 WiFi IP 地址。ESP32 通过 UART 主动推送 IP，STM32 接收后存储并在 OLED 上展示。

---

## 协议扩展

在现有 UART 协议中新增一条 ESP32 → STM32 的单向推送命令：

- **格式**：`IP:<ip地址>\n`
- **示例**：`IP:192.168.1.100\n`
- **方向**：ESP32 → STM32，STM32 不回复
- **推送时机**：
  - ESP32 WiFi 连接成功时推送一次
  - WiFi 断开重连后 IP 变化时再推送一次

在 `protocol.h` 中新增常量：
- STM32 侧：`constexpr const char* CMD_IP = "IP:";`
- ESP32 侧：同上，共用 `protocol.h`

---

## STM32 侧改动

### UartHandler

在 `handleLine()` 中识别 `IP:` 前缀，提取 IP 字符串存入全局变量。

具体逻辑：
- 检查 `line.startsWith("IP:")`
- 提取 `IP:` 之后的子串
- 校验长度 <= 15（最大 "255.255.255.255"）
- 将 IP 字符串复制到外部传入的缓冲区

为使 `UartHandler` 能写入全局 IP 缓冲区，在构造时传入 `char*` 指针，或在 `begin()` 中增加 setter。推荐在构造函数中增加一个 `char* ipBuffer` 参数。

### OledViewModel.h

- `OledPage` 枚举新增 `Network = 3`
- `OledUiState` 新增字段 `char espIp[16]`（默认空字符串，表示未连接）

### OledViewModel.cpp

新增网络页渲染逻辑：

| 行 | 内容 | 说明 |
|----|------|------|
| 标题 | `网络` | 中文标题 |
| 行0 | `IP 192.168.1.100` | 有 IP 时显示地址 |
| 行0 | `IP 未连接` | 无 IP 时显示未连接 |
| 行1 | `WIFI OK` / `WIFI WAIT` | 根据 espIp 是否非空判断 |
| 行2 | 空 | 预留 |
| 行3 | `SHORT NEXT` | 提示可切页 |

### OledDisplay.cpp

新增中文字模 `网`、`络`。

### main.cpp

- 新增全局 `char g_espIp[16] = ""`
- `UartHandler` 构造时传入 `g_espIp` 缓冲区指针
- `buildUiState()` 中将 `g_espIp` 复制到 `OledUiState.espIp`
- `nextPage()` 循环加入 Network 页：`Status → Calibration → Battery → Network → Status`

---

## ESP32 侧改动

### UartBridge

新增 `sendIp(const String& ip)` 方法，发送 `IP:<ip>\n`。

### main.cpp

- 在 `connectWiFi()` 成功后调用 `g_uartBridge.sendIp(WiFi.localIP().toString())`
- WiFi 断线重连成功后同样推送

---

## 数据流

```
ESP32 WiFi 连接成功
  → UartBridge.sendIp("192.168.1.100")
  → UART 发送 "IP:192.168.1.100\n"
  → STM32 UartHandler.handleLine() 识别 "IP:" 前缀
  → 解析 IP 字符串存入 g_espIp
  → buildUiState() 写入 OledUiState.espIp
  → OledViewModel 生成网络页帧
  → OledDisplay 渲染到屏幕
```

---

## 页面切换顺序

Status → Calibration → Battery → Network → Status

---

## Flash 预算

| 项目 | 预估大小 |
|------|----------|
| 2 个中文字模（`网`、`络`） | 48 bytes |
| 协议解析 + ViewModel 分支 | ~200-300 bytes |
| **总增量** | **< 400 bytes** |

远在 Blue Pill 64KB Flash 余量内。

---

## 文件变更清单

### 新增文件
无

### 修改文件

| 文件 | 改动 |
|------|------|
| `firmware/stm32-ptz/include/protocol.h` | 新增 `CMD_IP` 常量 |
| `firmware/stm32-ptz/lib/uart_handler/UartHandler.h` | 构造函数增加 `ipBuffer` 参数，新增 `ipBuffer` 成员 |
| `firmware/stm32-ptz/lib/uart_handler/UartHandler.cpp` | `handleLine()` 识别 `IP:` 前缀，写入 IP 缓冲区 |
| `firmware/stm32-ptz/lib/oled_display/OledViewModel.h` | `OledPage` 新增 `Network`，`OledUiState` 新增 `espIp[16]` |
| `firmware/stm32-ptz/lib/oled_display/OledViewModel.cpp` | 新增网络页渲染逻辑 |
| `firmware/stm32-ptz/lib/oled_display/OledDisplay.cpp` | 新增 `网`、`络` 字模 |
| `firmware/stm32-ptz/src/main.cpp` | 新增 `g_espIp`，构造 `UartHandler` 传入 IP 缓冲区，`nextPage()` 加入 Network，`buildUiState()` 填充 espIp |
| `firmware/esp32-cam/include/protocol.h` | 新增 `CMD_IP` 常量（与 STM32 侧一致） |
| `firmware/esp32-cam/lib/uart_bridge/UartBridge.h` | 新增 `sendIp()` 声明 |
| `firmware/esp32-cam/lib/uart_bridge/UartBridge.cpp` | 实现 `sendIp()` |
| `firmware/esp32-cam/src/main.cpp` | WiFi 连接成功后调用 `sendIp()` |
