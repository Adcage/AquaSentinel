package com.springboot.model.vo;

import lombok.Data;

/** AI对话消息VO */
@Data
public class AiChatMessageVO {

    private Long id;

    private String role;

    private String content;

    private String functionName;

    private String functionArgs;

    private String functionResult;

    private Integer tokensUsed;

    private String createdAt;
}
