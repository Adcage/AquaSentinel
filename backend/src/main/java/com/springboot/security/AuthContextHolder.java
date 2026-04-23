package com.springboot.security;

import com.springboot.common.ErrorCode;
import com.springboot.exception.BusinessException;

public final class AuthContextHolder {

    private static final ThreadLocal<AuthUserContext> CONTEXT = new ThreadLocal<>();

    private AuthContextHolder() {}

    public static void set(AuthUserContext authUserContext) {
        CONTEXT.set(authUserContext);
    }

    public static AuthUserContext get() {
        return CONTEXT.get();
    }

    public static AuthUserContext getRequired() {
        AuthUserContext context = get();
        if (context == null || context.getUserId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        return context;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
