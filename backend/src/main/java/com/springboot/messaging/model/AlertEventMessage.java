package com.springboot.messaging.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

/**
 * 报警事件消息模型，用于 RabbitMQ 消息队列传输的报警事件对象。
 *
 * <p>该类是 AquaSentinel 水上安全监控系统的核心 DTO，负责在各模块间传递报警事件数据。 根据 eventType 不同，会被路由到不同的消费队列：
 *
 * <ul>
 *   <li>alert.record - 报警记录消费
 *   <li>alert.notification - 通知推送消费
 *   <li>alert.analytics - 数据分析消费
 * </ul>
 *
 * @author AquaSentinel Team
 * @since 1.0
 */
@Data
public class AlertEventMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息唯一标识，用于消息追踪、去重和问题排查。 格式建议：UUID 字符串 */
    private String messageId;

    /** 消息版本号，用于协议兼容性。 默认值为 1，当消息结构发生变更时递增版本号 */
    private Integer version = 1;

    /** 关联的原始监控事件唯一ID，对应 MonitoringEvent 表的 event_uid 字段。 用于与数据库中的事件记录建立关联 */
    private String eventUid;

    /** 触发事件的摄像头数据库ID，对应 VenueCamera 表的主键。 用于定位是哪个摄像头触发的报警 */
    private Long cameraId;

    /** 摄像头编码，用于设备标识和显示。 格式示例：CAM_001、POOL_A_CAM_02 */
    private String cameraCode;

    /** 关联的监控任务ID，对应 MonitoringTask 表的主键。 用于追踪该事件属于哪个监控任务 */
    private Long taskId;

    /** 监控任务编码，用于显示和标识。 格式示例：TASK_20240401_001 */
    private String taskCode;

    /** 场馆/泳池ID，对应 Venue 表的主键。 用于标识事件发生的具体场所 */
    private Long venueId;

    /**
     * 事件类型，描述报警事件的类别。
     *
     * <p>取值示例：
     *
     * <ul>
     *   <li>drowning_record - 溺水事件
     *   <li>drowning_warning - 溺水预警
     *   <li>lifeguard_off_post - 救生员离岗
     *   <li>device_offline - 设备离线
     *   <li>crowd_density - 人群密度异常
     * </ul>
     */
    private String eventType;

    /**
     * 风险等级，用于区分报警的紧急程度。
     *
     * <p>取值范围：
     *
     * <ul>
     *   <li>HIGH - 高风险，需要立即处理
     *   <li>MEDIUM - 中等风险，需要关注
     *   <li>LOW - 低风险，记录即可
     * </ul>
     */
    private String riskLevel;

    /** AI 识别置信度，表示模型对检测结果的置信程度。 取值范围：0.0 - 1.0 通常高于 0.7 才触发正式报警 */
    private BigDecimal confidence;

    /** 检测到的目标ID，用于关联具体的检测对象。 格式示例：person_12345、object_67890 */
    private String targetId;

    /** 泳池内人数统计，实时检测的区域内人数。 用于人群密度监控和异常预警 */
    private Integer poolHeadCount;

    /**
     * 检测框坐标 JSON，存储目标在画面中的位置信息。 格式：{"x": 100, "y": 200, "width": 50, "height": 80} 其中 x,y
     * 为左上角坐标，width,height 为宽高
     */
    private Object bboxJson;

    /** 位置描述，用于 human-readable 的位置信息展示。 格式示例："浅水区"、"深水区入口"、"儿童区" */
    private String positionDesc;

    /** 紧急联系人姓名，用于报警通知。 事件发生时需要通知的责任人姓名 */
    private String emergencyContactName;

    /** 紧急联系人电话，用于短信或电话通知。 格式示例：13800138000 */
    private String emergencyContactPhone;

    /** 事件发生具体位置描述，用于精确位置定位。 格式示例："A区泳池入口右侧2米" */
    private String incidentLocation;

    /** 视频流地址，用于推送实时视频或录像片段。 格式示例：rtsp://192.168.1.100/stream1 */
    private String videoStreamUrl;

    /** 事件实际发生时间，即 AI 检测到异常的时间点。 对应监控画面中事件发生的时刻 */
    private Date eventTime;

    /** AI 检测时间，字符串格式。 用于日志和调试，格式示例："2024-04-01 10:30:25" */
    private String detectTime;

    /**
     * 扩展字段 JSON，用于存储自定义的额外数据。 可根据业务需求灵活添加字段，如： {"weather": "sunny", "water_temp": 28.5,
     * "additional_info": "..."}
     */
    private Object extJson;

    /**
     * 报警类型细分，与 eventType 配合使用，提供更细粒度的分类。 例如 eventType 为 "drowning_record" 时，alertType 可为
     * "real_drowning" 或 "false_alarm"
     */
    private String alertType;

    /** 消息发布时间，即消息被发送到 RabbitMQ 的时间。 用于消息追踪和性能监控 */
    private Date publishedAt;

    /** 消息来源系统标识，用于区分消息来自哪个子系统。 格式示例："yolo-engine"、"api-gateway"、"manual-report" */
    private String source;
}
