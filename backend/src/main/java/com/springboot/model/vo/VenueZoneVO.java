package com.springboot.model.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
public class VenueZoneVO implements Serializable {

    private Long id;

    private Long venueId;

    private String zoneCode;

    private String zoneName;

    private String zoneType;

    private Object geoJson;

    private String riskLevel;

    private Date createdAt;

    private Date updatedAt;
}
