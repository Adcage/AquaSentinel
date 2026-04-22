package com.springboot.model.dto.monitoringevent;

import com.springboot.common.PageRequest;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MonitoringEventQueryRequest extends PageRequest {

    private Long id;

    private String eventUid;

    private Long cameraId;

    private Long taskId;

    private String eventType;

    private String riskLevel;

    private Date startEventTime;

    private Date endEventTime;
}
