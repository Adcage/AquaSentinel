package com.springboot.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.springboot.common.ErrorCode;
import com.springboot.constant.CommonConstant;
import com.springboot.exception.BusinessException;
import com.springboot.mapper.SystemAuditLogMapper;
import com.springboot.model.dto.systemauditlog.SystemAuditLogQueryRequest;
import com.springboot.model.entity.SystemAuditLog;
import com.springboot.model.vo.SystemAuditLogVO;
import com.springboot.service.SystemAuditLogService;
import com.springboot.utils.SqlUtils;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @description 针对表【system_audit_log(系统审计日志表)】的数据库操作Service实现
 */
@Service
public class SystemAuditLogServiceImpl extends ServiceImpl<SystemAuditLogMapper, SystemAuditLog>
        implements SystemAuditLogService {

    @Override
    public void validSystemAuditLog(SystemAuditLog systemAuditLog, boolean add) {
        if (systemAuditLog == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审计日志不能为空");
        }
        if (add && StringUtils.isBlank(systemAuditLog.getTrace_id())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "traceId不能为空");
        }
        if (add && StringUtils.isBlank(systemAuditLog.getLog_category())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "日志分类不能为空");
        }
    }

    @Override
    public QueryWrapper<SystemAuditLog> getQueryWrapper(
            SystemAuditLogQueryRequest systemAuditLogQueryRequest) {
        if (systemAuditLogQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper<SystemAuditLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(
                systemAuditLogQueryRequest.getId() != null,
                "id",
                systemAuditLogQueryRequest.getId());
        queryWrapper.eq(
                StringUtils.isNotBlank(systemAuditLogQueryRequest.getTraceId()),
                "trace_id",
                systemAuditLogQueryRequest.getTraceId());
        queryWrapper.eq(
                StringUtils.isNotBlank(systemAuditLogQueryRequest.getLogCategory()),
                "log_category",
                systemAuditLogQueryRequest.getLogCategory());
        queryWrapper.eq(
                systemAuditLogQueryRequest.getOperatorId() != null,
                "operator_id",
                systemAuditLogQueryRequest.getOperatorId());
        queryWrapper.like(
                StringUtils.isNotBlank(systemAuditLogQueryRequest.getOperatorName()),
                "operator_name",
                systemAuditLogQueryRequest.getOperatorName());
        queryWrapper.like(
                StringUtils.isNotBlank(systemAuditLogQueryRequest.getRequestUri()),
                "request_uri",
                systemAuditLogQueryRequest.getRequestUri());
        queryWrapper.eq(
                systemAuditLogQueryRequest.getResponseCode() != null,
                "response_code",
                systemAuditLogQueryRequest.getResponseCode());
        queryWrapper.ge(
                systemAuditLogQueryRequest.getStartCreatedAt() != null,
                "created_at",
                systemAuditLogQueryRequest.getStartCreatedAt());
        queryWrapper.le(
                systemAuditLogQueryRequest.getEndCreatedAt() != null,
                "created_at",
                systemAuditLogQueryRequest.getEndCreatedAt());
        String sortField = systemAuditLogQueryRequest.getSortField();
        String sortOrder = systemAuditLogQueryRequest.getSortOrder();
        queryWrapper.orderBy(
                SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder),
                sortField);
        return queryWrapper;
    }

    @Override
    public SystemAuditLogVO getSystemAuditLogVO(SystemAuditLog systemAuditLog) {
        if (systemAuditLog == null) {
            return null;
        }
        SystemAuditLogVO systemAuditLogVO = new SystemAuditLogVO();
        systemAuditLogVO.setId(systemAuditLog.getId());
        systemAuditLogVO.setTraceId(systemAuditLog.getTrace_id());
        systemAuditLogVO.setLogCategory(systemAuditLog.getLog_category());
        systemAuditLogVO.setOperatorId(systemAuditLog.getOperator_id());
        systemAuditLogVO.setOperatorName(systemAuditLog.getOperator_name());
        systemAuditLogVO.setClientIp(systemAuditLog.getClient_ip());
        systemAuditLogVO.setRequestUri(systemAuditLog.getRequest_uri());
        systemAuditLogVO.setRequestMethod(systemAuditLog.getRequest_method());
        systemAuditLogVO.setRequestBody(systemAuditLog.getRequest_body());
        systemAuditLogVO.setResponseCode(systemAuditLog.getResponse_code());
        systemAuditLogVO.setResponseMessage(systemAuditLog.getResponse_message());
        systemAuditLogVO.setCostMs(systemAuditLog.getCost_ms());
        systemAuditLogVO.setCreatedAt(systemAuditLog.getCreated_at());
        return systemAuditLogVO;
    }

    @Override
    public List<SystemAuditLogVO> getSystemAuditLogVO(List<SystemAuditLog> systemAuditLogList) {
        if (CollUtil.isEmpty(systemAuditLogList)) {
            return new ArrayList<>();
        }
        return systemAuditLogList.stream()
                .map(this::getSystemAuditLogVO)
                .collect(Collectors.toList());
    }
}
