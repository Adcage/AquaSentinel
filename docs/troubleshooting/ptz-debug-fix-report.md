# 云台控制与校准联调修复报告

> 本文档总结本轮 ESP32-CAM + STM32 云台控制链路、浏览器联调页面、校准流程与持久化支持的完整修复内容，说明修改前后的区别、根因判断、已验证结果与当前剩余风险。

---

## 1. 本轮目标

本轮工作的目标不是单纯“让舵机动一下”，而是把下面这条完整链路打通：

```text
浏览器 / 前端页面
    -> 后端（早期方案）或直接访问 ESP32（后期测试页）
    -> ESP32 HTTP 控制接口
    -> ESP32 UART 桥接
    -> STM32 串口协议解析
    -> 双路 SG90 舵机动作
    -> 校准参数保存到 STM32 Flash 并开机自动加载
```

同时要求：

1. 固件代码不能全部堆在一个 `main.cpp` 中
2. 后续要支持监控页面中单摄像头操作区集成
3. 测试阶段要支持浏览器直接联调
4. 校准流程不能只停留在内存，要能够持久化

---

## 2. 核心修复概览

本轮已完成以下四类修复：

1. **固件结构重构**：ESP32 与 STM32 从单文件演进为多模块结构
2. **控制链路打通**：浏览器 -> ESP32 -> STM32 -> 舵机 已验证收发成功
3. **校准能力落地**：支持进入校准、设置脉宽、保存、退出、读取校准参数
4. **前端测试页升级**：从设备选择模式切换为输入 IP 直连模式，并加入校准操作面板

---

## 3. 固件结构修复

### 3.1 STM32 结构改造

修改前：

- `firmware/stm32-ptz/src/main.cpp` 只是 LED 闪烁示例
- 没有舵机控制、串口协议、校准存储逻辑

修改后：

- `src/main.cpp`
  - 只负责初始化和轮询，不再承载业务细节
- `lib/servo/`
  - 负责双舵机状态维护、回中、微调、校准脉宽应用
- `lib/uart_handler/`
  - 负责解析 `HOME / NUDGE / STATUS? / CALIB:*` 指令并回包
- `lib/calibration/`
  - 负责校准参数持久化与读取
- `include/config.h`
  - 固定引脚、默认角度、限位等配置
- `include/protocol.h`
  - 串口协议常量定义

带来的效果：

- `main.cpp` 不再混杂协议、舵机、存储逻辑
- 后续新增 OLED、按键、ADC 或监控状态时，职责边界清晰
- 校准逻辑有明确落点，不会污染动作控制层

### 3.2 ESP32 结构改造

修改前：

- `firmware/esp32-cam/src/main.cpp` 既做摄像头初始化，又做 WiFi，又做 MJPEG 流输出
- 控制接口、串口桥接和视频流都耦合在一起

修改后：

- `src/main.cpp`
  - 只做模块启动与主循环
- `lib/camera/`
  - 负责摄像头初始化和 MJPEG 视频流
- `lib/network/`
  - 负责 HTTP 路由、控制接口、返回 JSON 封装
- `lib/uart_bridge/`
  - 负责与 STM32 的串口收发桥接
- `include/config.h`
  - WiFi、串口、端口等配置
- `include/protocol.h`
  - 桥接层使用的串口指令常量

带来的效果：

- 视频与控制分层清楚
- 后续调试网络接口不需要触碰摄像头实现
- 更适合后续增加 Web 校准界面、设备状态接口等能力

---

## 4. 串口链路关键修复

### 4.1 STM32 串口实例错误修复

现象：

- ESP32 `/api/ptz/status` 返回 `ERR:UART_TIMEOUT`
- STM32 明明已烧录，但 ESP32 收不到回包

根因：

- 硬件接线使用的是 `PA9/PA10 (USART1)`
- 代码初版把 PTZ 协议处理挂在了 `Serial1`
- Blue Pill 上实际应使用 `Serial` 对应 USART1

修复前：

```cpp
Serial1.begin(...)
g_uartHandler.begin(Serial1)
```

修复后：

```cpp
Serial.begin(...)
g_uartHandler.begin(Serial)
```

带来的效果：

- ESP32 能立即收到 `STATUS:90,90,0`
- 说明 `ESP32 -> UART -> STM32 -> UART -> ESP32` 链路被真正打通

### 4.2 POST 与浏览器地址栏不兼容问题

现象：

- `/api/ptz/status` 浏览器可打开
- `/api/ptz/home`、`/api/ptz/nudge` 地址栏访问显示 `Not found`

根因：

- `status` 是 GET
- `home/nudge` 初版只注册了 POST
- 浏览器地址栏只能直接发 GET

修复后：

- `home`、`nudge`、后续 `calib/*` 同时支持 `GET + POST`

带来的效果：

- 地址栏直接测试设备控制成为可能
- 适合没有前端页面时快速联调

---

## 5. 控制与状态返回结构修复

修改前：

- 返回值只有原始回包字符串，不利于前端直接使用

修改后：

- ESP32 控制接口统一返回结构化 JSON，例如：

```json
{
  "ok": true,
  "command": "NUDGE",
  "raw": "ACK:85,90",
  "pan": 85,
  "tilt": 90,
  "value3": -1,
  "panMinUs": -1,
  "panMaxUs": -1,
  "panCenterUs": -1,
  "tiltMinUs": -1,
  "tiltMaxUs": -1,
  "tiltCenterUs": -1
}
```

各字段含义：

- `ok`：本次命令是否被成功解析并返回有效数据
- `command`：本次执行的动作
- `raw`：STM32 原始串口回包，便于排查协议层问题
- `pan / tilt`：当前角度状态
- `value3`：状态扩展位，当前用于 `mode`（0 正常，1 校准）
- `*Us`：校准参数读取时返回的最小/最大/中位脉宽

带来的效果：

- 前端不必自己去解析原始串口字符串
- 后续接入监控详情弹层时可以直接复用这些字段

---

## 6. 校准模式落地修复

### 6.1 新增校准协议支持

在 STM32 上新增并接入了以下指令：

- `CALIB:START`
- `CALIB:PAN,<pulse_us>`
- `CALIB:TILT,<pulse_us>`
- `CALIB:SAVE`
- `CALIB:EXIT`
- `CALIB:DATA?`

对应回包：

- `CALIB:OK,<pan_us>,<tilt_us>`
- `CALIB:DATA,<pan_min>,<pan_max>,<pan_center>,<tilt_min>,<tilt_max>,<tilt_center>`

### 6.2 校准数据持久化

修改前：

- `saveCalibration()` 只是内存占位
- 板子重启后校准结果丢失

修改后：

- 新增 `CalibStorage` 模块
- 通过 STM32 Arduino 框架提供的 EEPROM 仿真接口保存结构体：

```cpp
struct CalibData {
    uint16_t panMin;
    uint16_t panMax;
    uint16_t panCenter;
    uint16_t tiltMin;
    uint16_t tiltMax;
    uint16_t tiltCenter;
    uint32_t magic;
};
```

- `magic = 0xA5A5A5A5` 用于判断是否存在有效校准数据

### 6.3 开机自动加载校准参数

修改前：

- 开机永远使用默认 `1000 / 2000 / 1500` 范围与中位

修改后：

- `PtzServo::begin()` 中启动时优先 `load()`
- 如果 EEPROM 中存在有效结构体，则覆盖默认值
- `home()` 使用加载后的 `centerUs`

带来的效果：

- 完成校准并 `save` 后，断电重启仍能保留参数
- 后续 `HOME` 和普通 `NUDGE` 动作都基于保存后的校准结果工作

---

## 7. 后端控制代理补充

虽然测试页后期切成了“输入 IP 直连 ESP32”模式，但本轮也补了后端控制代理，为后续正式接入保留路径。

新增：

- `backend/src/main/java/com/springboot/model/dto/cameradevice/CameraPtzControlRequest.java`
- `backend/src/main/java/com/springboot/service/Esp32PtzControlService.java`
- `POST /cameras/control/ptz`

后端支持动作：

- `HOME`
- `STATUS`
- `NUDGE`
- `CALIB_START`
- `CALIB_DATA`
- `CALIB_SAVE`
- `CALIB_EXIT`
- `CALIB_PAN`
- `CALIB_TILT`

带来的效果：

- 即使前端测试页改成直连模式，后端仍保留“正式业务接入”的可复用控制接口
- 后续监控页面如需通过权限和设备模型统一控制，不需要重做协议层

---

## 8. 前端测试页改造

### 8.1 从“选择设备”切换到“输入 IP 直连”

修改前：

- 前端测试页通过后台设备列表选取 PTZ 设备
- 再调用后端控制代理

问题：

- 测试阶段联调更常见的是手里只有一个在线 ESP32 IP
- 设备数据未完全录入后台时不方便测试

修改后：

- 测试页改为输入设备 IP，例如 `192.168.137.175`
- 自动生成：
  - 视频流地址：`http://<ip>/stream`
  - 控制地址：`http://<ip>/api/ptz/*`

新增工具文件：

- `frontend/src/utils/ptzDirectControl.ts`

### 8.2 增加校准面板

测试页新增功能：

- 进入校准
- 读取校准参数
- 保存校准
- 退出校准
- `PAN` / `TILT` 脉宽输入与应用
- `PAN` 快捷预设按钮（500 / 1400 / 2400）
- 页面实时显示当前校准参数

带来的效果：

- 不必再通过浏览器地址栏手敲每个校准接口
- 校准结果能在页面中即时回显
- 测试人员能明确区分：
  - 方向按钮 = 控制链路验证
  - 校准按钮 = 脉宽级校准

### 8.3 页面错误提示修复

修改前：

- 按钮失败时容易表现为“点击没反应”

修改后：

- 所有动作统一包裹错误处理
- 错误会显示到：
  - `ElMessage`
  - “最近结果”区域

带来的效果：

- 不再出现“页面像没点到，其实已经失败”的静默问题

---

## 9. 直连模式的跨域问题与修复

### 9.1 现象

当前端从 `http://localhost:5173` 直接访问 `http://192.168.137.175/api/ptz/*` 时，浏览器报错：

```text
Access to fetch ... has been blocked by CORS policy:
No 'Access-Control-Allow-Origin' header is present on the requested resource.
```

### 9.2 根因

- 测试页已切换成“前端直连 ESP32”模式
- 浏览器会对跨源请求执行 CORS 校验
- ESP32 当前在线固件的响应里没有稳定返回跨域头

### 9.3 已做修复

在 ESP32 的 `ControlServer` 中补了两层跨域支持：

1. 每个 JSON 响应前手动补头
2. `WebServer` 级别启用：

```cpp
server.enableCORS(true);
```

同时在 `OPTIONS` 场景下返回：

- `Access-Control-Allow-Origin: *`
- `Access-Control-Allow-Methods: GET, POST, OPTIONS`
- `Access-Control-Allow-Headers: Content-Type`

### 9.4 当前状态判断

如果浏览器仍报 CORS 错误，最可能原因不是前端页面，而是：

1. ESP32 仍运行旧固件，没有刷入最新版本
2. 烧录后未完成真正复位启动（用户怀疑与 `RST` 有关）

---

## 10. 当前已验证结果

### 10.1 设备链路验证

已确认成功的环节：

- `ESP32 /status` 返回：

```json
{"camera":"OK","wifi":"connected","ip":"192.168.137.175","uptime":120}
```

- `ESP32 /api/ptz/status` 能返回：

```json
{"ok":true,"response":"STATUS:90,90,0"}
```

- 后续又验证了：

```json
{"ok":true,"response":"ACK:85,90"}
```

这说明：

- ESP32 摄像头与 WiFi 正常
- ESP32 HTTP 接口正常
- ESP32 与 STM32 串口收发正常
- STM32 能解析协议并更新角度状态

### 10.2 构建验证

本轮各端都执行了构建验证：

- 后端：`mvn spotless:apply && mvn compile`
- 前端：`npm run build`
- STM32：`pio run`
- ESP32：`pio run`

均已通过。

---

## 11. 当前剩余问题与风险

### 11.1 ESP32 在线固件与本地源码不一致风险

当前最现实的风险不是代码逻辑本身，而是：

- 本地源码已经带有 CORS 与校准数据接口
- 但板子上实际运行的固件可能仍是旧版

表征：

- 浏览器地址栏访问接口可以正常返回 JSON
- 前端页面 fetch 同一接口时却被 CORS 拦截

这通常意味着：

- 设备功能已存在
- 但缺少最新跨域头修复

### 11.2 可能需要硬件复位确认新固件生效

用户提出“可能没有 RST 的原因”，这个判断是合理的。

原因：

- 某些板子烧录完成后虽然 OpenOCD 提示复位，但设备实际运行状态未完全切到新逻辑
- 尤其在串口监视器、供电、板载复位键配合不稳定时，会出现“看似烧录成功、实际行为仍像旧版本”的情况

建议执行：

1. 重新烧录 ESP32 最新固件
2. 手动按一次复位键（RST）
3. 再访问：
   - `http://<ip>/api/ptz/status`
4. 在浏览器开发者工具中检查响应头是否带：
   - `Access-Control-Allow-Origin: *`

---

## 12. 建议的后续步骤

### 12.1 短期联调步骤

1. 重新烧录 ESP32 最新固件
2. 手动 RST 复位 ESP32
3. 打开 `http://<ip>/api/ptz/status`
4. 检查响应头是否存在 `Access-Control-Allow-Origin`
5. 若存在，再回前端测试页验证所有按钮

### 12.2 校准流程建议

页面中继续完成：

1. 进入校准
2. 设置 `PAN/TILT` 脉宽
3. 读取校准参数确认当前值
4. 保存校准
5. 退出校准
6. 断电重启后再次读取校准参数，验证持久化

### 12.3 后续功能建议

后续值得继续补充的点：

1. 在前端校准页增加“记录为最小值 / 中位 / 最大值”明确按钮，避免依赖“最后一次脉宽即中位”的隐式逻辑
2. 在监控页单摄像头详情弹层中复用现在的控制与校准能力
3. 为 ESP32 增加更明确的设备信息接口，如 `/api/ptz/info`，统一返回版本、模式、校准参数和连通状态

---

## 13. 本轮结论

本轮已经完成的不是一个零散补丁，而是一套完整的云台联调基础设施：

- 固件结构已模块化
- 串口协议已打通
- 舵机控制已打通
- 校准模式已落地
- 校准参数已支持持久化和开机自动加载
- 前端测试页已支持输入 IP 直连与校准面板
- 当前唯一未完全闭环的问题是：**ESP32 在线固件的跨域头是否已真正生效**

因此，本轮工作的整体状态可以总结为：

> 控制链路、校准链路和持久化链路都已在代码层完成，当前剩余问题主要集中在 ESP32 最新固件是否已实际生效以及是否需要手动 RST 复位确认。
