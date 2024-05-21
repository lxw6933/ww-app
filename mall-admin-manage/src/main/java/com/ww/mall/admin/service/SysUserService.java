package com.ww.mall.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.mall.admin.entity.SysUser;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 重置用户密码
     *
     * @param userId 用户id
     * @return boolean
     */
    boolean resetPassword(Long userId);

}

