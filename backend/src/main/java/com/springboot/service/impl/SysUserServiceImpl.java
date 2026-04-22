package com.springboot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springboot.model.entity.SysUser;
import com.springboot.service.SysUserService;
import com.springboot.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

/**
* @description 针对表【sys_user(系统用户表)】的数据库操作Service实现
*/
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
    implements SysUserService{

}




