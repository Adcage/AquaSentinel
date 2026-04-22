package com.springboot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springboot.model.entity.SysRole;
import com.springboot.service.SysRoleService;
import com.springboot.mapper.SysRoleMapper;
import org.springframework.stereotype.Service;

/**
* @description 针对表【sys_role(系统角色表)】的数据库操作Service实现
*/
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole>
    implements SysRoleService{

}




