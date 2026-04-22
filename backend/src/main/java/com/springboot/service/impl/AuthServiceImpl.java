package com.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.springboot.common.ErrorCode;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.mapper.AuthRefreshTokenMapper;
import com.springboot.mapper.SysRoleMapper;
import com.springboot.mapper.SysUserMapper;
import com.springboot.mapper.SysUserRoleMapper;
import com.springboot.model.dto.auth.AdminLoginRequest;
import com.springboot.model.dto.auth.LoginRequest;
import com.springboot.model.dto.auth.LogoutRequest;
import com.springboot.model.dto.auth.RefreshTokenRequest;
import com.springboot.model.dto.auth.RegisterRequest;
import com.springboot.model.entity.AuthRefreshToken;
import com.springboot.model.entity.SysRole;
import com.springboot.model.entity.SysUser;
import com.springboot.model.entity.SysUserRole;
import com.springboot.model.vo.CaptchaVO;
import com.springboot.model.vo.LoginResultVO;
import com.springboot.security.JwtTokenProvider;
import com.springboot.model.entity.SystemAuditLog;
import com.springboot.service.AccessControlService;
import com.springboot.service.AuthService;
import com.springboot.service.SystemAuditLogService;
import com.springboot.utils.PasswordHashUtils;
import com.springboot.utils.TokenHashUtils;
import jakarta.annotation.Resource;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final int USER_ENABLE_STATUS = 1;

    private static final int MAX_LOGIN_FAIL_COUNT = 3;

    private static final int LOCK_MINUTES = 30;

    private static final int CAPTCHA_TTL_SECONDS = 300;

    private static final String CLIENT_TYPE_UNKNOWN = "unknown";

    private final Map<String, CaptchaEntry> captchaStore = new ConcurrentHashMap<>();

    private final SecureRandom secureRandom = new SecureRandom();

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private AuthRefreshTokenMapper authRefreshTokenMapper;

    @Resource
    private JwtTokenProvider jwtTokenProvider;

    @Resource
    private AccessControlService accessControlService;

    @Resource
    private SystemAuditLogService systemAuditLogService;

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    public LoginResultVO login(LoginRequest request, HttpServletRequest httpServletRequest) {
        validateLoginRequest(request);
        SysUser user = findUserByUsername(request.getUsername());
        verifyUserCanLogin(user);
        String encryptPassword = PasswordHashUtils.md5WithSalt(request.getPassword());
        if (!StringUtils.equals(encryptPassword, user.getPassword_hash())) {
            onPasswordMismatch(user);
        }
        resetLoginFailState(user.getId());
        List<String> roleCodes = listRoleCodes(user.getId());
        return issueTokens(user, roleCodes, request.getDeviceId(), request.getClientType(), request.getClientVersion(),
                getClientIp(httpServletRequest));
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    public LoginResultVO adminLogin(AdminLoginRequest request, HttpServletRequest httpServletRequest) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode());
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(request.getUsername());
        loginRequest.setPassword(request.getPassword());
        loginRequest.setDeviceId(request.getDeviceId());
        loginRequest.setClientType(request.getClientType());
        loginRequest.setClientVersion(request.getClientVersion());
        LoginResultVO loginResultVO = login(loginRequest, httpServletRequest);
        if (!isAdmin(loginResultVO.getUser().getRoles())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前账号不具备管理员登录权限");
        }
        return loginResultVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        if (StringUtils.isAnyBlank(request.getUsername(), request.getPassword(), request.getDisplayName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名、密码和显示名不能为空");
        }
        if (request.getPassword().length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能少于6位");
        }
        verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode());
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", request.getUsername());
        queryWrapper.eq("is_delete", 0);
        Long count = sysUserMapper.selectCount(queryWrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已存在");
        }
        SysUser sysUser = new SysUser();
        sysUser.setUsername(request.getUsername());
        sysUser.setPassword_hash(PasswordHashUtils.md5WithSalt(request.getPassword()));
        sysUser.setDisplay_name(request.getDisplayName());
        sysUser.setStatus(USER_ENABLE_STATUS);
        sysUser.setFailed_login_count(0);
        sysUser.setForce_change_password(0);
        sysUser.setIs_delete(0);
        int insertRows = sysUserMapper.insert(sysUser);
        if (insertRows != 1 || sysUser.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }
        String targetRoleCode = StringUtils.defaultIfBlank(request.getRoleCode(), RoleConstant.USER);
        SysRole role = getRoleByCode(targetRoleCode);
        if (role == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色不存在");
        }
        SysUserRole sysUserRole = new SysUserRole();
        sysUserRole.setUser_id(sysUser.getId());
        sysUserRole.setRole_id(role.getId());
        sysUserRoleMapper.insert(sysUserRole);
        return sysUser.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResultVO refreshToken(RefreshTokenRequest request, HttpServletRequest httpServletRequest) {
        if (request == null || StringUtils.isBlank(request.getRefreshToken())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "refreshToken不能为空");
        }
        String refreshTokenHash = TokenHashUtils.sha256(request.getRefreshToken());
        QueryWrapper<AuthRefreshToken> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refresh_token_hash", refreshTokenHash);
        queryWrapper.eq("revoked", 0);
        AuthRefreshToken authRefreshToken = authRefreshTokenMapper.selectOne(queryWrapper);
        if (authRefreshToken == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "refreshToken无效");
        }
        if (authRefreshToken.getExpires_at() == null || authRefreshToken.getExpires_at().before(new Date())) {
            revokeToken(authRefreshToken, "REFRESH_TOKEN_EXPIRED");
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "refreshToken已过期");
        }
        if (StringUtils.isNotBlank(request.getDeviceId())
                && !StringUtils.equals(request.getDeviceId(), authRefreshToken.getDevice_id())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "设备不匹配");
        }
        SysUser user = sysUserMapper.selectById(authRefreshToken.getUser_id());
        if (user == null || Objects.equals(user.getIs_delete(), 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        verifyUserCanLogin(user);
        revokeToken(authRefreshToken, "REFRESH_ROTATE");
        List<String> roleCodes = listRoleCodes(user.getId());
        return issueTokens(user, roleCodes, authRefreshToken.getDevice_id(), authRefreshToken.getClient_type(),
                authRefreshToken.getClient_version(), getClientIp(httpServletRequest));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean logout(Long userId, LogoutRequest request) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        QueryWrapper<AuthRefreshToken> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("revoked", 0);
        if (request != null) {
            if (StringUtils.isNotBlank(request.getDeviceId())) {
                queryWrapper.eq("device_id", request.getDeviceId());
            }
            if (StringUtils.isNotBlank(request.getRefreshToken())) {
                queryWrapper.eq("refresh_token_hash", TokenHashUtils.sha256(request.getRefreshToken()));
            }
        }
        List<AuthRefreshToken> tokenList = authRefreshTokenMapper.selectList(queryWrapper);
        for (AuthRefreshToken token : tokenList) {
            revokeToken(token, "LOGOUT");
        }
        return true;
    }

    @Override
    public CaptchaVO getCaptcha() {
        clearExpiredCaptcha();
        String captchaId = "cpt_" + System.currentTimeMillis() + secureRandom.nextInt(1000);
        String captchaCode = randomCaptchaCode();
        long expireAt = Instant.now().plusSeconds(CAPTCHA_TTL_SECONDS).getEpochSecond();
        captchaStore.put(captchaId, new CaptchaEntry(captchaCode, expireAt));
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaId(captchaId);
        captchaVO.setCaptchaImageBase64(generateCaptchaImageBase64(captchaCode));
        captchaVO.setExpireAt(expireAt);
        return captchaVO;
    }

    private String generateCaptchaImageBase64(String captchaCode) {
        int width = 132;
        int height = 44;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(new Color(248, 250, 252));
            g2d.fillRect(0, 0, width, height);

            for (int i = 0; i < 8; i++) {
                g2d.setColor(new Color(190 + secureRandom.nextInt(40), 190 + secureRandom.nextInt(40),
                        190 + secureRandom.nextInt(40)));
                int x1 = secureRandom.nextInt(width);
                int y1 = secureRandom.nextInt(height);
                int x2 = secureRandom.nextInt(width);
                int y2 = secureRandom.nextInt(height);
                g2d.setStroke(new BasicStroke(1.2f));
                g2d.drawLine(x1, y1, x2, y2);
            }

            g2d.setFont(new Font("SansSerif", Font.BOLD, 30));
            for (int i = 0; i < captchaCode.length(); i++) {
                g2d.setColor(new Color(20 + secureRandom.nextInt(120), 20 + secureRandom.nextInt(120),
                        20 + secureRandom.nextInt(120)));
                String text = String.valueOf(captchaCode.charAt(i));
                int x = 18 + i * 26;
                int y = 32 + secureRandom.nextInt(6);
                g2d.drawString(text, x, y);
            }

            for (int i = 0; i < 40; i++) {
                g2d.setColor(new Color(130 + secureRandom.nextInt(100), 130 + secureRandom.nextInt(100),
                        130 + secureRandom.nextInt(100)));
                g2d.drawRect(secureRandom.nextInt(width), secureRandom.nextInt(height), 1, 1);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "验证码生成失败");
        } finally {
            g2d.dispose();
        }
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        if (StringUtils.isAnyBlank(request.getUsername(), request.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名或密码不能为空");
        }
    }

    private SysUser findUserByUsername(String username) {
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        queryWrapper.eq("is_delete", 0);
        SysUser user = sysUserMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }
        return user;
    }

    private void verifyUserCanLogin(SysUser user) {
        if (!Objects.equals(user.getStatus(), USER_ENABLE_STATUS)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已禁用");
        }
        Date lockUntil = user.getLock_until();
        if (lockUntil != null && lockUntil.after(new Date())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已锁定，请稍后重试");
        }
    }

    private void onPasswordMismatch(SysUser user) {
        int currentFailedCount = user.getFailed_login_count() == null ? 0 : user.getFailed_login_count();
        int nextFailedCount = currentFailedCount + 1;
        SysUser update = new SysUser();
        update.setId(user.getId());
        if (nextFailedCount >= MAX_LOGIN_FAIL_COUNT) {
            update.setFailed_login_count(MAX_LOGIN_FAIL_COUNT);
            update.setLock_until(Date.from(Instant.now().plus(LOCK_MINUTES, ChronoUnit.MINUTES)));
            sysUserMapper.updateById(update);
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "连续输错3次，账号已锁定30分钟");
        }
        update.setFailed_login_count(nextFailedCount);
        sysUserMapper.updateById(update);
        int remain = MAX_LOGIN_FAIL_COUNT - nextFailedCount;
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误，还可尝试" + remain + "次");
    }

    private void resetLoginFailState(Long userId) {
        SysUser update = new SysUser();
        update.setId(userId);
        update.setFailed_login_count(0);
        update.setLock_until(null);
        update.setLast_login_at(new Date());
        sysUserMapper.updateById(update);
    }

    private List<String> listRoleCodes(Long userId) {
        QueryWrapper<SysUserRole> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        List<SysUserRole> sysUserRoleList = sysUserRoleMapper.selectList(queryWrapper);
        if (sysUserRoleList == null || sysUserRoleList.isEmpty()) {
            return List.of(RoleConstant.USER);
        }
        List<Long> roleIdList = sysUserRoleList.stream().map(SysUserRole::getRole_id).distinct().toList();
        QueryWrapper<SysRole> roleQuery = new QueryWrapper<>();
        roleQuery.in("id", roleIdList);
        roleQuery.eq("status", 1);
        roleQuery.eq("is_delete", 0);
        List<SysRole> roleList = sysRoleMapper.selectList(roleQuery);
        if (roleList == null || roleList.isEmpty()) {
            return List.of(RoleConstant.USER);
        }
        return roleList.stream().map(SysRole::getRole_code).filter(StringUtils::isNotBlank).distinct()
                .collect(Collectors.toList());
    }

    private boolean isAdmin(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return false;
        }
        return roleCodes.contains(RoleConstant.SUPER_ADMIN) || roleCodes.contains(RoleConstant.VENUE_ADMIN);
    }

    private SysRole getRoleByCode(String roleCode) {
        QueryWrapper<SysRole> roleQuery = new QueryWrapper<>();
        roleQuery.eq("role_code", roleCode);
        roleQuery.eq("status", 1);
        roleQuery.eq("is_delete", 0);
        return sysRoleMapper.selectOne(roleQuery);
    }

    private LoginResultVO issueTokens(SysUser user, List<String> roleCodes, String deviceId, String clientType,
                                      String clientVersion, String ipAddress) {
        if (roleCodes.contains(RoleConstant.LIFEGUARD)) {
            revokeAllActiveTokens(user.getId(), "LIFEGUARD_SINGLE_DEVICE");
        }
        String normalizedDeviceId = StringUtils.isNotBlank(deviceId) ? deviceId : "unknown-device";
        String normalizedClientType = StringUtils.isNotBlank(clientType) ? clientType : CLIENT_TYPE_UNKNOWN;

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), roleCodes);
        String refreshToken = jwtTokenProvider.generateRefreshTokenValue();
        String refreshTokenHash = TokenHashUtils.sha256(refreshToken);
        Date expiresAt = Date.from(Instant.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpireSeconds()));

        AuthRefreshToken authRefreshToken = new AuthRefreshToken();
        authRefreshToken.setUser_id(user.getId());
        authRefreshToken.setRefresh_token_hash(refreshTokenHash);
        authRefreshToken.setDevice_id(normalizedDeviceId);
        authRefreshToken.setClient_type(normalizedClientType);
        authRefreshToken.setClient_version(clientVersion);
        authRefreshToken.setIp_address(ipAddress);
        authRefreshToken.setExpires_at(expiresAt);
        authRefreshToken.setRevoked(0);
        authRefreshTokenMapper.insert(authRefreshToken);

        LoginResultVO loginResultVO = new LoginResultVO();
        loginResultVO.setAccessToken(accessToken);
        loginResultVO.setRefreshToken(refreshToken);
        loginResultVO.setExpiresIn(jwtTokenProvider.getAccessTokenExpireSeconds());
        loginResultVO.setForceChangePassword(user.getForce_change_password());

        List<String> permissionCodes = new ArrayList<>(
                accessControlService.listPermissionsByRoleCodes(roleCodes));
        permissionCodes.sort(String::compareTo);

        LoginResultVO.UserInfo userInfo = new LoginResultVO.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setDisplayName(user.getDisplay_name());
        userInfo.setRoles(new ArrayList<>(roleCodes));
        userInfo.setPermissions(permissionCodes);
        loginResultVO.setUser(userInfo);

        saveLoginAuditLog(user.getId(), user.getUsername(), ipAddress);

        return loginResultVO;
    }

    private void saveLoginAuditLog(Long userId, String username, String ipAddress) {
        try {
            SystemAuditLog auditLog = new SystemAuditLog();
            auditLog.setTrace_id(java.util.UUID.randomUUID().toString());
            auditLog.setLog_category("LOGIN");
            auditLog.setOperator_id(userId);
            auditLog.setOperator_name(username);
            auditLog.setClient_ip(ipAddress);
            auditLog.setRequest_uri("/api/auth/admin/login");
            auditLog.setRequest_method("POST");
            auditLog.setResponse_code(0);
            auditLog.setResponse_message("登录成功");
            auditLog.setCost_ms(0);
            auditLog.setCreated_at(new Date());
            systemAuditLogService.save(auditLog);
            log.info("登录审计日志已保存, userId={}, username={}", userId, username);
        } catch (Exception e) {
            log.warn("保存登录审计日志失败, userId={}", userId, e);
        }
    }

    private void revokeAllActiveTokens(Long userId, String reason) {
        QueryWrapper<AuthRefreshToken> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("revoked", 0);
        queryWrapper.gt("expires_at", new Date());
        List<AuthRefreshToken> tokenList = authRefreshTokenMapper.selectList(queryWrapper);
        for (AuthRefreshToken token : tokenList) {
            revokeToken(token, reason);
        }
    }

    private void revokeToken(AuthRefreshToken token, String reason) {
        if (token == null || Objects.equals(token.getRevoked(), 1)) {
            return;
        }
        AuthRefreshToken update = new AuthRefreshToken();
        update.setId(token.getId());
        update.setRevoked(1);
        update.setRevoked_at(new Date());
        update.setRevoke_reason(reason);
        update.setLast_used_at(new Date());
        authRefreshTokenMapper.updateById(update);
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwarded)) {
            return StringUtils.split(forwarded, ',')[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void verifyCaptcha(String captchaId, String captchaCode) {
        if (StringUtils.isAnyBlank(captchaId, captchaCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不能为空");
        }
        clearExpiredCaptcha();
        CaptchaEntry captchaEntry = captchaStore.get(captchaId);
        if (captchaEntry == null || captchaEntry.expireAt < Instant.now().getEpochSecond()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码已过期");
        }
        if (!StringUtils.equalsIgnoreCase(captchaEntry.code, captchaCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }
        captchaStore.remove(captchaId);
    }

    private String randomCaptchaCode() {
        String chars = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            int index = secureRandom.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    private void clearExpiredCaptcha() {
        long now = Instant.now().getEpochSecond();
        captchaStore.entrySet().removeIf(entry -> entry.getValue().expireAt < now);
    }

    private record CaptchaEntry(String code, long expireAt) {
    }
}
