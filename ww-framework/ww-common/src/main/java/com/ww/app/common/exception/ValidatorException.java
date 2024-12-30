package com.ww.app.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description: 业务处理验证异常用于业务流程控制，全家异常捕获
 * @author: ww
 * @create: 2021-05-12 18:56
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ValidatorException extends RuntimeException {

    /** 标识码 */
    private int code;

    /** 错误信息 */
    private String message;

    public ValidatorException(String message) {
        this(500, message);
    }

    public ValidatorException(String message, Object... args) {
        this(500, message, args);
    }

    public ValidatorException(int code, String message, Object... args) {
        this.message = message;
        this.code = code;
    }

    public ValidatorException(String message, Throwable cause, Object... args) {
        super(cause);
        this.message = message;
        this.code = 500;
    }

    @Override
    public String toString() {
        return "ValidatorException{" +
                "code=" + code +
                ",message=" + getMessage() +
                '}';
    }
}
