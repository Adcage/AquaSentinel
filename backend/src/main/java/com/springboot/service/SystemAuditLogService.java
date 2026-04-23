package com.springboot.service;

import java.util.List;

import com.springboot.model.dto.systemauditlog.SystemAuditLogQueryRequest;
import com.springboot.model.entity.SystemAuditLog;
import com.springboot.model.vo.SystemAuditLogVO;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @description 针对表【system_audit_log(系统审计日志表)】的数据库操作Service
 */
public interface SystemAuditLogService extends IService<SystemAuditLog> {

    void validSystemAuditLog(SystemAuditLog systemAuditLog, boolean add);

    QueryWrapper<SystemAuditLog> getQueryWrapper(
            SystemAuditLogQueryRequest systemAuditLogQueryRequest);

    SystemAuditLogVO getSystemAuditLogVO(SystemAuditLog systemAuditLog);

    List<SystemAuditLogVO> getSystemAuditLogVO(List<SystemAuditLog> systemAuditLogList);
}
