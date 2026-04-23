package com.springboot.model.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 报警处置表 @TableName alert_disposal */
@TableName(value = "alert_disposal")
@Data
public class AlertDisposal implements Serializable {
    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 报警ID */
    @TableField(value = "alert_id")
    private Long alert_id;

    /** 操作人用户ID */
    @TableField(value = "operator_user_id")
    private Long operator_user_id;

    /** 操作人角色 */
    @TableField(value = "operator_role")
    private String operator_role;

    /** 动作类型 */
    @TableField(value = "action_type")
    private String action_type;

    /** 处理备注 */
    @TableField(value = "action_note")
    private String action_note;

    /** 处理时间 */
    @TableField(value = "action_time")
    private Date action_time;

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
        AlertDisposal other = (AlertDisposal) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getAlert_id() == null
                        ? other.getAlert_id() == null
                        : this.getAlert_id().equals(other.getAlert_id()))
                && (this.getOperator_user_id() == null
                        ? other.getOperator_user_id() == null
                        : this.getOperator_user_id().equals(other.getOperator_user_id()))
                && (this.getOperator_role() == null
                        ? other.getOperator_role() == null
                        : this.getOperator_role().equals(other.getOperator_role()))
                && (this.getAction_type() == null
                        ? other.getAction_type() == null
                        : this.getAction_type().equals(other.getAction_type()))
                && (this.getAction_note() == null
                        ? other.getAction_note() == null
                        : this.getAction_note().equals(other.getAction_note()))
                && (this.getAction_time() == null
                        ? other.getAction_time() == null
                        : this.getAction_time().equals(other.getAction_time()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getAlert_id() == null) ? 0 : getAlert_id().hashCode());
        result =
                prime * result
                        + ((getOperator_user_id() == null) ? 0 : getOperator_user_id().hashCode());
        result =
                prime * result + ((getOperator_role() == null) ? 0 : getOperator_role().hashCode());
        result = prime * result + ((getAction_type() == null) ? 0 : getAction_type().hashCode());
        result = prime * result + ((getAction_note() == null) ? 0 : getAction_note().hashCode());
        result = prime * result + ((getAction_time() == null) ? 0 : getAction_time().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", alert_id=").append(alert_id);
        sb.append(", operator_user_id=").append(operator_user_id);
        sb.append(", operator_role=").append(operator_role);
        sb.append(", action_type=").append(action_type);
        sb.append(", action_note=").append(action_note);
        sb.append(", action_time=").append(action_time);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
