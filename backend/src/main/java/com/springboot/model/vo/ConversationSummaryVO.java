package com.springboot.model.vo;

import lombok.Data;

/** AI对话会话摘要VO */
@Data
public class ConversationSummaryVO {

    private Long id;

    private String title;

    private String createdAt;

    private String updatedAt;
}
