package com.springboot.model.dto.alertdisposal;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class AlertDisposalEditRequest implements Serializable {

    private Long id;

    private String actionType;

    private String actionNote;

    private Date actionTime;
}
