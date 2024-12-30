package com.ww.app.pay.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author ww
 * @create 2024-06-04- 19:16
 * @description:
 */
@Data
public class PayResult implements Serializable {

    private static final long serialVersionUID = 6439646269084700779L;

    private int code = 0;

    private String message;

    private Object data;

    public boolean hasError() {
        return this.code != 0;
    }

    public PayResult addError(String message) {
        this.message = message;
        this.code = 1;
        return this;
    }

    public PayResult addConfirmError(String message) {
        this.message = message;
        this.code = 2;
        return this;
    }

    public PayResult success(Object data) {
        this.data = data;
        this.code = 0;
        return this;
    }

}
