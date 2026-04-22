package com.springboot.model.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
public class CameraMaintenanceLogVO implements Serializable {

    private Long id;

    private Long cameraId;

    private String maintenanceType;

    private String maintenanceContent;

    private String maintainedBy;

    private Date maintainedAt;

    private Date nextMaintenanceAt;
}
