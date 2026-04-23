package com.springboot.model.vo;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class CameraDeviceVO implements Serializable {

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

    private Date lastHeartbeatAt;

    private Date createdAt;

    private Date updatedAt;
}
