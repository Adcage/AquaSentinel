package com.springboot.model.dto.lifeguarddutylog;

import java.util.Date;

import com.springboot.common.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class LifeguardDutyLogQueryRequest extends PageRequest {

    private Long id;

    private Long lifeguardId;

    private String actionType;

    private Long approvedBy;

    private Date plannedReturnAt;

    private Date actualReturnAt;
}
