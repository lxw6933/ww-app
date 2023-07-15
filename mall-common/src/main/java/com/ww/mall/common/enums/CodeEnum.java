package com.ww.mall.common.enums;

/**
 * @author ww
 * @create 2023-07-15- 10:19
 * @description:
 */
public enum CodeEnum {

    SUCCESS("0","成功"),
    FAIL("-1", "失败"),
    SYSTEM_ERROR("-99", "服务器繁忙，请联系客服人员"),
    LIMIT_ERROR("-2", "当前访问人数过多，请稍候再试"),
    ;

    CodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    private String code;

    private String message;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
