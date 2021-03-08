package com.ww.mall.common.exception;

/**
 * Author:         ww
 * Datetime:       2021\3\8 0008
 * Description:    mall异常状态码
 */
public enum MallCodeEnum {
    VALID_EXCEPTION(10001,"数据效验异常"),
    UNKNOWN_EXCEPTION(10000,"未知异常");

    private Integer code;
    private String msg;

    MallCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
