package com.springboot.model.dto.aistreamtask;

import com.springboot.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AiStreamTaskQueryRequest extends PageRequest {

    private Long id;

    private String taskCode;

    private Long cameraId;

    private String taskStatus;
}
