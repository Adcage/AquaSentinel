package com.springboot.model.dto.data;

import java.io.Serializable;

import lombok.Data;

@Data
public class DataAnalysisReportQueryRequest implements Serializable {

    private Long venueId;

    private String type;

    private Long startTime;

    private Long endTime;
}
