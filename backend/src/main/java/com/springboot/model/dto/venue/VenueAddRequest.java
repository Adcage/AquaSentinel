package com.springboot.model.dto.venue;

import java.io.Serializable;

import lombok.Data;

@Data
public class VenueAddRequest implements Serializable {

    private String venueCode;

    private String venueName;

    private String address;

    private String contactName;

    private String contactPhone;

    private String timezone;

    private Integer status;

    private Object fenceGeoJson;
}
