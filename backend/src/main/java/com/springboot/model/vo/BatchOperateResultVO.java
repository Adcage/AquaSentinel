package com.springboot.model.vo;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class BatchOperateResultVO {

    private List<Long> successIds = new ArrayList<>();

    private List<FailedItem> failed = new ArrayList<>();

    private Integer successCount = 0;

    private Integer failedCount = 0;

    @Data
    public static class FailedItem {
        private Long id;

        private String reason;
    }
}
