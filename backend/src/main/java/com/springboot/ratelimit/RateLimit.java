package com.springboot.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 *
 * <p>用于标记需要限流的 Controller 方法。支持按用户、IP 或全局限流。
 *
 * <p>参数说明：
 *
 * <ul>
 *   <li>capacity: 令牌桶容量
 *   <li>refillRate: 每 refillPeriodSeconds 秒补充的令牌数
 *   <li>refillPeriodSeconds: 补充周期（秒）
 *   <li>key: 自定义限流 Key（可选）
 *   <li>keyType: 限流 Key 类型，可选 USER/IP/GLOBAL
 *   <li>fallbackMessage: 限流触发时的提示消息
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    int capacity() default 10;

    int refillRate() default 10;

    int refillPeriodSeconds() default 1;

    String key() default "";

    String keyType() default "USER";

    String fallbackMessage() default "请求过于频繁，请稍后重试";
}
