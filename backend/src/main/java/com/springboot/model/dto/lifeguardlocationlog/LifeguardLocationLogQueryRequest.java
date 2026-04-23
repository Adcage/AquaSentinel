package com.springboot.model.dto.lifeguardlocationlog;

import com.springboot.common.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class LifeguardLocationLogQueryRequest extends PageRequest {

    private Long id;

    private Long lifeguardId;

    private Long venueId;

    private Integer inFence;

    private String reportSource;
}
