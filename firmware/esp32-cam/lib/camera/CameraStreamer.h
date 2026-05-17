#pragma once

#include <Arduino.h>
#include <ESPAsyncWebServer.h>
#include "esp_camera.h"

class CameraStreamer {
   public:
    bool begin();
    bool tryStartStream();
    void stopStream();
    size_t fillStreamChunk(uint8_t* buffer, size_t maxLen);
    String statusJson() const;
    bool initialized() const;
    bool isStreaming() const;
    camera_fb_t* captureFrame();
    void releaseFrame(camera_fb_t* fb);

   private:
    bool cameraInitialized = false;
    unsigned long lastFrameTime = 0;
    bool streamActive = false;
    unsigned long streamStartTime = 0;
    camera_fb_t* currentFrame = nullptr;
    size_t currentFrameOffset = 0;
    size_t currentFrameHeaderOffset = 0;
    size_t currentFrameFooterOffset = 0;

    static constexpr int MJPEG_HEADER_BUF_SIZE = 96;
    char mjpegHeaderBuf[MJPEG_HEADER_BUF_SIZE];

    void releaseCurrentFrame();
};
