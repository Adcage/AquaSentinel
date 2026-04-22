package com.springboot.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class TokenHashUtils {

    private TokenHashUtils() {
    }

    public static String sha256(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("token hash failed", e);
        }
    }
}
