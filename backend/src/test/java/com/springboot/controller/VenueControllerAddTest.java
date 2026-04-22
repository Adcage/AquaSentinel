package com.springboot.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.exception.BusinessException;
import com.springboot.model.dto.venue.VenueAddRequest;
import com.springboot.model.entity.Venue;
import com.springboot.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VenueControllerAddTest {

    @Mock
    private VenueService venueService;

    private VenueController controller;

    @BeforeEach
    void setUp() {
        controller = new VenueController();
        ReflectionTestUtils.setField(controller, "venueService", venueService);
        ReflectionTestUtils.setField(controller, "objectMapper", new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    void addVenueShouldFillDefaultContactFieldsWhenMissing() {
        VenueAddRequest request = new VenueAddRequest();
        request.setVenueCode("SH-PD-001");
        request.setVenueName("浦东场馆");
        request.setAddress("浦东新区XX路88号");

        when(venueService.save(any(Venue.class))).thenAnswer(invocation -> {
            Venue venue = invocation.getArgument(0);
            venue.setId(3001L);
            return true;
        });

        BaseResponse<Long> response = controller.addVenue(request);

        assertEquals(0, response.getCode());
        assertEquals(3001L, response.getData());

        ArgumentCaptor<Venue> captor = ArgumentCaptor.forClass(Venue.class);
        org.mockito.Mockito.verify(venueService).save(captor.capture());
        Venue saved = captor.getValue();
        assertEquals("未设置", saved.getContact_name());
        assertEquals("未设置", saved.getContact_phone());
        assertEquals("Asia/Shanghai", saved.getTimezone());
    }

    @Test
    void addVenueShouldThrowBusinessExceptionWhenConstraintViolated() {
        VenueAddRequest request = new VenueAddRequest();
        request.setVenueCode("SH-PD-002");
        request.setVenueName("测试场馆");

        when(venueService.save(any(Venue.class)))
                .thenThrow(new DataIntegrityViolationException("Field does not have a default value"));

        BusinessException exception = assertThrows(BusinessException.class, () -> controller.addVenue(request));

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertEquals("场馆信息不完整，请补充必填字段", exception.getMessage());
    }
}
