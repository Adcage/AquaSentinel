package com.springboot.model.dto.venue;

import com.springboot.common.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class VenueQueryRequest extends PageRequest {

    private Long id;

    private String venueCode;

    private String venueName;

    private Integer status;

    private String contactName;
}
