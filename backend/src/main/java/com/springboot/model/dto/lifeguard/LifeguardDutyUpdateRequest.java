package com.springboot.model.dto.lifeguard;

import java.io.Serializable;

import lombok.Data;

@Data
public class LifeguardDutyUpdateRequest implements Serializable {

    private Long lifeguardId;

    private String dutyStatus;

    private Long operatorId;
}
