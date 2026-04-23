package com.springboot.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.springboot.common.ErrorCode;
import com.springboot.config.AppSecurityProperties;
import com.springboot.exception.BusinessException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_USERNAME = "username";

    private static final String CLAIM_ROLE_CODES = "roleCodes";

    private static final String CLAIM_TOKEN_TYPE = "tokenType";

    private final AppSecurityProperties appSecurityProperties;

    private final SecretKey key;

    public JwtTokenProvider(AppSecurityProperties appSecurityProperties) {
        this.appSecurityProperties = appSecurityProperties;
        String secret = appSecurityProperties.getJwt().getSecret();
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String username, List<String> roleCodes) {
        Instant now = Instant.now();
        Instant expireAt =
                now.plusSeconds(appSecurityProperties.getJwt().getAccessTokenExpireSeconds());
        return Jwts.builder()
                .issuer(appSecurityProperties.getJwt().getIssuer())
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE_CODES, roleCodes)
                .claim(CLAIM_TOKEN_TYPE, "ACCESS")
                .signWith(key)
                .compact();
    }

    public String generateRefreshTokenValue() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    public AuthUserContext parseAccessToken(String accessToken) {
        Claims claims = parseToken(accessToken);
        if (!"ACCESS".equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "令牌类型错误");
        }
        AuthUserContext authUserContext = new AuthUserContext();
        authUserContext.setUserId(parseLong(claims.getSubject()));
        authUserContext.setUsername(claims.get(CLAIM_USERNAME, String.class));
        Object roleClaim = claims.get(CLAIM_ROLE_CODES);
        Set<String> roleCodes = new HashSet<>();
        if (roleClaim instanceof List<?> claimList) {
            for (Object item : claimList) {
                if (item != null && StringUtils.isNotBlank(item.toString())) {
                    roleCodes.add(item.toString());
                }
            }
        }
        authUserContext.setRoleCodes(roleCodes);
        return authUserContext;
    }

    public long getAccessTokenExpireSeconds() {
        return appSecurityProperties.getJwt().getAccessTokenExpireSeconds();
    }

    public long getRefreshTokenExpireSeconds() {
        return appSecurityProperties.getJwt().getRefreshTokenExpireSeconds();
    }

    private Claims parseToken(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "访问令牌已过期");
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "访问令牌无效");
        }
    }

    private Long parseLong(String text) {
        try {
            return Long.parseLong(text);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "访问令牌无效");
        }
    }
}
