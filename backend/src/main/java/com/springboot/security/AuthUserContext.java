package com.springboot.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class AuthUserContext {

    private Long userId;

    private String username;

    private Set<String> roleCodes = new HashSet<>();

    private Set<String> permissionCodes = new HashSet<>();

    public Set<String> getRoleCodes() {
        return Collections.unmodifiableSet(roleCodes);
    }

    public void setRoleCodes(Set<String> roleCodes) {
        this.roleCodes = roleCodes == null ? new HashSet<>() : new HashSet<>(roleCodes);
    }

    public Set<String> getPermissionCodes() {
        return Collections.unmodifiableSet(permissionCodes);
    }

    public void setPermissionCodes(Set<String> permissionCodes) {
        this.permissionCodes = permissionCodes == null ? new HashSet<>() : new HashSet<>(permissionCodes);
    }

    public boolean hasRole(String roleCode) {
        if (StringUtils.isBlank(roleCode)) {
            return true;
        }
        return roleCodes.contains(roleCode);
    }

    public boolean hasPermission(String permissionCode) {
        if (StringUtils.isBlank(permissionCode)) {
            return true;
        }
        if (permissionCodes.contains("ALL:*") || permissionCodes.contains(permissionCode)) {
            return true;
        }
        for (String grantedPermission : permissionCodes) {
            if (StringUtils.isBlank(grantedPermission)) {
                continue;
            }
            if (grantedPermission.endsWith(":*")) {
                String prefix = grantedPermission.substring(0, grantedPermission.length() - 1);
                if (permissionCode.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }
}
