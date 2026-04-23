package com.springboot.model.dto.venuezone;

import java.io.Serializable;

import lombok.Data;

@Data
public class VenueZoneUpdateRequest implements Serializable {

    private Long id;

    private Long venueId;

    private String zoneCode;

    private String zoneName;

    private String zoneType;

    private String geoJson;

    private String riskLevel;
}
