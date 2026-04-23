package com.springboot.model.dto.systemsettings;

import java.io.Serializable;

import lombok.Data;

@Data
public class NoticeSettingsUpdateRequest implements Serializable {

    private Integer offDutyThreshold;

    private Integer deviceOfflineThreshold;

    private Integer drowningAlertThreshold;
}
