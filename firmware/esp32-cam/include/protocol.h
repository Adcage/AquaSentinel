#pragma once

namespace bridge_protocol {

constexpr const char* CMD_HOME = "HOME\n";
constexpr const char* CMD_STATUS = "STATUS?\n";
constexpr const char* CMD_CALIB_START = "CALIB:START\n";
constexpr const char* CMD_CALIB_SAVE = "CALIB:SAVE\n";
constexpr const char* CMD_CALIB_EXIT = "CALIB:EXIT\n";
constexpr const char* CMD_CALIB_DATA = "CALIB:DATA?\n";
constexpr const char* CMD_RESET_CALIB = "RESET_CALIB\n";
constexpr const char* CMD_CALIB_SET = "CALIB:SET,";

// MOVE 命令需要通过 sendMove(pan, tilt) 方法动态构造，格式为 "MOVE:pan,tilt\n"

constexpr const char* CMD_IP_PREFIX = "IP:";

}  // namespace bridge_protocol
