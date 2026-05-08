# STM32 PB12 按键与 OLED 页面交互 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `firmware/stm32-ptz` 增加 `PB12` 单按键交互，使 OLED 支持短按切页、长按回中，并预留后续超长按进入校准模式的扩展位。

**Architecture:** 在现有 `UART + 舵机 + OLED` 结构上新增一个轻量按键状态机模块，负责 `PB12` 的消抖、短按、长按事件输出；主循环只消费事件，不直接处理原始电平。OLED 页面模型从“按当前运行状态自动生成一页”扩展为“按显式页面枚举 + 运行状态生成不同页面”，同时保留固定中文子集字模和轻量 SSD1306 驱动，避免重新引入大字库导致 Flash 超限。

**Tech Stack:** PlatformIO、STM32duino (Arduino framework)、C++11、`Wire`、自实现 SSD1306 I2C 驱动、现有 `PtzServo`/`UartHandler`、手工主机侧 `clang++` 回归测试程序。

---

## 文件结构与职责

### 新增文件

- `firmware/stm32-ptz/lib/button/ButtonHandler.h`
  - 定义按键事件枚举、按键状态结构、按键处理器对外接口。
- `firmware/stm32-ptz/lib/button/ButtonHandler.cpp`
  - 负责 `PB12` 消抖、短按/长按识别、一次性事件发射。
- `firmware/stm32-ptz/test/host/test_button_handler.cpp`
  - 主机侧回归测试，验证按键状态机的短按/长按边界。
- `firmware/stm32-ptz/test/host/test_oled_view_model_pages.cpp`
  - 主机侧回归测试，验证页面切换后的文案输出是否符合预期。

### 修改文件

- `firmware/stm32-ptz/include/config.h`
  - 增加 `PB12` 引脚、消抖时间、长按阈值、页面枚举相关常量。
- `firmware/stm32-ptz/lib/oled_display/OledViewModel.h`
  - 增加页面枚举、UI 状态结构、页面渲染入口。
- `firmware/stm32-ptz/lib/oled_display/OledViewModel.cpp`
  - 从单页渲染改为显式页面渲染，新增“电量预留页”“回中中提示”。
- `firmware/stm32-ptz/lib/oled_display/OledDisplay.h`
  - 更新 `update` 输入参数，使其接收更完整的 UI 状态。
- `firmware/stm32-ptz/lib/oled_display/OledDisplay.cpp`
  - 补充需要显示的新中文子集字模，如 `电量`、`未接入`、`回中中`。
- `firmware/stm32-ptz/src/main.cpp`
  - 初始化 `PB12`、接入 `ButtonHandler`、维护页面索引、消费短按/长按事件。

---

### Task 1: 定义按键与页面状态契约

**Files:**
- Create: `firmware/stm32-ptz/lib/button/ButtonHandler.h`
- Modify: `firmware/stm32-ptz/include/config.h`
- Modify: `firmware/stm32-ptz/lib/oled_display/OledViewModel.h`
- Test: `firmware/stm32-ptz/test/host/test_oled_view_model_pages.cpp`

- [ ] **Step 1: 先写页面模型失败测试**

```cpp
#include <cassert>

#include "OledViewModel.h"

int main() {
    OledUiState state{};
    state.page = OledPage::Status;
    state.pan = 90;
    state.tilt = 120;

    OledFrame statusFrame = OledViewModel::build(state);
    assert(std::string(statusFrame.title) == "云台");
    assert(std::string(statusFrame.lines[0]) == "MODE NORM");

    state.page = OledPage::Battery;
    OledFrame batteryFrame = OledViewModel::build(state);
    assert(std::string(batteryFrame.title) == "电量");
    assert(std::string(batteryFrame.lines[0]) == "ADC 预留");
}
```

- [ ] **Step 2: 运行测试，确认当前接口还不支持页面枚举**

Run:
```bash
clang++ -std=gnu++11 -I"firmware/stm32-ptz/lib/oled_display" "firmware/stm32-ptz/test/host/test_oled_view_model_pages.cpp" "firmware/stm32-ptz/lib/oled_display/OledViewModel.cpp" -o "/var/folders/cc/vmnhxdy55k79lk4ylmtsz26m0000gn/T/opencode/test_oled_pages"
```

Expected:
- 编译失败，提示 `OledUiState`、`OledPage` 未定义，或 `build` 签名不匹配。

- [ ] **Step 3: 在 `config.h` 和 `OledViewModel.h` 中定义页面与按键基础常量**

```cpp
namespace ptz_config {

constexpr uint8_t PIN_BUTTON_USER = PB12;
constexpr uint32_t BUTTON_DEBOUNCE_MS = 30;
constexpr uint32_t BUTTON_LONG_PRESS_MS = 800;
constexpr uint32_t BUTTON_SUPER_LONG_PRESS_MS = 2500;
constexpr uint32_t OLED_ACTION_MESSAGE_MS = 1200;

}  // namespace ptz_config
```

```cpp
enum class OledPage : uint8_t {
    Status = 0,
    Calibration = 1,
    Battery = 2,
};

struct OledUiState {
    OledPage page = OledPage::Status;
    uint8_t pan = 0;
    uint8_t tilt = 0;
    bool calibrationMode = false;
    uint16_t panPulseUs = 1500;
    uint16_t tiltPulseUs = 1500;
    uint32_t uptimeMs = 0;
    bool showActionMessage = false;
    char actionMessage[OLED_TEXT_BUFFER_SIZE] = {0};
};

class OledViewModel {
   public:
    static OledFrame build(const OledUiState& state);
};
```

- [ ] **Step 4: 更新 `OledViewModel.cpp` 以支持显式页面**

```cpp
OledFrame OledViewModel::build(const OledUiState& state) {
    OledFrame frame{};
    const bool isBootScreen = state.uptimeMs < BOOT_SCREEN_DURATION_MS;

    if (isBootScreen) {
        copyText(frame.title, "AQUASENTINEL");
        copyText(frame.lines[0], "启动中");
        copyText(frame.lines[1], "OLED READY");
        buildAngleLine(frame.lines[2], "PAN", state.pan);
        buildAngleLine(frame.lines[3], "TILT", state.tilt);
        return frame;
    }

    if (state.showActionMessage) {
        copyText(frame.title, "云台");
        copyText(frame.lines[0], state.actionMessage);
        buildAngleLine(frame.lines[1], "PAN", state.pan);
        buildAngleLine(frame.lines[2], "TILT", state.tilt);
        copyText(frame.lines[3], "UART READY");
        return frame;
    }

    switch (state.page) {
        case OledPage::Status:
            copyText(frame.title, "云台");
            copyText(frame.lines[0], "MODE NORM");
            buildAngleLine(frame.lines[1], "PAN", state.pan);
            buildAngleLine(frame.lines[2], "TILT", state.tilt);
            copyText(frame.lines[3], "UART READY");
            break;
        case OledPage::Calibration:
            copyText(frame.title, "校准");
            copyText(frame.lines[0], state.calibrationMode ? "MODE CAL" : "CAL READY");
            buildPulseLine(frame.lines[1], "PAN", state.panPulseUs);
            buildPulseLine(frame.lines[2], "TILT", state.tiltPulseUs);
            copyText(frame.lines[3], "HOLD FOR CAL");
            break;
        case OledPage::Battery:
            copyText(frame.title, "电量");
            copyText(frame.lines[0], "ADC 预留");
            copyText(frame.lines[1], "PA0 WAIT");
            copyText(frame.lines[2], "BAT WAIT");
            copyText(frame.lines[3], "SHORT NEXT");
            break;
    }

    return frame;
}
```

- [ ] **Step 5: 重新运行页面模型测试，确认页面接口生效**

Run:
```bash
clang++ -std=gnu++11 -I"firmware/stm32-ptz/lib/oled_display" "firmware/stm32-ptz/test/host/test_oled_view_model_pages.cpp" "firmware/stm32-ptz/lib/oled_display/OledViewModel.cpp" -o "/var/folders/cc/vmnhxdy55k79lk4ylmtsz26m0000gn/T/opencode/test_oled_pages" && "/var/folders/cc/vmnhxdy55k79lk4ylmtsz26m0000gn/T/opencode/test_oled_pages"
```

Expected:
- 命令退出码为 `0`
- 无断言失败输出

- [ ] **Step 6: Commit**

```bash
git add "firmware/stm32-ptz/include/config.h" "firmware/stm32-ptz/lib/oled_display/OledViewModel.h" "firmware/stm32-ptz/lib/oled_display/OledViewModel.cpp" "firmware/stm32-ptz/test/host/test_oled_view_model_pages.cpp"
git commit -m "feat(firmware): 定义 OLED 页面与按键状态模型"
```

---

### Task 2: 实现 PB12 按键状态机

**Files:**
- Create: `firmware/stm32-ptz/lib/button/ButtonHandler.cpp`
- Create: `firmware/stm32-ptz/lib/button/ButtonHandler.h`
- Test: `firmware/stm32-ptz/test/host/test_button_handler.cpp`
- Modify: `firmware/stm32-ptz/include/config.h`

- [ ] **Step 1: 先写失败测试，覆盖短按与长按边界**

```cpp
#include <cassert>

#include "ButtonHandler.h"

int main() {
    ButtonHandler button;

    ButtonEvent event = button.update(false, 0);
    assert(event == ButtonEvent::None);

    button.update(true, 10);
    button.update(true, 50);
    button.update(false, 120);
    event = button.update(false, 160);
    assert(event == ButtonEvent::ShortPress);

    button.reset();
    button.update(true, 1000);
    button.update(true, 1900);
    event = button.update(true, 2000);
    assert(event == ButtonEvent::LongPress);
}
```

- [ ] **Step 2: 运行测试，确认当前按键模块不存在**

Run:
```bash
clang++ -std=gnu++11 -I"firmware/stm32-ptz/include" -I"firmware/stm32-ptz/lib/button" "firmware/stm32-ptz/test/host/test_button_handler.cpp" "firmware/stm32-ptz/lib/button/ButtonHandler.cpp" -o "/var/folders/cc/vmnhxdy55k79lk4ylmtsz26m0000gn/T/opencode/test_button_handler"
```

Expected:
- 编译失败，提示 `ButtonHandler` 或 `ButtonEvent` 未定义。

- [ ] **Step 3: 实现最小按键状态机**

```cpp
enum class ButtonEvent : uint8_t {
    None = 0,
    ShortPress,
    LongPress,
};

class ButtonHandler {
   public:
    void begin(bool activeLow = true);
    ButtonEvent update(bool rawPressed, uint32_t nowMs);
    void reset();

   private:
    bool activeLow = true;
    bool stablePressed = false;
    bool lastRawPressed = false;
    bool longPressFired = false;
    uint32_t lastBounceMs = 0;
    uint32_t pressStartMs = 0;
};
```

```cpp
ButtonEvent ButtonHandler::update(bool rawPressed, uint32_t nowMs) {
    if (rawPressed != lastRawPressed) {
        lastRawPressed = rawPressed;
        lastBounceMs = nowMs;
    }

    if ((nowMs - lastBounceMs) < ptz_config::BUTTON_DEBOUNCE_MS) {
        return ButtonEvent::None;
    }

    if (stablePressed != rawPressed) {
        stablePressed = rawPressed;
        if (stablePressed) {
            pressStartMs = nowMs;
            longPressFired = false;
        } else {
            if (!longPressFired) {
                return ButtonEvent::ShortPress;
            }
        }
    }

    if (stablePressed && !longPressFired && (nowMs - pressStartMs) >= ptz_config::BUTTON_LONG_PRESS_MS) {
        longPressFired = true;
        return ButtonEvent::LongPress;
    }

    return ButtonEvent::None;
}
```

- [ ] **Step 4: 运行测试，确认短按/长按识别通过**

Run:
```bash
clang++ -std=gnu++11 -I"firmware/stm32-ptz/include" -I"firmware/stm32-ptz/lib/button" "firmware/stm32-ptz/test/host/test_button_handler.cpp" "firmware/stm32-ptz/lib/button/ButtonHandler.cpp" -o "/var/folders/cc/vmnhxdy55k79lk4ylmtsz26m0000gn/T/opencode/test_button_handler" && "/var/folders/cc/vmnhxdy55k79lk4ylmtsz26m0000gn/T/opencode/test_button_handler"
```

Expected:
- 命令退出码为 `0`
- 无断言失败输出

- [ ] **Step 5: Commit**

```bash
git add "firmware/stm32-ptz/lib/button/ButtonHandler.h" "firmware/stm32-ptz/lib/button/ButtonHandler.cpp" "firmware/stm32-ptz/test/host/test_button_handler.cpp"
git commit -m "feat(firmware): 为 PB12 增加短按与长按识别"
```

---

### Task 3: 将页面切换与长按回中接入主循环

**Files:**
- Modify: `firmware/stm32-ptz/src/main.cpp`
- Modify: `firmware/stm32-ptz/lib/oled_display/OledDisplay.h`
- Modify: `firmware/stm32-ptz/lib/oled_display/OledDisplay.cpp`
- Modify: `firmware/stm32-ptz/lib/oled_display/OledViewModel.h`
- Modify: `firmware/stm32-ptz/lib/oled_display/OledViewModel.cpp`

- [ ] **Step 1: 先写失败测试，要求状态页切到电量页**

```cpp
#include <cassert>

#include "OledViewModel.h"

int main() {
    OledUiState state{};
    state.page = OledPage::Battery;
    OledFrame frame = OledViewModel::build(state);

    assert(std::string(frame.title) == "电量");
    assert(std::string(frame.lines[0]) == "ADC 预留");

    state.showActionMessage = true;
    OledViewModel::copyText(state.actionMessage, "回中中");
    frame = OledViewModel::build(state);
    assert(std::string(frame.lines[0]) == "回中中");
}
```

- [ ] **Step 2: 运行测试，确认当前还没有动作提示页或电量页中文字模**

Run:
```bash
clang++ -std=gnu++11 -I"firmware/stm32-ptz/lib/oled_display" "firmware/stm32-ptz/test/host/test_oled_view_model_pages.cpp" "firmware/stm32-ptz/lib/oled_display/OledViewModel.cpp" -o "/var/folders/cc/vmnhxdy55k79lk4ylmtsz26m0000gn/T/opencode/test_oled_pages"
```

Expected:
- 若文案或接口未补齐，断言失败或编译失败。

- [ ] **Step 3: 在 `main.cpp` 接入按键事件与页面索引**

```cpp
PtzServo g_servo;
UartHandler g_uartHandler(g_servo);
OledDisplay g_oledDisplay;
ButtonHandler g_buttonHandler;
OledPage g_currentPage = OledPage::Status;
uint32_t g_actionMessageUntilMs = 0;
char g_actionMessage[OLED_TEXT_BUFFER_SIZE] = {0};

void setup() {
    Serial.begin(ptz_config::UART_BAUD_RATE);
    pinMode(ptz_config::PIN_BUTTON_USER, INPUT_PULLUP);
    g_buttonHandler.begin(true);
    g_servo.begin(ptz_config::PIN_SERVO_PAN, ptz_config::PIN_SERVO_TILT);
    g_uartHandler.begin(Serial);
    g_oledDisplay.begin();
}

void loop() {
    const uint32_t nowMs = millis();
    g_uartHandler.poll();

    const bool rawPressed = digitalRead(ptz_config::PIN_BUTTON_USER) == LOW;
    const ButtonEvent event = g_buttonHandler.update(rawPressed, nowMs);

    if (event == ButtonEvent::ShortPress) {
        g_currentPage = nextPage(g_currentPage);
    } else if (event == ButtonEvent::LongPress) {
        g_servo.home();
        strncpy(g_actionMessage, "回中中", OLED_TEXT_BUFFER_SIZE - 1);
        g_actionMessageUntilMs = nowMs + ptz_config::OLED_ACTION_MESSAGE_MS;
    }

    OledUiState uiState = buildUiState(nowMs);
    g_oledDisplay.update(uiState);
    delay(2);
}
```

- [ ] **Step 4: 扩展 `OledDisplay` 接口，直接接收 `OledUiState`**

```cpp
class OledDisplay {
   public:
    void begin();
    void update(const OledUiState& state);

   private:
    static bool hasStateChanged(const OledUiState& current, const OledUiState& previous);
    void drawFrame(const OledFrame& frame);
};
```

```cpp
void OledDisplay::update(const OledUiState& state) {
    if (!initialized) {
        return;
    }

    const bool intervalElapsed = (state.uptimeMs - lastRenderMs) >= REFRESH_INTERVAL_MS;
    const bool stateChanged = !hasLastState || hasStateChanged(state, lastState);
    if (!intervalElapsed && !stateChanged) {
        return;
    }

    drawFrame(OledViewModel::build(state));
    lastState = state;
    hasLastState = true;
    lastRenderMs = state.uptimeMs;
}
```

- [ ] **Step 5: 为新页面补全中文子集字模**

```cpp
const Utf8Glyph CHINESE_GLYPHS[] = {
    {"启", {0x040, 0x3FE, 0x202, 0x202, 0x3FE, 0x200, 0x200, 0x3FC, 0x504, 0x504, 0x5FC, 0x000}},
    {"动", {0x008, 0x788, 0x008, 0x03E, 0x7CA, 0x212, 0x292, 0x492, 0x4D2, 0x762, 0x02E, 0x000}},
    {"中", {0x000, 0x040, 0x7FE, 0x442, 0x442, 0x442, 0x7FE, 0x442, 0x040, 0x040, 0x040, 0x000}},
    {"云", {0x3FC, 0x000, 0x000, 0x7FE, 0x080, 0x088, 0x108, 0x104, 0x21C, 0x3E2, 0x000, 0x000}},
    {"台", {0x040, 0x080, 0x108, 0x204, 0x7FA, 0x002, 0x3FC, 0x204, 0x204, 0x204, 0x3FC, 0x000}},
    {"校", {0x210, 0x208, 0x27E, 0x724, 0x242, 0x366, 0x6A4, 0xA18, 0x218, 0x21C, 0x2E2, 0x000}},
    {"准", {0x050, 0x448, 0x2FE, 0x288, 0x188, 0x1FE, 0x288, 0x2FE, 0x488, 0x488, 0x4FE, 0x000}},
    {"电", {0x040, 0x040, 0x3FC, 0x244, 0x3FC, 0x244, 0x244, 0x3FC, 0x040, 0x042, 0x03E, 0x000}},
    {"量", {0x3FC, 0x204, 0x3FC, 0x1F8, 0x7FE, 0x3FC, 0x244, 0x3FC, 0x1F8, 0x3FC, 0x7FE, 0x000}},
    {"回", {0x7FE, 0x402, 0x402, 0x5F2, 0x512, 0x512, 0x4F2, 0x402, 0x402, 0x7FE, 0x000, 0x000}},
};
```

要求：
- `电量` 页标题必须可读
- `ADC 预留` 若只保留 `电量` 中文，其余状态继续使用 ASCII 即可
- `回中中` 提示必须至少保证 `回`、`中` 可正确显示

- [ ] **Step 6: 编译 STM32 固件，确认功能接入后仍未超 Flash**

Run:
```bash
pio run -e bluepill_f103c8
```

Expected:
- 构建成功
- `Flash` 仍明显低于 `65536 bytes`
- `RAM` 未出现异常暴涨

- [ ] **Step 7: Commit**

```bash
git add "firmware/stm32-ptz/src/main.cpp" "firmware/stm32-ptz/lib/oled_display/OledDisplay.h" "firmware/stm32-ptz/lib/oled_display/OledDisplay.cpp" "firmware/stm32-ptz/lib/oled_display/OledViewModel.h" "firmware/stm32-ptz/lib/oled_display/OledViewModel.cpp"
git commit -m "feat(firmware): 为 OLED 接入 PB12 切页与长按回中"
```

---

### Task 4: 板级联调与交互验收

**Files:**
- Modify: `docs/规划文档/stm32-pb12-oled-interaction-plan.md`

- [ ] **Step 1: 进行板级手动验证，按顺序记录结果**

Run:
```bash
pio run -e bluepill_f103c8 -t upload
```

Expected:
- 烧录成功

手动检查项：
- 上电后先显示 `启动中`
- 短按 `PB12`：`云台 -> 校准 -> 电量 -> 云台` 循环
- 长按 `PB12`：云台立即回中，OLED 临时显示 `回中中`
- 长按释放后不会额外触发短按切页
- 中文显示不乱码

- [ ] **Step 2: 如果手动验证出现偏差，只修当前需求范围内的问题**

允许修复：
- 按键阈值过短或过长
- 页面切换顺序错误
- 长按后误触发短按
- 中文字模缺字或错字

禁止顺手做：
- 超长按进入校准
- ADC 真实采样
- 新增第二按键或双击功能

- [ ] **Step 3: 记录验收结果到计划文档末尾**

```markdown
## 验收记录

- [ ] 启动页正常
- [ ] 状态页正常
- [ ] 校准页正常
- [ ] 电量预留页正常
- [ ] 短按切页正常
- [ ] 长按回中正常
- [ ] 中文显示无乱码
- [ ] 编译体积可接受
```

- [ ] **Step 4: Commit**

```bash
git add "docs/规划文档/stm32-pb12-oled-interaction-plan.md"
git commit -m "docs(firmware): 补充 PB12 与 OLED 交互验收记录"
```

---

## 后续阶段（本计划不实现）

1. `PB12` 超长按进入校准模式
- 使用 `BUTTON_SUPER_LONG_PRESS_MS`
- 与长按回中逻辑互斥，且只触发一次

2. `PA0` 电量采样接入
- 电量预留页由当前固定文案替换为实际电压和估算电量

3. 校准页本地入口增强
- 在 OLED 上明确提示 `HOLD FOR CAL`
- 后续与 Web 校准页面联动

---

## 自检结论

- 该计划只覆盖一个明确子项目：`PB12` 按键与 OLED 本地交互。
- 未把 `ADC 实采`、`超长按校准`、`校准调参流程` 混入当前实施范围，满足单阶段聚焦原则。
- 计划中每个任务都给出了明确文件、验证命令和最小实现方向，避免出现只写目标、不写落点的问题。
