package com.springboot.model.dto.internalai;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

@Data
public class InternalAiEventRequest implements Serializable {

    private String eventUid;

    @JsonAlias({"camera_id"})
    private Long cameraId;

    @JsonAlias({"camera_code"})
    private String cameraCode;

    private Long taskId;

    private String taskCode;

    private Long venueId;

    @JsonAlias({"event_type"})
    private String eventType;

    @JsonAlias({"risk_type"})
    private String riskType;

    @JsonAlias({"risk_level"})
    private String riskLevel;

    private BigDecimal confidence;

    @JsonAlias({"target_id"})
    private String targetId;

    @JsonAlias({"pool_head_count"})
    private Integer poolHeadCount;

    @JsonAlias({"bbox"})
    private Object bboxJson;

    @JsonAlias({"position_desc"})
    private String positionDesc;

    @JsonAlias({"emergency_contact_name"})
    private String emergencyContactName;

    @JsonAlias({"emergency_contact_phone"})
    private String emergencyContactPhone;

    @JsonAlias({"incident_location"})
    private String incidentLocation;

    @JsonAlias({"video_stream_url"})
    private String videoStreamUrl;

    @JsonAlias({"event_time"})
    private Date eventTime;

    private String detectTime;

    @JsonAlias({"ext_json"})
    private Object extJson;

    private String alertType;
}
