package com.springboot.resilience;

import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnErrorEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Resilience4j 配置类
 *
 * <p>监听熔断器事件，记录状态变更和错误日志。实例注册由 YAML 配置驱动。
 */
@Slf4j
@Configuration
public class ResilienceConfig {

    @EventListener
    public void onCircuitBreakerError(CircuitBreakerOnErrorEvent event) {
        log.warn(
                "熔断器 {} 处理失败: {}",
                event.getCircuitBreakerName(),
                event.getThrowable().getMessage());
    }

    @EventListener
    public void onCircuitBreakerStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        log.warn(
                "熔断器 {} 状态变更: {} -> {}",
                event.getCircuitBreakerName(),
                event.getStateTransition().getFromState(),
                event.getStateTransition().getToState());
    }
}
