package com.springboot.controller;

import com.springboot.common.BaseResponse;
import com.springboot.common.ResultUtils;
import com.springboot.model.dto.auth.AdminLoginRequest;
import com.springboot.model.dto.auth.LoginRequest;
import com.springboot.model.dto.auth.LogoutRequest;
import com.springboot.model.dto.auth.RefreshTokenRequest;
import com.springboot.model.dto.auth.RegisterRequest;
import com.springboot.model.vo.CaptchaVO;
import com.springboot.model.vo.LoginResultVO;
import com.springboot.ratelimit.RateLimit;
import com.springboot.security.AuthContextHolder;
import com.springboot.security.AuthUserContext;
import com.springboot.service.AuthService;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource private AuthService authService;

    @RateLimit(
            capacity = 5,
            refillRate = 5,
            refillPeriodSeconds = 60,
            keyType = "IP",
            fallbackMessage = "登录请求过于频繁，请1分钟后重试")
    @PostMapping("/login")
    public BaseResponse<LoginResultVO> login(
            @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        return ResultUtils.success(authService.login(request, httpServletRequest));
    }

    @RateLimit(
            capacity = 3,
            refillRate = 3,
            refillPeriodSeconds = 60,
            keyType = "IP",
            fallbackMessage = "管理员登录请求过于频繁，请1分钟后重试")
    @PostMapping("/admin/login")
    public BaseResponse<LoginResultVO> adminLogin(
            @RequestBody AdminLoginRequest request, HttpServletRequest httpServletRequest) {
        return ResultUtils.success(authService.adminLogin(request, httpServletRequest));
    }

    @RateLimit(
            capacity = 3,
            refillRate = 3,
            refillPeriodSeconds = 60,
            keyType = "IP",
            fallbackMessage = "注册请求过于频繁，请1分钟后重试")
    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody RegisterRequest request) {
        return ResultUtils.success(authService.register(request));
    }

    @RateLimit(
            capacity = 10,
            refillRate = 10,
            refillPeriodSeconds = 60,
            keyType = "IP",
            fallbackMessage = "刷新令牌请求过于频繁")
    @PostMapping("/refresh")
    public BaseResponse<LoginResultVO> refresh(
            @RequestBody RefreshTokenRequest request, HttpServletRequest httpServletRequest) {
        return ResultUtils.success(authService.refreshToken(request, httpServletRequest));
    }

    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(@RequestBody(required = false) LogoutRequest request) {
        AuthUserContext authUserContext = AuthContextHolder.getRequired();
        LogoutRequest logoutRequest = request == null ? new LogoutRequest() : request;
        return ResultUtils.success(authService.logout(authUserContext.getUserId(), logoutRequest));
    }

    @RateLimit(
            capacity = 20,
            refillRate = 20,
            refillPeriodSeconds = 60,
            keyType = "IP",
            fallbackMessage = "验证码请求过于频繁")
    @GetMapping("/captcha")
    public BaseResponse<CaptchaVO> getCaptcha() {
        return ResultUtils.success(authService.getCaptcha());
    }
}
