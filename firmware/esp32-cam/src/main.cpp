#include <Arduino.h>
#include "esp_camera.h"
#include <WiFi.h>
#include <WebServer.h>

// ESP32-CAM AI-Thinker 引脚定义
#define PWDN_GPIO_NUM     32
#define RESET_GPIO_NUM    -1
#define XCLK_GPIO_NUM      0
#define SIOD_GPIO_NUM     26
#define SIOC_GPIO_NUM     27

#define Y9_GPIO_NUM       35
#define Y8_GPIO_NUM       34
#define Y7_GPIO_NUM       39
#define Y6_GPIO_NUM       36
#define Y5_GPIO_NUM       21
#define Y4_GPIO_NUM       19
#define Y3_GPIO_NUM       18
#define Y2_GPIO_NUM        5
#define VSYNC_GPIO_NUM    25
#define HREF_GPIO_NUM     23
#define PCLK_GPIO_NUM     22

// WiFi 配置 - 请根据实际环境修改
const char* WIFI_SSID = "ADCAGE";
const char* WIFI_PASSWORD = "12345678";

// MJPEG 流配置
#define FRAME_SIZE FRAMESIZE_SVGA  // 800x600
#define JPEG_QUALITY 4             // 10-63, 越小质量越高 (4=高质量)
#define FRAME_INTERVAL_MS 33      // 约30fps

// 画面方向配置
#define FLIP_VERTICAL  1           // 上下翻转
#define FLIP_HORIZONTAL 1          // 左右翻转

WebServer server(80);

bool cameraInitialized = false;
unsigned long lastFrameTime = 0;

void initCamera() {
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
    config.frame_size = FRAME_SIZE;
    config.pixel_format = PIXFORMAT_JPEG;
    config.grab_mode = CAMERA_GRAB_LATEST;
    config.fb_location = CAMERA_FB_IN_PSRAM;
    config.jpeg_quality = JPEG_QUALITY;
    config.fb_count = 2;

    esp_err_t err = esp_camera_init(&config);
    if (err != ESP_OK) {
        Serial.printf("摄像头初始化失败: 0x%x\n", err);
        cameraInitialized = false;
        return;
    }
    
    cameraInitialized = true;
    Serial.println("摄像头初始化成功");
    
    sensor_t* s = esp_camera_sensor_get();
    if (s) {
        // 画面方向设置
        s->set_vflip(s, FLIP_VERTICAL);     // 上下翻转
        s->set_hmirror(s, FLIP_HORIZONTAL); // 左右镜像
        
        // 图像质量优化
        s->set_brightness(s, 0);      // 亮度 (-2 to 2)
        s->set_contrast(s, 1);        // 对比度 (-2 to 2)，略微提高
        s->set_saturation(s, 0);      // 饱和度 (-2 to 2)
        s->set_whitebal(s, 1);        // 自动白平衡
        s->set_awb_gain(s, 1);        // AWB增益
        s->set_exposure_ctrl(s, 1);   // 自动曝光
        s->set_aec2(s, 1);            // 自动曝光算法2
        s->set_gain_ctrl(s, 1);       // 自动增益
        s->set_agc_gain(s, 0);        // AGC增益值
        s->set_gainceiling(s, (gainceiling_t)GAINCEILING_16X); // 增益上限提高
        s->set_aec_value(s, 300);     // 曝光值
        s->set_framesize(s, FRAME_SIZE);
        s->set_quality(s, JPEG_QUALITY);
        
        Serial.println("摄像头参数配置完成");
        Serial.printf("方向: VFlip=%d, HMirror=%d\n", FLIP_VERTICAL, FLIP_HORIZONTAL);
    }
}

void connectWiFi() {
    Serial.println("正在连接WiFi...");
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    
    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 20) {
        delay(500);
        Serial.print(".");
        attempts++;
    }
    
    if (WiFi.status() == WL_CONNECTED) {
        Serial.println("\nWiFi连接成功");
        Serial.print("IP地址: ");
        Serial.println(WiFi.localIP());
    } else {
        Serial.println("\nWiFi连接失败");
    }
}

void handleStream() {
    if (!cameraInitialized) {
        server.send(500, "text/plain", "摄像头未初始化");
        return;
    }

    server.setContentLength(CONTENT_LENGTH_UNKNOWN);
    server.send(200, "multipart/x-mixed-replace; boundary=frame", "");

    while (server.client().connected()) {
        camera_fb_t* fb = esp_camera_fb_get();
        if (!fb) {
            Serial.println("获取帧失败");
            break;
        }

        server.sendContent("--frame\r\n");
        server.sendContent("Content-Type: image/jpeg\r\n");
        server.sendContent("Content-Length: " + String(fb->len) + "\r\n\r\n");
        server.sendContent((const char*)fb->buf, fb->len);
        server.sendContent("\r\n");
        
        esp_camera_fb_return(fb);
        
        unsigned long now = millis();
        int delayMs = FRAME_INTERVAL_MS - (now - lastFrameTime);
        if (delayMs > 0) {
            delay(delayMs);
        }
        lastFrameTime = millis();
    }
    Serial.println("客户端断开连接");
}

void handleRoot() {
    String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>AquaSentinel 视频流</title></head>";
    html += "<body style='margin:0;padding:0;background:#000;display:flex;justify-content:center;align-items:center;height:100vh'>";
    html += "<img src='/stream' style='max-width:100%;max-height:100%'>";
    html += "</body></html>";
    server.send(200, "text/html", html);
}

void handleStatus() {
    String status = "{\n";
    status += "  \"camera\": " + String(cameraInitialized ? "\"OK\"" : "\"FAIL\"") + ",\n";
    status += "  \"wifi\": \"" + String(WiFi.status() == WL_CONNECTED ? "connected" : "disconnected") + "\",\n";
    status += "  \"ip\": \"" + WiFi.localIP().toString() + "\",\n";
    status += "  \"uptime\": " + String(millis() / 1000) + "\n";
    status += "}";
    server.send(200, "application/json", status);
}

void setup() {
    Serial.begin(115200);
    delay(1000);
    
    Serial.println("\n=== AquaSentinel ESP32-CAM 视频流测试 ===");
    
    initCamera();
    connectWiFi();
    
    if (WiFi.status() == WL_CONNECTED) {
        server.on("/", handleRoot);
        server.on("/stream", handleStream);
        server.on("/status", handleStatus);
        server.begin();
        Serial.println("Web服务器已启动");
        Serial.println("访问 http://" + WiFi.localIP().toString() + " 查看视频流");
    }
}

void loop() {
    if (WiFi.status() == WL_CONNECTED) {
        server.handleClient();
    }
    delay(1);
}