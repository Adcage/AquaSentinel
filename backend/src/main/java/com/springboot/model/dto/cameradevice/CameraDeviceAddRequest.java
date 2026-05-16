package com.springboot.model.dto.cameradevice;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class CameraDeviceAddRequest implements Serializable {

    private Long venueId;

    private Long zoneId;

    private String cameraCode;

    private String cameraName;

    private String streamUrl;

    private String protocol;

    private String deviceStatus;

    private String healthStatus;

    private Integer enabled;

    private Integer rotation;

    private Date lastHeartbeatAt;
}
