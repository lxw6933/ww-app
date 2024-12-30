package com.ww.app.common.common;

import com.ww.app.common.enums.UserType;
import lombok.Data;

/**
 * @author ww
 * @create 2024-05-21- 09:03
 * @description:
 */
@Data
public class BaseUser {

    /**
     * 发行人
     */
    protected String iss;

    /**
     * 过期时间
     */
    protected Long exp;

    /**
     * 生效时间
     */
    protected Long nbf;

    /**
     * 用户id
     */
    private Long id;

    /**
     * 用户类型
     */
    protected UserType userType;

}
