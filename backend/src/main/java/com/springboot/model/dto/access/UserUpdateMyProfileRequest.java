package com.springboot.model.dto.access;

import java.io.Serializable;
import lombok.Data;

@Data
public class UserUpdateMyProfileRequest implements Serializable {

    private Long id;

    private String displayName;

    private String phone;

    private String email;

    private String oldPassword;

    private String newPassword;
}
