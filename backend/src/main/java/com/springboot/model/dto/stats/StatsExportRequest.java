package com.springboot.model.dto.stats;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class StatsExportRequest implements Serializable {

    private Long venueId;

    private String metricType;

    private Date startDate;

    private Date endDate;
}
