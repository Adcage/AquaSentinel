package com.springboot.service.push;

import com.springboot.model.entity.AlertRecord;

public interface AppAlertPushClient {

    boolean push(AlertRecord alertRecord);
}
