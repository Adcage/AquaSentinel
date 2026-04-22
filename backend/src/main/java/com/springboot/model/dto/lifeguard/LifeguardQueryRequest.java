package com.springboot.model.dto.lifeguard;

import com.springboot.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class LifeguardQueryRequest extends PageRequest {

    private Long id;

    private Long userId;

    private String lifeguardCode;

    private String fullName;

    private String phone;

    private Long venueId;

    private String auditStatus;

    private String dutyStatus;
}
