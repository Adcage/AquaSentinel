package com.springboot.model.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

@Data
public class MonitoringEventVO implements Serializable {

    private Long id;

    private String eventUid;

    private Long cameraId;

    private Long taskId;

    private String eventType;

    private String riskLevel;

    private BigDecimal confidence;

    private String targetId;

    private Integer poolHeadCount;

    private Object bboxJson;

    private String positionDesc;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private String incidentLocation;

    private String videoStreamUrl;

    private Date eventTime;

    private Object extJson;

    private Date createdAt;
}
