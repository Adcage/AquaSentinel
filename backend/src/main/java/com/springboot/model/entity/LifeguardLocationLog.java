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
 * 救生员定位上报表
 * @TableName lifeguard_location_log
 */
@TableName(value ="lifeguard_location_log")
@Data
public class LifeguardLocationLog implements Serializable {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 救生员ID
     */
    @TableField(value = "lifeguard_id")
    private Long lifeguard_id;

    /**
     * 场馆ID
     */
    @TableField(value = "venue_id")
    private Long venue_id;

    /**
     * 经度
     */
    @TableField(value = "longitude")
    private BigDecimal longitude;

    /**
     * 纬度
     */
    @TableField(value = "latitude")
    private BigDecimal latitude;

    /**
     * 是否在围栏内
     */
    @TableField(value = "in_fence")
    private Integer in_fence;

    /**
     * 上报来源
     */
    @TableField(value = "report_source")
    private String report_source;

    /**
     * 上报时间
     */
    @TableField(value = "reported_at")
    private Date reported_at;

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
        LifeguardLocationLog other = (LifeguardLocationLog) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getLifeguard_id() == null ? other.getLifeguard_id() == null : this.getLifeguard_id().equals(other.getLifeguard_id()))
            && (this.getVenue_id() == null ? other.getVenue_id() == null : this.getVenue_id().equals(other.getVenue_id()))
            && (this.getLongitude() == null ? other.getLongitude() == null : this.getLongitude().equals(other.getLongitude()))
            && (this.getLatitude() == null ? other.getLatitude() == null : this.getLatitude().equals(other.getLatitude()))
            && (this.getIn_fence() == null ? other.getIn_fence() == null : this.getIn_fence().equals(other.getIn_fence()))
            && (this.getReport_source() == null ? other.getReport_source() == null : this.getReport_source().equals(other.getReport_source()))
            && (this.getReported_at() == null ? other.getReported_at() == null : this.getReported_at().equals(other.getReported_at()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getLifeguard_id() == null) ? 0 : getLifeguard_id().hashCode());
        result = prime * result + ((getVenue_id() == null) ? 0 : getVenue_id().hashCode());
        result = prime * result + ((getLongitude() == null) ? 0 : getLongitude().hashCode());
        result = prime * result + ((getLatitude() == null) ? 0 : getLatitude().hashCode());
        result = prime * result + ((getIn_fence() == null) ? 0 : getIn_fence().hashCode());
        result = prime * result + ((getReport_source() == null) ? 0 : getReport_source().hashCode());
        result = prime * result + ((getReported_at() == null) ? 0 : getReported_at().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", lifeguard_id=").append(lifeguard_id);
        sb.append(", venue_id=").append(venue_id);
        sb.append(", longitude=").append(longitude);
        sb.append(", latitude=").append(latitude);
        sb.append(", in_fence=").append(in_fence);
        sb.append(", report_source=").append(report_source);
        sb.append(", reported_at=").append(reported_at);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}