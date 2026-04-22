package com.springboot.model.dto.auth;

import java.io.Serializable;
import lombok.Data;

@Data
public class RefreshTokenRequest implements Serializable {

    private String refreshToken;

    private String deviceId;
}
