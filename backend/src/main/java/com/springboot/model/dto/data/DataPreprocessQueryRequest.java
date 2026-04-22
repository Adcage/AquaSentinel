package com.springboot.model.dto.data;

import com.springboot.common.PageRequest;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DataPreprocessQueryRequest extends PageRequest implements Serializable {

    private Long taskId;

    private Long venueId;

    private String status;

    private Long startTime;

    private Long endTime;
}
