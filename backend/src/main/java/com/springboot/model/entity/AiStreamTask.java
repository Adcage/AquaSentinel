package com.springboot.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * AI流任务表
 * @TableName ai_stream_task
 */
@TableName(value ="ai_stream_task")
@Data
public class AiStreamTask implements Serializable {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务编码
     */
    @TableField(value = "task_code")
    private String task_code;

    /**
     * 摄像头ID
     */
    @TableField(value = "camera_id")
    private Long camera_id;

    /**
     * 流地址
     */
    @TableField(value = "stream_url")
    private String stream_url;

    /**
     * 抽帧间隔毫秒
     */
    @TableField(value = "frame_interval_ms")
    private Integer frame_interval_ms;

    /**
     * 回调地址
     */
    @TableField(value = "callback_url")
    private String callback_url;

    /**
     * 任务状态
     */
    @TableField(value = "task_status")
    private String task_status;

    /**
     * 启动时间
     */
    @TableField(value = "started_at")
    private Date started_at;

    /**
     * 停止时间
     */
    @TableField(value = "stopped_at")
    private Date stopped_at;

    /**
     * 最近处理帧时间
     */
    @TableField(value = "last_frame_at")
    private Date last_frame_at;

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
        AiStreamTask other = (AiStreamTask) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getTask_code() == null ? other.getTask_code() == null : this.getTask_code().equals(other.getTask_code()))
            && (this.getCamera_id() == null ? other.getCamera_id() == null : this.getCamera_id().equals(other.getCamera_id()))
            && (this.getStream_url() == null ? other.getStream_url() == null : this.getStream_url().equals(other.getStream_url()))
            && (this.getFrame_interval_ms() == null ? other.getFrame_interval_ms() == null : this.getFrame_interval_ms().equals(other.getFrame_interval_ms()))
            && (this.getCallback_url() == null ? other.getCallback_url() == null : this.getCallback_url().equals(other.getCallback_url()))
            && (this.getTask_status() == null ? other.getTask_status() == null : this.getTask_status().equals(other.getTask_status()))
            && (this.getStarted_at() == null ? other.getStarted_at() == null : this.getStarted_at().equals(other.getStarted_at()))
            && (this.getStopped_at() == null ? other.getStopped_at() == null : this.getStopped_at().equals(other.getStopped_at()))
            && (this.getLast_frame_at() == null ? other.getLast_frame_at() == null : this.getLast_frame_at().equals(other.getLast_frame_at()))
            && (this.getCreated_at() == null ? other.getCreated_at() == null : this.getCreated_at().equals(other.getCreated_at()))
            && (this.getUpdated_at() == null ? other.getUpdated_at() == null : this.getUpdated_at().equals(other.getUpdated_at()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getTask_code() == null) ? 0 : getTask_code().hashCode());
        result = prime * result + ((getCamera_id() == null) ? 0 : getCamera_id().hashCode());
        result = prime * result + ((getStream_url() == null) ? 0 : getStream_url().hashCode());
        result = prime * result + ((getFrame_interval_ms() == null) ? 0 : getFrame_interval_ms().hashCode());
        result = prime * result + ((getCallback_url() == null) ? 0 : getCallback_url().hashCode());
        result = prime * result + ((getTask_status() == null) ? 0 : getTask_status().hashCode());
        result = prime * result + ((getStarted_at() == null) ? 0 : getStarted_at().hashCode());
        result = prime * result + ((getStopped_at() == null) ? 0 : getStopped_at().hashCode());
        result = prime * result + ((getLast_frame_at() == null) ? 0 : getLast_frame_at().hashCode());
        result = prime * result + ((getCreated_at() == null) ? 0 : getCreated_at().hashCode());
        result = prime * result + ((getUpdated_at() == null) ? 0 : getUpdated_at().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", task_code=").append(task_code);
        sb.append(", camera_id=").append(camera_id);
        sb.append(", stream_url=").append(stream_url);
        sb.append(", frame_interval_ms=").append(frame_interval_ms);
        sb.append(", callback_url=").append(callback_url);
        sb.append(", task_status=").append(task_status);
        sb.append(", started_at=").append(started_at);
        sb.append(", stopped_at=").append(stopped_at);
        sb.append(", last_frame_at=").append(last_frame_at);
        sb.append(", created_at=").append(created_at);
        sb.append(", updated_at=").append(updated_at);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
