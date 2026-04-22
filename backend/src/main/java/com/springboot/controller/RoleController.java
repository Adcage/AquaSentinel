package com.springboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.annotation.AuthCheck;
import com.springboot.common.BaseResponse;
import com.springboot.common.DeleteRequest;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.exception.ThrowUtils;
import com.springboot.model.dto.role.RoleAddRequest;
import com.springboot.model.dto.role.RoleQueryRequest;
import com.springboot.model.dto.role.RoleUpdateRequest;
import com.springboot.model.entity.SysRole;
import com.springboot.model.entity.SysUserRole;
import com.springboot.model.vo.RoleVO;
import com.springboot.service.SysRoleService;
import com.springboot.service.SysUserRoleService;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/roles")
public class RoleController {

    @Resource
    private SysRoleService sysRoleService;

    @Resource
    private SysUserRoleService sysUserRoleService;

    @Resource
    private ObjectMapper objectMapper;

    @PostMapping("/add")
    @AuthCheck(mustRole = RoleConstant.SUPER_ADMIN)
    public BaseResponse<Long> addRole(@RequestBody RoleAddRequest request) {
        if (request == null || StringUtils.isAnyBlank(request.getRoleCode(), request.getRoleName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "roleCode和roleName不能为空");
        }
        QueryWrapper<SysRole> existsQuery = new QueryWrapper<>();
        existsQuery.eq("role_code", request.getRoleCode().trim());
        existsQuery.eq("is_delete", 0);
        ThrowUtils.throwIf(sysRoleService.count(existsQuery) > 0, ErrorCode.PARAMS_ERROR, "角色编码已存在");

        SysRole role = new SysRole();
        role.setRole_code(request.getRoleCode().trim());
        role.setRole_name(request.getRoleName().trim());
        role.setPermission_json(toPermissionJson(request.getPermissions()));
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        role.setIs_delete(0);
        role.setCreated_at(new Date());
        role.setUpdated_at(new Date());
        boolean saved = sysRoleService.save(role);
        ThrowUtils.throwIf(!saved || role.getId() == null, ErrorCode.OPERATION_ERROR, "新增角色失败");
        return ResultUtils.success(role.getId());
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = RoleConstant.SUPER_ADMIN)
    public BaseResponse<Boolean> deleteRole(@RequestBody DeleteRequest request) {
        if (request == null || request.getId() == null || request.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id不能为空");
        }
        SysRole oldRole = sysRoleService.getById(request.getId());
        ThrowUtils.throwIf(oldRole == null || Integer.valueOf(1).equals(oldRole.getIs_delete()), ErrorCode.NOT_FOUND_ERROR, "角色不存在");

        QueryWrapper<SysUserRole> userRoleQuery = new QueryWrapper<>();
        userRoleQuery.eq("role_id", request.getId());
        ThrowUtils.throwIf(sysUserRoleService.count(userRoleQuery) > 0, ErrorCode.OPERATION_ERROR, "角色已绑定用户，不能删除");

        SysRole update = new SysRole();
        update.setId(request.getId());
        update.setIs_delete(1);
        update.setStatus(0);
        update.setUpdated_at(new Date());
        return ResultUtils.success(sysRoleService.updateById(update));
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = RoleConstant.SUPER_ADMIN)
    public BaseResponse<Boolean> updateRole(@RequestBody RoleUpdateRequest request) {
        if (request == null || request.getId() == null || request.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id不能为空");
        }
        SysRole oldRole = sysRoleService.getById(request.getId());
        ThrowUtils.throwIf(oldRole == null || Integer.valueOf(1).equals(oldRole.getIs_delete()), ErrorCode.NOT_FOUND_ERROR, "角色不存在");

        SysRole update = new SysRole();
        update.setId(request.getId());
        if (StringUtils.isNotBlank(request.getRoleCode()) && !StringUtils.equals(request.getRoleCode().trim(), oldRole.getRole_code())) {
            QueryWrapper<SysRole> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("role_code", request.getRoleCode().trim());
            queryWrapper.eq("is_delete", 0);
            ThrowUtils.throwIf(sysRoleService.count(queryWrapper) > 0, ErrorCode.PARAMS_ERROR, "角色编码已存在");
            update.setRole_code(request.getRoleCode().trim());
        }
        if (StringUtils.isNotBlank(request.getRoleName())) {
            update.setRole_name(request.getRoleName().trim());
        }
        if (request.getPermissions() != null) {
            update.setPermission_json(toPermissionJson(request.getPermissions()));
        }
        if (request.getStatus() != null) {
            update.setStatus(request.getStatus());
        }
        update.setUpdated_at(new Date());
        return ResultUtils.success(sysRoleService.updateById(update));
    }

    @GetMapping("/get")
    @AuthCheck(mustRole = RoleConstant.SUPER_ADMIN)
    public BaseResponse<SysRole> getRoleById(@RequestParam Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id不能为空");
        }
        SysRole role = sysRoleService.getById(id);
        ThrowUtils.throwIf(role == null || Integer.valueOf(1).equals(role.getIs_delete()), ErrorCode.NOT_FOUND_ERROR, "角色不存在");
        return ResultUtils.success(role);
    }

    @GetMapping("/get/vo")
    @AuthCheck(mustRole = RoleConstant.SUPER_ADMIN)
    public BaseResponse<RoleVO> getRoleVOById(@RequestParam Long id) {
        BaseResponse<SysRole> baseResponse = getRoleById(id);
        return ResultUtils.success(toRoleVO(baseResponse.getData()));
    }

    @PostMapping("/list/page")
    @AuthCheck(mustRole = RoleConstant.SUPER_ADMIN)
    public BaseResponse<Page<SysRole>> listRolePage(@RequestBody(required = false) RoleQueryRequest request) {
        RoleQueryRequest queryRequest = request == null ? new RoleQueryRequest() : request;
        long current = Math.max(1, queryRequest.getCurrent());
        long pageSize = Math.min(100, Math.max(1, queryRequest.getPageSize()));
        QueryWrapper<SysRole> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0);
        queryWrapper.eq(queryRequest.getId() != null, "id", queryRequest.getId());
        queryWrapper.like(StringUtils.isNotBlank(queryRequest.getRoleCode()), "role_code", StringUtils.trim(queryRequest.getRoleCode()));
        queryWrapper.like(StringUtils.isNotBlank(queryRequest.getRoleName()), "role_name", StringUtils.trim(queryRequest.getRoleName()));
        queryWrapper.eq(queryRequest.getStatus() != null, "status", queryRequest.getStatus());
        queryWrapper.orderByDesc("id");
        Page<SysRole> page = sysRoleService.page(new Page<>(current, pageSize), queryWrapper);
        return ResultUtils.success(page);
    }

    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = RoleConstant.SUPER_ADMIN)
    public BaseResponse<Page<RoleVO>> listRolePageVO(@RequestBody(required = false) RoleQueryRequest request) {
        BaseResponse<Page<SysRole>> baseResponse = listRolePage(request);
        Page<SysRole> rolePage = baseResponse.getData();
        Page<RoleVO> voPage = new Page<>(rolePage.getCurrent(), rolePage.getSize(), rolePage.getTotal());
        voPage.setRecords(rolePage.getRecords().stream().map(this::toRoleVO).toList());
        return ResultUtils.success(voPage);
    }

    private String toPermissionJson(List<String> permissions) {
        List<String> normalized = permissions == null ? List.of() : permissions.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        java.util.ArrayList::new));
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "权限序列化失败");
        }
    }

    private RoleVO toRoleVO(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRole_code());
        vo.setRoleName(role.getRole_name());
        Object permJson = role.getPermission_json();
        if (permJson instanceof String jsonStr) {
            try {
                vo.setPermissionJson(objectMapper.readValue(jsonStr, Object.class));
            } catch (Exception e) {
                vo.setPermissionJson(java.util.List.of());
            }
        } else {
            vo.setPermissionJson(permJson);
        }
        vo.setStatus(role.getStatus());
        vo.setCreatedAt(role.getCreated_at());
        vo.setUpdatedAt(role.getUpdated_at());
        return vo;
    }
}
