package com.springboot.model.dto.role;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class RoleAddRequest implements Serializable {

    private String roleCode;

    private String roleName;

    private List<String> permissions;

    private Integer status;
}
