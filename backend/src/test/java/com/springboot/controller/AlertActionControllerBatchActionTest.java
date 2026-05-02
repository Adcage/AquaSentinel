package com.springboot.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.exception.BusinessException;
import com.springboot.model.dto.alertrecord.AlertActionRequest;
import com.springboot.model.dto.alertrecord.AlertBatchActionRequest;
import com.springboot.model.vo.BatchOperateResultVO;
import com.springboot.service.AlertDisposalService;
import com.springboot.service.AlertRecordService;
import com.springboot.service.MonitoringEventService;
import com.springboot.websocket.AlertWsPublisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AlertActionControllerBatchActionTest {

    @Mock private AlertRecordService alertRecordService;

    @Mock private AlertDisposalService alertDisposalService;

    @Mock private MonitoringEventService monitoringEventService;

    @Mock private AlertWsPublisher alertWsPublisher;

    private AlertActionController controller;

    @BeforeEach
    void setUp() {
        controller = spy(new AlertActionController());
        ReflectionTestUtils.setField(controller, "alertRecordService", alertRecordService);
        ReflectionTestUtils.setField(controller, "alertDisposalService", alertDisposalService);
        ReflectionTestUtils.setField(controller, "monitoringEventService", monitoringEventService);
        ReflectionTestUtils.setField(controller, "alertWsPublisher", alertWsPublisher);
    }

    @Test
    void batchActionShouldReturnDetailResult() {
        AlertBatchActionRequest request = new AlertBatchActionRequest();
        request.setAlertIds(Arrays.asList(1L, 2L, 3L));
        request.setActionType("DONE");

        doAnswer(
                        invocation -> {
                            AlertActionRequest single = invocation.getArgument(0);
                            if (single.getAlertId() != null && single.getAlertId().equals(2L)) {
                                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "报警记录不存在");
                            }
                            return new BaseResponse<Map<String, Object>>(0, new HashMap<>(), "");
                        })
                .when(controller)
                .action(any(AlertActionRequest.class));

        BaseResponse<BatchOperateResultVO> response = controller.batchAction(request);

        assertEquals(0, response.getCode());
        assertEquals(2, response.getData().getSuccessCount());
        assertEquals(1, response.getData().getFailedCount());
        assertEquals("报警记录不存在", response.getData().getFailed().get(0).getReason());
    }
}
