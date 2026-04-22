package com.springboot.model.dto.monitor;

import java.io.Serializable;
import lombok.Data;

@Data
public class MonitorTaskControlRequest implements Serializable {

    private String taskCode;
}
