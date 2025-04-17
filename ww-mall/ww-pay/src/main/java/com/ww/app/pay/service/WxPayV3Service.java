package com.ww.app.pay.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ww
 * @create 2024-06-05- 15:31
 * @description:
 */
public interface WxPayV3Service {

    /**
     * 获取商户v3证书序列号
     *
     * @return 序列号
     */
    String getSerialNumber();

    /**
     * 获取平台公钥证书序列号【接口包含感信息时使用】
     *
     * @return 平台公钥证书序列号
     */
    String getPlatSerialNumber();

    /**
     * 查询微信支付结果
     *
     * @param outTradeNo 外部交易号【系统】
     * @return 支付结果
     */
    String queryPayResult(String outTradeNo);

    /**
     * 微信app支付
     *
     * @return 支付结果
     */
    String appPay();

    /**
     * 微信jsApi支付
     *
     * @param openId 用户openId
     * @return 支付结果
     */
    String jsApiPay(String openId);

    /**
     * 微信h5支付
     *
     * @return 支付结果
     */
    String wapPay();

    /**
     * 微信申请退款
     *
     * @param transactionId 事务id
     * @param outTradeNo 商户交易单号
     * @return 申请退款结果
     */
    String refund(String transactionId, String outTradeNo);

    /**
     * 微信支付回调
     *
     * @param request 回调请求参数
     * @param response 回调响应参数
     */
    void payNotify(HttpServletRequest request, HttpServletResponse response);

    /**
     * 微信退款回调
     *
     * @param request 回调请求参数
     * @param response 回调响应参数
     */
    void refundNotify(HttpServletRequest request, HttpServletResponse response);

}
