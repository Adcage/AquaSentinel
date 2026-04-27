package com.springboot.ratelimit;

import java.util.Optional;

import com.springboot.common.ErrorCode;
import com.springboot.exception.BusinessException;
import com.springboot.security.AuthContextHolder;
import com.springboot.security.AuthUserContext;

import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 限流 AOP 切面
 *
 * <p>拦截 @RateLimit 注解的方法，根据 keyType 构建限流 Key， 从 BucketFactory 获取令牌桶，尝试消费1个令牌。成功则放行，失败抛出
 * BusinessException。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final BucketFactory bucketFactory;

    private final RateLimitProperties properties;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        String key = buildKey(rateLimit);

        Bucket bucket =
                bucketFactory.getLocalBucket(
                        key,
                        rateLimit.capacity(),
                        rateLimit.refillRate(),
                        rateLimit.refillPeriodSeconds());

        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn(
                    "限流触发: key={}, 方法={}",
                    key,
                    ((MethodSignature) joinPoint.getSignature()).getMethod().getName());
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED, rateLimit.fallbackMessage());
        }

        return joinPoint.proceed();
    }

    private String buildKey(RateLimit rateLimit) {
        String prefix = "rate-limit:";
        String baseKey = rateLimit.key().isEmpty() ? "" : rateLimit.key() + ":";

        String keyType = rateLimit.keyType();
        String suffix;
        switch (keyType) {
            case "USER":
                AuthUserContext ctx = AuthContextHolder.get();
                suffix = ctx != null ? String.valueOf(ctx.getUserId()) : getClientIp();
                break;
            case "IP":
                suffix = getClientIp();
                break;
            case "GLOBAL":
                suffix = "global";
                break;
            default:
                suffix = getClientIp();
        }
        return prefix + baseKey + suffix;
    }

    private String getClientIp() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(attrs -> attrs instanceof ServletRequestAttributes)
                .map(attrs -> (ServletRequestAttributes) attrs)
                .map(ServletRequestAttributes::getRequest)
                .map(this::extractClientIpFromRequest)
                .orElse("unknown");
    }

    private String extractClientIpFromRequest(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
