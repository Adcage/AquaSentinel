package com.springboot.model.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 报警向量嵌入表 */
@TableName(value = "alert_embedding")
@Data
public class AlertEmbedding implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "alert_id")
    private Long alert_id;

    @TableField(value = "alert_uid")
    private String alert_uid;

    @TableField(value = "source_text")
    private String source_text;

    @TableField(value = "embedding")
    private String embedding;

    @TableField(value = "embedding_model")
    private String embedding_model;

    @TableField(value = "similarity_search_text")
    private String similarity_search_text;

    @TableField(value = "created_at")
    private Date created_at;

    @TableField(value = "updated_at")
    private Date updated_at;

    @TableField(value = "is_delete")
    private Integer is_delete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
