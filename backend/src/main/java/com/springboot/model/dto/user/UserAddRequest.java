package com.springboot.model.dto.user;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class UserAddRequest implements Serializable {

    private String username;

    private String password;

    private String displayName;

    private String phone;

    private String email;

    private Integer status;

    private Integer forceChangePassword;

    private List<String> roleCodes;
}
