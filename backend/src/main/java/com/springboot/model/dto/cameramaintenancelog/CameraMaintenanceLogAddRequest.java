package com.springboot.model.dto.cameramaintenancelog;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
public class CameraMaintenanceLogAddRequest implements Serializable {

    private Long cameraId;

    private String maintenanceType;

    private String maintenanceContent;

    private String maintainedBy;

    private Date maintainedAt;

    private Date nextMaintenanceAt;
}
