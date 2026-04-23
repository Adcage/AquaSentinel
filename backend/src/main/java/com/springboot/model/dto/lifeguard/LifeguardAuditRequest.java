package com.springboot.model.dto.lifeguard;

import java.io.Serializable;

import lombok.Data;

@Data
public class LifeguardAuditRequest implements Serializable {

    private Long lifeguardId;

    private String auditStatus;

    private Long approvedBy;
}
