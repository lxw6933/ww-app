package com.ww.mall.netty.service;

/**
 * @author ww
 * @create 2024-05-07 22:04
 * @description:
 */
public interface UserService {

    /**
     * 登录
     * @param username 用户名
     * @param password 密码
     * @return 登录成功返回 true, 否则返回 false
     */
    boolean login(String username, String password);

}
