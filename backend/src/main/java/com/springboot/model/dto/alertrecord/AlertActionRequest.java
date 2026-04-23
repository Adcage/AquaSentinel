package com.springboot.model.dto.alertrecord;

import java.io.Serializable;

import lombok.Data;

@Data
public class AlertActionRequest implements Serializable {

    private Long alertId;

    private String actionType;

    private String actionNote;

    private Long assigneeLifeguardId;
}
