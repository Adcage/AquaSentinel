package com.springboot.model.dto.statssnapshot;

import com.springboot.common.PageRequest;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class StatsSnapshotQueryRequest extends PageRequest {

    private Long id;

    private String granularity;

    private Date snapshotDate;

    private Integer snapshotHour;

    private Long venueId;

    private String metricType;

    private String metricKey;
}
