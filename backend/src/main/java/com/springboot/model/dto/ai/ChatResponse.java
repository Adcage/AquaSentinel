package com.springboot.model.dto.ai;

import lombok.Data;

/** AI聊天响应 */
@Data
public class ChatResponse {

    private Long conversationId;

    private String message;

    private String functionName;

    private String functionArgs;

    private String functionResult;

    private Long userMessageId;

    private Long assistantMessageId;
}
