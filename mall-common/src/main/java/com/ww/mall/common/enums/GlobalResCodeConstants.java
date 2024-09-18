package com.ww.mall.common.enums;

import com.ww.mall.common.common.ResCode;

/**
 * @author ww
 * @create 2023-07-15- 10:19
 * @description:
 */
public interface GlobalResCodeConstants {

    ResCode SUCCESS = new ResCode(0, "成功");

    // ========== 客户端错误段 ==========
    ResCode BAD_REQUEST = new ResCode(400, "请求参数不正确");
    ResCode UNAUTHORIZED = new ResCode(401, "账号未登录");
    ResCode FORBIDDEN = new ResCode(403, "没有该操作权限");
    ResCode NOT_FOUND = new ResCode(404, "请求未找到");
    ResCode METHOD_NOT_ALLOWED = new ResCode(405, "请求方法不正确");
    ResCode NOT_SUPPORTED_MEDIA = new ResCode(415,"不支持当前媒体类型");
    ResCode LOCKED = new ResCode(423, "请求失败，请稍后重试");
    ResCode TOO_MANY_REQUESTS = new ResCode(429, "请求过于频繁，请稍后重试");
    // ========== 服务端错误段 ==========
    ResCode SYSTEM_ERROR = new ResCode(500, "系统异常");
    ResCode NOT_IMPLEMENTED = new ResCode(501, "功能未实现/未开启");
    ResCode ERROR_CONFIGURATION = new ResCode(502, "错误的配置项");
    // ========== 自定义错误段 ==========
    ResCode REPEATED_REQUESTS = new ResCode(900, "重复请求，请稍后重试");
    ResCode IP_LIMITED = new ResCode(995, "Ip访问过于频繁");
    ResCode TOKEN_TIMEOUT = new ResCode(996, "Token已过期");
    ResCode TOKEN_ERROR = new ResCode(997, "Token错误");
    ResCode CODE_ERROR = new ResCode(998, "验证码错误");
    ResCode ILLEGAL_REQUEST = new ResCode(999, "非法请求");
    ResCode SIGN_ERROR = new ResCode(1000, "签名异常");
    ResCode FLOW_EXCEPTION = new ResCode(1001, "当前访问人数过多，请稍后再试");
    ResCode DEGRADE_EXCEPTION = new ResCode(1002, "服务正在排队，请稍后再试");
    ResCode PARMA_FLOW_EXCEPTION = new ResCode(1003, "访问人数过于爆满，请稍后再试");
    ResCode SYSTEM_BLOCK_EXCEPTION = new ResCode(1004, "当前系统流量过大，请稍后再试");
    ResCode AUTH_LIMIT_EXCEPTION = new ResCode(1005, "当前没有访问权限");
    ResCode LIMIT_REQUEST = new ResCode(1006, "当前页面访问人数过多，请稍候再试");

    ResCode UNKNOWN = new ResCode(9999, "未知错误");
}
