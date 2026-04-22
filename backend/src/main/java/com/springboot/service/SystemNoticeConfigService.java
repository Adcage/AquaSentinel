package com.springboot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springboot.model.entity.SystemNoticeConfig;
import com.springboot.model.vo.NoticeSettingsVO;

public interface SystemNoticeConfigService extends IService<SystemNoticeConfig> {

    NoticeSettingsVO getNoticeSettings();

    NoticeSettingsVO updateNoticeSettings(Integer offDutyThresholdSec,
            Integer deviceOfflineThresholdSec, Integer drowningAlertThresholdSec);

    int getOffDutyThresholdSec();

    int getDeviceOfflineThresholdSec();

    int getDrowningAlertThresholdSec();
}
