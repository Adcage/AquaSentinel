package com.springboot.model.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class LoginResultVO implements Serializable {

    private String accessToken;

    private String refreshToken;

    private long expiresIn;

    private Integer forceChangePassword;

    private UserInfo user;

    @Data
    public static class UserInfo implements Serializable {

        private Long id;

        private String username;

        private String displayName;

        private List<String> roles = new ArrayList<>();

        private List<String> permissions = new ArrayList<>();
    }
}
