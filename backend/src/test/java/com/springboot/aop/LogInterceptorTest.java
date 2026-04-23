package com.springboot.aop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class LogInterceptorTest {

    private final LogInterceptor logInterceptor = new LogInterceptor();

    @AfterEach
    void cleanupRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void doInterceptorShouldProceedWhenNoRequestContext() throws Throwable {
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(point.getArgs()).thenReturn(new Object[] {"demo"});
        when(point.proceed()).thenReturn("ok");

        Object result = logInterceptor.doInterceptor(point);

        assertEquals("ok", result);
        verify(point).proceed();
    }

    @Test
    void doInterceptorShouldProceedWithRequestContext() throws Throwable {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/monitor/tasks/realtime/by-camera");
        request.setRemoteHost("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(point.getArgs()).thenReturn(new Object[] {1L});
        when(point.proceed()).thenReturn("ok");

        Object result = logInterceptor.doInterceptor(point);

        assertEquals("ok", result);
        verify(point).proceed();
    }
}
