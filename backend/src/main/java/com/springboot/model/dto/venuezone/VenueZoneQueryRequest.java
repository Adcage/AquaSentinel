package com.springboot.model.dto.venuezone;

import com.springboot.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class VenueZoneQueryRequest extends PageRequest {

    private Long id;

    private Long venueId;

    private String zoneCode;

    private String zoneName;

    private String zoneType;

    private String riskLevel;
}
