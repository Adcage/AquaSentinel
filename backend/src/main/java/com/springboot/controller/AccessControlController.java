package com.springboot.controller;

import com.springboot.annotation.AuthCheck;
import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.model.dto.access.RolePermissionUpdateRequest;
import com.springboot.model.dto.access.UserAssignRoleRequest;
import com.springboot.model.dto.access.UserUpdateMyProfileRequest;
import com.springboot.security.AuthContextHolder;
import com.springboot.security.AuthUserContext;
import com.springboot.service.AccessControlService;
import jakarta.annotation.Resource;
import java.util.Objects;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccessControlController {

    @Resource
    private AccessControlService accessControlService;

    @PostMapping("/users/assign/role")
    @AuthCheck(mustRole = RoleConstant.SUPER_ADMIN, mustPermission = "user:role:assign")
    public BaseResponse<Boolean> assignUserRole(@RequestBody UserAssignRoleRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        boolean updated = accessControlService.assignRoles(request.getUserId(), request.getRoleCodes());
        return ResultUtils.success(updated);
    }

    @PostMapping("/roles/permissions/update")
    @AuthCheck(mustRole = RoleConstant.SUPER_ADMIN, mustPermission = "role:permission:update")
    public BaseResponse<Boolean> updateRolePermission(@RequestBody RolePermissionUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        boolean updated = accessControlService.updateRolePermissions(request.getRoleCode(), request.getPermissions());
        return ResultUtils.success(updated);
    }

    @PostMapping("/users/update/my")
    public BaseResponse<Boolean> updateMyProfile(@RequestBody UserUpdateMyProfileRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        AuthUserContext authUserContext = AuthContextHolder.getRequired();
        if (request.getId() != null && !Objects.equals(request.getId(), authUserContext.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅允许修改当前登录账号");
        }
        boolean updated = accessControlService.updateMyProfile(
                authUserContext.getUserId(),
                request.getDisplayName(),
                request.getPhone(),
                request.getEmail(),
                request.getOldPassword(),
                request.getNewPassword());
        return ResultUtils.success(updated);
    }
}
