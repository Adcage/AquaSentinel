# STM32 PTZ 云台控制器 — 嵌入式小白完全指南

## 一、这个项目是什么？

这是一个运行在 **STM32 微控制器**上的**云台（PTZ）控制器**固件。

> **PTZ** = Pan-Tilt-Zoom（水平旋转-俯仰-变焦），本项目只控制 Pan 和 Tilt 两个轴，通过**舵机（Servo）**驱动。

**一句话总结**：STM32 小板子通过串口接收上位机（ESP32）发来的命令，控制两个舵机转动摄像头方向，同时在 OLED 小屏幕上显示状态，还能检测电池电量。

---

## 二、前置知识（小白必读）

### 2.1 什么是微控制器（MCU）？

微控制器就像一台超小型电脑，但和普通电脑不同：

| 特性 | 普通电脑 | 微控制器（STM32） |
|------|---------|-------------------|
| 有操作系统吗？ | 有 Windows/Linux | **没有**，裸机运行 |
| 程序怎么运行？ | 操作系统调度 | 从 `main()` 开始，死循环运行 |
| 存储在哪？ | 硬盘 | Flash 芯片内（烧录进去） |
| 内存多大？ | 几 GB | 几十 KB（本项目芯片 20KB RAM） |
| 速度多快？ | 几 GHz | 72 MHz（STM32F103） |

### 2.2 什么是 STM32F103C8？

- **STM32**：意法半导体（ST）的 32 位 ARM Cortex-M 系列微控制器
- **F103**：属于 "主流性能" 系列，Cortex-M3 内核，主频 72MHz
- **C8**：48 引脚封装，64KB Flash，20KB RAM
- 俗称 **Blue Pill（蓝色药丸）**，是最便宜、最常见的 STM32 开发板之一

### 2.3 什么是舵机（Servo）？

舵机是一种可以**精确控制角度**的小马达：

- 输入一个 **PWM 信号**（脉宽 500~2500 微秒），舵机就转到对应角度（0°~180°）
- 1500μs = 中位（90°），500μs = 一端（0°），2500μs = 另一端（180°）
- 本项目用 **两个舵机**：一个控制水平旋转（Pan），一个控制俯仰（Tilt）

### 2.4 什么是串口（UART）？

串口是设备之间最简单的通信方式：

- 只需要两根线：**TX**（发送）和 **RX**（接收）
- 像"对讲机"一样，一边说一边听
- 本项目 STM32 的 PA9(TX) 和 PA10(RX) 连接到 ESP32，接收控制命令

### 2.5 什么是 PlatformIO？

PlatformIO 是嵌入式开发的"一站式工具"：

- **编译**：把 C/C++ 代码翻译成芯片能执行的机器码
- **烧录**：把编译好的程序写入芯片的 Flash
- **调试**：可以单步执行、看变量值
- **串口监视器**：看芯片打印出来的文字
- 类比：前端开发用 npm，嵌入式开发用 PlatformIO

### 2.6 什么是 Arduino 框架？

Arduino 是一套简化嵌入式编程的库和规范：

- `setup()`：上电后只执行一次的初始化函数
- `loop()`：之后反复执行的死循环
- 提供了 `Serial`（串口）、`Wire`（I2C）、`Servo`（舵机）等现成的库
- 本项目用 **Arduino 框架 + STM32 芯片**，享受 Arduino 的简单，同时拥有 STM32 的性能

---

## 三、目录结构总览

```
stm32-ptz/
├── platformio.ini          ← 项目配置文件（最重要！）
├── .gitignore              ← Git 忽略规则
├── src/                    ← 源代码目录
│   └── main.cpp            ← 程序入口（setup + loop）
├── include/                ← 头文件目录（全局共享）
│   ├── config.h            ← 所有配置常量（引脚、角度、时间等）
│   ├── protocol.h          ← 串口通信协议定义（命令和响应字符串）
│   └── README              ← 占位文件
├── lib/                    ← 自定义库目录（按功能分文件夹）
│   ├── servo/              ← 舵机控制
│   │   ├── PtzServo.h
│   │   └── PtzServo.cpp
│   ├── uart_handler/       ← 串口命令解析
│   │   ├── UartHandler.h
│   │   └── UartHandler.cpp
│   ├── oled_display/       ← OLED 屏幕显示
│   │   ├── OledDisplay.h
│   │   ├── OledDisplay.cpp
│   │   ├── OledViewModel.h
│   │   └── OledViewModel.cpp
│   ├── button/             ← 按键处理
│   │   ├── ButtonHandler.h
│   │   └── ButtonHandler.cpp
│   ├── battery_monitor/    ← 电池电量监测
│   │   ├── BatteryMonitor.h
│   │   └── BatteryMonitor.cpp
│   ├── calibration/        ← 校准数据存储
│   │   ├── CalibStorage.h
│   │   └── CalibStorage.cpp
│   └── README              ← 占位文件
├── test/                   ← 测试目录
│   └── host/               ← 主机端测试（不需要真实硬件）
│       ├── test_battery_monitor.cpp
│       ├── test_button_handler.cpp
│       └── test_oled_view_model_pages.cpp
├── .vscode/                ← VS Code 配置（自动生成，不用管）
│   ├── launch.json         ← 调试配置
│   ├── extensions.json     ← 推荐安装的 VS Code 插件
│   └── c_cpp_properties.json ← 代码智能提示配置
└── .pio/                   ← PlatformIO 构建产物（自动生成，不用管）
```

---

## 四、核心配置文件详解

### 4.1 `platformio.ini` — 项目总配置

```ini
[env:bluepill_f103c8]    ; 环境名：对应 Blue Pill 开发板
platform = ststm32       ; 使用 STM32 平台
board = bluepill_f103c8  ; 开发板型号
framework = arduino      ; 使用 Arduino 框架

; ST-LINK 烧录配置（用 ST-Link 调试器烧录）
upload_protocol = stlink
monitor_speed = 115200   ; 串口监视器波特率 115200

build_flags =
    -Iinclude            ; 把 include/ 目录加入头文件搜索路径
    -std=gnu++11         ; 使用 C++11 标准
```

**逐行解释**：

| 配置项 | 含义 |
|--------|------|
| `platform = ststm32` | 告诉 PlatformIO 下载 STM32 的编译工具链和库 |
| `board = bluepill_f103c8` | 指定芯片型号，PlatformIO 据此设置 Flash 大小、时钟频率等 |
| `framework = arduino` | 用 Arduino 方式写代码（有 `setup()/loop()`，有 `Serial` 等） |
| `upload_protocol = stlink` | 用 ST-Link 调试器烧录（另一种方式是串口 `serial`，被注释掉了） |
| `monitor_speed = 115200` | 串口通信速度，双方必须一致（像对讲机的频道） |
| `-Iinclude` | 让编译器能找到 `include/` 下的头文件 |
| `-std=gnu++11` | C++ 语言标准，决定能用哪些语法特性 |

### 4.2 `.gitignore`

```
.pio         ← PlatformIO 构建产物（编译生成的 .o 文件等），不需要提交到 Git
.vscode/     ← VS Code 配置是自动生成的，不需要提交
```

---

## 五、头文件详解（`include/`）

### 5.1 `config.h` — 全局配置常量

这个文件定义了**所有可调参数**，修改这里就能改变系统行为，不用改其他代码。

```cpp
namespace ptz_config {           // 所有配置放在 ptz_config 命名空间里，避免名字冲突

constexpr int UART_BAUD_RATE = 115200;   // 串口波特率

// 引脚定义（只在真实硬件上有效）
constexpr uint8_t PIN_SERVO_PAN = PA6;        // 水平舵机接 PA6
constexpr uint8_t PIN_SERVO_TILT = PA7;       // 俯仰舵机接 PA7
constexpr uint8_t PIN_BUTTON_USER = PB12;     // 用户按键接 PB12
constexpr uint8_t PIN_BATTERY_ADC = PA0;      // 电池电压检测接 PA0（ADC 输入）

// 角度范围
constexpr uint8_t PAN_MIN_ANGLE = 0;          // 水平最小角度
constexpr uint8_t PAN_MAX_ANGLE = 180;        // 水平最大角度
constexpr int8_t TILT_MIN_ANGLE = 0;          // 俯仰最小角度（仰视）
constexpr uint8_t TILT_MAX_ANGLE = 180;       // 俯仰最大角度（俯视）

// 默认角度
constexpr uint8_t DEFAULT_PAN_ANGLE = 90;     // 默认水平居中
constexpr uint8_t DEFAULT_TILT_ANGLE = 90;    // 默认俯仰水平

// TILT 偏移量：逻辑角度 + 偏移 = 物理角度
// 设为 0 表示用户输入 = 舵机物理角度；校准后可微调
constexpr int8_t TILT_ANGLE_OFFSET = 0;

// 步进和按键参数
constexpr uint8_t DEFAULT_NUDGE_STEP = 5;     // 每次微调步进 5 度
constexpr uint8_t MAX_NUDGE_STEP = 10;        // 最大步进 10 度
constexpr uint32_t BUTTON_DEBOUNCE_MS = 30;   // 按键消抖时间 30ms
constexpr uint32_t BUTTON_LONG_PRESS_MS = 800;     // 长按判定 800ms
constexpr uint32_t BUTTON_SUPER_LONG_PRESS_MS = 2500; // 超长按 2500ms（预留）
constexpr uint32_t OLED_ACTION_MESSAGE_MS = 1200;   // 操作提示显示时长 1200ms

// 电池采样参数
constexpr uint32_t BATTERY_SAMPLE_INTERVAL_MS = 200; // 每 200ms 采一次样
constexpr uint8_t BATTERY_SAMPLE_WINDOW = 8;         // 滑动窗口 8 个样本取平均

// PAN 校准安全上限
constexpr uint16_t PAN_CALIBRATION_SAFE_MAX_US = 2350; // 脉宽超过 2350μs 可能堵转
```

**小白理解**：

- `PA6`、`PB12` 等：STM32 芯片的引脚编号。PA6 表示 A 组第 6 脚，PB12 表示 B 组第 12 脚
- `constexpr`：编译期常量，不占 RAM，直接编译到代码里
- `#ifdef ARDUINO ... #else ... #endif`：条件编译。在真实硬件上用真实引脚号，在电脑上测试时用假数字

### 5.2 `protocol.h` — 串口通信协议

定义了 STM32 和 ESP32 之间"说什么话"的约定：

```cpp
namespace ptz_protocol {

// 命令（ESP32 → STM32）
constexpr const char* CMD_HOME = "HOME";              // 回中位
constexpr const char* CMD_STATUS = "STATUS?";          // 查询当前状态
constexpr const char* CMD_RESET_CALIB = "RESET_CALIB"; // 重置校准
constexpr const char* CMD_NUDGE = "NUDGE:";            // 微调方向，如 NUDGE:LEFT,5
constexpr const char* CMD_MOVE = "MOVE:";              // 绝对移动，如 MOVE:90,45
constexpr const char* CMD_CALIB_START = "CALIB:START"; // 进入校准模式
constexpr const char* CMD_CALIB_SAVE = "CALIB:SAVE";   // 保存校准数据
constexpr const char* CMD_CALIB_EXIT = "CALIB:EXIT";   // 退出校准模式
constexpr const char* CMD_CALIB_DATA = "CALIB:DATA?";  // 查询校准数据
constexpr const char* CMD_CALIB_SET = "CALIB:SET,";    // 设置校准值
constexpr const char* CMD_CALIB_PAN = "CALIB:PAN,";    // 校准时直接设置 PAN 脉宽
constexpr const char* CMD_CALIB_TILT = "CALIB:TILT,";  // 校准时直接设置 TILT 脉宽

// 响应（STM32 → ESP32）
constexpr const char* RESP_ACK = "ACK:";               // 确认
constexpr const char* RESP_STATUS = "STATUS:";         // 状态回复
constexpr const char* RESP_ERR = "ERR:";               // 错误
constexpr const char* RESP_CALIB_OK = "CALIB:OK,";     // 校准操作确认
constexpr const char* RESP_CALIB_DATA = "CALIB:DATA,"; // 校准数据回复

// 错误码
constexpr const char* ERR_BAD_CMD = "BAD_CMD";         // 不认识的命令
constexpr const char* ERR_BAD_ARG = "BAD_ARG";         // 参数错误
constexpr const char* ERR_LIMIT = "LIMIT";             // 超出安全限制
```

**通信示例**：

```
ESP32 发送:  MOVE:90,45\n       ← 把云台转到 Pan=90°, Tilt=45°
STM32 回复:  ACK:90,45\n        ← 确认，当前角度 Pan=90°, Tilt=45°

ESP32 发送:  NUDGE:LEFT,5\n     ← 向左微调 5°
STM32 回复:  ACK:95,45\n        ← 确认，当前角度变为 Pan=95°, Tilt=45°

ESP32 发送:  HOME\n             ← 回中位
STM32 回复:  ACK:90,90\n        ← 确认，回到 Pan=90°, Tilt=90°

ESP32 发送:  STATUS?\n          ← 查询状态
STM32 回复:  STATUS:95,45,0\n   ← Pan=95°, Tilt=45°, 校准模式=0(关)

ESP32 发送:  FOOBAR\n           ← 不认识的命令
STM32 回复:  ERR:BAD_CMD\n      ← 错误：命令不认识
```

---

## 六、主程序详解（`src/main.cpp`）

这是整个程序的**入口和主循环**，是理解系统运行的起点。

### 6.1 全局对象

```cpp
PtzServo g_servo;                          // 舵机控制器
UartHandler g_uartHandler(g_servo);        // 串口命令处理器（引用舵机）
OledDisplay g_oledDisplay;                 // OLED 显示器
ButtonHandler g_buttonHandler;             // 按键处理器
BatteryMonitor g_batteryMonitor;           // 电池监测器
OledPage g_currentPage = OledPage::Status; // 当前 OLED 页面（默认：状态页）
uint32_t g_actionMessageUntilMs = 0;       // 操作提示显示截止时间
char g_actionMessage[OLED_TEXT_BUFFER_SIZE] = {0}; // 操作提示文字
```

**小白理解**：这些 `g_` 前缀的变量是全局的，整个程序都能访问。`UartHandler` 构造时传入 `g_servo` 的引用，这样串口收到命令就能直接控制舵机。

### 6.2 `setup()` — 上电初始化

```cpp
void setup() {
    Serial.begin(ptz_config::UART_BAUD_RATE);  // 初始化串口，波特率 115200
    pinMode(ptz_config::PIN_BUTTON_USER, INPUT_PULLUP); // 按键引脚设为上拉输入
    g_buttonHandler.begin();                    // 初始化按键处理器
    g_batteryMonitor.begin();                   // 初始化电池监测
    g_servo.begin(ptz_config::PIN_SERVO_PAN, ptz_config::PIN_SERVO_TILT); // 初始化舵机
    g_uartHandler.begin(Serial);                // 初始化串口命令处理器
    g_oledDisplay.begin();                      // 初始化 OLED 显示
    Serial.println("STM32 PTZ controller ready"); // 打印就绪信息
}
```

**小白理解**：`setup()` 只在上电时执行一次。就像开机——先把所有设备打开，再开始工作。

- `INPUT_PULLUP`：引脚内部接一个上拉电阻到 3.3V，按键按下时引脚变 LOW（低电平），松开时为 HIGH（高电平）
- `Serial`：Arduino 内置的串口对象，对应硬件 USART1（PA9/PA10）

### 6.3 `loop()` — 主循环

```cpp
void loop() {
    const uint32_t nowMs = millis();           // 获取当前时间（毫秒）

    g_uartHandler.poll();                      // 轮询串口，处理收到的命令
    g_batteryMonitor.update(nowMs);            // 定时采样电池电压

    // 读取按键状态并处理
    const bool rawPressed = digitalRead(ptz_config::PIN_BUTTON_USER) == LOW; // 按下=LOW
    const ButtonEvent event = g_buttonHandler.update(rawPressed, nowMs);
    if (event == ButtonEvent::ShortPress) {    // 短按：切换 OLED 页面
        g_currentPage = nextPage(g_currentPage);
        g_actionMessageUntilMs = 0;            // 清除操作提示
        g_actionMessage[0] = '\0';
    } else if (event == ButtonEvent::LongPress) { // 长按：云台回中
        g_servo.home();
        setActionMessage("正在回中", nowMs);   // 显示"正在回中"1.2秒
    }

    g_oledDisplay.update(buildUiState(nowMs)); // 更新 OLED 显示
    delay(2);                                   // 延时 2ms，避免跑太快
}
```

**小白理解**：`loop()` 被 Arduino 框架无限循环调用。每次循环做的事情：

1. 看串口有没有新命令 → 有就执行
2. 看是否该采样电池了 → 该就采样
3. 看按键有没有按下 → 短按切页面，长按回中
4. 更新 OLED 显示
5. 延时 2ms 防止 CPU 跑满

### 6.4 辅助函数

```cpp
// 页面循环：状态 → 校准 → 电量 → 状态
OledPage nextPage(OledPage page) { ... }

// 设置操作提示消息（显示一段时间后自动消失）
void setActionMessage(const char* message, uint32_t nowMs) { ... }

// 收集所有 UI 状态，打包给 OLED 显示
OledUiState buildUiState(uint32_t nowMs) { ... }
```

---

## 七、各功能模块详解（`lib/`）

### 7.1 `lib/servo/` — 舵机控制（PtzServo）

这是最核心的模块，控制两个舵机的角度。

#### 7.1.1 关键数据结构

```cpp
struct PtzState {
    uint8_t pan;    // 水平角度（用户视角：0=最左，180=最右）
    uint8_t tilt;   // 俯仰角度（0=仰视，90=平视，180=俯视）
};
```

#### 7.1.2 关键成员变量

```cpp
Servo panServo;          // Arduino Servo 对象，控制水平舵机
Servo tiltServo;         // Arduino Servo 对象，控制俯仰舵机
uint8_t panAngle = 90;   // 当前水平角度（内部值，可能是镜像的）
uint8_t tiltAngle = 90;  // 当前俯仰角度（物理值）

// 校准脉宽（微秒）
uint16_t panMinUs = 500;     // PAN 最小脉宽
uint16_t panMaxUs = 2500;    // PAN 最大脉宽
uint16_t panCenterUs = 1500; // PAN 中位脉宽
// TILT 同理...

bool calibrationMode = false; // 是否在校准模式
CalibStorage calibStorage;    // 校准数据存储
```

#### 7.1.3 核心方法详解

**`begin(pinPan, pinTilt)`** — 初始化
1. 设置角度范围（从 config.h 读取）
2. 把舵机对象绑定到引脚（`panServo.attach(PA6)`）
3. 从 EEPROM 加载校准数据（如果有的话）
4. 调用 `home()` 回到中位

**`home()`** — 回中位
- PAN：`180 - DEFAULT_PAN_ANGLE`（因为 PAN 做了镜像，用户 90° = 内部 90°）
- TILT：`DEFAULT_TILT_ANGLE + OFFSET`（加上安装偏移）
- 计算脉宽并输出

**`nudge(dir, step)`** — 微调
- `LEFT`：panAngle += step（向左 = 角度增大，因为镜像）
- `RIGHT`：panAngle -= step
- `UP`：tiltAngle -= step（仰视 = 角度减小）
- `DOWN`：tiltAngle += step（俯视 = 角度增大）
- 校准模式下禁止微调

**`moveTo(targetPan, targetTilt)`** — 绝对移动
- PAN 做镜像：`panAngle = 180 - targetPan`（用户 0°=最左 → 内部 180°）
- TILT 加偏移：`tiltAngle = targetTilt + OFFSET`
- 校准模式下禁止移动

**`angleToPulseUs(minUs, centerUs, maxUs, angle)`** — 角度转脉宽
- 0°~90° 线性映射到 minUs~centerUs
- 90°~180° 线性映射到 centerUs~maxUs
- 这样校准后可以非对称地控制舵机

**校准相关方法**：
- `enterCalibration()` / `exitCalibration()`：进入/退出校准模式
- `saveCalibration()`：把当前脉宽参数保存到 EEPROM
- `setCalibrationPulse(panAxis, pulseUs)`：校准时直接设置脉宽（微调用）
- `setCalibrationValue(axis, key, pulseUs)`：设置某个轴的 MIN/CENTER/MAX 值
- `resetCalibration()`：重置为默认值（500/1500/2500μs）

**`apply()`** — 实际输出
```cpp
void PtzServo::apply() {
    panServo.writeMicroseconds(panCurrentUs);   // 写 PAN 脉宽
    tiltServo.writeMicroseconds(tiltCurrentUs); // 写 TILT 脉宽
}
```

#### 7.1.4 PAN 镜像映射

为什么要镜像？因为舵机的安装方向可能和用户期望的方向相反：

```
用户视角：  0°（最左）←——→ 180°（最右）
内部角度：  180°          ←——→ 0°
映射公式：  内部角度 = 180 - 用户角度
```

这样用户说"向左转"（角度减小），舵机实际向左转。

### 7.2 `lib/uart_handler/` — 串口命令解析（UartHandler）

负责从串口读取命令、解析、执行、回复。

#### 7.2.1 工作原理

```
串口数据流 → poll() 逐字符读取 → 遇到 \n 则一行完整 → handleLine() 解析执行
```

#### 7.2.2 `poll()` — 轮询串口

```cpp
void UartHandler::poll() {
    while (serial->available() > 0) {    // 串口有数据？
        char ch = serial->read();        // 读一个字符
        if (ch == '\r') continue;        // 忽略回车
        if (ch == '\n') {                // 换行 = 一条完整命令
            handleLine(lineBuffer);      // 处理这行
            lineBuffer = "";             // 清空缓冲区
            continue;
        }
        if (lineBuffer.length() < 90) {  // 防止缓冲区溢出
            lineBuffer += ch;            // 追加到缓冲区
        }
    }
}
```

**小白理解**：串口数据是一个字符一个字符来的，`poll()` 把它们攒起来，遇到换行符 `\n` 就认为收到了一条完整命令。

#### 7.2.3 `handleLine()` — 命令分发

按优先级依次匹配命令字符串：

| 命令 | 格式 | 作用 |
|------|------|------|
| `HOME` | `HOME` | 云台回中 |
| `CALIB:START` | `CALIB:START` | 进入校准模式 |
| `CALIB:EXIT` | `CALIB:EXIT` | 退出校准模式 |
| `CALIB:SAVE` | `CALIB:SAVE` | 保存校准数据 |
| `CALIB:DATA?` | `CALIB:DATA?` | 查询校准数据 |
| `CALIB:PAN,1500` | `CALIB:PAN,<脉宽>` | 校准时设置 PAN 脉宽 |
| `CALIB:TILT,1500` | `CALIB:TILT,<脉宽>` | 校准时设置 TILT 脉宽 |
| `CALIB:SET,PAN,MIN,500` | `CALIB:SET,<轴>,<键>,<脉宽>` | 设置校准参数 |
| `STATUS?` | `STATUS?` | 查询当前状态 |
| `RESET_CALIB` | `RESET_CALIB` | 重置校准为默认 |
| `MOVE:90,45` | `MOVE:<pan>,<tilt>` | 绝对移动 |
| `NUDGE:LEFT,5` | `NUDGE:<方向>,<步进>` | 微调 |

不匹配任何命令 → 回复 `ERR:BAD_CMD`。

#### 7.2.4 安全保护

- **脉宽范围检查**：只接受 500~2500μs
- **PAN 安全上限**：脉宽超过 2350μs 拒绝执行（防止堵转或串口异常）
- **角度范围检查**：PAN 和 TILT 都限制在 0~180°
- **步进上限**：微调步进最大 10°

### 7.3 `lib/oled_display/` — OLED 显示

分为两层：**ViewModel**（决定显示什么内容）和 **Display**（怎么画到屏幕上）。

#### 7.3.1 `OledViewModel.h/cpp` — 视图模型

**数据结构**：

```cpp
enum class OledPage : uint8_t {
    Status = 0,       // 状态页：显示角度
    Calibration = 1,  // 校准页：显示脉宽
    Battery = 2,      // 电量页：显示电池信息
};

struct OledUiState {    // UI 状态（输入）
    OledPage page;
    uint8_t pan, tilt;
    bool calibrationMode;
    uint16_t panPulseUs, tiltPulseUs;
    uint32_t uptimeMs;
    bool showActionMessage;
    char actionMessage[20];
    uint16_t batteryRaw, batteryMv;
    uint8_t batteryPercent;
    bool batteryValid;
};

struct OledFrame {      // 显示帧（输出）
    char title[20];     // 标题行
    char lines[4][20];  // 4 行内容
    bool isBootScreen;  // 是否是开机画面
};
```

**页面内容**：

| 页面 | 标题 | 行1 | 行2 | 行3 | 行4 |
|------|------|-----|-----|-----|-----|
| 开机画面 | AQUASENTINEL | 启动中 | OLED READY | PAN 90 | TILT 90 |
| 状态页 | 云台 | MODE NORM | PAN 90 | TILT 90 | UART READY |
| 校准页 | 校准 | MODE CAL / CAL READY | PAN 1500US | TILT 1500US | WEB CAL |
| 电量页 | 电量 | BAT 4.01V | PCT 78% | RAW 2489 | SHORT NEXT |
| 操作提示 | 云台 | 正在回中 | PAN 90 | TILT 90 | UART READY |

**`build()` 方法**：根据 `OledUiState` 生成 `OledFrame`，就是"把数据变成要显示的文字"。

#### 7.3.2 `OledDisplay.h/cpp` — 显示驱动

**硬件信息**：
- 芯片：SSD1306，0.96 寸 128×64 像素 OLED
- 接口：I2C，地址 0x3C
- 引脚：SCL=PB6，SDA=PB7

**工作流程**：
1. `begin()`：初始化 I2C，发送 SSD1306 初始化命令序列，清屏
2. `update(state)`：
   - 每 120ms 刷新一次，或者状态变化时立即刷新
   - 调用 `OledViewModel::build()` 生成帧
   - 调用 `drawFrame()` 画到屏幕上

**自定义字库**：

因为 SSD1306 没有内置中文字库，代码里**手绘了 12 个常用汉字**的点阵数据：

```cpp
const Utf8Glyph CHINESE_GLYPHS[] = {
    {"启", {0x40, 0x3FE, ...}},  // "启"字的 12×12 点阵
    {"动", {...}},                // "动"
    {"中", {...}},                // "中"
    {"云", {...}},                // "云"
    {"台", {...}},                // "台"
    {"校", {...}},                // "校"
    {"准", {...}},                // "准"
    {"电", {...}},                // "电"
    {"量", {...}},                // "量"
    {"回", {...}},                // "回"
    {"正", {...}},                // "正"
    {"在", {...}},                // "在"
};
```

每个汉字 12×12 像素，用 12 个 16 位整数表示每行的点阵。ASCII 字符是 5×7 像素。

**绘制流程**：
1. `clearBuffer()`：清空 128×64/8 = 1024 字节的显示缓冲区
2. `drawCompactText()`：逐字符绘制，ASCII 用 `drawAsciiChar()`，中文用 `drawChineseChar()`
3. `drawHLine()`：画水平分割线
4. `sendBuffer()`：通过 I2C 把整个缓冲区发送到 OLED

**条件编译**：`#ifdef ARDUINO ... #else ... #endif`，在电脑上编译测试时，OLED 相关代码变成空函数，不会报错。

### 7.4 `lib/button/` — 按键处理（ButtonHandler）

处理物理按键的**消抖**和**长短按判定**。

#### 7.4.1 什么是消抖？

机械按键按下/松开时，触点会在几毫秒内弹跳，产生多次通断：

```
理想信号：  ─────┃          ┃─────
实际信号：  ─────┃┃┃┃      ┃┃┃┃─────  （弹跳！）
                按下        松开
```

消抖就是等弹跳稳定后再判定状态。本项目等待 30ms（`BUTTON_DEBOUNCE_MS`）。

#### 7.4.2 工作原理

```cpp
ButtonEvent ButtonHandler::update(bool rawPressed, uint32_t nowMs) {
    // 1. 检测电平变化，记录弹跳时间
    if (rawPressed != lastRawPressed) {
        lastBounceMs = nowMs;  // 重新计时
    }

    // 2. 弹跳期间不判定
    if ((nowMs - lastBounceMs) < 30ms) return None;

    // 3. 弹跳结束，更新稳定状态
    if (stablePressed != rawPressed) {
        stablePressed = rawPressed;
        if (stablePressed) {
            pressStartMs = nowMs;  // 记录按下时刻
        } else if (!longPressFired) {
            return ShortPress;     // 松手且没触发长按 → 短按
        }
    }

    // 4. 持续按下超过 800ms → 长按
    if (stablePressed && !longPressFired && (nowMs - pressStartMs) >= 800ms) {
        longPressFired = true;
        return LongPress;
    }

    return None;
}
```

**小白理解**：
- 短按 = 按下后松手，且没超过 800ms
- 长按 = 按住不放超过 800ms（触发后不会重复触发）

### 7.5 `lib/battery_monitor/` — 电池监测（BatteryMonitor）

通过 ADC（模数转换器）读取电池电压，估算剩余电量。

#### 7.5.1 硬件原理

```
电池(3.7~4.2V) ──┬── 100kΩ ──┬── PA0 (ADC)
                  │            │
                  │          100kΩ
                  │            │
                  └────────── GND
```

两个 100kΩ 电阻分压，PA0 测到的是电池电压的一半。ADC 是 12 位精度（0~4095 对应 0~3.3V）。

#### 7.5.2 关键公式

```cpp
// ADC 原始值 → 电池电压(mV)
uint16_t rawToBatteryMv(uint16_t raw) {
    return (raw * 3300 * 2 + 2048) / 4096;
    //       ↑     ↑   ↑       ↑
    //       ADC值  参考电压 分压比  四舍五入  ADC满量程
}
```

**举例**：ADC 读到 2480 → `(2480 * 3300 * 2 + 2048) / 4096 = 3996mV ≈ 4.0V`

#### 7.5.3 电量百分比

锂电池电压和电量不是线性关系，所以用**查表插值**：

```cpp
{3000mV, 0%},   // 3.0V = 0%（几乎没电）
{3200mV, 5%},   // 3.2V = 5%
{3450mV, 15%},  // 3.45V = 15%
{3600mV, 35%},  // 3.6V = 35%
{3700mV, 50%},  // 3.7V = 50%
{3800mV, 65%},  // 3.8V = 65%
{3900mV, 75%},  // 3.9V = 75%
{4000mV, 85%},  // 4.0V = 85%
{4100mV, 95%},  // 4.1V = 95%
{4200mV, 100%}, // 4.2V = 100%（满电）
```

两个点之间线性插值。比如 3850mV 在 3800(65%) 和 3900(75%) 之间，插值得到 70%。

#### 7.5.4 滑动窗口平均

每 200ms 采一次样，保留最近 8 个样本取平均，滤除瞬间波动。

#### 7.5.5 STM32F1 专用 ADC 读取

STM32F1 系列的 ADC 有个特点：第一次读取可能不准。所以代码里：

1. 先做一次"丢弃读数"（预热 ADC）
2. 再读 16 次取平均
3. 每次读之间延时 200μs 让 ADC 稳定

### 7.6 `lib/calibration/` — 校准数据存储（CalibStorage）

把舵机校准参数存到 **EEPROM**（电可擦除只读存储器），掉电不丢失。

#### 7.6.1 数据结构

```cpp
struct CalibData {
    uint16_t panMin;      // PAN 最小脉宽(μs)
    uint16_t panMax;      // PAN 最大脉宽(μs)
    uint16_t panCenter;   // PAN 中位脉宽(μs)
    uint16_t tiltMin;     // TILT 最小脉宽(μs)
    uint16_t tiltMax;     // TILT 最大脉宽(μs)
    uint16_t tiltCenter;  // TILT 中位脉宽(μs)
    uint32_t magic;       // 魔数 0xA5A5A5A5，用于验证数据有效性
};
```

#### 7.6.2 读写流程

**读取**：
1. `eeprom_buffer_fill()`：从 Flash 模拟的 EEPROM 加载到内存缓冲区
2. `EEPROM.get()`：从缓冲区读取 `CalibData`
3. 检查 `magic` 是否等于 `0xA5A5A5A5`，不是说明没存过或数据损坏

**写入**：
1. `eeprom_buffer_fill()`：加载缓冲区
2. `EEPROM.put()`：写入数据到缓冲区
3. `eeprom_buffer_flush()`：把缓冲区写回 Flash
4. **回读验证**：再读一次确认写入成功

**小白理解**：STM32 没有 true EEPROM，PlatformIO 的 EEPROM 库用 Flash 的一部分模拟。Flash 写入寿命有限（约 1 万次），所以不要频繁保存校准数据。

---

## 八、测试详解（`test/host/`）

这些测试在**电脑上**运行（不需要真实硬件），验证纯逻辑是否正确。

### 8.1 `test_button_handler.cpp` — 按键测试

```cpp
// 测试1：没按时应该返回 None
button.update(false, 0) → None

// 测试2：按下再松手 → 短按
按下(t=10) → 稳定(t=50) → 松手(t=120) → 稳定(t=160) → ShortPress

// 测试3：按住不放超过 800ms → 长按
按下(t=1000) → 稳定(t=1040) → 持续到(t=1900) → LongPress

// 测试4：长按触发后继续按住 → 不再触发
继续按住(t=2000) → None
```

### 8.2 `test_battery_monitor.cpp` — 电池测试

```cpp
// 测试 ADC → 电压转换
rawToBatteryMv(2048) == 3300   // ADC 中点 = 3.3V × 2 = 3.3V... 不对，应该是 3300mV

// 测试电压 → 百分比
batteryMvToPercent(4200) == 100  // 满电
batteryMvToPercent(3000) == 0    // 没电
batteryMvToPercent(3100) == 3    // 插值计算

// 测试采样窗口
ingestRawSample(2480) → ingesting 多次 → reading.raw > 0
```

### 8.3 `test_oled_view_model_pages.cpp` — OLED 视图模型测试

```cpp
// 状态页：标题="云台"，第一行="MODE NORM"
// 电量页：BAT 4.01V, PCT 78%, RAW 2489
// 操作提示："正在回中"
// 无效电池：BAT --.--V, PCT --%, RAW ----
```

---

## 九、VS Code 配置（`.vscode/`）

这些文件由 PlatformIO **自动生成**，一般不需要手动修改。

### 9.1 `extensions.json`

推荐安装 `platformio.platformio-ide` 插件，不推荐 `ms-vscode.cpptools-extension-pack`（和 PlatformIO 冲突）。

### 9.2 `launch.json`

配置了三种调试模式：
- **PIO Debug**：标准调试（先编译再调试）
- **PIO Debug (skip Pre-Debug)**：跳过预调试步骤
- **PIO Debug (without uploading)**：不烧录，只连接已烧录的芯片调试

### 9.3 `c_cpp_properties.json`

告诉 VS Code 的 C/C++ 插件：
- 头文件搜索路径（`includePath`）
- 预定义宏（`defines`，如 `STM32F1`、`ARDUINO`）
- 编译器路径（ARM GCC 交叉编译器）
- C/C++ 标准

---

## 十、`.pio/` 目录

PlatformIO 的**构建产物**目录，包含：

- 编译生成的 `.o` 目标文件
- 最终的 `firmware.elf`（可执行文件）和 `firmware.bin`（烧录文件）
- STM32 HAL/LL 库的编译产物

**不要手动修改这个目录的内容**，每次编译都会重新生成。`.gitignore` 已排除此目录。

---

## 十一、系统整体运行流程

```
上电
  │
  ▼
setup()
  ├── 初始化串口 (115200)
  ├── 初始化按键 (PB12, 上拉输入)
  ├── 初始化电池 ADC (PA0)
  ├── 初始化舵机 (PA6/Pan, PA7/Tilt)
  │     └── 从 EEPROM 加载校准数据
  ├── 初始化串口命令处理器
  └── 初始化 OLED (I2C, PB6/PB7)
        └── 显示开机画面 "AQUASENTINEL 启动中"

loop() ← 无限循环
  │
  ├── g_uartHandler.poll()          ← 串口有命令？
  │     └── 解析命令 → 控制舵机 → 回复结果
  │
  ├── g_batteryMonitor.update()     ← 该采样电池了？
  │     └── ADC 读取 → 滑动平均 → 电压 → 百分比
  │
  ├── 读取按键 → 消抖 → 判定
  │     ├── 短按 → 切换 OLED 页面
  │     └── 长按 → 云台回中 + 显示"正在回中"
  │
  ├── buildUiState()                ← 收集所有状态
  │     └── 角度、脉宽、电池、页面、提示消息
  │
  └── g_oledDisplay.update()        ← 刷新 OLED
        └── ViewModel.build() → drawFrame() → I2C 发送
```

---

## 十二、引脚分配总表

| 引脚 | 功能 | 方向 | 说明 |
|------|------|------|------|
| PA0 | 电池 ADC | 输入 | 检测电池电压（分压后） |
| PA6 | PAN 舵机 | 输出(PWM) | 水平旋转舵机信号线 |
| PA7 | TILT 舵机 | 输出(PWM) | 俯仰舵机信号线 |
| PA9 | USART1 TX | 输出 | 串口发送（连 ESP32 RX） |
| PA10 | USART1 RX | 输入 | 串口接收（连 ESP32 TX） |
| PB6 | I2C SCL | 输出 | OLED 时钟线 |
| PB7 | I2C SDA | 双向 | OLED 数据线 |
| PB12 | 用户按键 | 输入(上拉) | 按下=LOW，松开=HIGH |

---

## 十三、常用操作速查

### 编译

```bash
cd firmware/stm32-ptz
pio run                    # 编译
```

### 烧录

```bash
pio run --target upload    # 编译并烧录（需要 ST-Link）
```

### 串口监视

```bash
pio device monitor         # 查看串口输出（Ctrl+C 退出）
```

### 调试

在 VS Code 中按 F5，选择 "PIO Debug"。

### 清理构建

```bash
pio run --target clean     # 清除编译产物
```

---

## 十四、术语表

| 术语 | 全称 | 解释 |
|------|------|------|
| MCU | Microcontroller Unit | 微控制器，单片机 |
| PTZ | Pan-Tilt-Zoom | 云台控制：水平-俯仰-变焦 |
| PWM | Pulse Width Modulation | 脉宽调制，用脉冲宽度控制舵机角度 |
| UART | Universal Asynchronous Receiver/Transmitter | 通用异步收发器，即串口 |
| I2C | Inter-Integrated Circuit | 两线制串行总线，OLED 用的通信协议 |
| ADC | Analog-to-Digital Converter | 模数转换器，把电压变成数字 |
| EEPROM | Electrically Erasable Programmable Read-Only Memory | 电可擦除只读存储器，掉电不丢 |
| Flash | Flash Memory | 闪存，存程序代码和模拟 EEPROM |
| HAL | Hardware Abstraction Layer | 硬件抽象层，ST 官方库 |
| LL | Low-Layer | 低层库，比 HAL 更接近硬件 |
| ST-Link | — | ST 官方的调试/烧录器 |
| GPIO | General Purpose Input/Output | 通用输入输出引脚 |
| DMA | Direct Memory Access | 直接内存访问，不经过 CPU 搬运数据 |
| NVIC | Nested Vectored Interrupt Controller | 嵌套向量中断控制器 |
| ELF | Executable and Linkable Format | 可执行链接格式（.elf 文件） |
| BIN | Binary | 纯二进制烧录文件（.bin 文件） |
