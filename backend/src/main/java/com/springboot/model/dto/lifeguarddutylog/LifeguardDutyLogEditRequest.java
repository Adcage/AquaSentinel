package com.springboot.model.dto.lifeguarddutylog;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
public class LifeguardDutyLogEditRequest implements Serializable {

    private Long id;

    private String actionType;

    private String leaveReason;

    private Date plannedReturnAt;

    private Date actualReturnAt;

    private Long approvedBy;
}
