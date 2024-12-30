package com.ww.app.pay.service;

import com.ijpay.alipay.AliPayApiConfig;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ww
 * @create 2024-06-04- 19:23
 * @description:
 */
public interface AliPayService {

    /**
     * 获取支付宝配置
     */
    AliPayApiConfig getApiConfig();

    /**
     * app支付
     */
    String appPay();

    /**
     * wap支付
     */
    void wapPay(HttpServletResponse response);

    /**
     * PC支付
     */
    void pcPay(HttpServletResponse response);

    /**
     * 生成支付二维码
     */
    java.lang.String tradePreCreatePay();

    /**
     * 支付查询
     */
    java.lang.String tradeQuery(@RequestParam(required = false, name = "outTradeNo") java.lang.String outTradeNo, @RequestParam(required = false, name = "tradeNo") java.lang.String tradeNo);

    /**
     * 退款
     */
    java.lang.String refund(java.lang.String outTradeNo, java.lang.String tradeNo);

    /**
     * 后端唤起支付宝应用授权URL并授权
     */
    void toAuth(HttpServletResponse response);

    /**
     * 使用 app_auth_code 换取 app_auth_token 用于获取用户信息
     */
    java.lang.String getAuthToken(java.lang.String appId, java.lang.String appAuthCode);

    /**
     * 查询支付宝用户信息
     */
    java.lang.String getAuthTokenUserInfo(java.lang.String appAuthToken);

    /**
     * 支付跳转
     */
    java.lang.String returnUrl(HttpServletRequest request);

    /**
     * 支付回调
     */
    java.lang.String notifyUrl(HttpServletRequest request);

}
