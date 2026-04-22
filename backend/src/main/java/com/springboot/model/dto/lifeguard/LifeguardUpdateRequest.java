package com.springboot.model.dto.lifeguard;

import java.io.Serializable;
import lombok.Data;

@Data
public class LifeguardUpdateRequest implements Serializable {

    private Long id;

    private Long userId;

    private String lifeguardCode;

    private String fullName;

    private String phone;

    private Long venueId;

    private Object fenceGeoJson;

    private String auditStatus;

    private String dutyStatus;
}
