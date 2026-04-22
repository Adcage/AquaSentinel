package com.springboot.model.dto.lifeguard;

import java.io.Serializable;
import lombok.Data;

@Data
public class LifeguardAddRequest implements Serializable {

    private Long userId;

    private String username;

    private String password;

    private String email;

    private String lifeguardCode;

    private String fullName;

    private String phone;

    private Long venueId;

    private Object fenceGeoJson;

    private String auditStatus;

    private String dutyStatus;
}
