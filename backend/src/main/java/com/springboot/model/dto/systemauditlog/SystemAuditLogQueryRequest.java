package com.springboot.model.dto.systemauditlog;

import com.springboot.common.PageRequest;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SystemAuditLogQueryRequest extends PageRequest {

    private Long id;

    private String traceId;

    private String logCategory;

    private Long operatorId;

    private String operatorName;

    private String requestUri;

    private Integer responseCode;

    private Date startCreatedAt;

    private Date endCreatedAt;
}
