package com.springboot.model.dto.lifeguardlocationlog;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

@Data
public class LifeguardLocationReportRequest implements Serializable {

    private Long lifeguardId;

    private Long venueId;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private Integer inFence;

    private String reportSource;

    private Date reportedAt;
}
