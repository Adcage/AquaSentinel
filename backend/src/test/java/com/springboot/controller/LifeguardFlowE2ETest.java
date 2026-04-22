package com.springboot.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.springboot.common.BaseResponse;
import com.springboot.model.dto.auth.LoginRequest;
import com.springboot.model.dto.lifeguard.LifeguardAddRequest;
import com.springboot.model.dto.lifeguardlocationlog.LifeguardLocationReportRequest;
import com.springboot.model.entity.Lifeguard;
import com.springboot.model.entity.LifeguardLocationLog;
import com.springboot.model.entity.SysRole;
import com.springboot.model.entity.SysUser;
import com.springboot.model.vo.LoginResultVO;
import com.springboot.service.AuthService;
import com.springboot.service.LifeguardLocationLogService;
import com.springboot.service.LifeguardService;
import com.springboot.service.VenueService;
import com.springboot.service.impl.LifeguardOffPostAlertService;
import com.springboot.mapper.SysRoleMapper;
import com.springboot.mapper.SysUserMapper;
import com.springboot.mapper.SysUserRoleMapper;
import com.springboot.security.AuthContextHolder;
import com.springboot.security.AuthUserContext;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LifeguardFlowE2ETest {

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

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void endToEndFlowShouldSupportAddLoginAndLocationReport() {
        LifeguardAddRequest addRequest = new LifeguardAddRequest();
        addRequest.setUserId(20001L);
        addRequest.setFullName("联调救生员");
        addRequest.setPhone("13800138123");
        addRequest.setVenueId(2001L);
        addRequest.setAuditStatus("APPROVED");
        addRequest.setDutyStatus("OFF_DUTY");

        SysUser user = new SysUser();
        user.setId(20001L);
        user.setIs_delete(0);
        when(sysUserMapper.selectById(20001L)).thenReturn(user);
        when(lifeguardService.count(any())).thenReturn(0L);

        SysRole role = new SysRole();
        role.setId(3L);
        when(sysRoleMapper.selectOne(any())).thenReturn(role);
        when(sysUserRoleMapper.selectCount(any())).thenReturn(0L);
        when(sysUserRoleMapper.insert(any())).thenReturn(1);
        when(lifeguardService.save(any(Lifeguard.class))).thenAnswer(invocation -> {
            Lifeguard lifeguard = invocation.getArgument(0);
            lifeguard.setId(91001L);
            return true;
        });

        BaseResponse<Long> addResponse = controller.addLifeguard(addRequest);
        assertEquals(0, addResponse.getCode());
        assertEquals(91001L, addResponse.getData());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("lg.e2e.user");
        loginRequest.setPassword("123456");

        LoginResultVO loginResultVO = new LoginResultVO();
        LoginResultVO.UserInfo userInfo = new LoginResultVO.UserInfo();
        userInfo.setId(20001L);
        userInfo.setUsername("lg.e2e.user");
        userInfo.setDisplayName("联调救生员");
        userInfo.setRoles(new java.util.ArrayList<>(Set.of("LIFEGUARD")));
        loginResultVO.setUser(userInfo);
        when(authService.login(any(), any())).thenReturn(loginResultVO);

        Lifeguard lifeguard = new Lifeguard();
        lifeguard.setId(91001L);
        lifeguard.setUser_id(20001L);
        lifeguard.setVenue_id(2001L);
        lifeguard.setAudit_status("APPROVED");
        lifeguard.setIs_delete(0);
        when(lifeguardService.getOne(any())).thenReturn(lifeguard);
        when(lifeguardService.getById(91001L)).thenReturn(lifeguard);
        when(lifeguardService.updateDutyStatus(91001L, "ON_DUTY", 20001L)).thenReturn(true);

        BaseResponse<LoginResultVO> loginResponse = controller.lifeguardLogin(loginRequest, null);
        assertEquals(0, loginResponse.getCode());
        assertNotNull(loginResponse.getData());

        AuthUserContext authUserContext = new AuthUserContext();
        authUserContext.setUserId(20001L);
        authUserContext.setRoleCodes(Set.of("LIFEGUARD"));
        AuthContextHolder.set(authUserContext);

        LifeguardLocationReportRequest locationReportRequest = new LifeguardLocationReportRequest();
        locationReportRequest.setLifeguardId(91001L);
        locationReportRequest.setLongitude(new BigDecimal("121.480312"));
        locationReportRequest.setLatitude(new BigDecimal("31.225341"));
        locationReportRequest.setReportSource("APP_GPS");
        locationReportRequest.setReportedAt(new Date());

        when(lifeguardLocationLogService.reportLocation(any(LifeguardLocationLog.class))).thenReturn(true);
        when(lifeguardOffPostAlertService.checkAfterLocationReport(any(LifeguardLocationLog.class)))
                .thenReturn(Map.of("offPostAlert", false, "created", false));

        BaseResponse<Map<String, Object>> locationResponse = controller.reportLocation(locationReportRequest);
        assertEquals(0, locationResponse.getCode());
        assertTrue((Boolean) locationResponse.getData().get("saved"));
    }
}
