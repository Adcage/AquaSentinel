package com.springboot.model.dto.auth;

import java.io.Serializable;
import lombok.Data;

@Data
public class RegisterRequest implements Serializable {

    private String displayName;

    private String username;

    private String password;

    private String roleCode;

    private String captchaId;

    private String captchaCode;
}
