package com.ww.mall.admin.view.vo;

import lombok.Data;

/**
 * @author ww
 * @create 2024-09-12 21:12
 * @description:
 */
@Data
public class CurrentSysUserInfoVO {
    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户名（账号名称）
     */
    private String username;

    /**
     * 用户昵称（姓名）
     */
    private String nickname;

    /**
     * 头像
     */
    private String headPicture;
}
