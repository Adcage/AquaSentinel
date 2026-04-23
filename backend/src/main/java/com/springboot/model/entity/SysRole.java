package com.springboot.model.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 系统角色表 @TableName sys_role */
@TableName(value = "sys_role")
@Data
public class SysRole implements Serializable {
    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 角色编码 */
    @TableField(value = "role_code")
    private String role_code;

    /** 角色名称 */
    @TableField(value = "role_name")
    private String role_name;

    /** 权限集合JSON */
    @TableField(value = "permission_json")
    private Object permission_json;

    /** 状态:1启用,0禁用 */
    @TableField(value = "status")
    private Integer status;

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
        SysRole other = (SysRole) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getRole_code() == null
                        ? other.getRole_code() == null
                        : this.getRole_code().equals(other.getRole_code()))
                && (this.getRole_name() == null
                        ? other.getRole_name() == null
                        : this.getRole_name().equals(other.getRole_name()))
                && (this.getPermission_json() == null
                        ? other.getPermission_json() == null
                        : this.getPermission_json().equals(other.getPermission_json()))
                && (this.getStatus() == null
                        ? other.getStatus() == null
                        : this.getStatus().equals(other.getStatus()))
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
        result = prime * result + ((getRole_code() == null) ? 0 : getRole_code().hashCode());
        result = prime * result + ((getRole_name() == null) ? 0 : getRole_name().hashCode());
        result =
                prime * result
                        + ((getPermission_json() == null) ? 0 : getPermission_json().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
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
        sb.append(", role_code=").append(role_code);
        sb.append(", role_name=").append(role_name);
        sb.append(", permission_json=").append(permission_json);
        sb.append(", status=").append(status);
        sb.append(", created_at=").append(created_at);
        sb.append(", updated_at=").append(updated_at);
        sb.append(", is_delete=").append(is_delete);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
