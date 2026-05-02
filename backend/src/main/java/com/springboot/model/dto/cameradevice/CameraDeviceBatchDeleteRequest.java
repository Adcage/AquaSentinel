package com.springboot.model.dto.cameradevice;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class CameraDeviceBatchDeleteRequest implements Serializable {

    private List<Long> cameraIds;
}
