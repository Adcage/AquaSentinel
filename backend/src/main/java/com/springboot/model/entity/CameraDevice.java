package com.springboot.model.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 摄像头设备表 @TableName camera_device */
@TableName(value = "camera_device")
@Data
public class CameraDevice implements Serializable {
    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 场馆ID */
    @TableField(value = "venue_id")
    private Long venue_id;

    /** 区域ID */
    @TableField(value = "zone_id")
    private Long zone_id;

    /** 摄像头编码 */
    @TableField(value = "camera_code")
    private String camera_code;

    /** 摄像头名称 */
    @TableField(value = "camera_name")
    private String camera_name;

    /** 视频流地址 */
    @TableField(value = "stream_url")
    private String stream_url;

    /** 流协议 */
    @TableField(value = "protocol")
    private String protocol;

    /** 设备在线状态 */
    @TableField(value = "device_status")
    private String device_status;

    /** 健康状态 */
    @TableField(value = "health_status")
    private String health_status;

    /** 是否启用 */
    @TableField(value = "enabled")
    private Integer enabled;

    /** 最近心跳时间 */
    @TableField(value = "last_heartbeat_at")
    private Date last_heartbeat_at;

    /** 创建时间 */
    @TableField(value = "created_at")
    private Date created_at;

    /** 更新时间 */
    @TableField(value = "updated_at")
    private Date updated_at;

    /** 逻辑删除 */
    @TableField(value = "is_delete")
    private Integer is_delete;

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
        CameraDevice other = (CameraDevice) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getVenue_id() == null
                        ? other.getVenue_id() == null
                        : this.getVenue_id().equals(other.getVenue_id()))
                && (this.getZone_id() == null
                        ? other.getZone_id() == null
                        : this.getZone_id().equals(other.getZone_id()))
                && (this.getCamera_code() == null
                        ? other.getCamera_code() == null
                        : this.getCamera_code().equals(other.getCamera_code()))
                && (this.getCamera_name() == null
                        ? other.getCamera_name() == null
                        : this.getCamera_name().equals(other.getCamera_name()))
                && (this.getStream_url() == null
                        ? other.getStream_url() == null
                        : this.getStream_url().equals(other.getStream_url()))
                && (this.getProtocol() == null
                        ? other.getProtocol() == null
                        : this.getProtocol().equals(other.getProtocol()))
                && (this.getDevice_status() == null
                        ? other.getDevice_status() == null
                        : this.getDevice_status().equals(other.getDevice_status()))
                && (this.getHealth_status() == null
                        ? other.getHealth_status() == null
                        : this.getHealth_status().equals(other.getHealth_status()))
                && (this.getEnabled() == null
                        ? other.getEnabled() == null
                        : this.getEnabled().equals(other.getEnabled()))
                && (this.getLast_heartbeat_at() == null
                        ? other.getLast_heartbeat_at() == null
                        : this.getLast_heartbeat_at().equals(other.getLast_heartbeat_at()))
                && (this.getCreated_at() == null
                        ? other.getCreated_at() == null
                        : this.getCreated_at().equals(other.getCreated_at()))
                && (this.getUpdated_at() == null
                        ? other.getUpdated_at() == null
                        : this.getUpdated_at().equals(other.getUpdated_at()))
                && (this.getIs_delete() == null
                        ? other.getIs_delete() == null
                        : this.getIs_delete().equals(other.getIs_delete()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getVenue_id() == null) ? 0 : getVenue_id().hashCode());
        result = prime * result + ((getZone_id() == null) ? 0 : getZone_id().hashCode());
        result = prime * result + ((getCamera_code() == null) ? 0 : getCamera_code().hashCode());
        result = prime * result + ((getCamera_name() == null) ? 0 : getCamera_name().hashCode());
        result = prime * result + ((getStream_url() == null) ? 0 : getStream_url().hashCode());
        result = prime * result + ((getProtocol() == null) ? 0 : getProtocol().hashCode());
        result =
                prime * result + ((getDevice_status() == null) ? 0 : getDevice_status().hashCode());
        result =
                prime * result + ((getHealth_status() == null) ? 0 : getHealth_status().hashCode());
        result = prime * result + ((getEnabled() == null) ? 0 : getEnabled().hashCode());
        result =
                prime * result
                        + ((getLast_heartbeat_at() == null)
                                ? 0
                                : getLast_heartbeat_at().hashCode());
        result = prime * result + ((getCreated_at() == null) ? 0 : getCreated_at().hashCode());
        result = prime * result + ((getUpdated_at() == null) ? 0 : getUpdated_at().hashCode());
        result = prime * result + ((getIs_delete() == null) ? 0 : getIs_delete().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", venue_id=").append(venue_id);
        sb.append(", zone_id=").append(zone_id);
        sb.append(", camera_code=").append(camera_code);
        sb.append(", camera_name=").append(camera_name);
        sb.append(", stream_url=").append(stream_url);
        sb.append(", protocol=").append(protocol);
        sb.append(", device_status=").append(device_status);
        sb.append(", health_status=").append(health_status);
        sb.append(", enabled=").append(enabled);
        sb.append(", last_heartbeat_at=").append(last_heartbeat_at);
        sb.append(", created_at=").append(created_at);
        sb.append(", updated_at=").append(updated_at);
        sb.append(", is_delete=").append(is_delete);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
