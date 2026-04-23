package com.springboot.model.vo;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class VenueVO implements Serializable {

    private Long id;

    private String venueCode;

    private String venueName;

    private String address;

    private String contactName;

    private String contactPhone;

    private String timezone;

    private Integer status;

    private Object fenceGeoJson;

    private Date createdAt;

    private Date updatedAt;
}
