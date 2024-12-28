package com.ww.mall.common.common;

import cn.hutool.core.lang.Assert;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ww.mall.common.enums.GlobalResCodeConstants;
import com.ww.mall.common.exception.ApiException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author ww
 * @create 2023-07-15- 11:03
 * @description: 通用返回结果
 */
@Getter
@Setter
public class Result<T> implements Serializable {

    private Integer code;
    private String msg;
    private T data;

    public static <T> Result<T> error(Integer code, String message) {
        Assert.isTrue(!GlobalResCodeConstants.SUCCESS.getCode().equals(code), "code 必须是错误的！");
        Result<T> result = new Result<>();
        result.code = code;
        result.msg = message;
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = GlobalResCodeConstants.SUCCESS.getCode();
        result.msg = GlobalResCodeConstants.SUCCESS.getMsg();
        result.data = data;
        return result;
    }

    public static <T> Result<T> error(ResCode resCode) {
        return error(resCode.getCode(), resCode.getMsg());
    }

    public static <T> Result<T> error(ApiException e) {
        return error(e.getCode(), e.getMessage());
    }

    /**
     * 避免 jackson 序列化
     */
    @JsonIgnore
    public Boolean isSuccess() {
        return Objects.equals(GlobalResCodeConstants.SUCCESS.getCode(), this.code);
    }

    @JsonIgnore
    public boolean isError() {
        return !isSuccess();
    }

    public void checkError() throws ApiException {
        checkError(null);
    }

    public void checkError(Supplier<Void> handler) throws ApiException {
        if (isSuccess()) {
            return;
        }
        if (handler != null) {
            handler.get();
        }
        // 业务异常
        throw new ApiException(code, msg);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("code", code)
                .append("message", msg)
                .append("value", data)
                .toString();
    }
}
