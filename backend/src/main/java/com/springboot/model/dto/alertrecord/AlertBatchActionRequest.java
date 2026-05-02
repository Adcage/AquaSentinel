package com.springboot.model.dto.alertrecord;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class AlertBatchActionRequest implements Serializable {

    private List<Long> alertIds;

    private String actionType;

    private Long assigneeLifeguardId;

    private String actionNote;
}
