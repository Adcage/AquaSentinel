package com.springboot.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.springboot.common.ErrorCode;
import com.springboot.constant.CommonConstant;
import com.springboot.exception.BusinessException;
import com.springboot.mapper.AlertDisposalMapper;
import com.springboot.model.dto.alertdisposal.AlertDisposalQueryRequest;
import com.springboot.model.entity.AlertDisposal;
import com.springboot.model.vo.AlertDisposalVO;
import com.springboot.service.AlertDisposalService;
import com.springboot.utils.SqlUtils;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @description 针对表【alert_disposal(报警处置表)】的数据库操作Service实现
 */
@Service
public class AlertDisposalServiceImpl extends ServiceImpl<AlertDisposalMapper, AlertDisposal>
        implements AlertDisposalService {

    @Override
    public void validAlertDisposal(AlertDisposal alertDisposal, boolean add) {
        if (alertDisposal == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "报警处置不能为空");
        }
        if (add && alertDisposal.getAlert_id() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "报警ID不能为空");
        }
        if (add && alertDisposal.getOperator_user_id() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "操作人不能为空");
        }
        if (add && StringUtils.isBlank(alertDisposal.getAction_type())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "动作类型不能为空");
        }
    }

    @Override
    public QueryWrapper<AlertDisposal> getQueryWrapper(
            AlertDisposalQueryRequest alertDisposalQueryRequest) {
        if (alertDisposalQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper<AlertDisposal> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(
                alertDisposalQueryRequest.getId() != null, "id", alertDisposalQueryRequest.getId());
        queryWrapper.eq(
                alertDisposalQueryRequest.getAlertId() != null,
                "alert_id",
                alertDisposalQueryRequest.getAlertId());
        queryWrapper.eq(
                alertDisposalQueryRequest.getOperatorUserId() != null,
                "operator_user_id",
                alertDisposalQueryRequest.getOperatorUserId());
        queryWrapper.eq(
                StringUtils.isNotBlank(alertDisposalQueryRequest.getOperatorRole()),
                "operator_role",
                alertDisposalQueryRequest.getOperatorRole());
        queryWrapper.eq(
                StringUtils.isNotBlank(alertDisposalQueryRequest.getActionType()),
                "action_type",
                alertDisposalQueryRequest.getActionType());
        queryWrapper.ge(
                alertDisposalQueryRequest.getStartActionTime() != null,
                "action_time",
                alertDisposalQueryRequest.getStartActionTime());
        queryWrapper.le(
                alertDisposalQueryRequest.getEndActionTime() != null,
                "action_time",
                alertDisposalQueryRequest.getEndActionTime());
        String sortField = alertDisposalQueryRequest.getSortField();
        String sortOrder = alertDisposalQueryRequest.getSortOrder();
        queryWrapper.orderBy(
                SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder),
                sortField);
        return queryWrapper;
    }

    @Override
    public AlertDisposalVO getAlertDisposalVO(AlertDisposal alertDisposal) {
        if (alertDisposal == null) {
            return null;
        }
        AlertDisposalVO alertDisposalVO = new AlertDisposalVO();
        alertDisposalVO.setId(alertDisposal.getId());
        alertDisposalVO.setAlertId(alertDisposal.getAlert_id());
        alertDisposalVO.setOperatorUserId(alertDisposal.getOperator_user_id());
        alertDisposalVO.setOperatorRole(alertDisposal.getOperator_role());
        alertDisposalVO.setActionType(alertDisposal.getAction_type());
        alertDisposalVO.setActionNote(alertDisposal.getAction_note());
        alertDisposalVO.setActionTime(alertDisposal.getAction_time());
        return alertDisposalVO;
    }

    @Override
    public List<AlertDisposalVO> getAlertDisposalVO(List<AlertDisposal> alertDisposalList) {
        if (CollUtil.isEmpty(alertDisposalList)) {
            return new ArrayList<>();
        }
        return alertDisposalList.stream()
                .map(this::getAlertDisposalVO)
                .collect(Collectors.toList());
    }
}
