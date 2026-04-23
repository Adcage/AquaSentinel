package com.springboot.messaging.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

@Data
public class AlertEventMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String messageId;

    private Integer version = 1;

    private String eventUid;

    private Long cameraId;

    private String cameraCode;

    private Long taskId;

    private String taskCode;

    private Long venueId;

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

    private String detectTime;

    private Object extJson;

    private String alertType;

    private Date publishedAt;

    private String source;
}
