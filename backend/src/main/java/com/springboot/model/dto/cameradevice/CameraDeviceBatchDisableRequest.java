package com.springboot.model.dto.cameradevice;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class CameraDeviceBatchDisableRequest implements Serializable {

    private List<Long> cameraIds;
}
