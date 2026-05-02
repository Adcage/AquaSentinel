package com.springboot.model.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** AI对话消息表 */
@TableName(value = "ai_chat_message")
@Data
public class AiChatMessage implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "conversation_id")
    private Long conversation_id;

    @TableField(value = "role")
    private String role;

    @TableField(value = "content")
    private String content;

    @TableField(value = "function_name")
    private String function_name;

    @TableField(value = "function_args")
    private String function_args;

    @TableField(value = "function_result")
    private String function_result;

    @TableField(value = "tokens_used")
    private Integer tokens_used;

    @TableField(value = "created_at")
    private Date created_at;

    @TableField(value = "is_delete")
    private Integer is_delete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
