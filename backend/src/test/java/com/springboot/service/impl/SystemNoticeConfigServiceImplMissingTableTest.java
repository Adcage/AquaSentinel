package com.springboot.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.sql.SQLSyntaxErrorException;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.BadSqlGrammarException;

@ExtendWith(MockitoExtension.class)
class SystemNoticeConfigServiceImplMissingTableTest {

    @Test
    void getNoticeSettingsShouldFallbackToDefaultWhenNoticeTableMissing() {
        SystemNoticeConfigServiceImpl service =
                org.mockito.Mockito.spy(new SystemNoticeConfigServiceImpl());
        doThrow(
                        new BadSqlGrammarException(
                                "select",
                                "SELECT * FROM system_notice_config",
                                new SQLSyntaxErrorException(
                                        "Table 'aqua_sentinel.system_notice_config' doesn't exist")))
                .when(service)
                .getOne(any(QueryWrapper.class));

        var notice = service.getNoticeSettings();

        assertNotNull(notice);
        assertEquals(60, notice.getOffDutyThreshold());
        assertEquals(180, notice.getDeviceOfflineThreshold());
        assertEquals(3, notice.getDrowningAlertThreshold());
        assertEquals(180, service.getDeviceOfflineThresholdSec());
    }
}
