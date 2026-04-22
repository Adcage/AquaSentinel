package com.springboot.model.dto.auth;

import java.io.Serializable;
import lombok.Data;

@Data
public class LogoutRequest implements Serializable {

    private String deviceId;

    private String refreshToken;
}
