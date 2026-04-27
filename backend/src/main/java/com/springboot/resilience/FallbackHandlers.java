package com.springboot.resilience;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Resilience4j 降级处理器
 *
 * <p>提供各 Resilience4j 实例的 fallback 方法，供注解的 fallbackMethod 属性引用。
 * 每个方法只记录日志，不做业务补偿——报警类操作通过消息队列保证最终一致性。
 */
@Slf4j
@Component
public class FallbackHandlers {

    public void onYoloCallbackFailure(Throwable t) {
        log.error("YOLO 服务回调失败，降级处理: {}", t.getMessage());
    }

    public void onDeviceControlFailure(Throwable t) {
        log.error("设备控制调用失败，降级处理: {}", t.getMessage());
    }

    public Void onAiEngineQueryFailure(Throwable t) {
        log.error("AI 引擎查询失败，降级处理: {}", t.getMessage());
        return null;
    }
}
