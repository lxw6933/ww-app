package com.ww.mall.member.to;

import lombok.Data;

/**
 * @description: 是否登录用户信息
 * @author: ww
 * @create: 2021/7/3 下午9:00
 **/
@Data
public class UserInfoTo {

    /**
     * 登录用户存放用户id
     */
    private Long userId;

    /**
     * 临时用户存放cookie中的userKey
     */
    private String userKey;

    /**
     * 是否存在临时用户
     */
    private boolean tempUser = false;

}
