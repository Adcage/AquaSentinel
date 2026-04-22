package com.springboot.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springboot.common.ErrorCode;
import com.springboot.constant.CommonConstant;
import com.springboot.exception.BusinessException;
import com.springboot.model.entity.MonitoringEvent;
import com.springboot.model.dto.monitoringevent.MonitoringEventQueryRequest;
import com.springboot.model.vo.MonitoringEventVO;
import com.springboot.service.MonitoringEventService;
import com.springboot.mapper.MonitoringEventMapper;
import com.springboot.utils.SqlUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
* @description 针对表【monitoring_event(监控事件表)】的数据库操作Service实现
*/
@Service
public class MonitoringEventServiceImpl extends ServiceImpl<MonitoringEventMapper, MonitoringEvent>
    implements MonitoringEventService{

    @Override
    public void validMonitoringEvent(MonitoringEvent monitoringEvent, boolean add) {
        if (monitoringEvent == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "事件不能为空");
        }
        if (add && StringUtils.isBlank(monitoringEvent.getEvent_uid())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "事件唯一标识不能为空");
        }
        if (add && monitoringEvent.getCamera_id() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "摄像头ID不能为空");
        }
        if (add && StringUtils.isBlank(monitoringEvent.getEvent_type())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "事件类型不能为空");
        }
        if (StringUtils.isNotBlank(monitoringEvent.getEvent_uid())) {
            QueryWrapper<MonitoringEvent> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("event_uid", monitoringEvent.getEvent_uid());
            queryWrapper.ne(monitoringEvent.getId() != null, "id", monitoringEvent.getId());
            Long count = this.baseMapper.selectCount(queryWrapper);
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "事件唯一标识已存在");
            }
        }
    }

    @Override
    public QueryWrapper<MonitoringEvent> getQueryWrapper(MonitoringEventQueryRequest monitoringEventQueryRequest) {
        if (monitoringEventQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper<MonitoringEvent> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(monitoringEventQueryRequest.getId() != null, "id", monitoringEventQueryRequest.getId());
        queryWrapper.eq(StringUtils.isNotBlank(monitoringEventQueryRequest.getEventUid()), "event_uid",
                monitoringEventQueryRequest.getEventUid());
        queryWrapper.eq(monitoringEventQueryRequest.getCameraId() != null, "camera_id", monitoringEventQueryRequest.getCameraId());
        queryWrapper.eq(monitoringEventQueryRequest.getTaskId() != null, "task_id", monitoringEventQueryRequest.getTaskId());
        queryWrapper.eq(StringUtils.isNotBlank(monitoringEventQueryRequest.getEventType()), "event_type",
                monitoringEventQueryRequest.getEventType());
        queryWrapper.eq(StringUtils.isNotBlank(monitoringEventQueryRequest.getRiskLevel()), "risk_level",
                monitoringEventQueryRequest.getRiskLevel());
        queryWrapper.ge(monitoringEventQueryRequest.getStartEventTime() != null, "event_time",
                monitoringEventQueryRequest.getStartEventTime());
        queryWrapper.le(monitoringEventQueryRequest.getEndEventTime() != null, "event_time",
                monitoringEventQueryRequest.getEndEventTime());
        String sortField = monitoringEventQueryRequest.getSortField();
        String sortOrder = monitoringEventQueryRequest.getSortOrder();
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public MonitoringEventVO getMonitoringEventVO(MonitoringEvent monitoringEvent) {
        if (monitoringEvent == null) {
            return null;
        }
        MonitoringEventVO monitoringEventVO = new MonitoringEventVO();
        monitoringEventVO.setId(monitoringEvent.getId());
        monitoringEventVO.setEventUid(monitoringEvent.getEvent_uid());
        monitoringEventVO.setCameraId(monitoringEvent.getCamera_id());
        monitoringEventVO.setTaskId(monitoringEvent.getTask_id());
        monitoringEventVO.setEventType(monitoringEvent.getEvent_type());
        monitoringEventVO.setRiskLevel(monitoringEvent.getRisk_level());
        monitoringEventVO.setConfidence(monitoringEvent.getConfidence());
        monitoringEventVO.setTargetId(monitoringEvent.getTarget_id());
        monitoringEventVO.setPoolHeadCount(monitoringEvent.getPool_head_count());
        monitoringEventVO.setBboxJson(monitoringEvent.getBbox_json());
        monitoringEventVO.setPositionDesc(monitoringEvent.getPosition_desc());
        monitoringEventVO.setEmergencyContactName(monitoringEvent.getEmergency_contact_name());
        monitoringEventVO.setEmergencyContactPhone(monitoringEvent.getEmergency_contact_phone());
        monitoringEventVO.setIncidentLocation(monitoringEvent.getIncident_location());
        monitoringEventVO.setVideoStreamUrl(monitoringEvent.getVideo_stream_url());
        monitoringEventVO.setEventTime(monitoringEvent.getEvent_time());
        monitoringEventVO.setExtJson(monitoringEvent.getExt_json());
        monitoringEventVO.setCreatedAt(monitoringEvent.getCreated_at());
        return monitoringEventVO;
    }

    @Override
    public List<MonitoringEventVO> getMonitoringEventVO(List<MonitoringEvent> monitoringEventList) {
        if (CollUtil.isEmpty(monitoringEventList)) {
            return new ArrayList<>();
        }
        return monitoringEventList.stream().map(this::getMonitoringEventVO).collect(Collectors.toList());
    }
}




