package com.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.common.ErrorCode;
import com.springboot.exception.BusinessException;
import com.springboot.mapper.SysRoleMapper;
import com.springboot.mapper.SysUserMapper;
import com.springboot.mapper.SysUserRoleMapper;
import com.springboot.model.entity.SysRole;
import com.springboot.model.entity.SysUser;
import com.springboot.model.entity.SysUserRole;
import com.springboot.service.AccessControlService;
import com.springboot.utils.PasswordHashUtils;
import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessControlServiceImpl implements AccessControlService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRoles(Long userId, List<String> roleCodes) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "userId不能为空");
        }
        List<String> normalizedRoleCodes = normalizeCodes(roleCodes);
        if (normalizedRoleCodes.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "roleCodes不能为空");
        }
        SysUser sysUser = sysUserMapper.selectById(userId);
        if (sysUser == null || Objects.equals(sysUser.getIs_delete(), 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        QueryWrapper<SysRole> roleQuery = new QueryWrapper<>();
        roleQuery.in("role_code", normalizedRoleCodes);
        roleQuery.eq("status", 1);
        roleQuery.eq("is_delete", 0);
        List<SysRole> roleList = sysRoleMapper.selectList(roleQuery);
        if (roleList.size() != normalizedRoleCodes.size()) {
            Set<String> existed = new LinkedHashSet<>();
            for (SysRole role : roleList) {
                existed.add(role.getRole_code());
            }
            List<String> missing = new ArrayList<>();
            for (String code : normalizedRoleCodes) {
                if (!existed.contains(code)) {
                    missing.add(code);
                }
            }
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色不存在: " + String.join(",", missing));
        }

        QueryWrapper<SysUserRole> deleteQuery = new QueryWrapper<>();
        deleteQuery.eq("user_id", userId);
        sysUserRoleMapper.delete(deleteQuery);

        Date now = new Date();
        for (SysRole role : roleList) {
            SysUserRole relation = new SysUserRole();
            relation.setUser_id(userId);
            relation.setRole_id(role.getId());
            relation.setCreated_at(now);
            sysUserRoleMapper.insert(relation);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRolePermissions(String roleCode, List<String> permissions) {
        if (StringUtils.isBlank(roleCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "roleCode不能为空");
        }
        QueryWrapper<SysRole> roleQuery = new QueryWrapper<>();
        roleQuery.eq("role_code", roleCode.trim());
        roleQuery.eq("is_delete", 0);
        SysRole role = sysRoleMapper.selectOne(roleQuery);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "角色不存在");
        }
        List<String> normalizedPermissions = normalizeCodes(permissions);
        if (normalizedPermissions.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "permissions不能为空");
        }
        try {
            role.setPermission_json(objectMapper.writeValueAsString(normalizedPermissions));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "权限序列化失败");
        }
        role.setUpdated_at(new Date());
        return sysRoleMapper.updateById(role) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMyProfile(Long userId, String displayName, String phone, String email,
                                   String oldPassword, String newPassword) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        SysUser currentUser = sysUserMapper.selectById(userId);
        if (currentUser == null || Objects.equals(currentUser.getIs_delete(), 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        boolean changed = false;

        if (StringUtils.isNotBlank(displayName)) {
            update.setDisplay_name(displayName.trim());
            changed = true;
        }
        if (StringUtils.isNotBlank(phone)) {
            update.setPhone(phone.trim());
            changed = true;
        }
        if (StringUtils.isNotBlank(email)) {
            update.setEmail(email.trim());
            changed = true;
        }
        if (StringUtils.isNotBlank(newPassword)) {
            if (newPassword.length() < 6) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码长度不能少于6位");
            }
            if (StringUtils.isBlank(oldPassword)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "修改密码必须提供原密码");
            }
            String oldPasswordHash = PasswordHashUtils.md5WithSalt(oldPassword);
            if (!StringUtils.equals(oldPasswordHash, currentUser.getPassword_hash())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "原密码错误");
            }
            update.setPassword_hash(PasswordHashUtils.md5WithSalt(newPassword));
            update.setForce_change_password(0);
            changed = true;
        }
        if (!changed) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未提供可更新字段");
        }
        update.setUpdated_at(new Date());
        return sysUserMapper.updateById(update) > 0;
    }

    @Override
    public Set<String> listPermissionsByRoleCodes(Collection<String> roleCodes) {
        List<String> normalizedRoleCodes = normalizeCodes(roleCodes);
        if (normalizedRoleCodes.isEmpty()) {
            return Set.of();
        }
        QueryWrapper<SysRole> roleQuery = new QueryWrapper<>();
        roleQuery.in("role_code", normalizedRoleCodes);
        roleQuery.eq("status", 1);
        roleQuery.eq("is_delete", 0);
        List<SysRole> roleList = sysRoleMapper.selectList(roleQuery);
        Set<String> permissions = new LinkedHashSet<>();
        for (SysRole role : roleList) {
            appendPermissions(permissions, role.getPermission_json());
        }
        return permissions;
    }

    @Override
    public Set<String> listPermissionsByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            return Set.of();
        }
        QueryWrapper<SysUserRole> userRoleQuery = new QueryWrapper<>();
        userRoleQuery.eq("user_id", userId);
        List<SysUserRole> relations = sysUserRoleMapper.selectList(userRoleQuery);
        if (relations.isEmpty()) {
            return Set.of();
        }
        List<Long> roleIds = relations.stream().map(SysUserRole::getRole_id).filter(Objects::nonNull).distinct().toList();
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        QueryWrapper<SysRole> roleQuery = new QueryWrapper<>();
        roleQuery.in("id", roleIds);
        roleQuery.eq("status", 1);
        roleQuery.eq("is_delete", 0);
        List<SysRole> roleList = sysRoleMapper.selectList(roleQuery);
        Set<String> permissions = new LinkedHashSet<>();
        for (SysRole role : roleList) {
            appendPermissions(permissions, role.getPermission_json());
        }
        return permissions;
    }

    @Override
    public Set<String> listRoleCodesByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            return Set.of();
        }
        QueryWrapper<SysUserRole> userRoleQuery = new QueryWrapper<>();
        userRoleQuery.eq("user_id", userId);
        List<SysUserRole> relations = sysUserRoleMapper.selectList(userRoleQuery);
        if (relations.isEmpty()) {
            return Set.of();
        }
        List<Long> roleIds = relations.stream().map(SysUserRole::getRole_id).filter(Objects::nonNull).distinct().toList();
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        QueryWrapper<SysRole> roleQuery = new QueryWrapper<>();
        roleQuery.in("id", roleIds);
        roleQuery.eq("status", 1);
        roleQuery.eq("is_delete", 0);
        List<SysRole> roleList = sysRoleMapper.selectList(roleQuery);
        Set<String> roleCodes = new LinkedHashSet<>();
        for (SysRole role : roleList) {
            if (StringUtils.isNotBlank(role.getRole_code())) {
                roleCodes.add(role.getRole_code());
            }
        }
        return roleCodes;
    }

    private List<String> normalizeCodes(Collection<String> rawCodes) {
        if (rawCodes == null || rawCodes.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String item : rawCodes) {
            if (StringUtils.isBlank(item)) {
                continue;
            }
            normalized.add(item.trim());
        }
        return new ArrayList<>(normalized);
    }

    private void appendPermissions(Set<String> sink, Object permissionJson) {
        if (permissionJson == null) {
            return;
        }
        if (permissionJson instanceof Collection<?> collection) {
            for (Object item : collection) {
                appendPermissions(sink, item);
            }
            return;
        }
        if (permissionJson instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                appendPermissions(sink, value);
            }
            return;
        }
        if (permissionJson instanceof byte[] bytes) {
            appendPermissions(sink, new String(bytes, StandardCharsets.UTF_8));
            return;
        }
        if (!(permissionJson instanceof String text)) {
            sink.add(String.valueOf(permissionJson));
            return;
        }

        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (!trimmed.startsWith("[")) {
            sink.add(trimmed);
            return;
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(trimmed);
            if (!jsonNode.isArray()) {
                sink.add(trimmed);
                return;
            }
            for (JsonNode node : jsonNode) {
                if (node == null || node.isNull()) {
                    continue;
                }
                String permission = node.asText();
                if (StringUtils.isNotBlank(permission)) {
                    sink.add(permission.trim());
                }
            }
        } catch (Exception ignored) {
            sink.add(trimmed);
        }
    }
}
