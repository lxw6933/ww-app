package com.ww.mall.common.common;

import com.ww.mall.common.enums.CodeEnum;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

/**
 * @author ww
 * @create 2023-07-15- 11:03
 * @description: 通用返回结果
 */
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

    public Result(Result result) {
        this.code = SUCCESS_CODE;
        this.message = SUCCESS_MSG;
        this.setCode(result.getCode());
        this.setMessage(result.getMessage());
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

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getValue() {
        return this.value;
    }

    public void setValue(T value) {
        this.value = value;
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
