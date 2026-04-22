package com.springboot.service.push;

import com.springboot.model.entity.AlertRecord;

public interface PcAlertPushClient {

    boolean push(AlertRecord alertRecord);
}
