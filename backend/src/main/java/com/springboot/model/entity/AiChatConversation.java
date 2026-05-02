package com.springboot.model.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** AI对话会话表 */
@TableName(value = "ai_chat_conversation")
@Data
public class AiChatConversation implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "user_id")
    private Long user_id;

    @TableField(value = "title")
    private String title;

    @TableField(value = "created_at")
    private Date created_at;

    @TableField(value = "updated_at")
    private Date updated_at;

    @TableField(value = "is_delete")
    private Integer is_delete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
