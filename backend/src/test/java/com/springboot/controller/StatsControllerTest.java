package com.springboot.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.springboot.common.BaseResponse;
import com.springboot.model.dto.stats.StatsExportRequest;
import com.springboot.model.entity.AlertRecord;
import com.springboot.service.AlertRecordService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StatsControllerTest {

    @Mock
    private AlertRecordService alertRecordService;

    private StatsController controller;

    @BeforeEach
    void setUp() throws IOException {
        controller = new StatsController();
        ReflectionTestUtils.setField(controller, "alertRecordService", alertRecordService);
        Path tempDir = Files.createTempDirectory("stats-export-test");
        ReflectionTestUtils.setField(controller, "uploadPath", tempDir.toString());
    }

    @Test
    void exportCsvShouldReturnSuccessWhenRecordHasAllColumns() {
        AlertRecord record = new AlertRecord();
        record.setId(1L);
        record.setAlert_uid("ALERT-001");
        record.setEvent_id(2L);
        record.setCamera_id(3L);
        record.setVenue_id(4L);
        record.setLifeguard_id(5L);
        record.setAlert_type("DROWING");
        record.setAlert_status("DONE");
        record.setEmergency_contact_name("张三");
        record.setEmergency_contact_phone("13800000000");
        record.setIncident_location("深水区");
        record.setVideo_stream_url("rtsp://test");
        record.setDetection_result("ok");
        record.setPushed_to_app(1);
        record.setPushed_to_pc(1);
        Date now = new Date();
        record.setFirst_push_time(now);
        record.setResolved_time(now);
        record.setCreated_at(now);
        record.setUpdated_at(now);
        when(alertRecordService.list(any(QueryWrapper.class))).thenReturn(List.of(record));

        BaseResponse<Map<String, Object>> response = controller.exportCsv(new StatsExportRequest());

        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals("csv", response.getData().get("format"));
        assertTrue(String.valueOf(response.getData().get("downloadUrl")).endsWith(".csv"));
    }
}
