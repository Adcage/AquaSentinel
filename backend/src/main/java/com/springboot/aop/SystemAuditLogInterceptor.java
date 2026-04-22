package com.springboot.aop;

import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.common.RequestIdHolder;
import com.springboot.exception.BusinessException;
import com.springboot.model.entity.SystemAuditLog;
import com.springboot.security.AuthContextHolder;
import com.springboot.security.AuthUserContext;
import com.springboot.service.SystemAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class SystemAuditLogInterceptor {

    private static final int MAX_REQUEST_BODY_LENGTH = 2000;

    private static final List<String> AUDIT_URI_PREFIXES = List.of(
            "/api/auth/admin/login",
            "/api/auth/login",
            "/api/lifeguards/login",
            "/api/cameras/",
            "/api/monitor/tasks/",
            "/api/alerts/",
            "/api/lifeguards/",
            "/api/stats/",
            "/api/internal/ai/events");

    private final SystemAuditLogService systemAuditLogService;

    public SystemAuditLogInterceptor(SystemAuditLogService systemAuditLogService) {
        this.systemAuditLogService = systemAuditLogService;
    }

    @Around("execution(* com.springboot.controller.*.*(..))")
    public Object doInterceptor(ProceedingJoinPoint point) throws Throwable {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return point.proceed();
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String requestUri = request.getRequestURI();
        if (!shouldAudit(requestUri)) {
            return point.proceed();
        }

        long start = System.currentTimeMillis();
        Integer responseCode = ErrorCode.SUCCESS.getCode();
        String responseMessage = ErrorCode.SUCCESS.getMessage();
        Object result;
        try {
            result = point.proceed();
            if (result instanceof BaseResponse<?> baseResponse) {
                responseCode = baseResponse.getCode();
                responseMessage = baseResponse.getMessage();
            }
            return result;
        } catch (Throwable throwable) {
            if (throwable instanceof BusinessException businessException) {
                responseCode = businessException.getCode();
            } else {
                responseCode = ErrorCode.SYSTEM_ERROR.getCode();
            }
            responseMessage = throwable.getMessage();
            throw throwable;
        } finally {
            persistAuditLog(request, point.getArgs(), requestUri, responseCode, responseMessage,
                    System.currentTimeMillis() - start);
        }
    }

    private void persistAuditLog(HttpServletRequest request, Object[] args, String requestUri,
                                 Integer responseCode, String responseMessage, long costMs) {
        SystemAuditLog systemAuditLog = new SystemAuditLog();
        String traceId = RequestIdHolder.get();
        systemAuditLog.setTrace_id(StringUtils.defaultIfBlank(traceId, UUID.randomUUID().toString()));
        systemAuditLog.setLog_category(resolveLogCategory(requestUri));
        AuthUserContext authUserContext = AuthContextHolder.get();
        if (authUserContext != null) {
            systemAuditLog.setOperator_id(authUserContext.getUserId());
            systemAuditLog.setOperator_name(authUserContext.getUsername());
        }
        systemAuditLog.setClient_ip(resolveClientIp(request));
        systemAuditLog.setRequest_uri(requestUri);
        systemAuditLog.setRequest_method(request.getMethod());
        systemAuditLog.setRequest_body(buildRequestBody(args));
        systemAuditLog.setResponse_code(responseCode);
        systemAuditLog.setResponse_message(StringUtils.substring(StringUtils.defaultString(responseMessage), 0, 255));
        systemAuditLog.setCost_ms(Math.toIntExact(Math.min(Math.max(costMs, 0L), Integer.MAX_VALUE)));
        systemAuditLog.setCreated_at(new Date());
        try {
            systemAuditLogService.save(systemAuditLog);
        } catch (Exception e) {
            log.warn("persist system audit log failed, uri={}", requestUri, e);
        }
    }

    private String buildRequestBody(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        String requestBody = StringUtils.substring(StringUtils.join(args, ", "), 0, MAX_REQUEST_BODY_LENGTH);
        return StringUtils.defaultIfBlank(requestBody, null);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwardedFor)) {
            return StringUtils.substringBefore(forwardedFor, ",").trim();
        }
        return request.getRemoteAddr();
    }

    private boolean shouldAudit(String requestUri) {
        if (StringUtils.isBlank(requestUri)) {
            return false;
        }
        for (String prefix : AUDIT_URI_PREFIXES) {
            if (StringUtils.startsWith(requestUri, prefix)) {
                return true;
            }
        }
        return false;
    }

    private String resolveLogCategory(String requestUri) {
        if (StringUtils.contains(requestUri, "/login")) {
            return "LOGIN";
        }
        if (StringUtils.startsWith(requestUri, "/api/internal/ai/events")) {
            return "AI_CALLBACK";
        }
        if (StringUtils.startsWith(requestUri, "/api/alerts/")) {
            return "ALERT";
        }
        return "OP";
    }
}
