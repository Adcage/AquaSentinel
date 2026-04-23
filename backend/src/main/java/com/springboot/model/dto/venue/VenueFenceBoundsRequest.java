package com.springboot.model.dto.venue;

import com.springboot.common.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class VenueFenceBoundsRequest extends PageRequest {

    private Double minLng;

    private Double maxLng;

    private Double minLat;

    private Double maxLat;

    private Integer status;
}
