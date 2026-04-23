package com.springboot.model.dto.user;

import com.springboot.common.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest {

    private Long id;

    private String username;

    private String displayName;

    private String phone;

    private Integer status;

    private String roleCode;
}
