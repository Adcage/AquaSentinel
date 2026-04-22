package com.springboot.model.dto.auth;

import java.io.Serializable;
import lombok.Data;

@Data
public class LoginRequest implements Serializable {

    private String username;

    private String password;

    private String deviceId;

    private String clientType;

    private String clientVersion;
}
