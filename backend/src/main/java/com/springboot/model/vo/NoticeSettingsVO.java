package com.springboot.model.vo;

import java.io.Serializable;

import lombok.Data;

@Data
public class NoticeSettingsVO implements Serializable {

    private Integer offDutyThreshold;

    private Integer deviceOfflineThreshold;

    private Integer drowningAlertThreshold;
}
