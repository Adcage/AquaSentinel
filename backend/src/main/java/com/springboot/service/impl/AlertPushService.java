package com.springboot.service.impl;

import com.springboot.model.entity.AlertRecord;
import com.springboot.service.push.AppAlertPushClient;
import com.springboot.service.push.PcAlertPushClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AlertPushService {

    @Resource
    private AppAlertPushClient appAlertPushClient;

    @Resource
    private PcAlertPushClient pcAlertPushClient;

    public boolean pushToApp(AlertRecord alertRecord) {
        try {
            return appAlertPushClient.push(alertRecord);
        } catch (Exception e) {
            log.warn("App推送失败, alertId={}", alertRecord == null ? null : alertRecord.getId(), e);
            return false;
        }
    }

    public boolean pushToPc(AlertRecord alertRecord) {
        try {
            return pcAlertPushClient.push(alertRecord);
        } catch (Exception e) {
            log.warn("PC推送失败, alertId={}", alertRecord == null ? null : alertRecord.getId(), e);
            return false;
        }
    }
}
