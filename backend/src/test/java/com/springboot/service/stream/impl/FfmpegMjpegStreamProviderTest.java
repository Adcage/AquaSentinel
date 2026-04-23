package com.springboot.service.stream.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.springboot.config.AppStreamProxyProperties;

import org.junit.jupiter.api.Test;

class FfmpegMjpegStreamProviderTest {

    @Test
    void supportsShouldAcceptHttpFlvAndFlvProtocol() {
        FfmpegMjpegStreamProvider provider =
                new FfmpegMjpegStreamProvider(new AppStreamProxyProperties());

        assertTrue(provider.supports("HTTP-FLV"));
        assertTrue(provider.supports("FLV"));
    }
}
