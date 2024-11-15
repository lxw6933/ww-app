package com.ww.mall.admin.user.dto;

import lombok.Data;

import java.util.List;

/**
 * @author ww
 * @create 2024-09-11- 09:26
 * @description:
 */
@Data
public class SysUserDTO {

    private Long id;

    private String mobile;

    /**
     * 用户名（账号名称）
     */
    private String username;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 用户昵称（姓名）
     */
    private String realName;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 用户权限集合
     */
    private List<String> authorities;
}
