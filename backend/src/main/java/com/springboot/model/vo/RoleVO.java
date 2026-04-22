package com.springboot.model.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
public class RoleVO implements Serializable {

    private Long id;

    private String roleCode;

    private String roleName;

    private Object permissionJson;

    private Integer status;

    private Date createdAt;

    private Date updatedAt;
}
