package com.springboot.model.dto.stats;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
public class StatsTrendRequest implements Serializable {

    private Long venueId;

    private String metricType;

    private String metricKey;

    private String granularity;

    private Date startDate;

    private Date endDate;
}
