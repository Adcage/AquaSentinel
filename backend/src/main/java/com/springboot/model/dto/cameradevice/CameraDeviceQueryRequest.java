package com.springboot.model.dto.cameradevice;

import com.springboot.common.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CameraDeviceQueryRequest extends PageRequest {

    private Long id;

    private Long venueId;

    private Long zoneId;

    private String cameraCode;

    private String cameraName;

    private String streamUrl;

    private String protocol;

    private String deviceStatus;

    private String healthStatus;

    private Integer enabled;
}
