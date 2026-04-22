package com.springboot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springboot.model.entity.AuthRefreshToken;
import com.springboot.service.AuthRefreshTokenService;
import com.springboot.mapper.AuthRefreshTokenMapper;
import org.springframework.stereotype.Service;

/**
* @description 针对表【auth_refresh_token(刷新令牌会话表)】的数据库操作Service实现
*/
@Service
public class AuthRefreshTokenServiceImpl extends ServiceImpl<AuthRefreshTokenMapper, AuthRefreshToken>
    implements AuthRefreshTokenService{

}




