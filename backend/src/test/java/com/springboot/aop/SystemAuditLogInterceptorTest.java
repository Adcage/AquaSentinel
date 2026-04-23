package com.springboot.aop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.exception.BusinessException;
import com.springboot.model.entity.SystemAuditLog;
import com.springboot.service.SystemAuditLogService;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class SystemAuditLogInterceptorTest {

    private final SystemAuditLogService systemAuditLogService = mock(SystemAuditLogService.class);

    private final SystemAuditLogInterceptor interceptor =
            new SystemAuditLogInterceptor(systemAuditLogService);

    @BeforeEach
    void setUp() {
        when(systemAuditLogService.save(any(SystemAuditLog.class))).thenReturn(true);
    }

    @AfterEach
    void cleanupRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void doInterceptorShouldSaveAuditLogForWhitelistedRequest() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/alerts/action");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(point.getArgs()).thenReturn(new Object[] {"{\"alertId\":1}"});
        when(point.proceed())
                .thenReturn(new BaseResponse<>(ErrorCode.SUCCESS.getCode(), true, "ok"));

        Object result = interceptor.doInterceptor(point);

        assertEquals(BaseResponse.class, result.getClass());
        ArgumentCaptor<SystemAuditLog> logCaptor = ArgumentCaptor.forClass(SystemAuditLog.class);
        verify(systemAuditLogService).save(logCaptor.capture());
        SystemAuditLog savedLog = logCaptor.getValue();
        assertEquals("ALERT", savedLog.getLog_category());
        assertEquals("/api/alerts/action", savedLog.getRequest_uri());
        assertEquals("POST", savedLog.getRequest_method());
        assertEquals(ErrorCode.SUCCESS.getCode(), savedLog.getResponse_code());
    }

    @Test
    void doInterceptorShouldSkipAuditLogForNonWhitelistedRequest() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(point.getArgs()).thenReturn(new Object[] {});
        when(point.proceed())
                .thenReturn(new BaseResponse<>(ErrorCode.SUCCESS.getCode(), true, "ok"));

        interceptor.doInterceptor(point);

        verify(systemAuditLogService, never()).save(any(SystemAuditLog.class));
    }

    @Test
    void doInterceptorShouldSaveFailureAuditLogWhenBusinessExceptionThrown() throws Throwable {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/auth/admin/login");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(point.getArgs()).thenReturn(new Object[] {"admin"});
        when(point.proceed()).thenThrow(new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "账号已锁定"));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> interceptor.doInterceptor(point));
        assertEquals(ErrorCode.NOT_LOGIN_ERROR.getCode(), exception.getCode());

        ArgumentCaptor<SystemAuditLog> logCaptor = ArgumentCaptor.forClass(SystemAuditLog.class);
        verify(systemAuditLogService).save(logCaptor.capture());
        SystemAuditLog savedLog = logCaptor.getValue();
        assertEquals("LOGIN", savedLog.getLog_category());
        assertEquals(ErrorCode.NOT_LOGIN_ERROR.getCode(), savedLog.getResponse_code());
        assertEquals("账号已锁定", savedLog.getResponse_message());
    }
}
