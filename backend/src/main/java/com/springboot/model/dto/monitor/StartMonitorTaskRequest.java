package com.springboot.model.dto.monitor;

import java.io.Serializable;
import lombok.Data;

@Data
public class StartMonitorTaskRequest implements Serializable {

    private Long cameraId;

    private String taskCode;

    private Integer frameIntervalMs;

    private String callbackUrl;
}
