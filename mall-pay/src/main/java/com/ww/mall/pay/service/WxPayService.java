package com.ww.mall.pay.service;

import com.ijpay.wxpay.WxPayApiConfig;
import com.ww.mall.pay.vo.PayResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ww
 * @create 2024-06-05- 15:21
 * @description:
 */
public interface WxPayService {

    WxPayApiConfig getApiConfig();

    /**
     * app pay
     */
    PayResult appPay(HttpServletRequest request);

    /**
     * h5 pay
     * 注意：必须再web页面中发起支付且域名已添加到开发配置中
     */
    void wapPay(HttpServletRequest request, HttpServletResponse response);

    /**
     * 公众号支付
     */
    PayResult webPay(HttpServletRequest request, String totalFee);

    /**
     * 小程序支付
     */
    PayResult miniAppPay(HttpServletRequest request);

    /**
     * 刷卡支付
     */
    PayResult microPay(HttpServletRequest request, HttpServletResponse response);

    /**
     * 支付回调
     */
    String payNotify(HttpServletRequest request);

    /**
     * 扫码支付1
     */
    PayResult scanCode1(HttpServletRequest request, HttpServletResponse response, String productId);

    /**
     * 扫码支付2
     */
    PayResult scanCode2(HttpServletRequest request, HttpServletResponse response, String totalFee);

    /**
     * 扫码支付回调
     */
    String scanCodeNotify(HttpServletRequest request, HttpServletResponse response);

    /**
     * 支付结果查询
     */
    String queryPayResult(String transactionId, String outTradeNo);

    /**
     * 退款
     */
    String refund(String transactionId, String outTradeNo);

    /**
     * 退款回调
     */
    String refundNotify(HttpServletRequest request);

    /**
     * 退款查询
     */
    String refundQuery(String transactionId, String outTradeNo, String outRefundNo, String refundId);

    /**
     * 发红包
     */
    String sendRedPack(HttpServletRequest request, String openId);
}
