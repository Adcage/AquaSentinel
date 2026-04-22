package com.springboot.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.springboot.exception.BusinessException;
import com.springboot.mapper.SysRoleMapper;
import com.springboot.mapper.SysUserMapper;
import com.springboot.mapper.SysUserRoleMapper;
import com.springboot.model.dto.auth.RegisterRequest;
import com.springboot.model.entity.SysUser;
import com.springboot.model.vo.CaptchaVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @Test
    void registerShouldRequireCaptcha() {
        AuthServiceImpl service = new AuthServiceImpl();
        ReflectionTestUtils.setField(service, "sysUserMapper", sysUserMapper);
        ReflectionTestUtils.setField(service, "sysRoleMapper", sysRoleMapper);
        ReflectionTestUtils.setField(service, "sysUserRoleMapper", sysUserRoleMapper);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("new.user");
        request.setPassword("123456");
        request.setDisplayName("测试用户");
        request.setRoleCode("USER");

        assertThrows(BusinessException.class, () -> service.register(request));
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void getCaptchaShouldReturnImageDataInsteadOfPlainCode() {
        AuthServiceImpl service = new AuthServiceImpl();
        CaptchaVO captchaVO = service.getCaptcha();

        assertNotNull(captchaVO.getCaptchaId());
        assertTrue(captchaVO.getCaptchaImageBase64().startsWith("data:image/png;base64,"));
    }
}
