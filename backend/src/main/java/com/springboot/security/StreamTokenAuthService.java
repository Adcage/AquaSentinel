package com.springboot.security;

import com.springboot.common.ErrorCode;
import com.springboot.config.AppStreamProxyProperties;
import com.springboot.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class StreamTokenAuthService {

    private final JwtTokenProvider jwtTokenProvider;

    private final AppStreamProxyProperties appStreamProxyProperties;

    public StreamTokenAuthService(JwtTokenProvider jwtTokenProvider, AppStreamProxyProperties appStreamProxyProperties) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.appStreamProxyProperties = appStreamProxyProperties;
    }

    public void verifyPreviewToken(String token) {
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "缺少视频流访问令牌");
        }
        jwtTokenProvider.parseAccessToken(token);
    }

    public String resolveTokenParamName() {
        return StringUtils.defaultIfBlank(appStreamProxyProperties.getTokenParamName(), "token");
    }
}
