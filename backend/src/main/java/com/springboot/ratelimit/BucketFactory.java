package com.springboot.ratelimit;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.distributed.BucketProxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 令牌桶工厂
 *
 * <p>提供两种令牌桶实现：
 *
 * <ul>
 *   <li>分布式桶：基于 Redis + Redisson，多实例共享限流额度（需手动配置）
 *   <li>本地桶：基于 ConcurrentHashMap，用于开发或 Redis 不可用时降级
 * </ul>
 *
 * <p>当前版本默认使用本地桶实现。分布式限流需要 Redisson 的 CommandAsyncExecutor， 可在后续版本中启用。
 */
@Slf4j
@Component
public class BucketFactory {

    private final ConcurrentHashMap<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    public BucketFactory() {
        log.info("BucketFactory 初始化完成，使用本地令牌桶实现");
    }

    public BucketProxy getDistributedBucket(
            String key, int capacity, int refillRate, int refillPeriodSeconds) {
        throw new UnsupportedOperationException(
                "分布式限流未启用，请在 application.yml 中设置 app.rate-limit.distributed=false");
    }

    public Bucket getLocalBucket(
            String key, int capacity, int refillRate, int refillPeriodSeconds) {
        return localBuckets.computeIfAbsent(
                key,
                k -> {
                    Bandwidth bandwidth =
                            Bandwidth.builder()
                                    .capacity(capacity)
                                    .refillGreedy(
                                            refillRate, Duration.ofSeconds(refillPeriodSeconds))
                                    .initialTokens(capacity)
                                    .build();
                    return Bucket.builder().addLimit(bandwidth).build();
                });
    }

    public boolean isDistributedEnabled() {
        return false;
    }
}
