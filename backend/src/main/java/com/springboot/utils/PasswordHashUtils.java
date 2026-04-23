package com.springboot.utils;

import org.springframework.util.DigestUtils;

public final class PasswordHashUtils {

    private static final String SALT = "springboot";

    private PasswordHashUtils() {}

    public static String md5WithSalt(String plainPassword) {
        return DigestUtils.md5DigestAsHex((SALT + plainPassword).getBytes());
    }
}
