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

ServoReport parseServoReport(const String& response) {
    ServoReport report{false, -1, -1, -1, -1, -1, -1, -1, -1, -1, response};
    if (response.length() < 5) {
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

    if (response.startsWith("CALIB:DATA:")) {
        int values[6] = {-1, -1, -1, -1, -1, -1};
        String remain = payload;
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
        report.ok = report.panMinUs >= 0 && report.tiltCenterUs >= 0;
        return report;
    }

    report.pan = panText.toInt();
    report.tilt = tiltText.toInt();
    report.value3 = v3Text.length() > 0 ? v3Text.toInt() : -1;
    report.ok = report.pan >= 0 && report.tilt >= 0;
    return report;
}

String toControlJson(const char* command, const String& response) {
    ServoReport report = parseServoReport(response);
    String json = "{";
    json += "\"ok\":";
    json += report.ok ? "true" : "false";
    json += ",\"command\":\"";
    json += command;
    json += "\",\"raw\":\"";
    json += response;
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
    server.enableCORS(true);
    setupRoutes();
    server.begin();
}

void ControlServer::loop() { server.handleClient(); }

void ControlServer::setupRoutes() {
    server.on("/", HTTP_GET, [this]() {
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>AquaSentinel</title></head>";
        html += "<body style='margin:0;background:#000;display:flex;justify-content:center;align-items:center;height:100vh'>";
        html += "<img src='/stream' style='max-width:100%;max-height:100%'>";
        html += "</body></html>";
        server.send(200, "text/html", html);
    });

    server.on("/stream", HTTP_GET, [this]() { camera.handleStream(server); });

    server.on("/status", HTTP_GET, [this]() {
        sendJson(server, camera.statusJson());
    });

    auto homeHandler = [this]() {
        const String response = bridge.sendHome();
        sendJson(server, toControlJson("HOME", response));
    };
    server.on("/api/ptz/home", HTTP_POST, homeHandler);
    server.on("/api/ptz/home", HTTP_GET, homeHandler);

    server.on("/api/ptz/status", HTTP_GET, [this]() {
        const String response = bridge.sendStatus();
        sendJson(server, toControlJson("STATUS", response));
    });

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

    auto calibStartHandler = [this]() {
        const String response = bridge.calibStart();
        sendJson(server, toControlJson("CALIB_START", response));
    };
    server.on("/api/ptz/calib/start", HTTP_POST, calibStartHandler);
    server.on("/api/ptz/calib/start", HTTP_GET, calibStartHandler);

    auto calibDataHandler = [this]() {
        const String response = bridge.calibData();
        sendJson(server, toControlJson("CALIB_DATA", response));
    };
    server.on("/api/ptz/calib/data", HTTP_POST, calibDataHandler);
    server.on("/api/ptz/calib/data", HTTP_GET, calibDataHandler);

    auto calibSaveHandler = [this]() {
        const String response = bridge.calibSave();
        sendJson(server, toControlJson("CALIB_SAVE", response));
    };
    server.on("/api/ptz/calib/save", HTTP_POST, calibSaveHandler);
    server.on("/api/ptz/calib/save", HTTP_GET, calibSaveHandler);

    auto calibExitHandler = [this]() {
        const String response = bridge.calibExit();
        sendJson(server, toControlJson("CALIB_EXIT", response));
    };
    server.on("/api/ptz/calib/exit", HTTP_POST, calibExitHandler);
    server.on("/api/ptz/calib/exit", HTTP_GET, calibExitHandler);

    auto calibPanHandler = [this]() {
        int pulse = server.arg("pulse").toInt();
        if (pulse < 500) {
            pulse = 500;
        }
        if (pulse > 2500) {
            pulse = 2500;
        }
        const String response = bridge.calibSetPan(pulse);
        sendJson(server, toControlJson("CALIB_PAN", response));
    };
    server.on("/api/ptz/calib/pan", HTTP_POST, calibPanHandler);
    server.on("/api/ptz/calib/pan", HTTP_GET, calibPanHandler);

    auto calibTiltHandler = [this]() {
        int pulse = server.arg("pulse").toInt();
        if (pulse < 500) {
            pulse = 500;
        }
        if (pulse > 2500) {
            pulse = 2500;
        }
        const String response = bridge.calibSetTilt(pulse);
        sendJson(server, toControlJson("CALIB_TILT", response));
    };
    server.on("/api/ptz/calib/tilt", HTTP_POST, calibTiltHandler);
    server.on("/api/ptz/calib/tilt", HTTP_GET, calibTiltHandler);

    server.onNotFound([this]() {
        if (server.method() == HTTP_OPTIONS) {
            server.sendHeader("Access-Control-Allow-Origin", "*");
            server.sendHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            server.sendHeader("Access-Control-Allow-Headers", "Content-Type");
            server.send(204);
            return;
        }
        server.send(404, "text/plain", "Not found");
    });
}
