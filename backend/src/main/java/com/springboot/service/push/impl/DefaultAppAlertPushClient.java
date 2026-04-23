package com.springboot.service.push.impl;

import com.springboot.model.entity.AlertRecord;
import com.springboot.service.push.AppAlertPushClient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DefaultAppAlertPushClient implements AppAlertPushClient {

    @Override
    public boolean push(AlertRecord alertRecord) {
        if (alertRecord == null) {
            return false;
        }
        log.info(
                "App报警推送占位实现: alertId={}, alertUid={}, type={}",
                alertRecord.getId(),
                alertRecord.getAlert_uid(),
                alertRecord.getAlert_type());
        return true;
    }
}
