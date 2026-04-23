package com.springboot.model.dto.role;

import com.springboot.common.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RoleQueryRequest extends PageRequest {

    private Long id;

    private String roleCode;

    private String roleName;

    private Integer status;
}
