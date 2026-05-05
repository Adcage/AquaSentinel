#pragma once

namespace ptz_protocol {

constexpr const char* CMD_HOME = "HOME";
constexpr const char* CMD_STATUS = "STATUS?";
constexpr const char* CMD_RESET_CALIB = "RESET_CALIB";
constexpr const char* CMD_NUDGE = "NUDGE:";
constexpr const char* CMD_MOVE = "MOVE:";
constexpr const char* CMD_CALIB_START = "CALIB:START";
constexpr const char* CMD_CALIB_SAVE = "CALIB:SAVE";
constexpr const char* CMD_CALIB_EXIT = "CALIB:EXIT";
constexpr const char* CMD_CALIB_DATA = "CALIB:DATA?";
constexpr const char* CMD_CALIB_SET = "CALIB:SET,";
constexpr const char* CMD_CALIB_PAN = "CALIB:PAN,";
constexpr const char* CMD_CALIB_TILT = "CALIB:TILT,";

constexpr const char* RESP_ACK = "ACK:";
constexpr const char* RESP_STATUS = "STATUS:";
constexpr const char* RESP_ERR = "ERR:";
constexpr const char* RESP_CALIB_OK = "CALIB:OK,";
constexpr const char* RESP_CALIB_DATA = "CALIB:DATA,";

constexpr const char* ERR_BAD_CMD = "BAD_CMD";
constexpr const char* ERR_BAD_ARG = "BAD_ARG";
constexpr const char* ERR_LIMIT = "LIMIT";

}  // namespace ptz_protocol
