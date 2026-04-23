package com.springboot.model.dto.cameramaintenancelog;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class CameraMaintenanceLogEditRequest implements Serializable {

    private Long id;

    private String maintenanceType;

    private String maintenanceContent;

    private String maintainedBy;

    private Date maintainedAt;

    private Date nextMaintenanceAt;
}
