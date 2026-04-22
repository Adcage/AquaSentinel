package com.springboot.service;

import com.springboot.model.dto.auth.AdminLoginRequest;
import com.springboot.model.dto.auth.LoginRequest;
import com.springboot.model.dto.auth.LogoutRequest;
import com.springboot.model.dto.auth.RefreshTokenRequest;
import com.springboot.model.dto.auth.RegisterRequest;
import com.springboot.model.vo.CaptchaVO;
import com.springboot.model.vo.LoginResultVO;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    LoginResultVO login(LoginRequest request, HttpServletRequest httpServletRequest);

    LoginResultVO adminLogin(AdminLoginRequest request, HttpServletRequest httpServletRequest);

    Long register(RegisterRequest request);

    LoginResultVO refreshToken(RefreshTokenRequest request, HttpServletRequest httpServletRequest);

    Boolean logout(Long userId, LogoutRequest request);

    CaptchaVO getCaptcha();
}
