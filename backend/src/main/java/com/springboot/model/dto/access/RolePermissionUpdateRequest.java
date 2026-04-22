package com.springboot.model.dto.access;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class RolePermissionUpdateRequest implements Serializable {

    private String roleCode;

    private List<String> permissions;
}
