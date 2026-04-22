package com.springboot.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 系统用户表
 * @TableName sys_user
 */
@TableName(value ="sys_user")
@Data
public class SysUser implements Serializable {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 登录账号
     */
    @TableField(value = "username")
    private String username;

    /**
     * 密码哈希
     */
    @TableField(value = "password_hash")
    private String password_hash;

    /**
     * 显示名称
     */
    @TableField(value = "display_name")
    private String display_name;

    /**
     * 手机号
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 邮箱
     */
    @TableField(value = "email")
    private String email;

    /**
     * 状态:1启用,0禁用
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 连续登录失败次数
     */
    @TableField(value = "failed_login_count")
    private Integer failed_login_count;

    /**
     * 锁定截止时间
     */
    @TableField(value = "lock_until")
    private Date lock_until;

    /**
     * 首登强制改密
     */
    @TableField(value = "force_change_password")
    private Integer force_change_password;

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
        SysUser other = (SysUser) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUsername() == null ? other.getUsername() == null : this.getUsername().equals(other.getUsername()))
            && (this.getPassword_hash() == null ? other.getPassword_hash() == null : this.getPassword_hash().equals(other.getPassword_hash()))
            && (this.getDisplay_name() == null ? other.getDisplay_name() == null : this.getDisplay_name().equals(other.getDisplay_name()))
            && (this.getPhone() == null ? other.getPhone() == null : this.getPhone().equals(other.getPhone()))
            && (this.getEmail() == null ? other.getEmail() == null : this.getEmail().equals(other.getEmail()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getFailed_login_count() == null ? other.getFailed_login_count() == null : this.getFailed_login_count().equals(other.getFailed_login_count()))
            && (this.getLock_until() == null ? other.getLock_until() == null : this.getLock_until().equals(other.getLock_until()))
            && (this.getForce_change_password() == null ? other.getForce_change_password() == null : this.getForce_change_password().equals(other.getForce_change_password()))
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
        result = prime * result + ((getUsername() == null) ? 0 : getUsername().hashCode());
        result = prime * result + ((getPassword_hash() == null) ? 0 : getPassword_hash().hashCode());
        result = prime * result + ((getDisplay_name() == null) ? 0 : getDisplay_name().hashCode());
        result = prime * result + ((getPhone() == null) ? 0 : getPhone().hashCode());
        result = prime * result + ((getEmail() == null) ? 0 : getEmail().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getFailed_login_count() == null) ? 0 : getFailed_login_count().hashCode());
        result = prime * result + ((getLock_until() == null) ? 0 : getLock_until().hashCode());
        result = prime * result + ((getForce_change_password() == null) ? 0 : getForce_change_password().hashCode());
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
        sb.append(", username=").append(username);
        sb.append(", password_hash=").append(password_hash);
        sb.append(", display_name=").append(display_name);
        sb.append(", phone=").append(phone);
        sb.append(", email=").append(email);
        sb.append(", status=").append(status);
        sb.append(", failed_login_count=").append(failed_login_count);
        sb.append(", lock_until=").append(lock_until);
        sb.append(", force_change_password=").append(force_change_password);
        sb.append(", last_login_at=").append(last_login_at);
        sb.append(", created_at=").append(created_at);
        sb.append(", updated_at=").append(updated_at);
        sb.append(", is_delete=").append(is_delete);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}