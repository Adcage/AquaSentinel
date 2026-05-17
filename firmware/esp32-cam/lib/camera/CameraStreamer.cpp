#include "CameraStreamer.h"

#include <WiFi.h>

#include "config.h"

namespace {

constexpr int PWDN_GPIO_NUM = 32;
constexpr int RESET_GPIO_NUM = -1;
constexpr int XCLK_GPIO_NUM = 0;
constexpr int SIOD_GPIO_NUM = 26;
constexpr int SIOC_GPIO_NUM = 27;

constexpr int Y9_GPIO_NUM = 35;
constexpr int Y8_GPIO_NUM = 34;
constexpr int Y7_GPIO_NUM = 39;
constexpr int Y6_GPIO_NUM = 36;
constexpr int Y5_GPIO_NUM = 21;
constexpr int Y4_GPIO_NUM = 19;
constexpr int Y3_GPIO_NUM = 18;
constexpr int Y2_GPIO_NUM = 5;
constexpr int VSYNC_GPIO_NUM = 25;
constexpr int HREF_GPIO_NUM = 23;
constexpr int PCLK_GPIO_NUM = 22;

}  // namespace

bool CameraStreamer::begin() {
    camera_config_t config;
    config.ledc_channel = LEDC_CHANNEL_0;
    config.ledc_timer = LEDC_TIMER_0;
    config.pin_d0 = Y2_GPIO_NUM;
    config.pin_d1 = Y3_GPIO_NUM;
    config.pin_d2 = Y4_GPIO_NUM;
    config.pin_d3 = Y5_GPIO_NUM;
    config.pin_d4 = Y6_GPIO_NUM;
    config.pin_d5 = Y7_GPIO_NUM;
    config.pin_d6 = Y8_GPIO_NUM;
    config.pin_d7 = Y9_GPIO_NUM;
    config.pin_xclk = XCLK_GPIO_NUM;
    config.pin_pclk = PCLK_GPIO_NUM;
    config.pin_vsync = VSYNC_GPIO_NUM;
    config.pin_href = HREF_GPIO_NUM;
    config.pin_sccb_sda = SIOD_GPIO_NUM;
    config.pin_sccb_scl = SIOC_GPIO_NUM;
    config.pin_pwdn = PWDN_GPIO_NUM;
    config.pin_reset = RESET_GPIO_NUM;
    config.xclk_freq_hz = 20000000;
    config.frame_size = cam_config::FRAME_SIZE;
    config.pixel_format = PIXFORMAT_JPEG;
    config.grab_mode = CAMERA_GRAB_LATEST;
    config.fb_location = CAMERA_FB_IN_PSRAM;
    config.jpeg_quality = cam_config::JPEG_QUALITY;
    config.fb_count = 2;

    const esp_err_t err = esp_camera_init(&config);
    if (err != ESP_OK) {
        Serial.printf("摄像头初始化失败: 0x%x\n", err);
        cameraInitialized = false;
        return false;
    }

    sensor_t* sensor = esp_camera_sensor_get();
    if (sensor != nullptr) {
        sensor->set_vflip(sensor, cam_config::FLIP_VERTICAL ? 1 : 0);
        sensor->set_hmirror(sensor, cam_config::FLIP_HORIZONTAL ? 1 : 0);
        sensor->set_contrast(sensor, 1);
        sensor->set_framesize(sensor, cam_config::FRAME_SIZE);
        sensor->set_quality(sensor, cam_config::JPEG_QUALITY);
    }

    cameraInitialized = true;
    Serial.println("摄像头初始化成功");
    return true;
}

bool CameraStreamer::tryStartStream() {
    if (streamActive) {
        return false;
    }

    streamActive = true;
    streamStartTime = millis();
    currentFrame = nullptr;
    currentFrameOffset = 0;
    currentFrameHeaderOffset = 0;
    currentFrameFooterOffset = 0;

    Serial.println("流客户端已连接，开始推流");
    return true;
}

void CameraStreamer::releaseCurrentFrame() {
    if (currentFrame != nullptr) {
        esp_camera_fb_return(currentFrame);
        currentFrame = nullptr;
    }
}

void CameraStreamer::stopStream() {
    releaseCurrentFrame();
    streamActive = false;
    currentFrameOffset = 0;
    currentFrameHeaderOffset = 0;
    currentFrameFooterOffset = 0;
}

size_t CameraStreamer::fillStreamChunk(uint8_t* buffer, size_t maxLen) {
    if (!streamActive || !cameraInitialized) {
        return 0;
    }

    if (millis() - streamStartTime > cam_config::STREAM_CLIENT_TIMEOUT_MS) {
        Serial.println("流客户端超时，停止推流");
        stopStream();
        return 0;
    }

    if (maxLen < 128) {
        return RESPONSE_TRY_AGAIN;
    }

    if (currentFrame == nullptr) {
        const unsigned long now = millis();
        if (now - lastFrameTime < static_cast<unsigned long>(cam_config::FRAME_INTERVAL_MS)) {
            return RESPONSE_TRY_AGAIN;
        }

        currentFrame = esp_camera_fb_get();
        if (currentFrame == nullptr) {
            Serial.println("摄像头捕获帧失败");
            return RESPONSE_TRY_AGAIN;
        }

        currentFrameOffset = 0;
        currentFrameHeaderOffset = 0;
        currentFrameFooterOffset = 0;

        snprintf(
            mjpegHeaderBuf, MJPEG_HEADER_BUF_SIZE,
            "--frame\r\nContent-Type: image/jpeg\r\nContent-Length: %u\r\n\r\n",
            static_cast<unsigned int>(currentFrame->len));
        lastFrameTime = now;
    }

    size_t written = 0;
    const size_t headerLen = strlen(mjpegHeaderBuf);
    static const char frameFooter[] = "\r\n";

    while (written < maxLen) {
        if (currentFrameHeaderOffset < headerLen) {
            const size_t remaining = headerLen - currentFrameHeaderOffset;
            const size_t copyLen = std::min(remaining, maxLen - written);
            memcpy(buffer + written, mjpegHeaderBuf + currentFrameHeaderOffset, copyLen);
            currentFrameHeaderOffset += copyLen;
            written += copyLen;
            continue;
        }

        if (currentFrameOffset < currentFrame->len) {
            const size_t remaining = currentFrame->len - currentFrameOffset;
            const size_t copyLen = std::min(remaining, maxLen - written);
            memcpy(buffer + written, currentFrame->buf + currentFrameOffset, copyLen);
            currentFrameOffset += copyLen;
            written += copyLen;
            continue;
        }

        if (currentFrameFooterOffset < sizeof(frameFooter) - 1) {
            const size_t remaining = (sizeof(frameFooter) - 1) - currentFrameFooterOffset;
            const size_t copyLen = std::min(remaining, maxLen - written);
            memcpy(buffer + written, frameFooter + currentFrameFooterOffset, copyLen);
            currentFrameFooterOffset += copyLen;
            written += copyLen;
            continue;
        }

        releaseCurrentFrame();
        currentFrameOffset = 0;
        currentFrameHeaderOffset = 0;
        currentFrameFooterOffset = 0;
        break;
    }

    return written;
}

bool CameraStreamer::isStreaming() const {
    return streamActive;
}

String CameraStreamer::statusJson() const {
    String status = "{";
    status += "\"camera\":\"";
    status += cameraInitialized ? "OK" : "FAIL";
    status += "\",";
    status += "\"streaming\":";
    status += isStreaming() ? "true" : "false";
    status += ",";
    status += "\"wifi\":\"";
    status += WiFi.status() == WL_CONNECTED ? "connected" : "disconnected";
    status += "\",";
    status += "\"ip\":\"";
    status += WiFi.localIP().toString();
    status += "\",";
    status += "\"uptime\":";
    status += String(millis() / 1000);
    status += "}";
    return status;
}

bool CameraStreamer::initialized() const { return cameraInitialized; }

camera_fb_t* CameraStreamer::captureFrame() {
    if (!cameraInitialized) return nullptr;
    return esp_camera_fb_get();
}

void CameraStreamer::releaseFrame(camera_fb_t* fb) {
    if (fb != nullptr) {
        esp_camera_fb_return(fb);
    }
}
