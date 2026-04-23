package com.springboot.controller;

import com.springboot.annotation.AuthCheck;
import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.model.dto.systemsettings.NoticeSettingsUpdateRequest;
import com.springboot.model.vo.NoticeSettingsVO;
import com.springboot.service.SystemNoticeConfigService;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system-settings")
public class SystemSettingsController {

    @Resource private SystemNoticeConfigService systemNoticeConfigService;

    @GetMapping("/notice")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<NoticeSettingsVO> getNoticeSettings() {
        return ResultUtils.success(systemNoticeConfigService.getNoticeSettings());
    }

    @PostMapping("/notice")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<NoticeSettingsVO> saveNoticeSettings(
            @RequestBody NoticeSettingsUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        return ResultUtils.success(
                systemNoticeConfigService.updateNoticeSettings(
                        request.getOffDutyThreshold(),
                        request.getDeviceOfflineThreshold(),
                        request.getDrowningAlertThreshold()));
    }
}
