package com.springboot.model.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 救生员上下岗日志表 @TableName lifeguard_duty_log */
@TableName(value = "lifeguard_duty_log")
@Data
public class LifeguardDutyLog implements Serializable {
    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 救生员ID */
    @TableField(value = "lifeguard_id")
    private Long lifeguard_id;

    /** 动作类型 */
    @TableField(value = "action_type")
    private String action_type;

    /** 离岗原因 */
    @TableField(value = "leave_reason")
    private String leave_reason;

    /** 预计返回时间 */
    @TableField(value = "planned_return_at")
    private Date planned_return_at;

    /** 实际返回时间 */
    @TableField(value = "actual_return_at")
    private Date actual_return_at;

    /** 审批管理员ID */
    @TableField(value = "approved_by")
    private Long approved_by;

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
        LifeguardDutyLog other = (LifeguardDutyLog) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getLifeguard_id() == null
                        ? other.getLifeguard_id() == null
                        : this.getLifeguard_id().equals(other.getLifeguard_id()))
                && (this.getAction_type() == null
                        ? other.getAction_type() == null
                        : this.getAction_type().equals(other.getAction_type()))
                && (this.getLeave_reason() == null
                        ? other.getLeave_reason() == null
                        : this.getLeave_reason().equals(other.getLeave_reason()))
                && (this.getPlanned_return_at() == null
                        ? other.getPlanned_return_at() == null
                        : this.getPlanned_return_at().equals(other.getPlanned_return_at()))
                && (this.getActual_return_at() == null
                        ? other.getActual_return_at() == null
                        : this.getActual_return_at().equals(other.getActual_return_at()))
                && (this.getApproved_by() == null
                        ? other.getApproved_by() == null
                        : this.getApproved_by().equals(other.getApproved_by()))
                && (this.getCreated_at() == null
                        ? other.getCreated_at() == null
                        : this.getCreated_at().equals(other.getCreated_at()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getLifeguard_id() == null) ? 0 : getLifeguard_id().hashCode());
        result = prime * result + ((getAction_type() == null) ? 0 : getAction_type().hashCode());
        result = prime * result + ((getLeave_reason() == null) ? 0 : getLeave_reason().hashCode());
        result =
                prime * result
                        + ((getPlanned_return_at() == null)
                                ? 0
                                : getPlanned_return_at().hashCode());
        result =
                prime * result
                        + ((getActual_return_at() == null) ? 0 : getActual_return_at().hashCode());
        result = prime * result + ((getApproved_by() == null) ? 0 : getApproved_by().hashCode());
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
        sb.append(", lifeguard_id=").append(lifeguard_id);
        sb.append(", action_type=").append(action_type);
        sb.append(", leave_reason=").append(leave_reason);
        sb.append(", planned_return_at=").append(planned_return_at);
        sb.append(", actual_return_at=").append(actual_return_at);
        sb.append(", approved_by=").append(approved_by);
        sb.append(", created_at=").append(created_at);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
