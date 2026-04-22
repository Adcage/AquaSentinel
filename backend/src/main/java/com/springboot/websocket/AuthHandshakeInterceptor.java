package com.springboot.websocket;

import com.springboot.security.AuthUserContext;
import com.springboot.security.JwtTokenProvider;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public AuthHandshakeInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (StringUtils.isBlank(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            AuthUserContext authUserContext = jwtTokenProvider.parseAccessToken(token);
            attributes.put("userId", authUserContext.getUserId());
            attributes.put("username", authUserContext.getUsername());
            attributes.put("roleCodes", authUserContext.getRoleCodes());
            return true;
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception exception) {
    }

    private String resolveToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            if (StringUtils.isNotBlank(token)) {
                return token;
            }
        }
        HttpHeaders headers = request.getHeaders();
        List<String> authorizations = headers.get(AUTHORIZATION_HEADER);
        if (authorizations == null || authorizations.isEmpty()) {
            return null;
        }
        String authorization = authorizations.get(0);
        if (StringUtils.startsWithIgnoreCase(authorization, BEARER_PREFIX)) {
            return StringUtils.substringAfter(authorization, BEARER_PREFIX).trim();
        }
        return null;
    }
}
