package com.springboot.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 救生员表
 * @TableName lifeguard
 */
@TableName(value ="lifeguard")
@Data
public class Lifeguard implements Serializable {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联用户ID
     */
    @TableField(value = "user_id")
    private Long user_id;

    /**
     * 救生员编码
     */
    @TableField(value = "lifeguard_code")
    private String lifeguard_code;

    /**
     * 姓名
     */
    @TableField(value = "full_name")
    private String full_name;

    /**
     * 手机号
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 绑定场馆
     */
    @TableField(value = "venue_id")
    private Long venue_id;

    /**
     * 电子围栏GeoJSON
     */
    @TableField(value = "fence_geo_json")
    private Object fence_geo_json;

    /**
     * 审核状态
     */
    @TableField(value = "audit_status")
    private String audit_status;

    /**
     * 在岗状态
     */
    @TableField(value = "duty_status")
    private String duty_status;

    /**
     * 最近登录时间
     */
    @TableField(value = "last_login_at")
    private Date last_login_at;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private Date created_at;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private Date updated_at;

    /**
     * 逻辑删除
     */
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
        Lifeguard other = (Lifeguard) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUser_id() == null ? other.getUser_id() == null : this.getUser_id().equals(other.getUser_id()))
            && (this.getLifeguard_code() == null ? other.getLifeguard_code() == null : this.getLifeguard_code().equals(other.getLifeguard_code()))
            && (this.getFull_name() == null ? other.getFull_name() == null : this.getFull_name().equals(other.getFull_name()))
            && (this.getPhone() == null ? other.getPhone() == null : this.getPhone().equals(other.getPhone()))
            && (this.getVenue_id() == null ? other.getVenue_id() == null : this.getVenue_id().equals(other.getVenue_id()))
            && (this.getFence_geo_json() == null ? other.getFence_geo_json() == null : this.getFence_geo_json().equals(other.getFence_geo_json()))
            && (this.getAudit_status() == null ? other.getAudit_status() == null : this.getAudit_status().equals(other.getAudit_status()))
            && (this.getDuty_status() == null ? other.getDuty_status() == null : this.getDuty_status().equals(other.getDuty_status()))
            && (this.getLast_login_at() == null ? other.getLast_login_at() == null : this.getLast_login_at().equals(other.getLast_login_at()))
            && (this.getCreated_at() == null ? other.getCreated_at() == null : this.getCreated_at().equals(other.getCreated_at()))
            && (this.getUpdated_at() == null ? other.getUpdated_at() == null : this.getUpdated_at().equals(other.getUpdated_at()))
            && (this.getIs_delete() == null ? other.getIs_delete() == null : this.getIs_delete().equals(other.getIs_delete()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUser_id() == null) ? 0 : getUser_id().hashCode());
        result = prime * result + ((getLifeguard_code() == null) ? 0 : getLifeguard_code().hashCode());
        result = prime * result + ((getFull_name() == null) ? 0 : getFull_name().hashCode());
        result = prime * result + ((getPhone() == null) ? 0 : getPhone().hashCode());
        result = prime * result + ((getVenue_id() == null) ? 0 : getVenue_id().hashCode());
        result = prime * result + ((getFence_geo_json() == null) ? 0 : getFence_geo_json().hashCode());
        result = prime * result + ((getAudit_status() == null) ? 0 : getAudit_status().hashCode());
        result = prime * result + ((getDuty_status() == null) ? 0 : getDuty_status().hashCode());
        result = prime * result + ((getLast_login_at() == null) ? 0 : getLast_login_at().hashCode());
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
        sb.append(", user_id=").append(user_id);
        sb.append(", lifeguard_code=").append(lifeguard_code);
        sb.append(", full_name=").append(full_name);
        sb.append(", phone=").append(phone);
        sb.append(", venue_id=").append(venue_id);
        sb.append(", fence_geo_json=").append(fence_geo_json);
        sb.append(", audit_status=").append(audit_status);
        sb.append(", duty_status=").append(duty_status);
        sb.append(", last_login_at=").append(last_login_at);
        sb.append(", created_at=").append(created_at);
        sb.append(", updated_at=").append(updated_at);
        sb.append(", is_delete=").append(is_delete);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}