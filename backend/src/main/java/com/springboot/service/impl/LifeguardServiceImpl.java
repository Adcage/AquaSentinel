package com.springboot.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.springboot.common.ErrorCode;
import com.springboot.constant.CommonConstant;
import com.springboot.exception.BusinessException;
import com.springboot.mapper.LifeguardMapper;
import com.springboot.model.dto.lifeguard.LifeguardQueryRequest;
import com.springboot.model.entity.Lifeguard;
import com.springboot.model.entity.LifeguardDutyLog;
import com.springboot.model.vo.LifeguardVO;
import com.springboot.service.LifeguardDutyLogService;
import com.springboot.service.LifeguardService;
import com.springboot.utils.SqlUtils;
import com.springboot.websocket.AlertWsPublisher;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @description 针对表【lifeguard(救生员表)】的数据库操作Service实现
 */
@Service
public class LifeguardServiceImpl extends ServiceImpl<LifeguardMapper, Lifeguard>
        implements LifeguardService {

    @Resource private LifeguardDutyLogService lifeguardDutyLogService;

    @Resource private AlertWsPublisher alertWsPublisher;

    @Override
    public void validLifeguard(Lifeguard lifeguard, boolean add) {
        if (lifeguard == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "救生员信息不能为空");
        }
        if (add && StringUtils.isBlank(lifeguard.getFull_name())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "救生员姓名不能为空");
        }
        if (StringUtils.isNotBlank(lifeguard.getLifeguard_code())
                && lifeguard.getLifeguard_code().length() > 32) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "救生员编码过长");
        }
        if (StringUtils.isNotBlank(lifeguard.getFull_name())
                && lifeguard.getFull_name().length() > 64) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "救生员姓名过长");
        }
        if (StringUtils.isNotBlank(lifeguard.getPhone()) && lifeguard.getPhone().length() > 32) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式错误");
        }
        if (StringUtils.isNotBlank(lifeguard.getLifeguard_code())) {
            QueryWrapper<Lifeguard> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("lifeguard_code", lifeguard.getLifeguard_code());
            queryWrapper.eq("is_delete", 0);
            queryWrapper.ne(lifeguard.getId() != null, "id", lifeguard.getId());
            Long count = this.baseMapper.selectCount(queryWrapper);
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "救生员编码已存在");
            }
        }
    }

    @Override
    public QueryWrapper<Lifeguard> getQueryWrapper(LifeguardQueryRequest lifeguardQueryRequest) {
        if (lifeguardQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper<Lifeguard> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(lifeguardQueryRequest.getId() != null, "id", lifeguardQueryRequest.getId());
        queryWrapper.eq(
                lifeguardQueryRequest.getUserId() != null,
                "user_id",
                lifeguardQueryRequest.getUserId());
        queryWrapper.eq(
                StringUtils.isNotBlank(lifeguardQueryRequest.getLifeguardCode()),
                "lifeguard_code",
                lifeguardQueryRequest.getLifeguardCode());
        queryWrapper.like(
                StringUtils.isNotBlank(lifeguardQueryRequest.getFullName()),
                "full_name",
                lifeguardQueryRequest.getFullName());
        queryWrapper.eq(
                StringUtils.isNotBlank(lifeguardQueryRequest.getPhone()),
                "phone",
                lifeguardQueryRequest.getPhone());
        queryWrapper.eq(
                lifeguardQueryRequest.getVenueId() != null,
                "venue_id",
                lifeguardQueryRequest.getVenueId());
        queryWrapper.eq(
                StringUtils.isNotBlank(lifeguardQueryRequest.getAuditStatus()),
                "audit_status",
                lifeguardQueryRequest.getAuditStatus());
        queryWrapper.eq(
                StringUtils.isNotBlank(lifeguardQueryRequest.getDutyStatus()),
                "duty_status",
                lifeguardQueryRequest.getDutyStatus());
        queryWrapper.eq("is_delete", 0);
        String sortField = lifeguardQueryRequest.getSortField();
        String sortOrder = lifeguardQueryRequest.getSortOrder();
        queryWrapper.orderBy(
                SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder),
                sortField);
        return queryWrapper;
    }

    @Override
    public LifeguardVO getLifeguardVO(Lifeguard lifeguard) {
        if (lifeguard == null) {
            return null;
        }
        LifeguardVO lifeguardVO = new LifeguardVO();
        lifeguardVO.setId(lifeguard.getId());
        lifeguardVO.setUserId(lifeguard.getUser_id());
        lifeguardVO.setLifeguardCode(lifeguard.getLifeguard_code());
        lifeguardVO.setFullName(lifeguard.getFull_name());
        lifeguardVO.setPhone(lifeguard.getPhone());
        lifeguardVO.setVenueId(lifeguard.getVenue_id());
        lifeguardVO.setFenceGeoJson(lifeguard.getFence_geo_json());
        lifeguardVO.setAuditStatus(lifeguard.getAudit_status());
        lifeguardVO.setDutyStatus(lifeguard.getDuty_status());
        lifeguardVO.setLastLoginAt(lifeguard.getLast_login_at());
        lifeguardVO.setCreatedAt(lifeguard.getCreated_at());
        lifeguardVO.setUpdatedAt(lifeguard.getUpdated_at());
        return lifeguardVO;
    }

    @Override
    public List<LifeguardVO> getLifeguardVO(List<Lifeguard> lifeguardList) {
        if (CollUtil.isEmpty(lifeguardList)) {
            return new ArrayList<>();
        }
        return lifeguardList.stream().map(this::getLifeguardVO).collect(Collectors.toList());
    }

    @Override
    public boolean audit(Long lifeguardId, String auditStatus, Long approvedBy) {
        if (lifeguardId == null || lifeguardId <= 0 || StringUtils.isBlank(auditStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        Lifeguard lifeguard = getActiveLifeguardById(lifeguardId);
        lifeguard.setAudit_status(auditStatus);
        lifeguard.setUpdated_at(new Date());
        boolean result = this.updateById(lifeguard);
        if (result) {
            LifeguardDutyLog dutyLog = new LifeguardDutyLog();
            dutyLog.setLifeguard_id(lifeguardId);
            dutyLog.setAction_type("AUDIT");
            dutyLog.setApproved_by(approvedBy);
            dutyLog.setCreated_at(new Date());
            lifeguardDutyLogService.save(dutyLog);
        }
        return result;
    }

    @Override
    public boolean updateDutyStatus(Long lifeguardId, String dutyStatus, Long operatorId) {
        if (lifeguardId == null || lifeguardId <= 0 || StringUtils.isBlank(dutyStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        Lifeguard lifeguard = getActiveLifeguardById(lifeguardId);
        if (Objects.equals("ON_DUTY", dutyStatus)) {
            kickPreviousSessionIfNeeded(lifeguardId);
        }
        lifeguard.setDuty_status(dutyStatus);
        lifeguard.setUpdated_at(new Date());
        boolean result = this.updateById(lifeguard);
        if (result) {
            LifeguardDutyLog dutyLog = new LifeguardDutyLog();
            dutyLog.setLifeguard_id(lifeguardId);
            dutyLog.setAction_type(dutyStatus);
            dutyLog.setApproved_by(operatorId);
            dutyLog.setCreated_at(new Date());
            lifeguardDutyLogService.save(dutyLog);
            publishLifeguardStatusChanged(lifeguard, dutyStatus);
        }
        return result;
    }

    @Override
    public boolean submitLeaveReport(Long lifeguardId, String leaveReason, Date plannedReturnAt) {
        if (lifeguardId == null || lifeguardId <= 0 || StringUtils.isBlank(leaveReason)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        Lifeguard lifeguard = getActiveLifeguardById(lifeguardId);
        lifeguard.setDuty_status("LEAVE");
        lifeguard.setUpdated_at(new Date());
        boolean result = this.updateById(lifeguard);
        if (result) {
            LifeguardDutyLog dutyLog = new LifeguardDutyLog();
            dutyLog.setLifeguard_id(lifeguardId);
            dutyLog.setAction_type("LEAVE_REPORT");
            dutyLog.setLeave_reason(leaveReason);
            dutyLog.setPlanned_return_at(plannedReturnAt);
            dutyLog.setCreated_at(new Date());
            lifeguardDutyLogService.save(dutyLog);
            publishLifeguardStatusChanged(lifeguard, "LEAVE");
        }
        return result;
    }

    @Override
    public boolean kickPreviousSessionIfNeeded(Long lifeguardId) {
        if (lifeguardId == null || lifeguardId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        return true;
    }

    private Lifeguard getActiveLifeguardById(Long lifeguardId) {
        QueryWrapper<Lifeguard> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", lifeguardId);
        queryWrapper.eq("is_delete", 0);
        Lifeguard lifeguard = this.getOne(queryWrapper);
        if (lifeguard == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "救生员不存在");
        }
        return lifeguard;
    }

    private void publishLifeguardStatusChanged(Lifeguard lifeguard, String dutyStatus) {
        Map<String, Object> data = new HashMap<>();
        data.put("lifeguardId", lifeguard.getId());
        data.put("userId", lifeguard.getUser_id());
        data.put("venueId", lifeguard.getVenue_id());
        data.put("dutyStatus", dutyStatus);
        alertWsPublisher.publishLifeguardStatusChanged(
                "lifeguard-status-" + lifeguard.getId() + "-" + System.currentTimeMillis(), data);
    }
}
