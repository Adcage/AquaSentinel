package com.springboot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springboot.model.entity.SysUserRole;
import com.springboot.service.SysUserRoleService;
import com.springboot.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Service;

/**
* @description 针对表【sys_user_role(用户角色关联表)】的数据库操作Service实现
*/
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole>
    implements SysUserRoleService{

}




