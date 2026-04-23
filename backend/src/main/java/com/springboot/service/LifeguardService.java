package com.springboot.service;

import java.util.Date;
import java.util.List;

import com.springboot.model.dto.lifeguard.LifeguardQueryRequest;
import com.springboot.model.entity.Lifeguard;
import com.springboot.model.vo.LifeguardVO;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @description 针对表【lifeguard(救生员表)】的数据库操作Service
 */
public interface LifeguardService extends IService<Lifeguard> {

    void validLifeguard(Lifeguard lifeguard, boolean add);

    QueryWrapper<Lifeguard> getQueryWrapper(LifeguardQueryRequest lifeguardQueryRequest);

    LifeguardVO getLifeguardVO(Lifeguard lifeguard);

    List<LifeguardVO> getLifeguardVO(List<Lifeguard> lifeguardList);

    boolean audit(Long lifeguardId, String auditStatus, Long approvedBy);

    boolean updateDutyStatus(Long lifeguardId, String dutyStatus, Long operatorId);

    boolean submitLeaveReport(Long lifeguardId, String leaveReason, Date plannedReturnAt);

    boolean kickPreviousSessionIfNeeded(Long lifeguardId);
}
