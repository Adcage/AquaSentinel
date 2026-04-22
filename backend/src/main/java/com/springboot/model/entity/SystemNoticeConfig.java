package com.springboot.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@TableName(value = "system_notice_config")
@Data
public class SystemNoticeConfig implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "off_duty_threshold_sec")
    private Integer off_duty_threshold_sec;

    @TableField(value = "device_offline_threshold_sec")
    private Integer device_offline_threshold_sec;

    @TableField(value = "drowning_alert_threshold_sec")
    private Integer drowning_alert_threshold_sec;

    @TableField(value = "created_at")
    private Date created_at;

    @TableField(value = "updated_at")
    private Date updated_at;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
