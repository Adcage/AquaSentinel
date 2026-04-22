package com.springboot.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
public class SystemAuditLogVO implements Serializable {

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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
}
