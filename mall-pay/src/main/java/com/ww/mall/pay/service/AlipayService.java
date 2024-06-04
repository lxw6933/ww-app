package com.ww.mall.pay.service;

import com.ijpay.alipay.AliPayApiConfig;
import com.ww.mall.pay.vo.PayResult;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ww
 * @create 2024-06-04- 19:23
 * @description:
 */
public interface AlipayService {

    /**
     * 获取支付宝配置
     */
    AliPayApiConfig getApiConfig();

    /**
     * app支付
     */
    PayResult appPay();

    /**
     * wap支付
     */
    void wapPay(HttpServletResponse response);

    /**
     * PC支付
     */
    void pcPay(HttpServletResponse response);

    /**
     * 扫码支付
     */
    String tradePreCreatePay();

    /**
     * 支付查询
     */
    String tradeQuery(@RequestParam(required = false, name = "outTradeNo") String outTradeNo, @RequestParam(required = false, name = "tradeNo") String tradeNo);

    /**
     * 退款
     */
    String refund(String outTradeNo, String tradeNo);

    /**
     * 后端唤起支付宝应用授权URL并授权
     */
    void toAuth(HttpServletResponse response);

    /**
     * 授权获取到的code，来获取用户支付宝授权信息
     */
    String redirectUri(String appId, String appAuthCode);

    /**
     * 查询支付宝用户信息
     */
    String openAuthTokenAppQuery(String appAuthToken);

    String returnUrl(HttpServletRequest request);

    String certReturnUrl(HttpServletRequest request);

    String notifyUrl(HttpServletRequest request);

    String certNotifyUrl(HttpServletRequest request);
}
