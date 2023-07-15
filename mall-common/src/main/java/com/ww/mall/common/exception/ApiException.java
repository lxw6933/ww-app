package com.ww.mall.common.exception;

import com.ww.mall.common.enums.CodeEnum;

/**
 * @author ww
 * @create 2023-07-15- 10:18
 * @description: 接口异常类
 */
public class ApiException extends RuntimeException {

    private String code;

    public ApiException(String message) {
        super(message);
        this.code = CodeEnum.SYSTEM_ERROR.getCode();
    }

    public ApiException(String code, String message) {
        this(message);
        this.code = code;
    }

    public ApiException(Throwable throwable) {
        super(throwable);
        this.code = CodeEnum.SYSTEM_ERROR.getCode();
    }

    public ApiException(String message, Throwable throwable) {
        super(message, throwable);
        this.code = CodeEnum.SYSTEM_ERROR.getCode();
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
