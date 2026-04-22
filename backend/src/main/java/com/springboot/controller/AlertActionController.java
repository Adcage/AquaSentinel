package com.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.exception.ThrowUtils;
import com.springboot.model.dto.alertrecord.AlertActionRequest;
import com.springboot.model.dto.alertrecord.AlertRecordQueryRequest;
import com.springboot.model.entity.AlertDisposal;
import com.springboot.model.entity.AlertRecord;
import com.springboot.model.entity.MonitoringEvent;
import com.springboot.model.vo.AlertRecordVO;
import com.springboot.security.AuthContextHolder;
import com.springboot.security.AuthUserContext;
import com.springboot.service.AlertDisposalService;
import com.springboot.service.AlertRecordService;
import com.springboot.service.MonitoringEventService;
import com.springboot.websocket.AlertWsPublisher;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/alerts")
public class AlertActionController {

    @Resource
    private AlertRecordService alertRecordService;

    @Resource
    private AlertDisposalService alertDisposalService;

    @Resource
    private MonitoringEventService monitoringEventService;

    @Resource
    private AlertWsPublisher alertWsPublisher;

    @PostMapping("/list/page")
    public BaseResponse<Page<AlertRecordVO>> listByPage(@RequestBody(required = false) AlertRecordQueryRequest request) {
        AlertRecordQueryRequest queryRequest = request == null ? new AlertRecordQueryRequest() : request;
        long current = Math.max(1, queryRequest.getCurrent());
        long pageSize = Math.min(100, Math.max(1, queryRequest.getPageSize()));
        Page<AlertRecord> page = alertRecordService.page(
                new Page<>(current, pageSize),
                alertRecordService.getQueryWrapper(queryRequest));
        Page<AlertRecordVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(alertRecordService.getAlertRecordVO(page.getRecords()));
        return ResultUtils.success(voPage);
    }

    @GetMapping("/stats/today")
    public BaseResponse<Map<String, Object>> getTodayAlertStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        AlertRecordQueryRequest queryRequest = new AlertRecordQueryRequest();
        queryRequest.setStartTime(java.sql.Timestamp.valueOf(startOfDay));
        queryRequest.setEndTime(java.sql.Timestamp.valueOf(endOfDay));
        long count = alertRecordService.count(alertRecordService.getQueryWrapper(queryRequest));
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        data.put("date", today.toString());
        return ResultUtils.success(data);
    }

    @PostMapping("/{id}/assign")
    public BaseResponse<Map<String, Object>> assign(@PathVariable("id") Long id,
            @RequestBody AlertActionRequest request) {
        AlertActionRequest actionRequest = request == null ? new AlertActionRequest() : request;
        actionRequest.setAlertId(id);
        actionRequest.setActionType("ASSIGN");
        return action(actionRequest);
    }

    @PostMapping("/{id}/actions")
    public BaseResponse<Map<String, Object>> actions(@PathVariable("id") Long id,
            @RequestBody AlertActionRequest request) {
        AlertActionRequest actionRequest = request == null ? new AlertActionRequest() : request;
        actionRequest.setAlertId(id);
        return action(actionRequest);
    }

    @GetMapping("/{id}")
    public BaseResponse<AlertRecordVO> getAlertById(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id不能为空");
        }
        AlertRecord alertRecord = alertRecordService.getById(id);
        ThrowUtils.throwIf(alertRecord == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(alertRecordService.getAlertRecordVO(alertRecord));
    }

    @PostMapping("/action")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Map<String, Object>> action(@RequestBody AlertActionRequest request) {
        if (request == null || request.getAlertId() == null || StringUtils.isBlank(request.getActionType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "alertId/actionType不能为空");
        }
        AuthUserContext authUserContext = AuthContextHolder.getRequired();
        boolean hasPermission = authUserContext.hasRole(RoleConstant.VENUE_ADMIN)
                || authUserContext.hasRole(RoleConstant.LIFEGUARD)
                || authUserContext.hasRole(RoleConstant.SUPER_ADMIN);
        if (!hasPermission) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前角色无报警处置权限");
        }

        AlertRecord alertRecord = alertRecordService.getById(request.getAlertId());
        ThrowUtils.throwIf(alertRecord == null, ErrorCode.NOT_FOUND_ERROR);

        String actionType = request.getActionType().trim().toUpperCase();
        Date now = new Date();
        AlertRecord update = new AlertRecord();
        update.setId(alertRecord.getId());
        switch (actionType) {
            case "CONFIRM":
                update.setAlert_status("CONFIRMED");
                break;
            case "DONE":
                update.setAlert_status("DONE");
                update.setResolved_time(now);
                break;
            case "FALSE_ALARM":
                update.setAlert_status("FALSE_ALARM");
                update.setResolved_time(now);
                break;
            case "ASSIGN":
                if (request.getAssigneeLifeguardId() == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "ASSIGN动作必须提供assigneeLifeguardId");
                }
                update.setAlert_status("ASSIGNED");
                update.setLifeguard_id(request.getAssigneeLifeguardId());
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的actionType");
        }
        boolean updateResult = alertRecordService.updateById(update);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR);

        AlertDisposal alertDisposal = new AlertDisposal();
        alertDisposal.setAlert_id(alertRecord.getId());
        alertDisposal.setOperator_user_id(authUserContext.getUserId());
        String operatorRole = authUserContext.getRoleCodes().stream().findFirst().orElse("UNKNOWN");
        alertDisposal.setOperator_role(operatorRole);
        alertDisposal.setAction_type(actionType);
        alertDisposal.setAction_note(request.getActionNote());
        alertDisposal.setAction_time(now);
        alertDisposalService.validAlertDisposal(alertDisposal, true);
        alertDisposalService.save(alertDisposal);

        AlertRecord latestAlert = alertRecordService.getById(alertRecord.getId());
        MonitoringEvent monitoringEvent = latestAlert == null ? null : monitoringEventService.getById(latestAlert.getEvent_id());
        Map<String, Object> wsData = new HashMap<>();
        wsData.put("alertId", alertRecord.getId());
        wsData.put("alertUid", alertRecord.getAlert_uid());
        wsData.put("actionType", actionType);
        wsData.put("alertStatus", latestAlert == null ? null : latestAlert.getAlert_status());
        wsData.put("updatedAt", now);
        alertWsPublisher.publishAlertUpdated(monitoringEvent == null ? null : monitoringEvent.getEvent_uid(),
                alertRecord.getAlert_uid(), wsData);

        Map<String, Object> data = new HashMap<>();
        data.put("alertId", alertRecord.getId());
        data.put("actionType", actionType);
        data.put("status", latestAlert == null ? null : latestAlert.getAlert_status());
        data.put("disposalId", alertDisposal.getId());
        return ResultUtils.success(data);
    }
}
