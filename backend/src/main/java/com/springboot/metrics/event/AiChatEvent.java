package com.springboot.metrics.event;

import org.springframework.context.ApplicationEvent;

/** AI对话事件 */
public class AiChatEvent extends ApplicationEvent {

    private final boolean success;
    private final String functionName;
    private final Integer tokensUsed;

    public AiChatEvent(Object source, boolean success, String functionName, Integer tokensUsed) {
        super(source);
        this.success = success;
        this.functionName = functionName;
        this.tokensUsed = tokensUsed;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getFunctionName() {
        return functionName;
    }

    public Integer getTokensUsed() {
        return tokensUsed;
    }
}
