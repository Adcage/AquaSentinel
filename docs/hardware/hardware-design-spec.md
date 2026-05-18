# AquaSentinel 硬件设计方案

> 本文档定义防溺水监控系统的硬件部分完整规范，包括器件选型、引脚分配、接线方案、通信协议、项目结构和校准机制。
> 后端软件对接部分参见 `hardware-integration-guide.md`。

---

## 1. 硬件清单

| 序号 | 器件 | 型号/规格 | 数量 | 备注 |
|------|------|-----------|------|------|
| U1 | ESP32-CAM | AI-Thinker + MB 底板 | 1 | 视频+网络+UART通信 |
| U2 | STM32 | STM32F103C8T6 Blue Pill | 1 | 云台控制+OLED+按键 |
| M1 | 舵机 | SG90 9g 微型（Tower Pro） | 2 | PAN(水平) + TILT(垂直) |
| D1 | OLED | SSD1306 0.96" 128x64 I2C | 1 | 4引脚版本 |
| PWR | 供电 | TP4056+升压5V模块 + 3.7V 锂电池 | 1 | 主供电 |
| BAT_SENSE | 电量指示 | 4LED 电量指示板 | 1 | 直连电池 |
| SW1 | 用户按键 | 轻触按键 | 1 | 校准/回中 |
| — | 导线 | 杜邦线公母头若干 | — | — |
| — | 面包板 | 400孔或800孔 | 1 | 原型验证 |
| — | (推荐)电容 | 470uF/10V 电解电容 | 1 | 功能验证可省，稳定版必加 |
| — | (推荐)电容 | 100uF/10V 电解电容 | 1 | ESP32-CAM 旁路 |
| — | (可选)电阻 | 2x 100kΩ 1/4W + 1x 100nF 陶瓷电容 | 1套 | 电池ADC分压 |

---

## 2. 整体架构

```
                    ┌──────────────────────────────────────────┐
                    │           后端服务 (服务器)               │
                    │   Backend (8300) + YOLO Service (5000)    │
                    └────────────┬─────────────────────────────┘
                                 │ WiFi (MJPEG 流 + 控制指令)
                    ┌────────────┴────────────────┐
                    │        ESP32-CAM (U1)         │
                    │  · 摄像头采集 → MJPEG 视频流   │
                    │  · WiFi 连接后端              │
                    │  · 接收云台控制指令            │
                    │  · GPIO13 (UART2 TX) ─┐       │
                    │  · GPIO14 (UART2 RX) ──┤       │
                    └──────────────────────┼───────┘
                                           │ UART 115200
                    ┌──────────────────────┴───────┐
                    │     STM32F103C8T6 (U2)        │
                    │  · PA9  (USART1 TX) ──────────┤──→ ESP32 GPIO14
                    │  · PA10 (USART1 RX) ←─────────┤──← ESP32 GPIO13
                    │  · PA6  (TIM3_CH1) ──┐        │
                    │  · PA7  (TIM3_CH2) ──┤        │
                    │  · PB6  (I2C1_SCL) ──┤        │
                    │  · PB7  (I2C1_SDA) ──┤        │
                    │  · PB12 (用户按键) ───┤        │
                    └──────────────────────┼────────┘
                              │            │         │
                    ┌─────────┴──┐  ┌──────┴───┐  ┌──┴──────────┐
                    │ 舵机×2 SG90 │  │ OLED 4P  │  │ 用户按键 ├GND│
                    │ PAN + TILT  │  │ SSD1306  │  │ PB12──┘     │
                    └─────────────┘  └──────────┘  └─────────────┘
```

### 2.1 职责划分

| 模块 | 职责 |
|------|------|
| **ESP32-CAM** | 摄像头采集、MJPEG 视频流输出、WiFi 连接后端、接收控制指令、UART 转发指令给 STM32 |
| **STM32** | 双舵机 PWM 控制、角度限位与缓动、I2C 驱动 OLED 显示、用户按键检测、UART 接收/解析指令、校准参数 Flash 存储、(可选)电池 ADC 采集 |
| **后端** | 视频流接收与 AI 推理、控制指令下发、设备状态管理 |

---

## 3. 引脚配置

### 3.1 ESP32-CAM (AI-Thinker) 引脚分配

| 功能 | 引脚 | 方向 | 连接目标 | 备注 |
|------|------|------|---------|------|
| UART2 TX | GPIO 13 | 输出 | → STM32 PA10 (RX) | 安全引脚，无启动依赖 |
| UART2 RX | GPIO 14 | 输入 | ← STM32 PA9 (TX) | 安全引脚，无启动依赖 |
| 摄像头 | OV2640 | — | 板载连接 | 不可复用 |
| WiFi | 内置 | — | — | 不可复用 |
| 调试 TX | GPIO 1 | 输出 | USB(MB底板) | 仅开发调试 |
| 调试 RX | GPIO 3 | 输入 | USB(MB底板) | 仅开发调试 |

**不可用 / 慎用引脚：**

| 引脚 | 说明 |
|------|------|
| GPIO 0 | 启动模式控制，正常运行必须 HIGH，烧录必须 LOW |
| GPIO 2 | 启动时不能上拉，烧录时必须 LOW |
| GPIO 4 | 摄像头闪光灯，启动时短暂拉高 |
| GPIO 12 | 启动时决定闪存电压（HIGH=1.8V/LOW=3.3V），外部拉高会导致启动失败 |
| GPIO 15 | 启动时必须 LOW，会影响调试输出 |
| GPIO 16 | 被 PSRAM 占用，不可用 |

**为什么不用 UART0 (GPIO 1/3) 连 STM32：**

UART0 (GPIO 1/3) 保留给 MB 底板的 USB 串口用于调试和烧录。开发时需要同时看 ESP32-CAM 串口日志和与 STM32 通信，共用 UART0 会冲突，每次烧录需物理断开 STM32 连线。

### 3.2 STM32F103C8T6 (Blue Pill) 引脚分配

| 功能 | 引脚 | 复用功能 | 方向 | 连接目标 | 备注 |
|------|------|---------|------|---------|------|
| USART1 TX | PA9 | USART1_TX | 输出 | → ESP32 GPIO 14 (RX) | 与ESP32通信 |
| USART1 RX | PA10 | USART1_RX | 输入 | ← ESP32 GPIO 13 (TX) | 与ESP32通信 |
| 舵机 PAN | PA6 | TIM3_CH1 | 输出 | → 舵机1 信号线 | 水平云台 PWM |
| 舵机 TILT | PA7 | TIM3_CH2 | 输出 | → 舵机2 信号线 | 垂直云台 PWM |
| I2C 时钟 | PB6 | I2C1_SCL | 输出 | → OLED SCL | OLED 显示 |
| I2C 数据 | PB7 | I2C1_SDA | 双向 | → OLED SDA | OLED 显示 |
| 用户按键 | PB12 | GPIO_Input | 输入 | → 按键 → GND | 内部上拉，按下拉低 |
| 电池ADC | PA0 | ADC1_IN0 | 输入 | ← 分压电阻中点 | 可选，后期补 |

**未使用但保留的引脚（可用于扩展）：**

| 引脚 | 可能用途 |
|------|---------|
| PA2 / PA3 | USART2（调试串口） |
| PB0 / PB1 | TIM3_CH3 / CH4（额外 PWM） |
| PB3 / PB4 | SPI1（扩展 SPI 设备） |
| PA8 | TIM1_CH1（额外 PWM） |
| PC13 | 板载 LED（调试指示） |

---

## 4. 接线详表

### 4.1 电源连线

```
5V 升压模块输出端:
  ├── 5V  ────┬── ESP32-CAM 5V 引脚
  │            ├── STM32 Blue Pill 5V 引脚
  │            ├── 舵机1 红线 (VCC)
  │            └── 舵机2 红线 (VCC)
  │
  └── GND ────┬── ESP32-CAM GND
               ├── STM32 Blue Pill GND
               ├── 舵机1 棕线 (GND)
               ├── 舵机2 棕线 (GND)
               └── OLED GND
```

**必须共地**：所有模块的 GND 必须连接在一起。

**OLED 供电**：SSD1306 4引脚版支持 3.3V-5V，建议从 STM32 的 3.3V 引脚取电：

```
STM32 3.3V ──→ OLED VCC
STM32 GND   ──→ OLED GND
```

### 4.2 信号连线

```
ESP32-CAM ─────────── STM32F103C8T6

GPIO 13 (UART2 TX) ─→ PA10 (USART1_RX)    [ESP→STM32 数据]
GPIO 14 (UART2 RX) ←─ PA9  (USART1_TX)    [STM32→ESP32 数据]
```

```
STM32F103C8T6 ─────── 舵机

PA6 (TIM3_CH1) ──→ 舵机1 信号线(橙/黄色)   [PAN 水平]
PA7 (TIM3_CH2) ──→ 舵机2 信号线(橙/黄色)   [TILT 垂直]
```

```
STM32F103C8T6 ─────── OLED 4引脚

PB6 (I2C1_SCL) ──→ OLED SCL
PB7 (I2C1_SDA) ──→ OLED SDA
```

```
STM32F103C8T6 ─────── 用户按键

PB12 ──┬──→ 3.3V (STM32 内部上拉)
       │
    按键 ──→ GND
       
常态: PB12 = HIGH
按下: PB12 = LOW
```

### 4.3 电量指示板连线

```
锂电正极 ──→ 电量板 B+
锂电负极 ──→ 电量板 B-
(独立显示电池电量，不经过 STM32)
```

### 4.4 电池ADC分压（可选，后期补）

```
锂电正极 ──→ 100kΩ ──┬──→ STM32 PA0 (ADC)
                       │
                     100nF ──→ GND
                       │
                    100kΩ ──→ GND

ADC读数 = 电池电压 × 0.5
   4.2V → 2.1V → ADC 约 2610
   3.7V → 1.85V → ADC 约 2300
   3.0V → 1.5V → ADC 约 1860

换成毫伏: battery_mv = adc_value * 3300 * 2 / 4096
```

---

## 5. 关键设计要点

### 5.1 电压匹配

ESP32-CAM 和 STM32F103 都工作在 **3.3V 逻辑电平**，UART 之间直接连接，**不需要电平转换器**。SG90 舵机信号线接受 **3.3V TTL**，STM32 输出 3.3V PWM 可以正常驱动。

### 5.2 舵机供电

```
⚠️ 舵机红线(VCC)必须从 5V 主干线取电
⚠️ 不要从 STM32 的 5V 或 3.3V 引脚取电给舵机
⚠️ 不要从 ESP32-CAM 的 5V 或 3.3V 引脚取电给舵机
⚠️ 舵机信号线接 STM32 PA6/PA7，3.3V 逻辑无问题
```

### 5.3 ESP32-CAM 启动引脚

```
⚠️ GPIO 0  正常运行必须 HIGH（上拉），烧录必须 LOW
⚠️ GPIO 2  启动时不能上拉
⚠️ GPIO 12 启动时必须 LOW，HIGH 会导致闪存电压切换为 1.8V 而启动失败
⚠️ GPIO 15 启动时必须 LOW
   → 这就是为什么 UART2 不用 GPIO 12，改用 GPIO 13
```

### 5.4 I2C 上拉电阻

SSD1306 OLED 模块通常**板载 4.7kΩ 上拉电阻**，不需要外部上拉。如果 OLED 通信不稳定，在 SCL 和 SDA 上各加一个 4.7kΩ 上拉到 3.3V。

### 5.5 STM32 Blue Pill 3.3V 供电能力

Blue Pill 板载 3.3V 稳压器最大输出约 **150mA**，足够驱动 OLED（约 10-20mA）。不要用它驱动其他大电流负载。

### 5.6 电容建议（功能验证阶段可省，稳定版必加）

| 位置 | 容值 | 作用 |
|------|------|------|
| 舵机供电分支处 | 470uF/10V | 吸收舵机瞬态电流，防止电压跌落 |
| ESP32-CAM 5V 旁 | 100uF/10V | ESP32 WiFi 峰值电流缓冲 |
| OLED 3.3V 旁 | 100nF 陶瓷 | 高频去耦 |

功能验证阶段可以不焊电容，但如出现 ESP32 重启、断流、OLED 闪屏等问题，优先加 470uF 电容。

---

## 6. STM32 PWM 配置

### 6.1 TIM3 配置（双舵机共用定时器）

```
定时器时钟 = 72MHz
预分频 (PSC) = 71        → 计数频率 = 72MHz / 72 = 1MHz
自动重载 (ARR) = 19999    → PWM频率 = 1MHz / 20000 = 50Hz (周期20ms)

舵机控制脉宽：
  1.0ms → 0° (SG90)   → 比较值 = 1000
  1.5ms → 90° (中位)  → 比较值 = 1500
  2.0ms → 180° (SG90) → 比较值 = 2000

通道分配：
  TIM3_CH1 (PA6) → 舵机 PAN (水平)
  TIM3_CH2 (PA7) → 舵机 TILT (垂直)
```

### 6.2 角度到比较值映射

```c
uint32_t angle_to_compare(uint8_t angle) {
    if (angle < SERVO_MIN_ANGLE) angle = SERVO_MIN_ANGLE;
    if (angle > SERVO_MAX_ANGLE) angle = SERVO_MAX_ANGLE;
    return 1000 + (uint32_t)angle * 1000 / 180;
}
```

### 6.3 软件限位

```
PAN (水平):  10° ~ 170° (避免机械硬顶)
TILT (垂直): 20° ~ 160° (避免机械硬顶)
中位: PAN=90°, TILT=90°
速度档位: 1(最慢) ~ 10(最快)，默认5
```

---

## 7. 通信协议

### 7.1 串口参数

```
波特率：115200
数据位：8
停止位：1
校验位：None
流控：None
换行符：\n (LF)
```

### 7.2 ESP32-CAM → STM32 指令集

| 指令 | 格式 | 说明 |
|------|------|------|
| 云台控制 | `PTZ:<pan>,<tilt>,<speed>\n` | pan/tilt 为角度(0-180)，speed 为速度(1-10) |
| 回中 | `HOME\n` | 云台回到 90°,90° |
| 微调 | `NUDGE:<dir>,<step>\n` | dir: LEFT/RIGHT/UP/DOWN，step: 1-10° |
| 查询状态 | `STATUS?\n` | 请求 STM32 报告当前角度和状态 |
| 心跳 | `PING\n` | ESP32 定期发送，STM32 回 PONG |
| 进入校准 | `CALIB:START\n` | STM32 进入校准模式，停止正常响应 |
| 设置 PAN 脉冲 | `CALIB:PAN,<pulse_us>\n` | 直接设置 PAN 舵机脉冲宽度(us) |
| 设置 TILT 脉冲 | `CALIB:TILT,<pulse_us>\n` | 直接设置 TILT 舵机脉冲宽度(us) |
| 保存校准 | `CALIB:SAVE\n` | 将校准参数存入 Flash |
| 退出校准 | `CALIB:EXIT\n` | 回到正常工作模式 |

### 7.3 STM32 → ESP32-CAM 响应集

| 响应 | 格式 | 说明 |
|------|------|------|
| 指令确认 | `ACK:<pan>,<tilt>\n` | 确认收到并执行 |
| 状态上报 | `STATUS:<pan>,<tilt>,<voltage>\n` | voltage 为毫伏或 ADC 原始值 |
| 心跳回复 | `PONG\n` | 响应 PING |
| 错误 | `ERR:<code>\n` | code: LIMIT/SERVO_TIMEOUT/UART_FAIL |
| 校准确认 | `CALIB:OK,<pan_us>,<tilt_us>\n` | 当前脉冲值 |
| 校准数据 | `CALIB:DATA,<pan_min>,<pan_max>,<pan_center>,<tilt_min>,<tilt_max>,<tilt_center>\n` | 存储的校准值 |

---

## 8. 舵机校准机制

### 8.1 校准流程

```
1. 通过 UART 发送 "CALIB:START\n" 进入校准模式
2. 发送 "CALIB:PAN,<pulse_us>\n" 逐步调整水平舵机脉冲宽度
   - 从 500 开始，逐步增加到 2500
   - 记录最小脉冲值（刚好开始转动的位置）
   - 记录最大脉冲值（刚好到达极限位置）
   - 记录中位脉冲值（正前方/正水平位置）
3. 同样方式用 "CALIB:TILT,<pulse_us>\n" 校准垂直舵机
4. 发送 "CALIB:SAVE\n" 将参数存入 STM32 Flash
5. 发送 "CALIB:EXIT\n" 退出校准模式
```

### 8.2 校准数据结构

```c
// 存储在 STM32 Flash 最后一页，避免和程序冲突
typedef struct {
    uint16_t pan_min;     // PAN 最小脉冲 (us)，例：500
    uint16_t pan_max;     // PAN 最大脉冲 (us)，例：2500
    uint16_t pan_center;  // PAN 中位脉冲 (us)，例：1500
    uint16_t tilt_min;    // TILT 最小脉冲 (us)
    uint16_t tilt_max;    // TILT 最大脉冲 (us)
    uint16_t tilt_center; // TILT 中位脉冲 (us)
    uint32_t magic;       // 校验值，0xA5A5A5A5 表示数据有效
} CalibData;
```

### 8.3 用户按键功能

| 操作 | 行为 |
|------|------|
| 短按 (<1秒) | 云台回中 (HOME) |
| 长按 (3秒) | 进入校准模式，OLED 显示 "CALIB" |
| 校准中短按 | 无效果 |

### 8.4 Web 校准界面（ESP32-CAM 端，后续实现）

```
ESP32-CAM 启动一个简单 Web 页面
  → 滑块调整 PAN 角度
  → 滑块调整 TILT 角度
  → 点击 "保存校准" 按钮
  → ESP32 通过 UART 发送校准指令给 STM32
  → STM32 存入 Flash 并确认
```

---

## 9. PlatformIO 项目结构

```
AquaSentinel/
├── android/
├── backend/
├── docs/
├── frontend/
├── yolo-service/
├── firmware/                          ← 新增硬件固件目录
│   ├── esp32-cam/                     ← ESP32-CAM 项目
│   │   ├── platformio.ini
│   │   ├── src/
│   │   │   └── main.cpp
│   │   ├── lib/
│   │   │   ├── camera/
│   │   │   │   ├── CameraStreamer.h
│   │   │   │   └── CameraStreamer.cpp
│   │   │   ├── network/
│   │   │   │   ├── MjpegServer.h
│   │   │   │   ├── MjpegServer.cpp
│   │   │   │   ├── WsClient.h
│   │   │   │   └── WsClient.cpp
│   │   │   └── uart_bridge/
│   │   │       ├── UartBridge.h
│   │   │       └── UartBridge.cpp
│   │   └── include/
│   │       ├── config.h
│   │       └── protocol.h
│   │
│   ├── stm32-ptz/                     ← STM32 云台控制器项目
│   │   ├── platformio.ini
│   │   ├── src/
│   │   │   └── main.cpp
│   │   ├── lib/
│   │   │   ├── servo/
│   │   │   │   ├── PtzServo.h
│   │   │   │   └── PtzServo.cpp
│   │   │   ├── uart_handler/
│   │   │   │   ├── UartHandler.h
│   │   │   │   └── UartHandler.cpp
│   │   │   ├── oled_display/
│   │   │   │   ├── OledDisplay.h
│   │   │   │   └── OledDisplay.cpp
│   │   │   ├── calibration/
│   │   │   │   ├── CalibStorage.h
│   │   │   │   └── CalibStorage.cpp
│   │   │   └── battery/
│   │   │       ├── BatteryMonitor.h
│   │   │       └── BatteryMonitor.cpp
│   │   └── include/
│   │       ├── config.h
│   │       └── protocol.h
│   │
│   └── shared/                         ← 共享协议定义（手动同步）
│       └── protocol.h
```

### 9.1 ESP32-CAM platformio.ini

```ini
[env:esp32cam]
platform = espressif32
board = esp32cam
framework = arduino
monitor_speed = 115200
upload_speed = 115200
board_build.flash_mode = dio
board_build.flash_size = 4MB
build_flags =
    -DCORE_DEBUG_LEVEL=3
lib_deps =
    esp32-camera
```

### 9.2 STM32 platformio.ini

```ini
[env:bluepill_f103c8]
platform = ststm32
board = bluepill_f103c8
framework = arduino
monitor_speed = 115200
upload_protocol = stlink
build_flags =
    -Os
lib_deps =
    adafruit/Adafruit SSD1306@^2.5.7
    adafruit/Adafruit GFX Library@^1.11.9
```

---

## 10. KiCad 原理图规范

### 10.1 网络标签

| 网络名 | 说明 |
|--------|------|
| `VCC_5V` | 主 5V 电源线 |
| `VCC_3V3` | STM32 稳压器输出 3.3V |
| `GND` | 公共地 |
| `UART_ESP_TX` | ESP32 GPIO13 → STM32 PA10 |
| `UART_ESP_RX` | ESP32 GPIO14 ← STM32 PA9 |
| `SERVO_PAN` | STM32 PA6 → 舵机1 信号 |
| `SERVO_TILT` | STM32 PA7 → 舵机2 信号 |
| `I2C_SCL` | STM32 PB6 → OLED SCL |
| `I2C_SDA` | STM32 PB7 → OLED SDA |
| `BTN_USER` | STM32 PB12 → 按键 → GND |
| `BAT_SENSE` | 分压中点 → STM32 PA0 (可选) |

### 10.2 原理图分组建议

```
Sheet 1: 电源区
  - 电池 → TP4056+升压模块 → VCC_5V
  - VCC_5V → 各模块供电连线
  - 所有 GND 连在一起
  - 电容位置（标注 DNI）

Sheet 2: ESP32-CAM 区
  - ESP32-CAM 符号（引脚标注完整GPIO编号和复用功能）
  - GPIO13/14 → UART_ESP_TX / UART_ESP_RX 标签
  - 5V 和 GND 供电标注

Sheet 3: STM32 区
  - STM32F103C8T6 符号（标注引脚编号和复用功能）
  - PA9/PA10 → UART_ESP_RX / UART_ESP_TX 标签
  - PA6 → SERVO_PAN 标签
  - PA7 → SERVO_TILT 标签
  - PB6/PB7 → I2C_SCL / I2C_SDA 标签
  - PB12 → BTN_USER 标签
  - PA0 → BAT_SENSE 标签（可选）

Sheet 4: 外设区
  - 2x SG90 舵机符号（3引脚：VCC/GND/Signal）
  - SSD1306 OLED 符号（4引脚：VCC/GND/SCL/SDA）
  - 用户按键符号
  - 电量指示板符号
  - (可选) 电池分压电阻网络
```

### 10.3 电源滤波（原理图中标注为 DNI）

即使验证阶段不焊电容，原理图中仍应画出电容位置并标注 `DNI`：

```
C1: 470uF/10V  ── VCC_5V 与 GND 之间，靠近舵机供电分支  (DNI)
C2: 100uF/10V  ── VCC_5V 与 GND 之间，靠近 ESP32-CAM    (DNI)
C3: 100nF      ── VCC_3V3 与 GND 之间，靠近 OLED         (DNI)
```

正式版本直接贴上电容，不需要改原理图。

---

## 11. 功能验证步骤

| 步骤 | 操作 | 预期结果 | 失败排查 |
|------|------|---------|---------|
| 1 | ESP32-CAM 单独通电，烧录空白程序 | 串口输出可见，不重启 | 检查供电和接线 |
| 2 | ESP32-CAM 运行摄像头程序 | 浏览器可见 MJPEG 流 | 检查摄像头排线 |
| 3 | STM32 单独通电，烧录 OLED 程序 | OLED 显示文字 | 检查 I2C 接线 |
| 4 | STM32 驱动单个舵机 | 舵机转到指定角度 | 检查信号线和供电 |
| 5 | STM32 驱动两个舵机 | 两个舵机同时正常 | 检查共地 |
| 6 | ESP32-CAM 和 STM32 UART 互通 | 串口收发正确 | TX/RX 是否接反 |
| 7 | ESP32-CAM 通过 UART 控制舵机 | 云台响应指令 | 检查协议解析 |
| 8 | 用户按键短按 | 云台回中 | 检查 PB12 上拉 |
| 9 | 校准模式 | 舵机响应脉冲指令 | 检查 CALIB 协议 |
| 10 | 完整系统运行 | 视频流+云台控制+OLED | 重启问题加电容 |

---

## 12. 供电方案说明

### 12.1 功能验证阶段（当前）

```
3.7V 锂电池 → TP4056+升压模块
                        │
                   VCC_5V (约1A输出)
                    ├── ESP32-CAM 5V
                    ├── STM32 Blue Pill 5V
                    ├── 舵机1 VCC (红线)
                    └── 舵机2 VCC (红线)

OLED VCC ← STM32 3.3V (Blue Pill 板载稳压)
电量指示板 ← 直连电池正负极
```

**注意**：此方案舵机不要频繁同时大幅度转动，软件限制动作速度。

### 12.2 稳定版（推荐升级）

```
5V 2A-3A 电源适配器
        │
    VCC_5V
     ├── 470uF 电容 (靠近舵机供电分支)
     ├── 100uF 电容 (靠近 ESP32-CAM)
     ├── ESP32-CAM 5V
     ├── STM32 Blue Pill 5V
     ├── 舵机1 VCC
     └── 舵机2 VCC

OLED VCC ← STM32 3.3V
电量指示板 ← 直连电池正负极 (如有电池)
```

---

## 13. OLED 显示内容规范

OLED 主要用于调试和维护，显示以下信息（分页轮流）：

### 13.1 正常模式

```
页面1 (每3秒切换):
┌──────────────┐
│ AquaSentinel │
│ WiFi: OK     │
│ Srv: OK      │
│ Pan:90 T:90  │
└──────────────┘

页面2:
┌──────────────┐
│ IP:192.168.1 │
│ .100         │
│ Bat:78%      │
│ FPS:15       │
└──────────────┘
```

### 13.2 校准模式

```
┌──────────────┐
│ CALIB MODE   │
│ P:1500us     │
│ T:1500us     │
│ Press SAVE   │
└──────────────┘
```

### 13.3 错误状态

```
┌──────────────┐
│ ERROR         │
│ WiFi:FAIL     │
│ Srv:TIMEOUT   │
│ Pan:-- T:--   │
└──────────────┘
```

禁止在 OLED 上显示 Emoji 表情符号。