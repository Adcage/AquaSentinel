#include "ControlServer.h"

#include "protocol.h"

namespace {

constexpr int PAN_CALIBRATION_SAFE_MAX_US = 2350;

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

void sendJson(AsyncWebServerRequest* request, const String& payload) {
    AsyncWebServerResponse* response = request->beginResponse(200, "application/json", payload);
    response->addHeader("Access-Control-Allow-Origin", "*");
    response->addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    response->addHeader("Access-Control-Allow-Headers", "Content-Type");
    request->send(response);
}

void sendStreamBusy(AsyncWebServerRequest* request) {
    AsyncWebServerResponse* response = request->beginResponse(
        503,
        "application/json; charset=utf-8",
        "{\"code\":\"STREAM_BUSY\",\"message\":\"流已被占用，仅支持单路拉流\"}");
    response->addHeader("Access-Control-Allow-Origin", "*");
    response->addHeader("Retry-After", "5");
    request->send(response);
}

void sendCORSOptions(AsyncWebServerRequest* request) {
    AsyncWebServerResponse* response = request->beginResponse(204);
    response->addHeader("Access-Control-Allow-Origin", "*");
    response->addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    response->addHeader("Access-Control-Allow-Headers", "Content-Type");
    request->send(response);
}

}  // namespace

ControlServer::ControlServer(CameraStreamer& cameraRef, UartBridge& bridgeRef)
    : camera(cameraRef), bridge(bridgeRef), server(80) {}

void ControlServer::begin(int port) {
    (void)port;
    setupRoutes();
    server.begin();
}

void ControlServer::loop() {}

void ControlServer::setupRoutes() {
    server.on("/", HTTP_GET, [](AsyncWebServerRequest* request) {
        AsyncWebServerResponse* response = request->beginResponse(200, "text/plain", "AquaSentinel PTZ Control Server");
        response->addHeader("Access-Control-Allow-Origin", "*");
        request->send(response);
    });

    server.on("/stream", HTTP_GET, [this](AsyncWebServerRequest* request) {
        if (!camera.initialized()) {
            AsyncWebServerResponse* response = request->beginResponse(500, "text/plain", "摄像头未初始化");
            response->addHeader("Access-Control-Allow-Origin", "*");
            request->send(response);
            return;
        }

        if (camera.isStreaming()) {
            sendStreamBusy(request);
            Serial.println("拒绝流连接：已有客户端在拉流");
            return;
        }

        if (!camera.tryStartStream()) {
            sendStreamBusy(request);
            return;
        }

        request->onDisconnect([this]() {
            camera.stopStream();
            Serial.println("流客户端断开连接，释放流槽位");
        });

        AsyncWebServerResponse* response = request->beginChunkedResponse(
            "multipart/x-mixed-replace; boundary=frame",
            [this](uint8_t* buffer, size_t maxLen, size_t index) -> size_t {
                (void)index;
                return camera.fillStreamChunk(buffer, maxLen);
            });
        response->addHeader("Access-Control-Allow-Origin", "*");
        response->addHeader("Connection", "close");
        response->addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        request->send(response);
    });

    server.on("/stream", HTTP_OPTIONS, [](AsyncWebServerRequest* request) {
        AsyncWebServerResponse* response = request->beginResponse(204);
        response->addHeader("Access-Control-Allow-Origin", "*");
        response->addHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        response->addHeader("Access-Control-Allow-Headers", "Content-Type");
        request->send(response);
    });

    server.on("/status", HTTP_GET, [this](AsyncWebServerRequest* request) {
        sendJson(request, camera.statusJson());
    });

    server.on("/api/ptz/home", HTTP_POST, [this](AsyncWebServerRequest* request) {
        const String response = bridge.sendHome();
        sendJson(request, toControlJson("HOME", response));
    });
    server.on("/api/ptz/home", HTTP_GET, [this](AsyncWebServerRequest* request) {
        const String response = bridge.sendHome();
        sendJson(request, toControlJson("HOME", response));
    });
    server.on("/api/ptz/home", HTTP_OPTIONS, [](AsyncWebServerRequest* request) {
        sendCORSOptions(request);
    });

    server.on("/api/ptz/status", HTTP_GET, [this](AsyncWebServerRequest* request) {
        const String response = bridge.sendStatus();
        sendJson(request, toControlJson("STATUS", response));
    });
    server.on("/api/ptz/status", HTTP_OPTIONS, [](AsyncWebServerRequest* request) {
        sendCORSOptions(request);
    });

    server.on("/api/ptz/nudge", HTTP_POST, [this](AsyncWebServerRequest* request) {
        String dir = request->arg("dir");
        dir.trim();
        dir.toUpperCase();
        int step = request->arg("step").toInt();
        if (step <= 0) {
            step = 5;
        }
        const String response = bridge.sendNudge(dir, step);
        sendJson(request, toControlJson("NUDGE", response));
    });
    server.on("/api/ptz/nudge", HTTP_GET, [this](AsyncWebServerRequest* request) {
        String dir = request->arg("dir");
        dir.trim();
        dir.toUpperCase();
        int step = request->arg("step").toInt();
        if (step <= 0) {
            step = 5;
        }
        const String response = bridge.sendNudge(dir, step);
        sendJson(request, toControlJson("NUDGE", response));
    });
    server.on("/api/ptz/nudge", HTTP_OPTIONS, [](AsyncWebServerRequest* request) {
        sendCORSOptions(request);
    });

    server.on("/api/ptz/move", HTTP_POST, [this](AsyncWebServerRequest* request) {
        int pan = request->arg("pan").toInt();
        int tilt = request->arg("tilt").toInt();
        if (pan < 0 || pan > 180 || tilt < 0 || tilt > 180) {
            sendJson(request, toControlJson("MOVE", "ERR:BAD_ARG"));
            return;
        }
        const String response = bridge.sendMove(pan, tilt);
        sendJson(request, toControlJson("MOVE", response));
    });
    server.on("/api/ptz/move", HTTP_GET, [this](AsyncWebServerRequest* request) {
        int pan = request->arg("pan").toInt();
        int tilt = request->arg("tilt").toInt();
        if (pan < 0 || pan > 180 || tilt < 0 || tilt > 180) {
            sendJson(request, toControlJson("MOVE", "ERR:BAD_ARG"));
            return;
        }
        const String response = bridge.sendMove(pan, tilt);
        sendJson(request, toControlJson("MOVE", response));
    });
    server.on("/api/ptz/move", HTTP_OPTIONS, [](AsyncWebServerRequest* request) {
        sendCORSOptions(request);
    });

    server.on("/api/ptz/calib/start", HTTP_POST, [this](AsyncWebServerRequest* request) {
        const String response = bridge.calibStart();
        sendJson(request, toControlJson("CALIB_START", response));
    });
    server.on("/api/ptz/calib/start", HTTP_GET, [this](AsyncWebServerRequest* request) {
        const String response = bridge.calibStart();
        sendJson(request, toControlJson("CALIB_START", response));
    });
    server.on("/api/ptz/calib/start", HTTP_OPTIONS, [](AsyncWebServerRequest* request) {
        sendCORSOptions(request);
    });

    server.on("/api/ptz/calib/data", HTTP_POST, [this](AsyncWebServerRequest* request) {
        const String response = bridge.calibData();
        sendJson(request, toControlJson("CALIB_DATA", response));
    });
    server.on("/api/ptz/calib/data", HTTP_GET, [this](AsyncWebServerRequest* request) {
        const String response = bridge.calibData();
        sendJson(request, toControlJson("CALIB_DATA", response));
    });
    server.on("/api/ptz/calib/data", HTTP_OPTIONS, [](AsyncWebServerRequest* request) {
        sendCORSOptions(request);
    });

    server.on("/api/ptz/calib/save", HTTP_POST, [this](AsyncWebServerRequest* request) {
        const String response = bridge.calibSave();
        sendJson(request, toControlJson("CALIB_SAVE", response));
    });
    server.on("/api/ptz/calib/save", HTTP_GET, [this](AsyncWebServerRequest* request) {
        const String response = bridge.calibSave();
        sendJson(request, toControlJson("CALIB_SAVE", response));
    });
    server.on("/api/ptz/calib/save", HTTP_OPTIONS, [](AsyncWebServerRequest* request) {
        sendCORSOptions(request);
    });

    server.on("/api/ptz/calib/exit", HTTP_POST, [this](AsyncWebServerRequest* request) {
        const String response = bridge.calibExit();
        sendJson(request, toControlJson("CALIB_EXIT", response));
    });
    server.on("/api/ptz/calib/exit", HTTP_GET, [this](AsyncWebServerRequest* request) {
        const String response = bridge.calibExit();
        sendJson(request, toControlJson("CALIB_EXIT", response));
    });
    server.on("/api/ptz/calib/exit", HTTP_OPTIONS, [](AsyncWebServerRequest* request) {
        sendCORSOptions(request);
    });

    server.on("/api/ptz/calib/reset", HTTP_POST, [this](AsyncWebServerRequest* request) {
        const String response = bridge.resetCalibration();
        sendJson(request, toControlJson("RESET_CALIB", response));
    });
    server.on("/api/ptz/calib/reset", HTTP_GET, [this](AsyncWebServerRequest* request) {
        const String response = bridge.resetCalibration();
        sendJson(request, toControlJson("RESET_CALIB", response));
    });
    server.on("/api/ptz/calib/reset", HTTP_OPTIONS, [](AsyncWebServerRequest* request) {
        sendCORSOptions(request);
    });

    server.on("/api/ptz/calib/pan", HTTP_POST, [this](AsyncWebServerRequest* request) {
        int pulse = request->arg("pulse").toInt();
        if (pulse > PAN_CALIBRATION_SAFE_MAX_US) {
            sendJson(request, toControlJson("CALIB_PAN", "ERR:LIMIT"));
            return;
        }
        if (pulse < 500 || pulse > 2500) {
            sendJson(request, toControlJson("CALIB_PAN", "ERR:BAD_ARG"));
            return;
        }
        const String response = bridge.calibSetPan(pulse);
        sendJson(request, toControlJson("CALIB_PAN", response));
    });
    server.on("/api/ptz/calib/pan", HTTP_GET, [this](AsyncWebServerRequest* request) {
        int pulse = request->arg("pulse").toInt();
        if (pulse > PAN_CALIBRATION_SAFE_MAX_US) {
            sendJson(request, toControlJson("CALIB_PAN", "ERR:LIMIT"));
            return;
        }
        if (pulse < 500 || pulse > 2500) {
            sendJson(request, toControlJson("CALIB_PAN", "ERR:BAD_ARG"));
            return;
        }
        const String response = bridge.calibSetPan(pulse);
        sendJson(request, toControlJson("CALIB_PAN", response));
    });
    server.on("/api/ptz/calib/pan", HTTP_OPTIONS, [](AsyncWebServerRequest* request) {
        sendCORSOptions(request);
    });

    server.on("/api/ptz/calib/tilt", HTTP_POST, [this](AsyncWebServerRequest* request) {
        int pulse = request->arg("pulse").toInt();
        if (pulse < 500 || pulse > 2500) {
            sendJson(request, toControlJson("CALIB_TILT", "ERR:BAD_ARG"));
            return;
        }
        const String response = bridge.calibSetTilt(pulse);
        sendJson(request, toControlJson("CALIB_TILT", response));
    });
    server.on("/api/ptz/calib/tilt", HTTP_GET, [this](AsyncWebServerRequest* request) {
        int pulse = request->arg("pulse").toInt();
        if (pulse < 500 || pulse > 2500) {
            sendJson(request, toControlJson("CALIB_TILT", "ERR:BAD_ARG"));
            return;
        }
        const String response = bridge.calibSetTilt(pulse);
        sendJson(request, toControlJson("CALIB_TILT", response));
    });
    server.on("/api/ptz/calib/tilt", HTTP_OPTIONS, [](AsyncWebServerRequest* request) {
        sendCORSOptions(request);
    });

    server.on("/api/ptz/calib/set", HTTP_POST, [this](AsyncWebServerRequest* request) {
        String axis = request->arg("axis");
        String key = request->arg("key");
        int pulse = request->arg("pulse").toInt();
        axis.trim();
        key.trim();
        axis.toUpperCase();
        key.toUpperCase();
        if (axis == "PAN" && pulse > PAN_CALIBRATION_SAFE_MAX_US) {
            sendJson(request, toControlJson("CALIB_SET", "ERR:LIMIT"));
            return;
        }
        if (pulse < 500 || pulse > 2500) {
            sendJson(request, toControlJson("CALIB_SET", "ERR:BAD_ARG"));
            return;
        }
        const String response = bridge.calibSetValue(axis, key, pulse);
        sendJson(request, toControlJson("CALIB_SET", response));
    });
    server.on("/api/ptz/calib/set", HTTP_GET, [this](AsyncWebServerRequest* request) {
        String axis = request->arg("axis");
        String key = request->arg("key");
        int pulse = request->arg("pulse").toInt();
        axis.trim();
        key.trim();
        axis.toUpperCase();
        key.toUpperCase();
        if (axis == "PAN" && pulse > PAN_CALIBRATION_SAFE_MAX_US) {
            sendJson(request, toControlJson("CALIB_SET", "ERR:LIMIT"));
            return;
        }
        if (pulse < 500 || pulse > 2500) {
            sendJson(request, toControlJson("CALIB_SET", "ERR:BAD_ARG"));
            return;
        }
        const String response = bridge.calibSetValue(axis, key, pulse);
        sendJson(request, toControlJson("CALIB_SET", response));
    });
    server.on("/api/ptz/calib/set", HTTP_OPTIONS, [](AsyncWebServerRequest* request) {
        sendCORSOptions(request);
    });

    server.onNotFound([](AsyncWebServerRequest* request) {
        AsyncWebServerResponse* response = request->beginResponse(404, "text/plain", "Not found");
        response->addHeader("Access-Control-Allow-Origin", "*");
        request->send(response);
    });
}
