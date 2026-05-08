#include <cassert>

#include "BatteryMonitor.h"

int main() {
    BatteryMonitor monitor;

    assert(BatteryMonitor::rawToBatteryMv(2048) == 3300);
    assert(BatteryMonitor::batteryMvToPercent(4200) == 100);
    assert(BatteryMonitor::batteryMvToPercent(3300) == 0);

    monitor.reset();
    monitor.ingestRawSample(2480);
    monitor.ingestRawSample(2488);
    monitor.ingestRawSample(2496);
    const BatteryReading reading = monitor.reading();

    assert(reading.raw > 0);
    assert(reading.batteryMv > 3900 && reading.batteryMv < 4100);
    assert(reading.percent > 60 && reading.percent < 90);

    return 0;
}
