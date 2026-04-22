package com.springboot.model.dto.alertrecord;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
public class AlertRecordUpdateRequest implements Serializable {

    private Long id;

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

    private Integer pushedToApp;

    private Integer pushedToPc;

    private Date firstPushTime;

    private Date resolvedTime;
}
