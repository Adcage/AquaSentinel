package com.springboot.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.springboot.model.entity.AlertRecord;
import com.springboot.service.push.AppAlertPushClient;
import com.springboot.service.push.PcAlertPushClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertPushServiceTest {

    @Mock private AppAlertPushClient appAlertPushClient;

    @Mock private PcAlertPushClient pcAlertPushClient;

    @InjectMocks private AlertPushService alertPushService;

    @Test
    void pushToAppShouldReturnFalseWhenClientThrows() {
        AlertRecord alertRecord = new AlertRecord();
        doThrow(new RuntimeException("network down"))
                .when(appAlertPushClient)
                .push(any(AlertRecord.class));

        boolean result = alertPushService.pushToApp(alertRecord);

        assertFalse(result);
    }

    @Test
    void pushToPcShouldReturnTrueWhenClientSucceeds() {
        AlertRecord alertRecord = new AlertRecord();
        when(pcAlertPushClient.push(any(AlertRecord.class))).thenReturn(true);

        boolean result = alertPushService.pushToPc(alertRecord);

        assertTrue(result);
    }
}
