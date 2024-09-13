package com.ww.mall.common.common;

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
    private Long userId;;

}
