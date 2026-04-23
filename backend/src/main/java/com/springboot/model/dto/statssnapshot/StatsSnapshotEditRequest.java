package com.springboot.model.dto.statssnapshot;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class StatsSnapshotEditRequest implements Serializable {

    private Long id;

    private BigDecimal metricValue;

    private Object dimensionJson;
}
