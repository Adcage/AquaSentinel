package com.springboot.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.springboot.common.ErrorCode;
import com.springboot.constant.CommonConstant;
import com.springboot.exception.BusinessException;
import com.springboot.mapper.LifeguardDutyLogMapper;
import com.springboot.model.dto.lifeguarddutylog.LifeguardDutyLogQueryRequest;
import com.springboot.model.entity.LifeguardDutyLog;
import com.springboot.model.vo.LifeguardDutyLogVO;
import com.springboot.service.LifeguardDutyLogService;
import com.springboot.utils.SqlUtils;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @description 针对表【lifeguard_duty_log(救生员上下岗日志表)】的数据库操作Service实现
 */
@Service
public class LifeguardDutyLogServiceImpl
        extends ServiceImpl<LifeguardDutyLogMapper, LifeguardDutyLog>
        implements LifeguardDutyLogService {

    @Override
    public void validLifeguardDutyLog(LifeguardDutyLog lifeguardDutyLog, boolean add) {
        if (lifeguardDutyLog == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上下岗日志不能为空");
        }
        if (add && lifeguardDutyLog.getLifeguard_id() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "救生员ID不能为空");
        }
        if (add && StringUtils.isBlank(lifeguardDutyLog.getAction_type())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "动作类型不能为空");
        }
        if (StringUtils.isNotBlank(lifeguardDutyLog.getAction_type())
                && lifeguardDutyLog.getAction_type().length() > 32) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "动作类型过长");
        }
    }

    @Override
    public QueryWrapper<LifeguardDutyLog> getQueryWrapper(
            LifeguardDutyLogQueryRequest lifeguardDutyLogQueryRequest) {
        if (lifeguardDutyLogQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper<LifeguardDutyLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(
                lifeguardDutyLogQueryRequest.getId() != null,
                "id",
                lifeguardDutyLogQueryRequest.getId());
        queryWrapper.eq(
                lifeguardDutyLogQueryRequest.getLifeguardId() != null,
                "lifeguard_id",
                lifeguardDutyLogQueryRequest.getLifeguardId());
        queryWrapper.eq(
                StringUtils.isNotBlank(lifeguardDutyLogQueryRequest.getActionType()),
                "action_type",
                lifeguardDutyLogQueryRequest.getActionType());
        queryWrapper.eq(
                lifeguardDutyLogQueryRequest.getApprovedBy() != null,
                "approved_by",
                lifeguardDutyLogQueryRequest.getApprovedBy());
        queryWrapper.eq(
                lifeguardDutyLogQueryRequest.getPlannedReturnAt() != null,
                "planned_return_at",
                lifeguardDutyLogQueryRequest.getPlannedReturnAt());
        queryWrapper.eq(
                lifeguardDutyLogQueryRequest.getActualReturnAt() != null,
                "actual_return_at",
                lifeguardDutyLogQueryRequest.getActualReturnAt());
        String sortField = lifeguardDutyLogQueryRequest.getSortField();
        String sortOrder = lifeguardDutyLogQueryRequest.getSortOrder();
        queryWrapper.orderBy(
                SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder),
                sortField);
        return queryWrapper;
    }

    @Override
    public LifeguardDutyLogVO getLifeguardDutyLogVO(LifeguardDutyLog lifeguardDutyLog) {
        if (lifeguardDutyLog == null) {
            return null;
        }
        LifeguardDutyLogVO lifeguardDutyLogVO = new LifeguardDutyLogVO();
        lifeguardDutyLogVO.setId(lifeguardDutyLog.getId());
        lifeguardDutyLogVO.setLifeguardId(lifeguardDutyLog.getLifeguard_id());
        lifeguardDutyLogVO.setActionType(lifeguardDutyLog.getAction_type());
        lifeguardDutyLogVO.setLeaveReason(lifeguardDutyLog.getLeave_reason());
        lifeguardDutyLogVO.setPlannedReturnAt(lifeguardDutyLog.getPlanned_return_at());
        lifeguardDutyLogVO.setActualReturnAt(lifeguardDutyLog.getActual_return_at());
        lifeguardDutyLogVO.setApprovedBy(lifeguardDutyLog.getApproved_by());
        lifeguardDutyLogVO.setCreatedAt(lifeguardDutyLog.getCreated_at());
        return lifeguardDutyLogVO;
    }

    @Override
    public List<LifeguardDutyLogVO> getLifeguardDutyLogVO(
            List<LifeguardDutyLog> lifeguardDutyLogList) {
        if (CollUtil.isEmpty(lifeguardDutyLogList)) {
            return new ArrayList<>();
        }
        return lifeguardDutyLogList.stream()
                .map(this::getLifeguardDutyLogVO)
                .collect(Collectors.toList());
    }
}
