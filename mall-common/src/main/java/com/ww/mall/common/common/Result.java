package com.ww.mall.common.common;

import com.ww.mall.common.enums.CodeEnum;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

/**
 * @author ww
 * @create 2023-07-15- 11:03
 * @description: 通用返回结果
 */
@Getter
@Setter
public class Result<T> implements Serializable {
    protected static final String SUCCESS_CODE = CodeEnum.SUCCESS.getCode();
    protected static final String SUCCESS_MSG = CodeEnum.SUCCESS.getMessage();

    private String code;
    private String message;
    private T value;

    public Result() {
        this.code = SUCCESS_CODE;
        this.message = SUCCESS_MSG;
    }

    public Result(T value) {
        this.code = SUCCESS_CODE;
        this.message = SUCCESS_MSG;
        this.value = value;
    }

    public Result(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public Result(String code, String message,T value) {
        this.code = code;
        this.message = message;
        this.value = value;
    }

    public Boolean isSuccess() {
        return SUCCESS_CODE.equals(this.code);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("code", code)
                .append("message", message)
                .append("value", value)
                .toString();
    }
}
