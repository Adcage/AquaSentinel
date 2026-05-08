#include "CalibStorage.h"

#include <EEPROM.h>

extern "C" {
#include <utility/stm32_eeprom.h>
}

namespace {
constexpr int EEPROM_ADDR = 0;
}  // namespace

bool CalibStorage::load(CalibData& outData) {
    eeprom_buffer_fill();
    EEPROM.get(EEPROM_ADDR, outData);
    if (outData.magic != MAGIC) {
        return false;
    }
    return true;
}

bool CalibStorage::save(const CalibData& data) {
    eeprom_buffer_fill();
    EEPROM.put(EEPROM_ADDR, data);
    eeprom_buffer_flush();

    CalibData written{};
    eeprom_buffer_fill();
    EEPROM.get(EEPROM_ADDR, written);

    return written.magic == data.magic &&
           written.panMin == data.panMin &&
           written.panMax == data.panMax &&
           written.panCenter == data.panCenter &&
           written.tiltMin == data.tiltMin &&
           written.tiltMax == data.tiltMax &&
           written.tiltCenter == data.tiltCenter;
}
