package com.ww.mall.common.common;

import lombok.Data;

/**
 * @author ww
 * @create 2023-07-18- 14:31
 * @description:
 */
@Data
public class MallJwtPayload {

    /**
     * 发行人
     */
    private String iss;

    /**
     * 过期时间
     */
    private Long exp;

    /**
     * 生效时间
     */
    private Long nbf;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 用户id
     */
    private Long memberId;

}
