package com.springboot.model.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** AI聊天请求 */
@Data
public class ChatRequest {

    private Long conversationId;

    @NotBlank(message = "消息内容不能为空")
    private String message;
}
