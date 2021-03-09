package com.ww.mall.generator.service.impl;

import com.ww.mall.generator.entity.User;
import com.ww.mall.generator.dao.UserMapper;
import com.ww.mall.generator.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
* @author ww
* @since 2021-03-09
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

}
