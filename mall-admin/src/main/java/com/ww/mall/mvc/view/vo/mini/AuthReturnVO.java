package com.ww.mall.mvc.view.vo.mini;

import lombok.Data;

/**
 * @description:
 * @author: ww
 * @create: 2021-06-15 11:38
 */
@Data
public class AuthReturnVO {

    /**
     * 登录态
     */
    private String token;

    /**
     * 登录成功 code ：200
     */
    private Integer code;

    /**
     * 请求微信返回状态码
     * -1	    系统繁忙，此时请开发者稍候再试
     * 0	    请求成功
     * 40029	code 无效
     * 45011	频率限制，每个用户每分钟100次
     */
    private Integer errCode;

    /**
     * 错误信息
     */
    private String errMsg;

    /**
     * 用户对象信息
     */
    private Object user;

}
