package com.ww.mall.common.common;

import com.ww.mall.common.enums.SysPlatformType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2023-07-18- 14:31
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MallAdminUser extends MallBaseUser {

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

    /**
     * 所属平台
     */
    private SysPlatformType platform;

    /**
     * 平台id
     */
    private Long roleId;

}
