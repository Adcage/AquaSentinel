package com.springboot.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface AccessControlService {

    boolean assignRoles(Long userId, List<String> roleCodes);

    boolean updateRolePermissions(String roleCode, List<String> permissions);

    boolean updateMyProfile(
            Long userId,
            String displayName,
            String phone,
            String email,
            String oldPassword,
            String newPassword);

    Set<String> listPermissionsByRoleCodes(Collection<String> roleCodes);

    Set<String> listPermissionsByUserId(Long userId);

    Set<String> listRoleCodesByUserId(Long userId);
}
