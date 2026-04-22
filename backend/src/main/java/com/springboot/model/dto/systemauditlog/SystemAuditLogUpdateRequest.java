package com.springboot.model.dto.systemauditlog;

import java.io.Serializable;
import lombok.Data;

@Data
public class SystemAuditLogUpdateRequest implements Serializable {

    private Long id;

    private String traceId;

    private String logCategory;

    private Long operatorId;

    private String operatorName;

    private String clientIp;

    private String requestUri;

    private String requestMethod;

    private String requestBody;

    private Integer responseCode;

    private String responseMessage;

    private Integer costMs;
}
