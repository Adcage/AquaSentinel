package com.springboot.model.dto.venue;

import java.io.Serializable;
import lombok.Data;

@Data
public class VenueEditRequest implements Serializable {

    private Long id;

    private String venueName;

    private String address;

    private String contactName;

    private String contactPhone;

    private String timezone;

    private Integer status;

    private Object fenceGeoJson;
}
