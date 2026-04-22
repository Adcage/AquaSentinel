package com.springboot.model.dto.alertrecord;

import java.io.Serializable;
import lombok.Data;

@Data
public class AlertRecordAddRequest implements Serializable {

    private String alertUid;

    private Long eventId;

    private Long cameraId;

    private Long venueId;

    private Long lifeguardId;

    private String alertType;

    private String alertStatus;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private String incidentLocation;

    private String videoStreamUrl;
}
