package com.ww.mall.enums;

/**
 * @description: 限流类型
 * @author: ww
 * @create: 2021/5/21 下午7:48
 **/
public enum LimitType {
    /**
     * 传统类型
     */
    CUSTOMER,
    /**
     *  根据 IP地址限制
     */
    IP
}
