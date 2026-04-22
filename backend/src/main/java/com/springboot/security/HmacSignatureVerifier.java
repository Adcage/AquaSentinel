package com.springboot.security;

import com.springboot.config.AppSecurityProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class HmacSignatureVerifier {

    private final AppSecurityProperties appSecurityProperties;

    public HmacSignatureVerifier(AppSecurityProperties appSecurityProperties) {
        this.appSecurityProperties = appSecurityProperties;
    }

    public boolean verify(String key, String timestamp, String signature, String body) {
        if (StringUtils.isAnyBlank(key, timestamp, signature)) {
            return false;
        }
        AppSecurityProperties.AiCallback aiCallback = appSecurityProperties.getAiCallback();
        if (!StringUtils.equals(aiCallback.getKey(), key)) {
            return false;
        }
        long requestTs;
        try {
            requestTs = parseEpochSeconds(timestamp);
        } catch (Exception e) {
            return false;
        }
        long nowTs = System.currentTimeMillis() / 1000;
        long allowedSkew = aiCallback.getAllowedSkewSeconds();
        if (Math.abs(nowTs - requestTs) > allowedSkew) {
            return false;
        }
        String payload = timestamp + "\n" + StringUtils.defaultString(body);
        String expected = hmacSha256Hex(aiCallback.getSecret(), payload);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    private long parseEpochSeconds(String timestamp) {
        long ts = Long.parseLong(timestamp);
        if (ts > 1_000_000_000_000L) {
            return ts / 1000;
        }
        return ts;
    }

    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
