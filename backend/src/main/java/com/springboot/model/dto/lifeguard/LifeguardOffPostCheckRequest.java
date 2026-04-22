package com.springboot.model.dto.lifeguard;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
public class LifeguardOffPostCheckRequest implements Serializable {

    private Long lifeguardId;

    private String leaveReason;

    private Date plannedReturnAt;
}
