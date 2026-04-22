package com.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.springboot.model.entity.MonitoringEvent;
import com.springboot.model.dto.monitoringevent.MonitoringEventQueryRequest;
import com.springboot.model.vo.MonitoringEventVO;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
* @description 针对表【monitoring_event(监控事件表)】的数据库操作Service
*/
public interface MonitoringEventService extends IService<MonitoringEvent> {

    void validMonitoringEvent(MonitoringEvent monitoringEvent, boolean add);

    QueryWrapper<MonitoringEvent> getQueryWrapper(MonitoringEventQueryRequest monitoringEventQueryRequest);

    MonitoringEventVO getMonitoringEventVO(MonitoringEvent monitoringEvent);

    List<MonitoringEventVO> getMonitoringEventVO(List<MonitoringEvent> monitoringEventList);
}
