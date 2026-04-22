package com.springboot.model.dto.alertrecord;

import com.springboot.common.PageRequest;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AlertRecordQueryRequest extends PageRequest {

    private Long id;

    private String alertUid;

    private Long eventId;

    private Long cameraId;

    private Long venueId;

    private Long lifeguardId;

    private String alertType;

    private String alertStatus;

    private Date startCreatedAt;

    private Date endCreatedAt;

    private Date startTime;

    private Date endTime;

    private String keyword;
}
