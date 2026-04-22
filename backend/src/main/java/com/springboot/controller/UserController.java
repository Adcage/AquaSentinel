package com.springboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.springboot.annotation.AuthCheck;
import com.springboot.common.BaseResponse;
import com.springboot.common.DeleteRequest;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.exception.ThrowUtils;
import com.springboot.model.dto.user.UserAddRequest;
import com.springboot.model.dto.user.UserQueryRequest;
import com.springboot.model.dto.user.UserUpdateRequest;
import com.springboot.model.entity.SysRole;
import com.springboot.model.entity.SysUser;
import com.springboot.model.entity.SysUserRole;
import com.springboot.model.entity.Lifeguard;
import com.springboot.model.vo.UserVO;
import com.springboot.service.AccessControlService;
import com.springboot.service.SysRoleService;
import com.springboot.service.SysUserRoleService;
import com.springboot.service.SysUserService;
import com.springboot.service.LifeguardService;
import com.springboot.utils.PasswordHashUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private SysUserRoleService sysUserRoleService;

    @Resource
    private SysRoleService sysRoleService;

    @Resource
    private AccessControlService accessControlService;

    @Resource
    private LifeguardService lifeguardService;

    @PostMapping("/add")
    @AuthCheck(mustRole = RoleConstant.SUPER_ADMIN)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest request) {
        if (request == null || StringUtils.isAnyBlank(request.getUsername(), request.getPassword(), request.getDisplayName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名、密码、显示名不能为空");
        }
        if (request.getPassword().length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能少于6位");
        }
        QueryWrapper<SysUser> existsQuery = new QueryWrapper<>();
        existsQuery.eq("username", request.getUsername().trim());
        existsQuery.eq("is_delete", 0);
        ThrowUtils.throwIf(sysUserService.count(existsQuery) > 0, ErrorCode.PARAMS_ERROR, "用户名已存在");

        SysUser user = new SysUser();
        user.setUsername(request.getUsername().trim());
        user.setPassword_hash(PasswordHashUtils.md5WithSalt(request.getPassword()));
        user.setDisplay_name(request.getDisplayName().trim());
        user.setPhone(StringUtils.trimToNull(request.getPhone()));
        user.setEmail(StringUtils.trimToNull(request.getEmail()));
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        user.setFailed_login_count(0);
        user.setForce_change_password(request.getForceChangePassword() == null ? 1 : request.getForceChangePassword());
        user.setIs_delete(0);
        user.setCreated_at(new Date());
        user.setUpdated_at(new Date());
        boolean saved = sysUserService.save(user);
        ThrowUtils.throwIf(!saved || user.getId() == null, ErrorCode.OPERATION_ERROR, "新增用户失败");

        List<String> roleCodes = request.getRoleCodes();
        if (roleCodes == null || roleCodes.isEmpty()) {
            roleCodes = List.of(RoleConstant.USER);
        }
        accessControlService.assignRoles(user.getId(), roleCodes);
        return ResultUtils.success(user.getId());
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = RoleConstant.SUPER_ADMIN)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest request) {
        if (request == null || request.getId() == null || request.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id不能为空");
        }
        SysUser oldUser = sysUserService.getById(request.getId());
        ThrowUtils.throwIf(oldUser == null || Integer.valueOf(1).equals(oldUser.getIs_delete()), ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        SysUser update = new SysUser();
        update.setId(request.getId());
        update.setIs_delete(1);
        update.setStatus(0);
        update.setUpdated_at(new Date());
        boolean result = sysUserService.updateById(update);
        return ResultUtils.success(result);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = RoleConstant.SUPER_ADMIN)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest request) {
        if (request == null || request.getId() == null || request.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id不能为空");
        }
        SysUser oldUser = sysUserService.getById(request.getId());
        ThrowUtils.throwIf(oldUser == null || Integer.valueOf(1).equals(oldUser.getIs_delete()), ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        SysUser update = new SysUser();
        update.setId(request.getId());
        if (StringUtils.isNotBlank(request.getUsername()) && !StringUtils.equals(request.getUsername().trim(), oldUser.getUsername())) {
            QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", request.getUsername().trim());
            queryWrapper.eq("is_delete", 0);
            ThrowUtils.throwIf(sysUserService.count(queryWrapper) > 0, ErrorCode.PARAMS_ERROR, "用户名已存在");
            update.setUsername(request.getUsername().trim());
        }
        if (StringUtils.isNotBlank(request.getPassword())) {
            if (request.getPassword().length() < 6) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能少于6位");
            }
            update.setPassword_hash(PasswordHashUtils.md5WithSalt(request.getPassword()));
        }
        if (StringUtils.isNotBlank(request.getDisplayName())) {
            update.setDisplay_name(request.getDisplayName().trim());
        }
        if (request.getPhone() != null) {
            update.setPhone(StringUtils.trimToNull(request.getPhone()));
        }
        if (request.getEmail() != null) {
            update.setEmail(StringUtils.trimToNull(request.getEmail()));
        }
        if (request.getStatus() != null) {
            update.setStatus(request.getStatus());
        }
        if (request.getForceChangePassword() != null) {
            update.setForce_change_password(request.getForceChangePassword());
        }
        update.setUpdated_at(new Date());
        boolean updated = sysUserService.updateById(update);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新用户失败");

        if (request.getRoleCodes() != null && !request.getRoleCodes().isEmpty()) {
            accessControlService.assignRoles(request.getId(), request.getRoleCodes());
        }
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    @AuthCheck(mustRole = RoleConstant.SUPER_ADMIN)
    public BaseResponse<SysUser> getUserById(@RequestParam Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id不能为空");
        }
        SysUser user = sysUserService.getById(id);
        ThrowUtils.throwIf(user == null || Integer.valueOf(1).equals(user.getIs_delete()), ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        return ResultUtils.success(user);
    }

    @GetMapping("/get/vo")
    @AuthCheck(mustRole = RoleConstant.SUPER_ADMIN)
    public BaseResponse<UserVO> getUserVOById(@RequestParam Long id) {
        BaseResponse<SysUser> baseResponse = getUserById(id);
        return ResultUtils.success(toUserVO(baseResponse.getData(), accessControlService.listRoleCodesByUserId(id)));
    }

    @PostMapping("/list/page")
    @AuthCheck(mustRole = RoleConstant.SUPER_ADMIN)
    public BaseResponse<Page<SysUser>> listUserPage(@RequestBody(required = false) UserQueryRequest request) {
        UserQueryRequest queryRequest = request == null ? new UserQueryRequest() : request;
        long current = Math.max(1, queryRequest.getCurrent());
        long pageSize = Math.min(100, Math.max(1, queryRequest.getPageSize()));
        QueryWrapper<SysUser> queryWrapper = buildUserQueryWrapper(queryRequest);
        Page<SysUser> page = sysUserService.page(new Page<>(current, pageSize), queryWrapper);
        return ResultUtils.success(page);
    }

    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = RoleConstant.SUPER_ADMIN)
    public BaseResponse<Page<UserVO>> listUserPageVO(@RequestBody(required = false) UserQueryRequest request) {
        BaseResponse<Page<SysUser>> baseResponse = listUserPage(request);
        Page<SysUser> userPage = baseResponse.getData();
        Page<UserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserVO> records = buildUserVOList(userPage.getRecords());
        voPage.setRecords(records);
        return ResultUtils.success(voPage);
    }

    private QueryWrapper<SysUser> buildUserQueryWrapper(UserQueryRequest request) {
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0);
        queryWrapper.eq(request.getId() != null, "id", request.getId());
        queryWrapper.like(StringUtils.isNotBlank(request.getUsername()), "username", StringUtils.trim(request.getUsername()));
        queryWrapper.like(StringUtils.isNotBlank(request.getDisplayName()), "display_name", StringUtils.trim(request.getDisplayName()));
        queryWrapper.like(StringUtils.isNotBlank(request.getPhone()), "phone", StringUtils.trim(request.getPhone()));
        queryWrapper.eq(request.getStatus() != null, "status", request.getStatus());
        queryWrapper.orderByDesc("id");
        if (StringUtils.isNotBlank(request.getRoleCode())) {
            QueryWrapper<SysRole> roleQuery = new QueryWrapper<>();
            roleQuery.eq("role_code", request.getRoleCode().trim());
            roleQuery.eq("is_delete", 0);
            SysRole role = sysRoleService.getOne(roleQuery);
            if (role == null) {
                queryWrapper.eq("id", -1L);
                return queryWrapper;
            }
            QueryWrapper<SysUserRole> userRoleQuery = new QueryWrapper<>();
            userRoleQuery.eq("role_id", role.getId());
            List<SysUserRole> userRoles = sysUserRoleService.list(userRoleQuery);
            if (userRoles.isEmpty()) {
                queryWrapper.eq("id", -1L);
                return queryWrapper;
            }
            List<Long> userIds = userRoles.stream().map(SysUserRole::getUser_id).distinct().toList();
            queryWrapper.in("id", userIds);
        }
        return queryWrapper;
    }

    private List<UserVO> buildUserVOList(List<SysUser> userList) {
        if (userList == null || userList.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = userList.stream().map(SysUser::getId).toList();
        QueryWrapper<SysUserRole> relationQuery = new QueryWrapper<>();
        relationQuery.in("user_id", userIds);
        List<SysUserRole> relations = sysUserRoleService.list(relationQuery);

        List<Long> roleIds = relations.stream().map(SysUserRole::getRole_id).distinct().toList();
        Map<Long, String> roleCodeMap = new HashMap<>();
        if (!roleIds.isEmpty()) {
            QueryWrapper<SysRole> roleQuery = new QueryWrapper<>();
            roleQuery.in("id", roleIds);
            roleQuery.eq("is_delete", 0);
            List<SysRole> roles = sysRoleService.list(roleQuery);
            roleCodeMap = roles.stream().collect(Collectors.toMap(SysRole::getId, SysRole::getRole_code, (a, b) -> a));
        }

        Map<Long, List<String>> userRoleCodeMap = new HashMap<>();
        for (SysUserRole relation : relations) {
            String roleCode = roleCodeMap.get(relation.getRole_id());
            if (StringUtils.isBlank(roleCode)) {
                continue;
            }
            userRoleCodeMap.computeIfAbsent(relation.getUser_id(), key -> new ArrayList<>()).add(roleCode);
        }

        Map<Long, Long> linkedLifeguardMap = fetchLinkedLifeguardMap(userIds);
        List<UserVO> voList = new ArrayList<>(userList.size());
        for (SysUser user : userList) {
            List<String> roleCodes = userRoleCodeMap.getOrDefault(user.getId(), List.of());
            voList.add(toUserVO(user, Set.copyOf(roleCodes), linkedLifeguardMap.get(user.getId())));
        }
        return voList;
    }

    private UserVO toUserVO(SysUser user, Set<String> roleCodes) {
        return toUserVO(user, roleCodes, null);
    }

    private UserVO toUserVO(SysUser user, Set<String> roleCodes, Long linkedLifeguardId) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setDisplayName(user.getDisplay_name());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setForceChangePassword(user.getForce_change_password());
        vo.setLastLoginAt(user.getLast_login_at());
        vo.setCreatedAt(user.getCreated_at());
        vo.setUpdatedAt(user.getUpdated_at());
        vo.setRoleCodes(new ArrayList<>(roleCodes));
        vo.setLinkedLifeguardId(linkedLifeguardId);
        return vo;
    }

    private Map<Long, Long> fetchLinkedLifeguardMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<Lifeguard> lifeguardQuery = new QueryWrapper<>();
        lifeguardQuery.in("user_id", userIds);
        lifeguardQuery.eq("is_delete", 0);
        List<Lifeguard> lifeguards = lifeguardService.list(lifeguardQuery);
        Map<Long, Long> result = new HashMap<>();
        for (Lifeguard lifeguard : lifeguards) {
            if (lifeguard.getUser_id() != null && lifeguard.getId() != null) {
                result.put(lifeguard.getUser_id(), lifeguard.getId());
            }
        }
        return result;
    }
}
