package com.springboot.model.vo;

import lombok.Data;

/** 相似报警VO */
@Data
public class SimilarAlertVO {

    private Long alertId;

    private String alertUid;

    private String alertType;

    private String alertStatus;

    private String incidentLocation;

    private String detectionResult;

    private String createdAt;

    private Double similarity;
}
