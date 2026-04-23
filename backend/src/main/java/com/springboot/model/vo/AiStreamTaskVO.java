package com.springboot.model.vo;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class AiStreamTaskVO implements Serializable {

    private Long id;

    private String taskCode;

    private Long cameraId;

    private String streamUrl;

    private Integer frameIntervalMs;

    private String callbackUrl;

    private String taskStatus;

    private Date startedAt;

    private Date stoppedAt;

    private Date lastFrameAt;

    private Date createdAt;

    private Date updatedAt;
}
