package com.springboot.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.springboot.config.AppStreamProxyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StreamTokenAuthServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private StreamTokenAuthService streamTokenAuthService;

    @BeforeEach
    void setUp() {
        streamTokenAuthService = new StreamTokenAuthService(jwtTokenProvider, new AppStreamProxyProperties());
    }

    @Test
    void verifyPreviewTokenShouldCallJwtParser() {
        when(jwtTokenProvider.parseAccessToken("valid-token")).thenReturn(new AuthUserContext());

        streamTokenAuthService.verifyPreviewToken("valid-token");

        verify(jwtTokenProvider).parseAccessToken("valid-token");
    }

    @Test
    void verifyPreviewTokenShouldRejectBlankToken() {
        assertThrows(RuntimeException.class, () -> streamTokenAuthService.verifyPreviewToken(" "));
    }
}
