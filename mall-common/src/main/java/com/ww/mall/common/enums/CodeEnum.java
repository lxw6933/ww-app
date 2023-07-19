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
    SMS_CODE_EXCEPTION("10000", "验证码获取频率太高，请稍后再试"),
    LOGIN_EXCEPTION("10001", "账号或密码错误"),
    CODE_ERROR("10002", "验证码错误"),
    PARAM_ERROR("400", "参数错误"),
    ILLEGAL_REQUEST("-999", "非法请求"),
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
