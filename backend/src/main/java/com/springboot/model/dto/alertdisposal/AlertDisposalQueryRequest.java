package com.springboot.model.dto.alertdisposal;

import java.util.Date;

import com.springboot.common.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AlertDisposalQueryRequest extends PageRequest {

    private Long id;

    private Long alertId;

    private Long operatorUserId;

    private String operatorRole;

    private String actionType;

    private Date startActionTime;

    private Date endActionTime;
}
