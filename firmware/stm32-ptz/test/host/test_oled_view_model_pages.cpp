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
    state.batteryMv = 4012;
    state.batteryPercent = 78;
    state.batteryRaw = 2489;
    state.batteryValid = true;
    OledFrame batteryFrame = OledViewModel::build(state);
    assert(std::string(batteryFrame.title) == "电量");
    assert(std::string(batteryFrame.lines[0]) == "BAT 4.01V");
    assert(std::string(batteryFrame.lines[1]) == "PCT 78%");
    assert(std::string(batteryFrame.lines[2]) == "RAW 2489");

    state.showActionMessage = true;
    std::strncpy(state.actionMessage, "正在回中", OLED_TEXT_BUFFER_SIZE - 1);
    state.actionMessage[OLED_TEXT_BUFFER_SIZE - 1] = '\0';
    OledFrame actionFrame = OledViewModel::build(state);
    assert(std::string(actionFrame.title) == "云台");
    assert(std::string(actionFrame.lines[0]) == "正在回中");

    {
        OledUiState low{};
        low.page = OledPage::Battery;
        low.batteryMv = 3300;
        low.batteryPercent = 0;
        low.batteryRaw = 2048;
        low.batteryValid = true;
        low.uptimeMs = 3000;
        OledFrame lowFrame = OledViewModel::build(low);
        assert(std::string(lowFrame.lines[1]) == "PCT 0%");
    }

    {
        OledUiState invalid{};
        invalid.page = OledPage::Battery;
        invalid.batteryValid = false;
        invalid.uptimeMs = 3000;
        OledFrame invFrame = OledViewModel::build(invalid);
        assert(std::string(invFrame.lines[0]) == "BAT --.--V");
        assert(std::string(invFrame.lines[1]) == "PCT --%");
        assert(std::string(invFrame.lines[2]) == "RAW ----");
    }

    {
        OledUiState netState{};
        netState.page = OledPage::Network;
        netState.uptimeMs = 3000;
        OledFrame netFrame = OledViewModel::build(netState);
        assert(std::string(netFrame.title) == "网络");
        assert(std::string(netFrame.lines[0]) == "NO IP");
    }

    {
        OledUiState netState{};
        netState.page = OledPage::Network;
        std::strncpy(netState.espIp, "192.168.1.100", 15);
        netState.espIp[15] = '\0';
        netState.uptimeMs = 3000;
        OledFrame netFrame = OledViewModel::build(netState);
        assert(std::string(netFrame.title) == "网络");
        assert(std::string(netFrame.lines[0]) == "IP 192.168.1.100");
    }

    return 0;
}
