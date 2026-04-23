package com.springboot.service.stream.impl;

import com.springboot.common.ErrorCode;
import com.springboot.exception.BusinessException;
import com.springboot.model.entity.CameraDevice;
import com.springboot.service.stream.StreamOpenRequest;
import com.springboot.service.stream.StreamProvider;
import com.springboot.service.stream.StreamSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class DirectRtspStreamProvider implements StreamProvider {

    @Override
    public String name() {
        return "rtsp_direct";
    }

    @Override
    public boolean supports(String sourceProtocol) {
        if (StringUtils.isBlank(sourceProtocol)) {
            return true;
        }
        String normalized = sourceProtocol.trim().toUpperCase();
        return "RTSP".equals(normalized) || "PTZ".equals(normalized);
    }

    @Override
    public StreamSession open(CameraDevice cameraDevice, StreamOpenRequest request) {
        String source = StringUtils.trimToEmpty(cameraDevice.getStream_url());
        if (StringUtils.isBlank(source)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "摄像头流地址为空");
        }
        return new StreamSession(name(), "application/octet-stream", source, null, null);
    }
}
