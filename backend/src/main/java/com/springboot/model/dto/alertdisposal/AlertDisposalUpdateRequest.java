package com.springboot.model.dto.alertdisposal;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class AlertDisposalUpdateRequest implements Serializable {

    private Long id;

    private Long alertId;

    private Long operatorUserId;

    private String operatorRole;

    private String actionType;

    private String actionNote;

    private Date actionTime;
}
