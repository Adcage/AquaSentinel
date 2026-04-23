package com.springboot.model.dto.access;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class UserAssignRoleRequest implements Serializable {

    private Long userId;

    private List<String> roleCodes;
}
