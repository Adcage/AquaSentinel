package com.springboot.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 系统审计日志表
 * @TableName system_audit_log
 */
@TableName(value ="system_audit_log")
@Data
public class SystemAuditLog implements Serializable {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 链路ID
     */
    @TableField(value = "trace_id")
    private String trace_id;

    /**
     * 日志分类
     */
    @TableField(value = "log_category")
    private String log_category;

    /**
     * 操作人ID
     */
    @TableField(value = "operator_id")
    private Long operator_id;

    /**
     * 操作人名称
     */
    @TableField(value = "operator_name")
    private String operator_name;

    /**
     * 客户端IP
     */
    @TableField(value = "client_ip")
    private String client_ip;

    /**
     * 请求URI
     */
    @TableField(value = "request_uri")
    private String request_uri;

    /**
     * 请求方法
     */
    @TableField(value = "request_method")
    private String request_method;

    /**
     * 请求体快照
     */
    @TableField(value = "request_body")
    private String request_body;

    /**
     * 响应业务码
     */
    @TableField(value = "response_code")
    private Integer response_code;

    /**
     * 响应消息
     */
    @TableField(value = "response_message")
    private String response_message;

    /**
     * 耗时毫秒
     */
    @TableField(value = "cost_ms")
    private Integer cost_ms;

    /**
     * 创建时间
     */
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
        SystemAuditLog other = (SystemAuditLog) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getTrace_id() == null ? other.getTrace_id() == null : this.getTrace_id().equals(other.getTrace_id()))
            && (this.getLog_category() == null ? other.getLog_category() == null : this.getLog_category().equals(other.getLog_category()))
            && (this.getOperator_id() == null ? other.getOperator_id() == null : this.getOperator_id().equals(other.getOperator_id()))
            && (this.getOperator_name() == null ? other.getOperator_name() == null : this.getOperator_name().equals(other.getOperator_name()))
            && (this.getClient_ip() == null ? other.getClient_ip() == null : this.getClient_ip().equals(other.getClient_ip()))
            && (this.getRequest_uri() == null ? other.getRequest_uri() == null : this.getRequest_uri().equals(other.getRequest_uri()))
            && (this.getRequest_method() == null ? other.getRequest_method() == null : this.getRequest_method().equals(other.getRequest_method()))
            && (this.getRequest_body() == null ? other.getRequest_body() == null : this.getRequest_body().equals(other.getRequest_body()))
            && (this.getResponse_code() == null ? other.getResponse_code() == null : this.getResponse_code().equals(other.getResponse_code()))
            && (this.getResponse_message() == null ? other.getResponse_message() == null : this.getResponse_message().equals(other.getResponse_message()))
            && (this.getCost_ms() == null ? other.getCost_ms() == null : this.getCost_ms().equals(other.getCost_ms()))
            && (this.getCreated_at() == null ? other.getCreated_at() == null : this.getCreated_at().equals(other.getCreated_at()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getTrace_id() == null) ? 0 : getTrace_id().hashCode());
        result = prime * result + ((getLog_category() == null) ? 0 : getLog_category().hashCode());
        result = prime * result + ((getOperator_id() == null) ? 0 : getOperator_id().hashCode());
        result = prime * result + ((getOperator_name() == null) ? 0 : getOperator_name().hashCode());
        result = prime * result + ((getClient_ip() == null) ? 0 : getClient_ip().hashCode());
        result = prime * result + ((getRequest_uri() == null) ? 0 : getRequest_uri().hashCode());
        result = prime * result + ((getRequest_method() == null) ? 0 : getRequest_method().hashCode());
        result = prime * result + ((getRequest_body() == null) ? 0 : getRequest_body().hashCode());
        result = prime * result + ((getResponse_code() == null) ? 0 : getResponse_code().hashCode());
        result = prime * result + ((getResponse_message() == null) ? 0 : getResponse_message().hashCode());
        result = prime * result + ((getCost_ms() == null) ? 0 : getCost_ms().hashCode());
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
        sb.append(", trace_id=").append(trace_id);
        sb.append(", log_category=").append(log_category);
        sb.append(", operator_id=").append(operator_id);
        sb.append(", operator_name=").append(operator_name);
        sb.append(", client_ip=").append(client_ip);
        sb.append(", request_uri=").append(request_uri);
        sb.append(", request_method=").append(request_method);
        sb.append(", request_body=").append(request_body);
        sb.append(", response_code=").append(response_code);
        sb.append(", response_message=").append(response_message);
        sb.append(", cost_ms=").append(cost_ms);
        sb.append(", created_at=").append(created_at);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}