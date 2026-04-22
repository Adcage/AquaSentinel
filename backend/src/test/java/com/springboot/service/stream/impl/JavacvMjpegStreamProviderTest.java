package com.springboot.service.stream.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JavacvMjpegStreamProviderTest {

    @Test
    void supportsShouldAcceptHttpFlvAndFlvProtocol() {
        JavacvMjpegStreamProvider provider = new JavacvMjpegStreamProvider();

        assertTrue(provider.supports("HTTP-FLV"));
        assertTrue(provider.supports("FLV"));
    }
}
