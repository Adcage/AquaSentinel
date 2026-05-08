#include <cassert>
#include <cstring>
#include <string>

#include "OledViewModel.h"

int main() {
    OledUiState state{};
    state.page = OledPage::Status;
    state.pan = 90;
    state.tilt = 120;
    state.uptimeMs = 3000;

    OledFrame statusFrame = OledViewModel::build(state);
    assert(std::string(statusFrame.title) == "云台");
    assert(std::string(statusFrame.lines[0]) == "MODE NORM");

    state.page = OledPage::Battery;
    OledFrame batteryFrame = OledViewModel::build(state);
    assert(std::string(batteryFrame.title) == "电量");
    assert(std::string(batteryFrame.lines[0]) == "ADC WAIT");

    state.showActionMessage = true;
    std::strncpy(state.actionMessage, "回中中", OLED_TEXT_BUFFER_SIZE - 1);
    state.actionMessage[OLED_TEXT_BUFFER_SIZE - 1] = '\0';
    OledFrame actionFrame = OledViewModel::build(state);
    assert(std::string(actionFrame.title) == "云台");
    assert(std::string(actionFrame.lines[0]) == "回中中");

    return 0;
}
