package com.springboot.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 监控事件表
 * @TableName monitoring_event
 */
@TableName(value ="monitoring_event")
@Data
public class MonitoringEvent implements Serializable {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 事件唯一ID(幂等)
     */
    @TableField(value = "event_uid")
    private String event_uid;

    /**
     * 摄像头ID
     */
    @TableField(value = "camera_id")
    private Long camera_id;

    /**
     * 任务ID
     */
    @TableField(value = "task_id")
    private Long task_id;

    /**
     * 事件类型
     */
    @TableField(value = "event_type")
    private String event_type;

    /**
     * 风险等级
     */
    @TableField(value = "risk_level")
    private String risk_level;

    /**
     * 置信度
     */
    @TableField(value = "confidence")
    private BigDecimal confidence;

    /**
     * 跟踪目标ID
     */
    @TableField(value = "target_id")
    private String target_id;

    /**
     * 泳池人数
     */
    @TableField(value = "pool_head_count")
    private Integer pool_head_count;

    /**
     * 标注框
     */
    @TableField(value = "bbox_json")
    private Object bbox_json;

    /**
     * 位置描述
     */
    @TableField(value = "position_desc")
    private String position_desc;

    /**
     * 紧急联系人
     */
    @TableField(value = "emergency_contact_name")
    private String emergency_contact_name;

    /**
     * 紧急联系人电话
     */
    @TableField(value = "emergency_contact_phone")
    private String emergency_contact_phone;

    /**
     * 事发具体位置
     */
    @TableField(value = "incident_location")
    private String incident_location;

    /**
     * 视频流地址
     */
    @TableField(value = "video_stream_url")
    private String video_stream_url;

    /**
     * 事件发生时间
     */
    @TableField(value = "event_time")
    private Date event_time;

    /**
     * 扩展字段
     */
    @TableField(value = "ext_json")
    private Object ext_json;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private Date created_at;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        MonitoringEvent other = (MonitoringEvent) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getEvent_uid() == null ? other.getEvent_uid() == null : this.getEvent_uid().equals(other.getEvent_uid()))
            && (this.getCamera_id() == null ? other.getCamera_id() == null : this.getCamera_id().equals(other.getCamera_id()))
            && (this.getTask_id() == null ? other.getTask_id() == null : this.getTask_id().equals(other.getTask_id()))
            && (this.getEvent_type() == null ? other.getEvent_type() == null : this.getEvent_type().equals(other.getEvent_type()))
            && (this.getRisk_level() == null ? other.getRisk_level() == null : this.getRisk_level().equals(other.getRisk_level()))
            && (this.getConfidence() == null ? other.getConfidence() == null : this.getConfidence().equals(other.getConfidence()))
            && (this.getTarget_id() == null ? other.getTarget_id() == null : this.getTarget_id().equals(other.getTarget_id()))
            && (this.getPool_head_count() == null ? other.getPool_head_count() == null : this.getPool_head_count().equals(other.getPool_head_count()))
            && (this.getBbox_json() == null ? other.getBbox_json() == null : this.getBbox_json().equals(other.getBbox_json()))
            && (this.getPosition_desc() == null ? other.getPosition_desc() == null : this.getPosition_desc().equals(other.getPosition_desc()))
            && (this.getEmergency_contact_name() == null ? other.getEmergency_contact_name() == null : this.getEmergency_contact_name().equals(other.getEmergency_contact_name()))
            && (this.getEmergency_contact_phone() == null ? other.getEmergency_contact_phone() == null : this.getEmergency_contact_phone().equals(other.getEmergency_contact_phone()))
            && (this.getIncident_location() == null ? other.getIncident_location() == null : this.getIncident_location().equals(other.getIncident_location()))
            && (this.getVideo_stream_url() == null ? other.getVideo_stream_url() == null : this.getVideo_stream_url().equals(other.getVideo_stream_url()))
            && (this.getEvent_time() == null ? other.getEvent_time() == null : this.getEvent_time().equals(other.getEvent_time()))
            && (this.getExt_json() == null ? other.getExt_json() == null : this.getExt_json().equals(other.getExt_json()))
            && (this.getCreated_at() == null ? other.getCreated_at() == null : this.getCreated_at().equals(other.getCreated_at()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getEvent_uid() == null) ? 0 : getEvent_uid().hashCode());
        result = prime * result + ((getCamera_id() == null) ? 0 : getCamera_id().hashCode());
        result = prime * result + ((getTask_id() == null) ? 0 : getTask_id().hashCode());
        result = prime * result + ((getEvent_type() == null) ? 0 : getEvent_type().hashCode());
        result = prime * result + ((getRisk_level() == null) ? 0 : getRisk_level().hashCode());
        result = prime * result + ((getConfidence() == null) ? 0 : getConfidence().hashCode());
        result = prime * result + ((getTarget_id() == null) ? 0 : getTarget_id().hashCode());
        result = prime * result + ((getPool_head_count() == null) ? 0 : getPool_head_count().hashCode());
        result = prime * result + ((getBbox_json() == null) ? 0 : getBbox_json().hashCode());
        result = prime * result + ((getPosition_desc() == null) ? 0 : getPosition_desc().hashCode());
        result = prime * result + ((getEmergency_contact_name() == null) ? 0 : getEmergency_contact_name().hashCode());
        result = prime * result + ((getEmergency_contact_phone() == null) ? 0 : getEmergency_contact_phone().hashCode());
        result = prime * result + ((getIncident_location() == null) ? 0 : getIncident_location().hashCode());
        result = prime * result + ((getVideo_stream_url() == null) ? 0 : getVideo_stream_url().hashCode());
        result = prime * result + ((getEvent_time() == null) ? 0 : getEvent_time().hashCode());
        result = prime * result + ((getExt_json() == null) ? 0 : getExt_json().hashCode());
        result = prime * result + ((getCreated_at() == null) ? 0 : getCreated_at().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", event_uid=").append(event_uid);
        sb.append(", camera_id=").append(camera_id);
        sb.append(", task_id=").append(task_id);
        sb.append(", event_type=").append(event_type);
        sb.append(", risk_level=").append(risk_level);
        sb.append(", confidence=").append(confidence);
        sb.append(", target_id=").append(target_id);
        sb.append(", pool_head_count=").append(pool_head_count);
        sb.append(", bbox_json=").append(bbox_json);
        sb.append(", position_desc=").append(position_desc);
        sb.append(", emergency_contact_name=").append(emergency_contact_name);
        sb.append(", emergency_contact_phone=").append(emergency_contact_phone);
        sb.append(", incident_location=").append(incident_location);
        sb.append(", video_stream_url=").append(video_stream_url);
        sb.append(", event_time=").append(event_time);
        sb.append(", ext_json=").append(ext_json);
        sb.append(", created_at=").append(created_at);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}