package com.ww.mall.common.enums;

import lombok.Getter;

/**
 * @author ww
 * @create 2023-07-15- 10:19
 * @description:
 */
@Getter
public enum CodeEnum {

    SUCCESS("0","成功"),
    FAIL("-1", "失败"),
    SYSTEM_ERROR("-99", "服务器繁忙，请联系客服人员"),
    LIMIT_ERROR("-2", "当前访问人数过多，请稍候再试"),
    LOCK_SERVICE_EXCEPTION("-10001", "服务器繁忙，请稍后再试"),
    FAIL_GET_LOCK_EXCEPTION("-10002", "当前页面访问人数过多，请稍候再试"),
    UN_LOGIN("401", "未登录，请登录后再试"),
    SMS_CODE_EXCEPTION("10000", "验证码获取频率太高，请稍后再试"),
    LOGIN_EXCEPTION("10001", "账号或密码错误"),
    CODE_ERROR("10002", "验证码错误"),
    PARAM_ERROR("400", "参数错误"),
    NOT_SUPPORTED_METHOD("405", "不支持当前请求方法"),
    NOT_HANDLER_FOUND("404", "没找到相关请求处理器"),
    NOT_SUPPORTED_MEDIA("415","不支持当前媒体类型"),
    ILLEGAL_REQUEST("-999", "非法请求"),
    SIGN_EXCEPTION("-9999", "签名异常"),
    ;

    CodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    private final String code;

    private final String message;

}
