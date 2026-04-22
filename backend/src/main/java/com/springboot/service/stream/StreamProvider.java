package com.springboot.service.stream;

import com.springboot.model.entity.CameraDevice;

public interface StreamProvider {

    String name();

    boolean supports(String sourceProtocol);

    StreamSession open(CameraDevice cameraDevice, StreamOpenRequest request);
}
