package com.springboot.model.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 场馆区域表 @TableName venue_zone */
@TableName(value = "venue_zone")
@Data
public class VenueZone implements Serializable {
    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 场馆ID */
    @TableField(value = "venue_id")
    private Long venue_id;

    /** 区域编码 */
    @TableField(value = "zone_code")
    private String zone_code;

    /** 区域名称 */
    @TableField(value = "zone_name")
    private String zone_name;

    /** 区域类型 */
    @TableField(value = "zone_type")
    private String zone_type;

    /** 区域GeoJSON */
    @TableField(value = "geo_json")
    private Object geo_json;

    /** 风险等级 */
    @TableField(value = "risk_level")
    private String risk_level;

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
        VenueZone other = (VenueZone) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getVenue_id() == null
                        ? other.getVenue_id() == null
                        : this.getVenue_id().equals(other.getVenue_id()))
                && (this.getZone_code() == null
                        ? other.getZone_code() == null
                        : this.getZone_code().equals(other.getZone_code()))
                && (this.getZone_name() == null
                        ? other.getZone_name() == null
                        : this.getZone_name().equals(other.getZone_name()))
                && (this.getZone_type() == null
                        ? other.getZone_type() == null
                        : this.getZone_type().equals(other.getZone_type()))
                && (this.getGeo_json() == null
                        ? other.getGeo_json() == null
                        : this.getGeo_json().equals(other.getGeo_json()))
                && (this.getRisk_level() == null
                        ? other.getRisk_level() == null
                        : this.getRisk_level().equals(other.getRisk_level()))
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
        result = prime * result + ((getZone_code() == null) ? 0 : getZone_code().hashCode());
        result = prime * result + ((getZone_name() == null) ? 0 : getZone_name().hashCode());
        result = prime * result + ((getZone_type() == null) ? 0 : getZone_type().hashCode());
        result = prime * result + ((getGeo_json() == null) ? 0 : getGeo_json().hashCode());
        result = prime * result + ((getRisk_level() == null) ? 0 : getRisk_level().hashCode());
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
        sb.append(", zone_code=").append(zone_code);
        sb.append(", zone_name=").append(zone_name);
        sb.append(", zone_type=").append(zone_type);
        sb.append(", geo_json=").append(geo_json);
        sb.append(", risk_level=").append(risk_level);
        sb.append(", created_at=").append(created_at);
        sb.append(", updated_at=").append(updated_at);
        sb.append(", is_delete=").append(is_delete);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
