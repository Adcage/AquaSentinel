package com.springboot.model.dto.cameradevice;

import java.io.Serializable;

import lombok.Data;

@Data
public class CameraPtzControlRequest implements Serializable {

    private Long cameraId;

    private String action;

    private String direction;

    private Integer step;

    private Integer pan;

    private Integer tilt;

    private Integer pulse;
}
