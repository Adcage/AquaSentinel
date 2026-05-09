#include <cassert>

#include "BatteryMonitor.h"

int main() {
    BatteryMonitor monitor;

    assert(BatteryMonitor::rawToBatteryMv(2048) == 3300);
    assert(BatteryMonitor::batteryMvToPercent(4200) == 100);
    assert(BatteryMonitor::batteryMvToPercent(3000) == 0);
    assert(BatteryMonitor::batteryMvToPercent(2900) == 0);
    assert(BatteryMonitor::batteryMvToPercent(4300) == 100);

    assert(BatteryMonitor::batteryMvToPercent(3000) == 0);
    assert(BatteryMonitor::batteryMvToPercent(3200) == 5);
    assert(BatteryMonitor::batteryMvToPercent(3450) == 15);
    assert(BatteryMonitor::batteryMvToPercent(3600) == 35);
    assert(BatteryMonitor::batteryMvToPercent(3700) == 50);
    assert(BatteryMonitor::batteryMvToPercent(3800) == 65);
    assert(BatteryMonitor::batteryMvToPercent(3900) == 75);
    assert(BatteryMonitor::batteryMvToPercent(4000) == 85);
    assert(BatteryMonitor::batteryMvToPercent(4100) == 95);

    assert(BatteryMonitor::batteryMvToPercent(3100) == 2);
    assert(BatteryMonitor::batteryMvToPercent(3500) == 25);
    assert(BatteryMonitor::batteryMvToPercent(3850) == 70);

    monitor.reset();
    monitor.ingestRawSample(2480);
    monitor.ingestRawSample(2488);
    monitor.ingestRawSample(2496);
    const BatteryReading reading = monitor.reading();

    assert(reading.raw > 0);
    assert(reading.batteryMv > 3900 && reading.batteryMv < 4100);
    assert(reading.percent > 75 && reading.percent < 100);

    return 0;
}