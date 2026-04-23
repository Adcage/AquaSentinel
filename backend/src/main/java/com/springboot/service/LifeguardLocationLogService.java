package com.springboot.service;

import java.util.List;

import com.springboot.model.dto.lifeguardlocationlog.LifeguardLocationLogQueryRequest;
import com.springboot.model.entity.LifeguardLocationLog;
import com.springboot.model.vo.LifeguardLocationLogVO;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @description 针对表【lifeguard_location_log(救生员定位上报表)】的数据库操作Service
 */
public interface LifeguardLocationLogService extends IService<LifeguardLocationLog> {

    void validLifeguardLocationLog(LifeguardLocationLog lifeguardLocationLog, boolean add);

    QueryWrapper<LifeguardLocationLog> getQueryWrapper(
            LifeguardLocationLogQueryRequest lifeguardLocationLogQueryRequest);

    LifeguardLocationLogVO getLifeguardLocationLogVO(LifeguardLocationLog lifeguardLocationLog);

    List<LifeguardLocationLogVO> getLifeguardLocationLogVO(
            List<LifeguardLocationLog> lifeguardLocationLogList);

    boolean reportLocation(LifeguardLocationLog lifeguardLocationLog);

    List<LifeguardLocationLog> recentLocations(Long lifeguardId, Integer limit);

    boolean detectOffPost(Long lifeguardId);
}
