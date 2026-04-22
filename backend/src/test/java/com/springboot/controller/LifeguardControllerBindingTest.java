package com.springboot.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.springboot.common.ErrorCode;
import com.springboot.exception.BusinessException;
import com.springboot.mapper.SysRoleMapper;
import com.springboot.mapper.SysUserMapper;
import com.springboot.mapper.SysUserRoleMapper;
import com.springboot.model.dto.lifeguard.LifeguardAddRequest;
import com.springboot.model.entity.Lifeguard;
import com.springboot.model.entity.SysRole;
import com.springboot.model.entity.SysUser;
import com.springboot.model.entity.SysUserRole;
import com.springboot.service.AuthService;
import com.springboot.service.LifeguardLocationLogService;
import com.springboot.service.LifeguardService;
import com.springboot.service.VenueService;
import com.springboot.service.impl.LifeguardOffPostAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LifeguardControllerBindingTest {

    @Mock
    private LifeguardService lifeguardService;

    @Mock
    private LifeguardLocationLogService lifeguardLocationLogService;

    @Mock
    private LifeguardOffPostAlertService lifeguardOffPostAlertService;

    @Mock
    private AuthService authService;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @Mock
    private VenueService venueService;

    private LifeguardController controller;

    @BeforeEach
    void setUp() {
        controller = new LifeguardController();
        ReflectionTestUtils.setField(controller, "lifeguardService", lifeguardService);
        ReflectionTestUtils.setField(controller, "lifeguardLocationLogService", lifeguardLocationLogService);
        ReflectionTestUtils.setField(controller, "lifeguardOffPostAlertService", lifeguardOffPostAlertService);
        ReflectionTestUtils.setField(controller, "authService", authService);
        ReflectionTestUtils.setField(controller, "sysUserMapper", sysUserMapper);
        ReflectionTestUtils.setField(controller, "sysRoleMapper", sysRoleMapper);
        ReflectionTestUtils.setField(controller, "sysUserRoleMapper", sysUserRoleMapper);
        ReflectionTestUtils.setField(controller, "venueService", venueService);
        ReflectionTestUtils.setField(controller, "objectMapper", new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    void addLifeguardShouldRejectWhenUserAlreadyBound() {
        LifeguardAddRequest request = new LifeguardAddRequest();
        request.setUserId(10001L);
        request.setFullName("测试救生员");
        request.setPhone("13800138000");
        request.setVenueId(2001L);
        request.setAuditStatus("PENDING");
        request.setDutyStatus("OFF_DUTY");

        SysUser user = new SysUser();
        user.setId(10001L);
        user.setIs_delete(0);
        when(sysUserMapper.selectById(10001L)).thenReturn(user);
        when(lifeguardService.count(any(QueryWrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> controller.addLifeguard(request));
        org.junit.jupiter.api.Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        verify(lifeguardService, never()).save(any(Lifeguard.class));
    }

    @Test
    void addLifeguardShouldGrantRoleWhenBindingExistingUserWithoutLifeguardRole() {
        LifeguardAddRequest request = new LifeguardAddRequest();
        request.setUserId(10002L);
        request.setFullName("测试救生员");
        request.setPhone("13800138001");
        request.setVenueId(2001L);
        request.setAuditStatus("PENDING");
        request.setDutyStatus("OFF_DUTY");

        SysUser user = new SysUser();
        user.setId(10002L);
        user.setIs_delete(0);
        when(sysUserMapper.selectById(10002L)).thenReturn(user);
        when(lifeguardService.count(any(QueryWrapper.class))).thenReturn(0L);

        SysRole lifeguardRole = new SysRole();
        lifeguardRole.setId(3L);
        when(sysRoleMapper.selectOne(any(QueryWrapper.class))).thenReturn(lifeguardRole);
        when(sysUserRoleMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(sysUserRoleMapper.insert(any(SysUserRole.class))).thenReturn(1);
        when(lifeguardService.save(any(Lifeguard.class))).thenAnswer(invocation -> {
            Lifeguard entity = invocation.getArgument(0);
            entity.setId(9001L);
            return true;
        });

        controller.addLifeguard(request);

        verify(sysUserRoleMapper).insert(any(SysUserRole.class));
        verify(lifeguardService).save(any(Lifeguard.class));
    }

    @Test
    void addLifeguardShouldNotInsertRoleRelationWhenRoleAlreadyGranted() {
        LifeguardAddRequest request = new LifeguardAddRequest();
        request.setUserId(10003L);
        request.setFullName("测试救生员");
        request.setPhone("13800138002");
        request.setVenueId(2001L);

        SysUser user = new SysUser();
        user.setId(10003L);
        user.setIs_delete(0);
        when(sysUserMapper.selectById(10003L)).thenReturn(user);
        when(lifeguardService.count(any(QueryWrapper.class))).thenReturn(0L);

        SysRole lifeguardRole = new SysRole();
        lifeguardRole.setId(3L);
        when(sysRoleMapper.selectOne(any(QueryWrapper.class))).thenReturn(lifeguardRole);
        when(sysUserRoleMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        when(lifeguardService.save(any(Lifeguard.class))).thenAnswer(invocation -> {
            Lifeguard entity = invocation.getArgument(0);
            entity.setId(9002L);
            return true;
        });

        controller.addLifeguard(request);

        verify(sysUserRoleMapper, never()).insert(any(SysUserRole.class));
        verify(lifeguardService).save(any(Lifeguard.class));
    }

    @Test
    void addLifeguardCreateUserModeShouldRejectDuplicatedPhone() {
        LifeguardAddRequest request = new LifeguardAddRequest();
        request.setUsername("lg.new.user");
        request.setPassword("123456");
        request.setPhone("13800138003");
        request.setFullName("测试救生员");
        request.setVenueId(2001L);

        when(sysUserMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> controller.addLifeguard(request));
        org.junit.jupiter.api.Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        verify(sysUserMapper, never()).insert(any(SysUser.class));
        verify(lifeguardService, never()).save(any(Lifeguard.class));
    }
}
