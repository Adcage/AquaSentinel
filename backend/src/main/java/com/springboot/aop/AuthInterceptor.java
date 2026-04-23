package com.springboot.aop;

import java.util.Arrays;

import com.springboot.annotation.AuthCheck;
import com.springboot.common.ErrorCode;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.security.AuthContextHolder;
import com.springboot.security.AuthUserContext;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/** 权限校验 AOP */
@Aspect
@Component
public class AuthInterceptor {

    /**
     * 执行拦截
     *
     * @param joinPoint
     * @param authCheck
     * @return
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck)
            throws Throwable {
        String mustRole = authCheck.mustRole();
        String mustPermission = authCheck.mustPermission();

        AuthUserContext authUserContext = AuthContextHolder.get();
        if (authUserContext == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        boolean rolePass =
                StringUtils.isBlank(mustRole)
                        || authUserContext.hasRole(RoleConstant.SUPER_ADMIN)
                        || Arrays.stream(mustRole.split(","))
                                .map(String::trim)
                                .anyMatch(authUserContext::hasRole);
        if (!rolePass) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean permissionPass =
                StringUtils.isBlank(mustPermission)
                        || authUserContext.hasPermission(mustPermission);
        if (!permissionPass) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return joinPoint.proceed();
    }
}
