package com.springboot.model.dto.systemauditlog;

import java.io.Serializable;
import lombok.Data;

@Data
public class SystemAuditLogEditRequest implements Serializable {

    private Long id;

    private String responseMessage;

    private Integer responseCode;

    private Integer costMs;
}
