package com.springboot.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import com.springboot.mapper.SysRoleMapper;
import com.springboot.mapper.SysUserMapper;
import com.springboot.mapper.SysUserRoleMapper;
import com.springboot.model.entity.SysRole;
import com.springboot.model.entity.SysUserRole;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceImplTest {

    @Mock private SysUserMapper sysUserMapper;

    @Mock private SysRoleMapper sysRoleMapper;

    @Mock private SysUserRoleMapper sysUserRoleMapper;

    @Mock private ObjectMapper objectMapper;

    @InjectMocks private AccessControlServiceImpl accessControlService;

    @Test
    void listRoleCodesByUserIdShouldReturnDistinctCodes() {
        SysUserRole r1 = new SysUserRole();
        r1.setRole_id(1L);
        SysUserRole r2 = new SysUserRole();
        r2.setRole_id(2L);
        SysUserRole r3 = new SysUserRole();
        r3.setRole_id(1L);
        when(sysUserRoleMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(r1, r2, r3));

        SysRole role1 = new SysRole();
        role1.setRole_code("VENUE_ADMIN");
        SysRole role2 = new SysRole();
        role2.setRole_code("SUPER_ADMIN");
        when(sysRoleMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(role1, role2));

        Set<String> roleCodes = accessControlService.listRoleCodesByUserId(10001L);

        assertEquals(2, roleCodes.size());
        assertTrue(roleCodes.contains("VENUE_ADMIN"));
        assertTrue(roleCodes.contains("SUPER_ADMIN"));
    }
}
