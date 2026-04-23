package com.springboot.model.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 设备维护记录表 @TableName camera_maintenance_log */
@TableName(value = "camera_maintenance_log")
@Data
public class CameraMaintenanceLog implements Serializable {
    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 摄像头ID */
    @TableField(value = "camera_id")
    private Long camera_id;

    /** 维护类型 */
    @TableField(value = "maintenance_type")
    private String maintenance_type;

    /** 维护内容 */
    @TableField(value = "maintenance_content")
    private String maintenance_content;

    /** 维护人 */
    @TableField(value = "maintained_by")
    private String maintained_by;

    /** 维护时间 */
    @TableField(value = "maintained_at")
    private Date maintained_at;

    /** 下次维护时间 */
    @TableField(value = "next_maintenance_at")
    private Date next_maintenance_at;

    /** 逻辑删除 */
    @TableField(exist = false)
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
        CameraMaintenanceLog other = (CameraMaintenanceLog) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getCamera_id() == null
                        ? other.getCamera_id() == null
                        : this.getCamera_id().equals(other.getCamera_id()))
                && (this.getMaintenance_type() == null
                        ? other.getMaintenance_type() == null
                        : this.getMaintenance_type().equals(other.getMaintenance_type()))
                && (this.getMaintenance_content() == null
                        ? other.getMaintenance_content() == null
                        : this.getMaintenance_content().equals(other.getMaintenance_content()))
                && (this.getMaintained_by() == null
                        ? other.getMaintained_by() == null
                        : this.getMaintained_by().equals(other.getMaintained_by()))
                && (this.getMaintained_at() == null
                        ? other.getMaintained_at() == null
                        : this.getMaintained_at().equals(other.getMaintained_at()))
                && (this.getNext_maintenance_at() == null
                        ? other.getNext_maintenance_at() == null
                        : this.getNext_maintenance_at().equals(other.getNext_maintenance_at()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getCamera_id() == null) ? 0 : getCamera_id().hashCode());
        result =
                prime * result
                        + ((getMaintenance_type() == null) ? 0 : getMaintenance_type().hashCode());
        result =
                prime * result
                        + ((getMaintenance_content() == null)
                                ? 0
                                : getMaintenance_content().hashCode());
        result =
                prime * result + ((getMaintained_by() == null) ? 0 : getMaintained_by().hashCode());
        result =
                prime * result + ((getMaintained_at() == null) ? 0 : getMaintained_at().hashCode());
        result =
                prime * result
                        + ((getNext_maintenance_at() == null)
                                ? 0
                                : getNext_maintenance_at().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", camera_id=").append(camera_id);
        sb.append(", maintenance_type=").append(maintenance_type);
        sb.append(", maintenance_content=").append(maintenance_content);
        sb.append(", maintained_by=").append(maintained_by);
        sb.append(", maintained_at=").append(maintained_at);
        sb.append(", next_maintenance_at=").append(next_maintenance_at);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
