package com.springboot.model.dto.role;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class RoleUpdateRequest implements Serializable {

    private Long id;

    private String roleCode;

    private String roleName;

    private List<String> permissions;

    private Integer status;
}
