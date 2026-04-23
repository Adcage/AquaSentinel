package com.springboot.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.springboot.common.BaseResponse;
import com.springboot.common.DeleteRequest;
import com.springboot.common.ErrorCode;
import com.springboot.exception.BusinessException;
import com.springboot.service.VenueService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VenueControllerDeleteTest {

    @Mock private VenueService venueService;

    private VenueController controller;

    @BeforeEach
    void setUp() {
        controller = new VenueController();
        ReflectionTestUtils.setField(controller, "venueService", venueService);
        ReflectionTestUtils.setField(
                controller, "objectMapper", new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    void deleteVenueShouldReturnSuccessWhenNoReferenceData() {
        DeleteRequest request = new DeleteRequest();
        request.setId(2020L);
        when(venueService.removeById(2020L)).thenReturn(true);

        BaseResponse<Boolean> response = controller.deleteVenue(request);

        assertEquals(0, response.getCode());
        assertEquals(Boolean.TRUE, response.getData());
    }

    @Test
    void deleteVenueShouldThrowBusinessExceptionWhenReferenced() {
        DeleteRequest request = new DeleteRequest();
        request.setId(2020L);
        when(venueService.removeById(2020L))
                .thenThrow(new DataIntegrityViolationException("fk_alert_venue"));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> controller.deleteVenue(request));

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertEquals("场馆已被报警记录或其他业务数据引用，无法删除", exception.getMessage());
    }
}
