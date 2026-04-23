package com.springboot.model.dto.cameramaintenancelog;

import java.util.Date;

import com.springboot.common.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CameraMaintenanceLogQueryRequest extends PageRequest {

    private Long id;

    private Long cameraId;

    private String maintenanceType;

    private String maintenanceContent;

    private String maintainedBy;

    private Date startMaintainedAt;

    private Date endMaintainedAt;
}
