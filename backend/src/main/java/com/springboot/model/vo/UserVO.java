package com.springboot.model.vo;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class UserVO implements Serializable {

    private Long id;

    private String username;

    private String displayName;

    private String phone;

    private String email;

    private Integer status;

    private Integer forceChangePassword;

    private Date lastLoginAt;

    private Date createdAt;

    private Date updatedAt;

    private List<String> roleCodes;

    private Long linkedLifeguardId;
}
