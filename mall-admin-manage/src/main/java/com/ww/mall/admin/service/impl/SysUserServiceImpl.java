package com.ww.mall.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.mall.admin.dao.SysUserMapper;
import easycode.entity.SysUser;
import com.ww.mall.admin.service.SysUserService;
import org.springframework.stereotype.Service;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@Service("sysUserService")
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

}

