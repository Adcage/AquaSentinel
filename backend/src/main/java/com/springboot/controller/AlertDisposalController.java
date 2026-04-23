package com.springboot.controller;

import java.util.Date;
import java.util.List;

import com.springboot.annotation.AuthCheck;
import com.springboot.common.BaseResponse;
import com.springboot.common.DeleteRequest;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.exception.ThrowUtils;
import com.springboot.model.dto.alertdisposal.AlertDisposalAddRequest;
import com.springboot.model.dto.alertdisposal.AlertDisposalEditRequest;
import com.springboot.model.dto.alertdisposal.AlertDisposalQueryRequest;
import com.springboot.model.dto.alertdisposal.AlertDisposalUpdateRequest;
import com.springboot.model.entity.AlertDisposal;
import com.springboot.model.vo.AlertDisposalVO;
import com.springboot.service.AlertDisposalService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/alert-disposals")
public class AlertDisposalController {

    @Resource private AlertDisposalService alertDisposalService;

    @PostMapping("/add")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Long> addAlertDisposal(
            @RequestBody AlertDisposalAddRequest alertDisposalAddRequest) {
        if (alertDisposalAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        AlertDisposal alertDisposal = toAlertDisposal(alertDisposalAddRequest);
        alertDisposal.setAction_time(
                alertDisposal.getAction_time() == null
                        ? new Date()
                        : alertDisposal.getAction_time());
        alertDisposalService.validAlertDisposal(alertDisposal, true);
        boolean result = alertDisposalService.save(alertDisposal);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(alertDisposal.getId());
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> deleteAlertDisposal(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = alertDisposalService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> updateAlertDisposal(
            @RequestBody AlertDisposalUpdateRequest alertDisposalUpdateRequest) {
        if (alertDisposalUpdateRequest == null
                || alertDisposalUpdateRequest.getId() == null
                || alertDisposalUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        AlertDisposal alertDisposal = toAlertDisposal(alertDisposalUpdateRequest);
        alertDisposalService.validAlertDisposal(alertDisposal, false);
        boolean result = alertDisposalService.updateById(alertDisposal);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/edit")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> editAlertDisposal(
            @RequestBody AlertDisposalEditRequest alertDisposalEditRequest) {
        if (alertDisposalEditRequest == null
                || alertDisposalEditRequest.getId() == null
                || alertDisposalEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        AlertDisposal alertDisposal = new AlertDisposal();
        alertDisposal.setId(alertDisposalEditRequest.getId());
        alertDisposal.setAction_type(alertDisposalEditRequest.getActionType());
        alertDisposal.setAction_note(alertDisposalEditRequest.getActionNote());
        alertDisposal.setAction_time(alertDisposalEditRequest.getActionTime());
        alertDisposalService.validAlertDisposal(alertDisposal, false);
        boolean result = alertDisposalService.updateById(alertDisposal);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<AlertDisposal> getAlertDisposalById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        AlertDisposal alertDisposal = alertDisposalService.getById(id);
        ThrowUtils.throwIf(alertDisposal == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(alertDisposal);
    }

    @GetMapping("/get/vo")
    public BaseResponse<AlertDisposalVO> getAlertDisposalVOById(long id) {
        BaseResponse<AlertDisposal> response = getAlertDisposalById(id);
        return ResultUtils.success(alertDisposalService.getAlertDisposalVO(response.getData()));
    }

    @PostMapping("/list")
    public BaseResponse<List<AlertDisposal>> listAlertDisposal(
            @RequestBody AlertDisposalQueryRequest alertDisposalQueryRequest) {
        if (alertDisposalQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(
                alertDisposalService.list(
                        alertDisposalService.getQueryWrapper(alertDisposalQueryRequest)));
    }

    @PostMapping("/list/vo")
    public BaseResponse<List<AlertDisposalVO>> listAlertDisposalVO(
            @RequestBody AlertDisposalQueryRequest alertDisposalQueryRequest) {
        BaseResponse<List<AlertDisposal>> response = listAlertDisposal(alertDisposalQueryRequest);
        return ResultUtils.success(alertDisposalService.getAlertDisposalVO(response.getData()));
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<AlertDisposal>> listAlertDisposalByPage(
            @RequestBody AlertDisposalQueryRequest alertDisposalQueryRequest) {
        if (alertDisposalQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = alertDisposalQueryRequest.getCurrent();
        long size = alertDisposalQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 100, ErrorCode.PARAMS_ERROR, "分页大小不能超过100");
        Page<AlertDisposal> alertDisposalPage =
                alertDisposalService.page(
                        new Page<>(current, size),
                        alertDisposalService.getQueryWrapper(alertDisposalQueryRequest));
        return ResultUtils.success(alertDisposalPage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<AlertDisposalVO>> listAlertDisposalVOByPage(
            @RequestBody AlertDisposalQueryRequest alertDisposalQueryRequest) {
        BaseResponse<Page<AlertDisposal>> response =
                listAlertDisposalByPage(alertDisposalQueryRequest);
        Page<AlertDisposal> alertDisposalPage = response.getData();
        Page<AlertDisposalVO> alertDisposalVOPage =
                new Page<>(
                        alertDisposalPage.getCurrent(),
                        alertDisposalPage.getSize(),
                        alertDisposalPage.getTotal());
        alertDisposalVOPage.setRecords(
                alertDisposalService.getAlertDisposalVO(alertDisposalPage.getRecords()));
        return ResultUtils.success(alertDisposalVOPage);
    }

    private AlertDisposal toAlertDisposal(AlertDisposalAddRequest request) {
        AlertDisposal alertDisposal = new AlertDisposal();
        alertDisposal.setAlert_id(request.getAlertId());
        alertDisposal.setOperator_user_id(request.getOperatorUserId());
        alertDisposal.setOperator_role(request.getOperatorRole());
        alertDisposal.setAction_type(request.getActionType());
        alertDisposal.setAction_note(request.getActionNote());
        alertDisposal.setAction_time(request.getActionTime());
        return alertDisposal;
    }

    private AlertDisposal toAlertDisposal(AlertDisposalUpdateRequest request) {
        AlertDisposal alertDisposal = new AlertDisposal();
        alertDisposal.setId(request.getId());
        alertDisposal.setAlert_id(request.getAlertId());
        alertDisposal.setOperator_user_id(request.getOperatorUserId());
        alertDisposal.setOperator_role(request.getOperatorRole());
        alertDisposal.setAction_type(request.getActionType());
        alertDisposal.setAction_note(request.getActionNote());
        alertDisposal.setAction_time(request.getActionTime());
        return alertDisposal;
    }
}
