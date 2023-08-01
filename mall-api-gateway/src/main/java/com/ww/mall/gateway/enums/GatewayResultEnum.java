package com.ww.mall.gateway.enums;

/**
 * @author ww
 * @create 2023-07-18- 14:55
 * @description:
 */
public enum GatewayResultEnum {

    FAIL("-1", "网关内部异常,请您稍后重试!"),
    SUCCESS("200", "访问成功!"),
    SIGN_IS_NOT_PASS("401", "签名未通过!"),
    PAYLOAD_TOO_LARGE("403", "您的文件过大"),
    TOO_MANY_REQUESTS("429", "您已经被限流，请稍后重试!"),
    TIME_ERROR("-101", "您的时间参数错误或者已经过期!"),
    RULE_NOT_FIND("-102", "规则未匹配!"),
    SERVICE_RESULT_ERROR("-103", "服务调用异常，或者未返回结果"),
    SERVICE_TIMEOUT("-104", "服务调用超时"),
    SING_TIME_IS_TIMEOUT("-105", "登录已过期!"),
    CANNOT_FIND_URL("-106", "未能找到合适的调用url,请检查你的配置!"),
    CANNOT_FIND_SELECTOR("-107", "未能匹配选择器,请检查你的选择器配置！"),
    CANNOT_CONFIG_SPRINGCLOUD_SERVICEID("-108", "您并未配置或未匹配对应服务"),
    SPRINGCLOUD_SERVICEID_IS_ERROR("-109", "服务正在排队处理您的请求，请稍等一下再试");

    private final String code;
    private final String msg;

    public String getCode() {
        return this.code;
    }

    public String getMsg() {
        return this.msg;
    }

    private GatewayResultEnum(final String code, final String msg) {
        this.code = code;
        this.msg = msg;
    }

}
