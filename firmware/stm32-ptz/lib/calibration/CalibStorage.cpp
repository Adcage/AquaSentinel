#include "CalibStorage.h"

#include <EEPROM.h>

namespace {
constexpr int EEPROM_ADDR = 0;
}  // namespace

bool CalibStorage::load(CalibData& outData) {
    EEPROM.begin();
    EEPROM.get(EEPROM_ADDR, outData);
    if (outData.magic != MAGIC) {
        return false;
    }
    return true;
}

bool CalibStorage::save(const CalibData& data) {
    EEPROM.begin();
    EEPROM.put(EEPROM_ADDR, data);
    return true;
}
