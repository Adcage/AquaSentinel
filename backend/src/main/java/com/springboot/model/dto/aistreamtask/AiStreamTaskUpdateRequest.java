package com.springboot.model.dto.aistreamtask;

import java.io.Serializable;
import lombok.Data;

@Data
public class AiStreamTaskUpdateRequest implements Serializable {

    private Long id;

    private String taskCode;

    private Long cameraId;

    private String streamUrl;

    private Integer frameIntervalMs;

    private String callbackUrl;

    private String taskStatus;
}
