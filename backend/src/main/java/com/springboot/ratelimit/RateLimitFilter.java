package com.springboot.ratelimit;

import java.io.IOException;

import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 全局限流 Filter
 *
 * <p>在 RequestIdFilter 之后执行，对每个 IP 地址设置全局令牌桶。 粗粒度防护，防止整体过载。细粒度接口限流通过 @RateLimit 注解实现。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final BucketFactory bucketFactory;

    private final RateLimitProperties properties;

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String key = "rate-limit:global:" + clientIp;

        Bucket bucket =
                bucketFactory.getLocalBucket(
                        key,
                        properties.getGlobalCapacity(),
                        properties.getGlobalRefillRate(),
                        properties.getGlobalRefillPeriodSeconds());

        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn("全局限流触发: ip={}, path={}", clientIp, request.getRequestURI());
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            BaseResponse<Void> body =
                    ResultUtils.error(ErrorCode.RATE_LIMIT_EXCEEDED, "请求过于频繁，请稍后重试");
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
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
