package com.ww.app.cart.to;

import lombok.Data;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/17 20:29
 **/
@Data
public class UserInfoTo {

    /**
     * 登录用户id
     */
    private Long userId;

    /**
     * 未登录用户临时key
     */
    private String tempUserKey;

}
