package com.springboot.model.dto.statssnapshot;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

@Data
public class StatsSnapshotUpdateRequest implements Serializable {

    private Long id;

    private String granularity;

    private Date snapshotDate;

    private Integer snapshotHour;

    private Long venueId;

    private String metricType;

    private String metricKey;

    private BigDecimal metricValue;

    private Object dimensionJson;
}
