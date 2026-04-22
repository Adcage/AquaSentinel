package com.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.springboot.model.dto.lifeguarddutylog.LifeguardDutyLogQueryRequest;
import com.springboot.model.entity.LifeguardDutyLog;
import com.springboot.model.vo.LifeguardDutyLogVO;
import java.util.List;

/**
* @description 针对表【lifeguard_duty_log(救生员上下岗日志表)】的数据库操作Service
*/
public interface LifeguardDutyLogService extends IService<LifeguardDutyLog> {

    void validLifeguardDutyLog(LifeguardDutyLog lifeguardDutyLog, boolean add);

    QueryWrapper<LifeguardDutyLog> getQueryWrapper(LifeguardDutyLogQueryRequest lifeguardDutyLogQueryRequest);

    LifeguardDutyLogVO getLifeguardDutyLogVO(LifeguardDutyLog lifeguardDutyLog);

    List<LifeguardDutyLogVO> getLifeguardDutyLogVO(List<LifeguardDutyLog> lifeguardDutyLogList);
}
