# AquaSentinel 硬件引脚对照表

> 本文档用于交叉核对 `hardware-design-spec.md`、当前 KiCad 原理图、当前 PCB 丝印与外设连接器的最终映射。
> 基准文件：
> - `docs/hardware/hardware-design-spec.md`
> - `firmware/hardware/aquasentinel-hardware.kicad_sch`
> - `firmware/hardware/aquasentinel-hardware.kicad_pcb`

---

## 1. 串口映射

| 功能 | 文档要求 | 原理图网络 | ESP32-CAM 物理孔位/丝印 | STM32 Blue Pill 物理孔位/丝印 | 说明 |
|------|----------|------------|--------------------------|-------------------------------|------|
| `UART_ESP_TX` | ESP32 `GPIO13` -> STM32 `PA10` | `UART_ESP_TX` | `J4 pin 7` / `IO13` | `J5 pin 28` / `A10` / `PA10_USART1_RX` | ESP32 发，STM32 收 |
| `UART_ESP_RX` | ESP32 `GPIO14` <- STM32 `PA9` | `UART_ESP_RX` | `J4 pin 11` / `IO14` | `J5 pin 30` / `A9` / `PA9_USART1_TX` | STM32 发，ESP32 收 |

### 1.1 不再用于串口的 ESP32 引脚

| 引脚 | 当前状态 | 说明 |
|------|----------|------|
| `U0T` | 未连接 | 保留调试/烧录用途 |
| `U0R` | 未连接 | 保留调试/烧录用途 |

---

## 2. 舵机映射

| 外设 | 网络 | 文档要求 | STM32 Blue Pill 物理孔位/丝印 | 连接器孔位 |
|------|------|----------|-------------------------------|------------|
| `J6 SERVO_PAN_SG90` | `SERVO_PAN` | `PA6 / TIM3_CH1` | `J5 pin 21` / `A6` | `J6 pin 3` |
| `J7 SERVO_TILT_SG90` | `SERVO_TILT` | `PA7 / TIM3_CH2` | `J5 pin 23` / `A7` | `J7 pin 3` |

### 2.1 舵机供电

| 外设 | 电源网络 | 物理孔位 |
|------|----------|----------|
| `J6` | `VCC_5V` | `pin 1` |
| `J6` | `GND` | `pin 2` |
| `J7` | `VCC_5V` | `pin 1` |
| `J7` | `GND` | `pin 2` |

---

## 3. OLED 映射

| 外设 | 网络 | 文档要求 | STM32 Blue Pill 物理孔位/丝印 | 连接器孔位 |
|------|------|----------|-------------------------------|------------|
| `J8 SSD1306_OLED_I2C` | `VCC_3V3` | STM32 `3.3V` 供电 | `J5 pin 2` / `3V3` | `J8 pin 1` |
| `J8 SSD1306_OLED_I2C` | `GND` | 公共地 | `J5 pin 4` 或 `J5 pin 37` | `J8 pin 2` |
| `J8 SSD1306_OLED_I2C` | `I2C_SCL` | `PB6 / I2C1_SCL` | `J5 pin 14` / `B6` | `J8 pin 3` |
| `J8 SSD1306_OLED_I2C` | `I2C_SDA` | `PB7 / I2C1_SDA` | `J5 pin 12` / `B7` | `J8 pin 4` |

---

## 4. 用户按键映射

| 外设 | 网络 | 文档要求 | STM32 Blue Pill 物理孔位/丝印 | 连接器孔位 |
|------|------|----------|-------------------------------|------------|
| `SW1 USER_HOME_CALIB` | `BTN_USER` | `PB12` 输入，按下接地 | `J5 pin 40` / `B12` | `SW1 pin 1` |
| `SW1 USER_HOME_CALIB` | `GND` | 公共地 | `J5 pin 4` 或 `J5 pin 37` | `SW1 pin 2` |

---

## 5. 电池采样与主电源

| 功能 | 网络 | 文档要求 | STM32 Blue Pill 物理孔位/丝印 | 说明 |
|------|------|----------|-------------------------------|------|
| 电池采样 | `BAT_SENSE` | `PA0 / ADC1_IN0` | `J5 pin 9` / `A0` | 来自分压电阻中点 |
| 主 5V 输入 | `VCC_5V` | 升压模块 5V 主干 | `J5 pin 6` / `5V` | 同时分配到 ESP32 与两路舵机 |
| 主 3.3V | `VCC_3V3` | 板载 3.3V 输出 | `J5 pin 2` / `3V3` | 供 OLED 使用 |
| 系统地 | `GND` | 所有模块共地 | `J5 pin 4` / `GND`、`J5 pin 37` / `GND` | 推荐全系统共地 |

---

## 6. 快速排线清单

### 6.1 ESP32-CAM 到 STM32

| 起点 | 终点 |
|------|------|
| `J4 IO13` | `J5 A10 / PA10` |
| `J4 IO14` | `J5 A9 / PA9` |
| `J4 5V` | `VCC_5V` |
| `J4 GND` | `GND` |

### 6.2 STM32 到外设

| 起点 | 终点 |
|------|------|
| `J5 A6 / PA6` | `J6 pin 3` |
| `J5 A7 / PA7` | `J7 pin 3` |
| `J5 B6 / PB6` | `J8 pin 3` |
| `J5 B7 / PB7` | `J8 pin 4` |
| `J5 B12 / PB12` | `SW1 pin 1` |
| `J5 A0 / PA0` | `BAT_SENSE` 分压点 |

---

## 7. 本次修正后重点确认项

1. `J7` 必须接 `A7`，不能接 `A9`。
2. `ESP32-CAM` 串口必须走 `IO13/IO14`，不能走 `U0T/U0R`。
3. `OLED` 必须走 `B6/B7`，不能占用 `A9/A10`。
4. `BTN_USER` 必须接 `PB12/B12`，不能落到 `GND` 引脚。
5. `BAT_SENSE` 必须接 `PA0/A0`，不能和 `5V` 互换。

---

## 8. 验证记录

本对照表对应的当前工程已完成以下验证：

```bash
kicad-cli sch erc --format json --severity-all --exit-code-violations --output "$env:TEMP\aquasentinel-hardware-erc.json" "firmware/hardware/aquasentinel-hardware.kicad_sch"
kicad-cli sch export netlist --format kicadsexpr --output "$env:TEMP\aquasentinel-hardware.net" "firmware/hardware/aquasentinel-hardware.kicad_sch"
kicad-cli pcb export svg "firmware/hardware/aquasentinel-hardware.kicad_pcb" --layers F.Cu --mode-single --output "$env:TEMP\aquasentinel-hardware-front-final.svg"
```

结果：
- 原理图 ERC `0` 违规
- 原理图网表导出成功
- PCB SVG 导出成功
