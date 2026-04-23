package com.springboot.model.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 刷新令牌会话表 @TableName auth_refresh_token */
@TableName(value = "auth_refresh_token")
@Data
public class AuthRefreshToken implements Serializable {
    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    @TableField(value = "user_id")
    private Long user_id;

    /** RefreshToken哈希 */
    @TableField(value = "refresh_token_hash")
    private String refresh_token_hash;

    /** 设备标识 */
    @TableField(value = "device_id")
    private String device_id;

    /** 客户端类型 */
    @TableField(value = "client_type")
    private String client_type;

    /** 客户端版本 */
    @TableField(value = "client_version")
    private String client_version;

    /** 登录IP */
    @TableField(value = "ip_address")
    private String ip_address;

    /** 过期时间 */
    @TableField(value = "expires_at")
    private Date expires_at;

    /** 是否吊销 */
    @TableField(value = "revoked")
    private Integer revoked;

    /** 吊销时间 */
    @TableField(value = "revoked_at")
    private Date revoked_at;

    /** 吊销原因 */
    @TableField(value = "revoke_reason")
    private String revoke_reason;

    /** 最近使用时间 */
    @TableField(value = "last_used_at")
    private Date last_used_at;

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
        AuthRefreshToken other = (AuthRefreshToken) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getUser_id() == null
                        ? other.getUser_id() == null
                        : this.getUser_id().equals(other.getUser_id()))
                && (this.getRefresh_token_hash() == null
                        ? other.getRefresh_token_hash() == null
                        : this.getRefresh_token_hash().equals(other.getRefresh_token_hash()))
                && (this.getDevice_id() == null
                        ? other.getDevice_id() == null
                        : this.getDevice_id().equals(other.getDevice_id()))
                && (this.getClient_type() == null
                        ? other.getClient_type() == null
                        : this.getClient_type().equals(other.getClient_type()))
                && (this.getClient_version() == null
                        ? other.getClient_version() == null
                        : this.getClient_version().equals(other.getClient_version()))
                && (this.getIp_address() == null
                        ? other.getIp_address() == null
                        : this.getIp_address().equals(other.getIp_address()))
                && (this.getExpires_at() == null
                        ? other.getExpires_at() == null
                        : this.getExpires_at().equals(other.getExpires_at()))
                && (this.getRevoked() == null
                        ? other.getRevoked() == null
                        : this.getRevoked().equals(other.getRevoked()))
                && (this.getRevoked_at() == null
                        ? other.getRevoked_at() == null
                        : this.getRevoked_at().equals(other.getRevoked_at()))
                && (this.getRevoke_reason() == null
                        ? other.getRevoke_reason() == null
                        : this.getRevoke_reason().equals(other.getRevoke_reason()))
                && (this.getLast_used_at() == null
                        ? other.getLast_used_at() == null
                        : this.getLast_used_at().equals(other.getLast_used_at()))
                && (this.getCreated_at() == null
                        ? other.getCreated_at() == null
                        : this.getCreated_at().equals(other.getCreated_at()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUser_id() == null) ? 0 : getUser_id().hashCode());
        result =
                prime * result
                        + ((getRefresh_token_hash() == null)
                                ? 0
                                : getRefresh_token_hash().hashCode());
        result = prime * result + ((getDevice_id() == null) ? 0 : getDevice_id().hashCode());
        result = prime * result + ((getClient_type() == null) ? 0 : getClient_type().hashCode());
        result =
                prime * result
                        + ((getClient_version() == null) ? 0 : getClient_version().hashCode());
        result = prime * result + ((getIp_address() == null) ? 0 : getIp_address().hashCode());
        result = prime * result + ((getExpires_at() == null) ? 0 : getExpires_at().hashCode());
        result = prime * result + ((getRevoked() == null) ? 0 : getRevoked().hashCode());
        result = prime * result + ((getRevoked_at() == null) ? 0 : getRevoked_at().hashCode());
        result =
                prime * result + ((getRevoke_reason() == null) ? 0 : getRevoke_reason().hashCode());
        result = prime * result + ((getLast_used_at() == null) ? 0 : getLast_used_at().hashCode());
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
        sb.append(", user_id=").append(user_id);
        sb.append(", refresh_token_hash=").append(refresh_token_hash);
        sb.append(", device_id=").append(device_id);
        sb.append(", client_type=").append(client_type);
        sb.append(", client_version=").append(client_version);
        sb.append(", ip_address=").append(ip_address);
        sb.append(", expires_at=").append(expires_at);
        sb.append(", revoked=").append(revoked);
        sb.append(", revoked_at=").append(revoked_at);
        sb.append(", revoke_reason=").append(revoke_reason);
        sb.append(", last_used_at=").append(last_used_at);
        sb.append(", created_at=").append(created_at);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
