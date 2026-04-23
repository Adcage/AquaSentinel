package com.springboot.model.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 场馆表 @TableName venue */
@TableName(value = "venue")
@Data
public class Venue implements Serializable {
    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 场馆编码 */
    @TableField(value = "venue_code")
    private String venue_code;

    /** 场馆名称 */
    @TableField(value = "venue_name")
    private String venue_name;

    /** 详细地址 */
    @TableField(value = "address")
    private String address;

    /** 紧急联系人姓名 */
    @TableField(value = "contact_name")
    private String contact_name;

    /** 紧急联系人电话 */
    @TableField(value = "contact_phone")
    private String contact_phone;

    /** 时区 */
    @TableField(value = "timezone")
    private String timezone;

    /** 状态:1启用,0禁用 */
    @TableField(value = "status")
    private Integer status;

    /** 电子围栏GeoJSON */
    @TableField(value = "fence_geo_json")
    private Object fence_geo_json;

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
        Venue other = (Venue) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getVenue_code() == null
                        ? other.getVenue_code() == null
                        : this.getVenue_code().equals(other.getVenue_code()))
                && (this.getVenue_name() == null
                        ? other.getVenue_name() == null
                        : this.getVenue_name().equals(other.getVenue_name()))
                && (this.getAddress() == null
                        ? other.getAddress() == null
                        : this.getAddress().equals(other.getAddress()))
                && (this.getContact_name() == null
                        ? other.getContact_name() == null
                        : this.getContact_name().equals(other.getContact_name()))
                && (this.getContact_phone() == null
                        ? other.getContact_phone() == null
                        : this.getContact_phone().equals(other.getContact_phone()))
                && (this.getTimezone() == null
                        ? other.getTimezone() == null
                        : this.getTimezone().equals(other.getTimezone()))
                && (this.getStatus() == null
                        ? other.getStatus() == null
                        : this.getStatus().equals(other.getStatus()))
                && (this.getFence_geo_json() == null
                        ? other.getFence_geo_json() == null
                        : this.getFence_geo_json().equals(other.getFence_geo_json()))
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
        result = prime * result + ((getVenue_code() == null) ? 0 : getVenue_code().hashCode());
        result = prime * result + ((getVenue_name() == null) ? 0 : getVenue_name().hashCode());
        result = prime * result + ((getAddress() == null) ? 0 : getAddress().hashCode());
        result = prime * result + ((getContact_name() == null) ? 0 : getContact_name().hashCode());
        result =
                prime * result + ((getContact_phone() == null) ? 0 : getContact_phone().hashCode());
        result = prime * result + ((getTimezone() == null) ? 0 : getTimezone().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result =
                prime * result
                        + ((getFence_geo_json() == null) ? 0 : getFence_geo_json().hashCode());
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
        sb.append(", venue_code=").append(venue_code);
        sb.append(", venue_name=").append(venue_name);
        sb.append(", address=").append(address);
        sb.append(", contact_name=").append(contact_name);
        sb.append(", contact_phone=").append(contact_phone);
        sb.append(", timezone=").append(timezone);
        sb.append(", status=").append(status);
        sb.append(", fence_geo_json=").append(fence_geo_json);
        sb.append(", created_at=").append(created_at);
        sb.append(", updated_at=").append(updated_at);
        sb.append(", is_delete=").append(is_delete);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
