package com.springboot.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import com.springboot.common.BaseResponse;
import com.springboot.config.AppVideoHubProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VideoHubProxyControllerWebrtcConfigTest {

    private VideoHubProxyController controller;

    private AppVideoHubProperties properties;

    @BeforeEach
    void setUp() {
        controller = new VideoHubProxyController();
        properties = new AppVideoHubProperties();
        ReflectionTestUtils.setField(controller, "appVideoHubProperties", properties);
    }

    @Test
    void getWebrtcConfigReturnsPreferredIpWhenSet() {
        properties.setPreferredIp("192.168.0.221");

        BaseResponse<Map<String, String>> result = controller.getWebrtcConfig();

        assertEquals(0, result.getCode());
        assertEquals("192.168.0.221", result.getData().get("preferredIp"));
    }

    @Test
    void getWebrtcConfigReturnsEmptyWhenNotSet() {
        properties.setPreferredIp("");

        BaseResponse<Map<String, String>> result = controller.getWebrtcConfig();

        assertEquals(0, result.getCode());
        assertEquals("", result.getData().get("preferredIp"));
    }
}
