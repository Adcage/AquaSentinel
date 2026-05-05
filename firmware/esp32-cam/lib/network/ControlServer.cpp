#include "ControlServer.h"

#include "protocol.h"

namespace {

struct ServoReport {
    bool ok;
    int pan;
    int tilt;
    int value3;
    int panMinUs;
    int panMaxUs;
    int panCenterUs;
    int tiltMinUs;
    int tiltMaxUs;
    int tiltCenterUs;
    String raw;
};

constexpr int PAN_CALIBRATION_SAFE_MAX_US = 2350;

String buildDeviceMessage(const char* command, const String& response) {
    if (response == "ERR:LIMIT") {
        if (String(command) == "CALIB_PAN") {
            return "PAN 校准脉宽不能超过 2350us，请先回到安全位置后重试";
        }
        if (String(command) == "CALIB_SET") {
            return "PAN 校准值不能超过 2350us，请先回到安全位置后重试";
        }
        return "设备已触发安全限制，请回到安全位置后重试";
    }

    if (response == "ERR:BAD_CMD") {
        if (String(command) == "CALIB_EXIT") {
            return "设备未成功退出校准模式，请确认固件已更新并重试";
        }
        return "设备未识别当前控制命令，请确认固件版本是否一致";
    }

    if (response == "ERR:BAD_ARG") {
        return "设备参数不合法，请检查角度或脉宽范围后重试";
    }

    return "";
}

ServoReport parseServoReport(const String& response) {
    ServoReport report{false, -1, -1, -1, -1, -1, -1, -1, -1, -1, response};
    if (response.length() < 5) {
        return report;
    }

    if (response.startsWith("CALIB:OK,")) {
        String payload = response.substring(9);
        int c1 = payload.indexOf(',');
        if (c1 < 0) {
            return report;
        }
        String panText = payload.substring(0, c1);
        String tiltText = payload.substring(c1 + 1);
        panText.trim();
        tiltText.trim();
        report.pan = panText.toInt();
        report.tilt = tiltText.toInt();
        report.ok = report.pan >= 0 && report.tilt >= 0;
        return report;
    }

    if (response.startsWith("CALIB:DATA,")) {
        int values[6] = {-1, -1, -1, -1, -1, -1};
        String remain = response.substring(11);
        for (int i = 0; i < 6; i++) {
            int comma = remain.indexOf(',');
            String part = comma >= 0 ? remain.substring(0, comma) : remain;
            part.trim();
            values[i] = part.toInt();
            if (comma < 0) {
                break;
            }
            remain = remain.substring(comma + 1);
        }
        report.panMinUs = values[0];
        report.panMaxUs = values[1];
        report.panCenterUs = values[2];
        report.tiltMinUs = values[3];
        report.tiltMaxUs = values[4];
        report.tiltCenterUs = values[5];
        report.ok = report.panMinUs >= 0 && report.panMaxUs >= 0 && report.panCenterUs >= 0 &&
                    report.tiltMinUs >= 0 && report.tiltMaxUs >= 0 && report.tiltCenterUs >= 0;
        return report;
    }

    int colon = response.indexOf(':');
    if (colon < 0 || colon + 1 >= response.length()) {
        return report;
    }

    String payload = response.substring(colon + 1);
    payload.trim();
    int c1 = payload.indexOf(',');
    if (c1 < 0) {
        return report;
    }
    int c2 = payload.indexOf(',', c1 + 1);
    if (c2 < 0) {
        c2 = payload.length();
    }

    String panText = payload.substring(0, c1);
    String tiltText = payload.substring(c1 + 1, c2);
    String v3Text = c2 < payload.length() ? payload.substring(c2 + 1) : "";

    panText.trim();
    tiltText.trim();
    v3Text.trim();

    report.pan = panText.toInt();
    report.tilt = tiltText.toInt();
    report.value3 = v3Text.length() > 0 ? v3Text.toInt() : -1;
    report.ok = report.pan >= 0 && report.tilt >= 0;
    return report;
}

String toControlJson(const char* command, const String& response) {
    ServoReport report = parseServoReport(response);
    String message = buildDeviceMessage(command, response);
    String json = "{";
    json += "\"ok\":";
    json += report.ok ? "true" : "false";
    json += ",\"command\":\"";
    json += command;
    json += "\",\"raw\":\"";
    json += response;
    json += "\",\"message\":\"";
    json += message;
    json += "\",\"pan\":";
    json += String(report.pan);
    json += ",\"tilt\":";
    json += String(report.tilt);
    json += ",\"value3\":";
    json += String(report.value3);
    json += ",\"panMinUs\":";
    json += String(report.panMinUs);
    json += ",\"panMaxUs\":";
    json += String(report.panMaxUs);
    json += ",\"panCenterUs\":";
    json += String(report.panCenterUs);
    json += ",\"tiltMinUs\":";
    json += String(report.tiltMinUs);
    json += ",\"tiltMaxUs\":";
    json += String(report.tiltMaxUs);
    json += ",\"tiltCenterUs\":";
    json += String(report.tiltCenterUs);
    json += "}";
    return json;
}

void sendJson(WebServer& server, const String& payload) {
    server.sendHeader("Access-Control-Allow-Origin", "*");
    server.sendHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    server.sendHeader("Access-Control-Allow-Headers", "Content-Type");
    server.send(200, "application/json", payload);
}

}  // namespace

ControlServer::ControlServer(CameraStreamer& cameraRef, UartBridge& bridgeRef)
    : camera(cameraRef), bridge(bridgeRef), server(80) {}

void ControlServer::begin(int port) {
    (void)port;
    // 禁用自动 CORS，改为手动控制每个路由的 CORS 头
    // server.enableCORS(true);
    setupRoutes();
    server.begin();
}

void ControlServer::loop() { server.handleClient(); }

void ControlServer::setupRoutes() {
    // 根路径返回简单提示
    server.on("/", HTTP_GET, [this]() {
        server.sendHeader("Access-Control-Allow-Origin", "*");
        server.send(200, "text/plain", "AquaSentinel PTZ Control Server (Video stream disabled)");
    });

    // 视频流已禁用
    server.on("/stream", HTTP_GET, [this]() {
        server.sendHeader("Access-Control-Allow-Origin", "*");
        server.send(503, "text/plain", "Video stream temporarily disabled");
    });

    // 简化版状态接口
    server.on("/status", HTTP_GET, [this]() {
        String status = "{";
        status += "\"camera\":\"DISABLED\",";
        status += "\"wifi\":\"" + String(WiFi.status() == WL_CONNECTED ? "connected" : "disconnected") + "\",";
        status += "\"ip\":\"" + WiFi.localIP().toString() + "\",";
        status += "\"uptime\":" + String(millis() / 1000);
        status += "}";
        server.sendHeader("Access-Control-Allow-Origin", "*");
        server.send(200, "application/json", status);
    });

    auto homeHandler = [this]() {
        const String response = bridge.sendHome();
        sendJson(server, toControlJson("HOME", response));
    };
    auto optionsHandler = [this]() {
        server.sendHeader("Access-Control-Allow-Origin", "*");
        server.sendHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        server.sendHeader("Access-Control-Allow-Headers", "Content-Type");
        server.send(204);
    };
    server.on("/api/ptz/home", HTTP_POST, homeHandler);
    server.on("/api/ptz/home", HTTP_GET, homeHandler);
    server.on("/api/ptz/home", HTTP_OPTIONS, optionsHandler);

    server.on("/api/ptz/status", HTTP_GET, [this]() {
        const String response = bridge.sendStatus();
        sendJson(server, toControlJson("STATUS", response));
    });
    server.on("/api/ptz/status", HTTP_OPTIONS, optionsHandler);

    auto nudgeHandler = [this]() {
        String dir = server.arg("dir");
        dir.trim();
        dir.toUpperCase();
        int step = server.arg("step").toInt();
        if (step <= 0) {
            step = 5;
        }
        const String response = bridge.sendNudge(dir, step);
        sendJson(server, toControlJson("NUDGE", response));
    };
    server.on("/api/ptz/nudge", HTTP_POST, nudgeHandler);
    server.on("/api/ptz/nudge", HTTP_GET, nudgeHandler);
    server.on("/api/ptz/nudge", HTTP_OPTIONS, optionsHandler);

    auto moveHandler = [this]() {
        int pan = server.arg("pan").toInt();
        int tilt = server.arg("tilt").toInt();
        if (pan < 0 || pan > 180 || tilt < 0 || tilt > 180) {
            sendJson(server, toControlJson("MOVE", "ERR:BAD_ARG"));
            return;
        }
        const String response = bridge.sendMove(pan, tilt);
        sendJson(server, toControlJson("MOVE", response));
    };
    server.on("/api/ptz/move", HTTP_POST, moveHandler);
    server.on("/api/ptz/move", HTTP_GET, moveHandler);
    server.on("/api/ptz/move", HTTP_OPTIONS, optionsHandler);

    auto calibStartHandler = [this]() {
        const String response = bridge.calibStart();
        sendJson(server, toControlJson("CALIB_START", response));
    };
    server.on("/api/ptz/calib/start", HTTP_POST, calibStartHandler);
    server.on("/api/ptz/calib/start", HTTP_GET, calibStartHandler);
    server.on("/api/ptz/calib/start", HTTP_OPTIONS, optionsHandler);

    auto calibDataHandler = [this]() {
        const String response = bridge.calibData();
        sendJson(server, toControlJson("CALIB_DATA", response));
    };
    server.on("/api/ptz/calib/data", HTTP_POST, calibDataHandler);
    server.on("/api/ptz/calib/data", HTTP_GET, calibDataHandler);
    server.on("/api/ptz/calib/data", HTTP_OPTIONS, optionsHandler);

    auto calibSaveHandler = [this]() {
        const String response = bridge.calibSave();
        sendJson(server, toControlJson("CALIB_SAVE", response));
    };
    server.on("/api/ptz/calib/save", HTTP_POST, calibSaveHandler);
    server.on("/api/ptz/calib/save", HTTP_GET, calibSaveHandler);
    server.on("/api/ptz/calib/save", HTTP_OPTIONS, optionsHandler);

    auto calibExitHandler = [this]() {
        const String response = bridge.calibExit();
        sendJson(server, toControlJson("CALIB_EXIT", response));
    };
    server.on("/api/ptz/calib/exit", HTTP_POST, calibExitHandler);
    server.on("/api/ptz/calib/exit", HTTP_GET, calibExitHandler);
    server.on("/api/ptz/calib/exit", HTTP_OPTIONS, optionsHandler);

    auto resetCalibHandler = [this]() {
        const String response = bridge.resetCalibration();
        sendJson(server, toControlJson("RESET_CALIB", response));
    };
    server.on("/api/ptz/calib/reset", HTTP_POST, resetCalibHandler);
    server.on("/api/ptz/calib/reset", HTTP_GET, resetCalibHandler);
    server.on("/api/ptz/calib/reset", HTTP_OPTIONS, optionsHandler);

    auto calibPanHandler = [this]() {
        int pulse = server.arg("pulse").toInt();
        if (pulse > PAN_CALIBRATION_SAFE_MAX_US) {
            sendJson(server, toControlJson("CALIB_PAN", "ERR:LIMIT"));
            return;
        }
        if (pulse < 500 || pulse > 2500) {
            sendJson(server, toControlJson("CALIB_PAN", "ERR:BAD_ARG"));
            return;
        }
        const String response = bridge.calibSetPan(pulse);
        sendJson(server, toControlJson("CALIB_PAN", response));
    };
    server.on("/api/ptz/calib/pan", HTTP_POST, calibPanHandler);
    server.on("/api/ptz/calib/pan", HTTP_GET, calibPanHandler);
    server.on("/api/ptz/calib/pan", HTTP_OPTIONS, optionsHandler);

    auto calibTiltHandler = [this]() {
        int pulse = server.arg("pulse").toInt();
        if (pulse < 500 || pulse > 2500) {
            sendJson(server, toControlJson("CALIB_TILT", "ERR:BAD_ARG"));
            return;
        }
        const String response = bridge.calibSetTilt(pulse);
        sendJson(server, toControlJson("CALIB_TILT", response));
    };
    server.on("/api/ptz/calib/tilt", HTTP_POST, calibTiltHandler);
    server.on("/api/ptz/calib/tilt", HTTP_GET, calibTiltHandler);
    server.on("/api/ptz/calib/tilt", HTTP_OPTIONS, optionsHandler);

    auto calibSetHandler = [this]() {
        String axis = server.arg("axis");
        String key = server.arg("key");
        int pulse = server.arg("pulse").toInt();
        axis.trim();
        key.trim();
        axis.toUpperCase();
        key.toUpperCase();
        if (axis == "PAN" && pulse > PAN_CALIBRATION_SAFE_MAX_US) {
            sendJson(server, toControlJson("CALIB_SET", "ERR:LIMIT"));
            return;
        }
        if (pulse < 500 || pulse > 2500) {
            sendJson(server, toControlJson("CALIB_SET", "ERR:BAD_ARG"));
            return;
        }
        const String response = bridge.calibSetValue(axis, key, pulse);
        sendJson(server, toControlJson("CALIB_SET", response));
    };
    server.on("/api/ptz/calib/set", HTTP_POST, calibSetHandler);
    server.on("/api/ptz/calib/set", HTTP_GET, calibSetHandler);
    server.on("/api/ptz/calib/set", HTTP_OPTIONS, optionsHandler);

    server.onNotFound([this]() {
        server.sendHeader("Access-Control-Allow-Origin", "*");
        server.send(404, "text/plain", "Not found");
    });
}
