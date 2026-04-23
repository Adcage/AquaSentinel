package com.springboot.model.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 统计快照表 @TableName stats_snapshot */
@TableName(value = "stats_snapshot")
@Data
public class StatsSnapshot implements Serializable {
    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 粒度:HOUR/DAY */
    @TableField(value = "granularity")
    private String granularity;

    /** 快照日期 */
    @TableField(value = "snapshot_date")
    private Date snapshot_date;

    /** 小时(0-23,DAT粒度可空) */
    @TableField(value = "snapshot_hour")
    private Integer snapshot_hour;

    /** 场馆ID */
    @TableField(value = "venue_id")
    private Long venue_id;

    /** 指标类型 */
    @TableField(value = "metric_type")
    private String metric_type;

    /** 指标键 */
    @TableField(value = "metric_key")
    private String metric_key;

    /** 指标值 */
    @TableField(value = "metric_value")
    private BigDecimal metric_value;

    /** 维度JSON */
    @TableField(value = "dimension_json")
    private Object dimension_json;

    /** 创建时间 */
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
        StatsSnapshot other = (StatsSnapshot) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getGranularity() == null
                        ? other.getGranularity() == null
                        : this.getGranularity().equals(other.getGranularity()))
                && (this.getSnapshot_date() == null
                        ? other.getSnapshot_date() == null
                        : this.getSnapshot_date().equals(other.getSnapshot_date()))
                && (this.getSnapshot_hour() == null
                        ? other.getSnapshot_hour() == null
                        : this.getSnapshot_hour().equals(other.getSnapshot_hour()))
                && (this.getVenue_id() == null
                        ? other.getVenue_id() == null
                        : this.getVenue_id().equals(other.getVenue_id()))
                && (this.getMetric_type() == null
                        ? other.getMetric_type() == null
                        : this.getMetric_type().equals(other.getMetric_type()))
                && (this.getMetric_key() == null
                        ? other.getMetric_key() == null
                        : this.getMetric_key().equals(other.getMetric_key()))
                && (this.getMetric_value() == null
                        ? other.getMetric_value() == null
                        : this.getMetric_value().equals(other.getMetric_value()))
                && (this.getDimension_json() == null
                        ? other.getDimension_json() == null
                        : this.getDimension_json().equals(other.getDimension_json()))
                && (this.getCreated_at() == null
                        ? other.getCreated_at() == null
                        : this.getCreated_at().equals(other.getCreated_at()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getGranularity() == null) ? 0 : getGranularity().hashCode());
        result =
                prime * result + ((getSnapshot_date() == null) ? 0 : getSnapshot_date().hashCode());
        result =
                prime * result + ((getSnapshot_hour() == null) ? 0 : getSnapshot_hour().hashCode());
        result = prime * result + ((getVenue_id() == null) ? 0 : getVenue_id().hashCode());
        result = prime * result + ((getMetric_type() == null) ? 0 : getMetric_type().hashCode());
        result = prime * result + ((getMetric_key() == null) ? 0 : getMetric_key().hashCode());
        result = prime * result + ((getMetric_value() == null) ? 0 : getMetric_value().hashCode());
        result =
                prime * result
                        + ((getDimension_json() == null) ? 0 : getDimension_json().hashCode());
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
        sb.append(", granularity=").append(granularity);
        sb.append(", snapshot_date=").append(snapshot_date);
        sb.append(", snapshot_hour=").append(snapshot_hour);
        sb.append(", venue_id=").append(venue_id);
        sb.append(", metric_type=").append(metric_type);
        sb.append(", metric_key=").append(metric_key);
        sb.append(", metric_value=").append(metric_value);
        sb.append(", dimension_json=").append(dimension_json);
        sb.append(", created_at=").append(created_at);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
