package com.springboot.model.dto.venuezone;

import java.io.Serializable;

import lombok.Data;

@Data
public class VenueZoneEditRequest implements Serializable {

    private Long id;

    private String zoneName;

    private String zoneType;

    private String geoJson;

    private String riskLevel;
}
