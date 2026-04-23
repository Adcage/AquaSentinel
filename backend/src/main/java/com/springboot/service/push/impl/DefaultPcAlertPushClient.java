package com.springboot.service.push.impl;

import com.springboot.model.entity.AlertRecord;
import com.springboot.service.push.PcAlertPushClient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DefaultPcAlertPushClient implements PcAlertPushClient {

    @Override
    public boolean push(AlertRecord alertRecord) {
        if (alertRecord == null) {
            return false;
        }
        log.info(
                "PC报警推送占位实现: alertId={}, alertUid={}, type={}",
                alertRecord.getId(),
                alertRecord.getAlert_uid(),
                alertRecord.getAlert_type());
        return true;
    }
}
