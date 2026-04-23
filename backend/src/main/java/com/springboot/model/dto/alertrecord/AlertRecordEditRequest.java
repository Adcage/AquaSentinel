package com.springboot.model.dto.alertrecord;

import java.io.Serializable;

import lombok.Data;

@Data
public class AlertRecordEditRequest implements Serializable {

    private Long id;

    private Long lifeguardId;

    private String alertStatus;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private String incidentLocation;

    private String videoStreamUrl;
}
