package com.springboot.security;

import java.util.List;

import com.springboot.common.ErrorCode;
import com.springboot.exception.BusinessException;
import com.springboot.service.AccessControlService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_PREFIX = "Bearer ";

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<String> WHITE_LIST =
            List.of(
                    "/api/auth/login",
                    "/api/auth/admin/login",
                    "/api/auth/register",
                    "/api/auth/refresh",
                    "/api/auth/captcha",
                    "/api/lifeguards/login",
                    "/api/streams/**",
                    "/api/internal/**",
                    "/api/ws/**",
                    "/api/doc.html",
                    "/api/webjars/**",
                    "/api/v3/api-docs/**",
                    "/api/error",
                    "/error");

    private final JwtTokenProvider jwtTokenProvider;

    private final AccessControlService accessControlService;

    public JwtAuthInterceptor(
            JwtTokenProvider jwtTokenProvider, AccessControlService accessControlService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.accessControlService = accessControlService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String requestPath = request.getRequestURI();
        if (isWhiteListed(requestPath)) {
            return true;
        }
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.isBlank(authorization)
                || !StringUtils.startsWithIgnoreCase(authorization, BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未携带访问令牌");
        }
        String token = StringUtils.substringAfter(authorization, BEARER_PREFIX).trim();
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "访问令牌为空");
        }
        AuthUserContext authUserContext = jwtTokenProvider.parseAccessToken(token);
        authUserContext.setPermissionCodes(
                accessControlService.listPermissionsByRoleCodes(authUserContext.getRoleCodes()));
        AuthContextHolder.set(authUserContext);
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        AuthContextHolder.clear();
    }

    private boolean isWhiteListed(String requestPath) {
        for (String pattern : WHITE_LIST) {
            if (PATH_MATCHER.match(pattern, requestPath)) {
                return true;
            }
        }
        return false;
    }
}
