package com.ww.mall.auth.view.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2024-09-14 12:47
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminLoginResultVO extends LoginResultVO {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 账号
     */
    private String username;

    /**
     * 用户名称
     */
    private String realName;

    /**
     * 头像
     */
    private String headPicture;

}
