package com.springboot.model.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 环境传感器采样表 @TableName env_sensor_sample */
@TableName(value = "env_sensor_sample")
@Data
public class EnvSensorSample implements Serializable {
    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 场馆ID */
    @TableField(value = "venue_id")
    private Long venue_id;

    /** 区域ID */
    @TableField(value = "zone_id")
    private Long zone_id;

    /** 传感器编码 */
    @TableField(value = "sensor_code")
    private String sensor_code;

    /** 水温 */
    @TableField(value = "water_temperature")
    private BigDecimal water_temperature;

    /** 湿度 */
    @TableField(value = "humidity")
    private BigDecimal humidity;

    /** 数据质量标记 */
    @TableField(value = "quality_flag")
    private String quality_flag;

    /** 采样时间 */
    @TableField(value = "sample_time")
    private Date sample_time;

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
        EnvSensorSample other = (EnvSensorSample) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getVenue_id() == null
                        ? other.getVenue_id() == null
                        : this.getVenue_id().equals(other.getVenue_id()))
                && (this.getZone_id() == null
                        ? other.getZone_id() == null
                        : this.getZone_id().equals(other.getZone_id()))
                && (this.getSensor_code() == null
                        ? other.getSensor_code() == null
                        : this.getSensor_code().equals(other.getSensor_code()))
                && (this.getWater_temperature() == null
                        ? other.getWater_temperature() == null
                        : this.getWater_temperature().equals(other.getWater_temperature()))
                && (this.getHumidity() == null
                        ? other.getHumidity() == null
                        : this.getHumidity().equals(other.getHumidity()))
                && (this.getQuality_flag() == null
                        ? other.getQuality_flag() == null
                        : this.getQuality_flag().equals(other.getQuality_flag()))
                && (this.getSample_time() == null
                        ? other.getSample_time() == null
                        : this.getSample_time().equals(other.getSample_time()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getVenue_id() == null) ? 0 : getVenue_id().hashCode());
        result = prime * result + ((getZone_id() == null) ? 0 : getZone_id().hashCode());
        result = prime * result + ((getSensor_code() == null) ? 0 : getSensor_code().hashCode());
        result =
                prime * result
                        + ((getWater_temperature() == null)
                                ? 0
                                : getWater_temperature().hashCode());
        result = prime * result + ((getHumidity() == null) ? 0 : getHumidity().hashCode());
        result = prime * result + ((getQuality_flag() == null) ? 0 : getQuality_flag().hashCode());
        result = prime * result + ((getSample_time() == null) ? 0 : getSample_time().hashCode());
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
        sb.append(", sensor_code=").append(sensor_code);
        sb.append(", water_temperature=").append(water_temperature);
        sb.append(", humidity=").append(humidity);
        sb.append(", quality_flag=").append(quality_flag);
        sb.append(", sample_time=").append(sample_time);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
