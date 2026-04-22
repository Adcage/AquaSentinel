package com.springboot.model.dto.monitoringevent;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

@Data
public class MonitoringEventEditRequest implements Serializable {

    private Long id;

    private String riskLevel;

    private BigDecimal confidence;

    private Integer poolHeadCount;

    private String positionDesc;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private String incidentLocation;

    private String videoStreamUrl;

    private Date eventTime;

    private Object extJson;
}
