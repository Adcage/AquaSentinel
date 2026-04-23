package com.springboot.model.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 报警记录表 @TableName alert_record */
@TableName(value = "alert_record")
@Data
public class AlertRecord implements Serializable {
    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 报警唯一ID */
    @TableField(value = "alert_uid")
    private String alert_uid;

    /** 事件ID */
    @TableField(value = "event_id")
    private Long event_id;

    /** 摄像头ID */
    @TableField(value = "camera_id")
    private Long camera_id;

    /** 场馆ID */
    @TableField(value = "venue_id")
    private Long venue_id;

    /** 处理救生员ID */
    @TableField(value = "lifeguard_id")
    private Long lifeguard_id;

    /** 报警类型 */
    @TableField(value = "alert_type")
    private String alert_type;

    /** 报警状态 */
    @TableField(value = "alert_status")
    private String alert_status;

    /** 紧急联系人姓名 */
    @TableField(value = "emergency_contact_name")
    private String emergency_contact_name;

    /** 紧急联系人电话 */
    @TableField(value = "emergency_contact_phone")
    private String emergency_contact_phone;

    /** 事发具体位置 */
    @TableField(value = "incident_location")
    private String incident_location;

    /** 视频流地址 */
    @TableField(value = "video_stream_url")
    private String video_stream_url;

    /** 算法识别结果/检测摘要 */
    @TableField(value = "detection_result")
    private String detection_result;

    /** 是否推送到App */
    @TableField(value = "pushed_to_app")
    private Integer pushed_to_app;

    /** 是否推送到PC */
    @TableField(value = "pushed_to_pc")
    private Integer pushed_to_pc;

    /** 首次推送时间 */
    @TableField(value = "first_push_time")
    private Date first_push_time;

    /** 处理完成时间 */
    @TableField(value = "resolved_time")
    private Date resolved_time;

    /** 创建时间 */
    @TableField(value = "created_at")
    private Date created_at;

    /** 更新时间 */
    @TableField(value = "updated_at")
    private Date updated_at;

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
        AlertRecord other = (AlertRecord) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getAlert_uid() == null
                        ? other.getAlert_uid() == null
                        : this.getAlert_uid().equals(other.getAlert_uid()))
                && (this.getEvent_id() == null
                        ? other.getEvent_id() == null
                        : this.getEvent_id().equals(other.getEvent_id()))
                && (this.getCamera_id() == null
                        ? other.getCamera_id() == null
                        : this.getCamera_id().equals(other.getCamera_id()))
                && (this.getVenue_id() == null
                        ? other.getVenue_id() == null
                        : this.getVenue_id().equals(other.getVenue_id()))
                && (this.getLifeguard_id() == null
                        ? other.getLifeguard_id() == null
                        : this.getLifeguard_id().equals(other.getLifeguard_id()))
                && (this.getAlert_type() == null
                        ? other.getAlert_type() == null
                        : this.getAlert_type().equals(other.getAlert_type()))
                && (this.getAlert_status() == null
                        ? other.getAlert_status() == null
                        : this.getAlert_status().equals(other.getAlert_status()))
                && (this.getEmergency_contact_name() == null
                        ? other.getEmergency_contact_name() == null
                        : this.getEmergency_contact_name()
                                .equals(other.getEmergency_contact_name()))
                && (this.getEmergency_contact_phone() == null
                        ? other.getEmergency_contact_phone() == null
                        : this.getEmergency_contact_phone()
                                .equals(other.getEmergency_contact_phone()))
                && (this.getIncident_location() == null
                        ? other.getIncident_location() == null
                        : this.getIncident_location().equals(other.getIncident_location()))
                && (this.getVideo_stream_url() == null
                        ? other.getVideo_stream_url() == null
                        : this.getVideo_stream_url().equals(other.getVideo_stream_url()))
                && (this.getDetection_result() == null
                        ? other.getDetection_result() == null
                        : this.getDetection_result().equals(other.getDetection_result()))
                && (this.getPushed_to_app() == null
                        ? other.getPushed_to_app() == null
                        : this.getPushed_to_app().equals(other.getPushed_to_app()))
                && (this.getPushed_to_pc() == null
                        ? other.getPushed_to_pc() == null
                        : this.getPushed_to_pc().equals(other.getPushed_to_pc()))
                && (this.getFirst_push_time() == null
                        ? other.getFirst_push_time() == null
                        : this.getFirst_push_time().equals(other.getFirst_push_time()))
                && (this.getResolved_time() == null
                        ? other.getResolved_time() == null
                        : this.getResolved_time().equals(other.getResolved_time()))
                && (this.getCreated_at() == null
                        ? other.getCreated_at() == null
                        : this.getCreated_at().equals(other.getCreated_at()))
                && (this.getUpdated_at() == null
                        ? other.getUpdated_at() == null
                        : this.getUpdated_at().equals(other.getUpdated_at()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getAlert_uid() == null) ? 0 : getAlert_uid().hashCode());
        result = prime * result + ((getEvent_id() == null) ? 0 : getEvent_id().hashCode());
        result = prime * result + ((getCamera_id() == null) ? 0 : getCamera_id().hashCode());
        result = prime * result + ((getVenue_id() == null) ? 0 : getVenue_id().hashCode());
        result = prime * result + ((getLifeguard_id() == null) ? 0 : getLifeguard_id().hashCode());
        result = prime * result + ((getAlert_type() == null) ? 0 : getAlert_type().hashCode());
        result = prime * result + ((getAlert_status() == null) ? 0 : getAlert_status().hashCode());
        result =
                prime * result
                        + ((getEmergency_contact_name() == null)
                                ? 0
                                : getEmergency_contact_name().hashCode());
        result =
                prime * result
                        + ((getEmergency_contact_phone() == null)
                                ? 0
                                : getEmergency_contact_phone().hashCode());
        result =
                prime * result
                        + ((getIncident_location() == null)
                                ? 0
                                : getIncident_location().hashCode());
        result =
                prime * result
                        + ((getVideo_stream_url() == null) ? 0 : getVideo_stream_url().hashCode());
        result =
                prime * result
                        + ((getDetection_result() == null) ? 0 : getDetection_result().hashCode());
        result =
                prime * result + ((getPushed_to_app() == null) ? 0 : getPushed_to_app().hashCode());
        result = prime * result + ((getPushed_to_pc() == null) ? 0 : getPushed_to_pc().hashCode());
        result =
                prime * result
                        + ((getFirst_push_time() == null) ? 0 : getFirst_push_time().hashCode());
        result =
                prime * result + ((getResolved_time() == null) ? 0 : getResolved_time().hashCode());
        result = prime * result + ((getCreated_at() == null) ? 0 : getCreated_at().hashCode());
        result = prime * result + ((getUpdated_at() == null) ? 0 : getUpdated_at().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", alert_uid=").append(alert_uid);
        sb.append(", event_id=").append(event_id);
        sb.append(", camera_id=").append(camera_id);
        sb.append(", venue_id=").append(venue_id);
        sb.append(", lifeguard_id=").append(lifeguard_id);
        sb.append(", alert_type=").append(alert_type);
        sb.append(", alert_status=").append(alert_status);
        sb.append(", emergency_contact_name=").append(emergency_contact_name);
        sb.append(", emergency_contact_phone=").append(emergency_contact_phone);
        sb.append(", incident_location=").append(incident_location);
        sb.append(", video_stream_url=").append(video_stream_url);
        sb.append(", detection_result=").append(detection_result);
        sb.append(", pushed_to_app=").append(pushed_to_app);
        sb.append(", pushed_to_pc=").append(pushed_to_pc);
        sb.append(", first_push_time=").append(first_push_time);
        sb.append(", resolved_time=").append(resolved_time);
        sb.append(", created_at=").append(created_at);
        sb.append(", updated_at=").append(updated_at);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
