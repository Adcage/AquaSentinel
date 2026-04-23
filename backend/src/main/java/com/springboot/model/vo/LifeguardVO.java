package com.springboot.model.vo;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class LifeguardVO implements Serializable {

    private Long id;

    private Long userId;

    private String lifeguardCode;

    private String fullName;

    private String phone;

    private Long venueId;

    private Object fenceGeoJson;

    private String auditStatus;

    private String dutyStatus;

    private Date lastLoginAt;

    private Date createdAt;

    private Date updatedAt;
}
