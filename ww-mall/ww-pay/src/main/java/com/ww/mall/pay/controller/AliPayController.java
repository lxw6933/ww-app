package com.ww.mall.pay.controller;

import com.ijpay.alipay.AliPayApiConfig;
import com.ijpay.alipay.AliPayApiConfigKit;
import com.ww.mall.pay.service.AliPayService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ww
 * @create 2024-06-04- 18:26
 * @description:
 */
@RestController
@RequestMapping("/aliPay")
public class AliPayController {

    @Resource
    private AliPayService alipayService;

    @RequestMapping("/test")
    public AliPayApiConfig test() {
        return AliPayApiConfigKit.getAliPayApiConfig();
    }

    @RequestMapping(value = "/appPay")
    public String appPay() {
        return alipayService.appPay();
    }

    @RequestMapping(value = "/wapPay")
    public void wapPay(HttpServletResponse response) {
        alipayService.wapPay(response);
    }

    @RequestMapping(value = "/pcPay")
    public void pcPay(HttpServletResponse response) {
        alipayService.pcPay(response);
    }

    @RequestMapping(value = "/tradePreCreatePay")
    public java.lang.String tradePreCreatePay() {
        return alipayService.tradePreCreatePay();
    }

    @RequestMapping(value = "/tradeRefund")
    public java.lang.String tradeRefund(@RequestParam(required = false, name = "outTradeNo") java.lang.String outTradeNo, @RequestParam(required = false, name = "tradeNo") java.lang.String tradeNo) {
        return alipayService.refund(outTradeNo, tradeNo);
    }

    @RequestMapping(value = "/tradeQuery")
    public java.lang.String tradeQuery(@RequestParam(required = false, name = "outTradeNo") java.lang.String outTradeNo, @RequestParam(required = false, name = "tradeNo") java.lang.String tradeNo) {
        return alipayService.tradeQuery(outTradeNo, tradeNo);
    }

    @RequestMapping(value = "/toOauth")
    public void toOauth(HttpServletResponse response) {
        alipayService.toAuth(response);
    }

    @RequestMapping(value = "/getAuthToken")
    public java.lang.String getAuthToken(@RequestParam("app_id") java.lang.String appId, @RequestParam("app_auth_code") java.lang.String appAuthCode) {
        return alipayService.getAuthToken(appId, appAuthCode);
    }

    @RequestMapping(value = "/getAuthTokenUserInfo")
    public java.lang.String getAuthTokenUserInfo(@RequestParam("appAuthToken") java.lang.String appAuthToken) {
        return alipayService.getAuthTokenUserInfo(appAuthToken);
    }

    @RequestMapping(value = "/return_url")
    public java.lang.String returnUrl(HttpServletRequest request) {
        return alipayService.returnUrl(request);
    }

    @RequestMapping(value = "/notify_url")
    public java.lang.String notifyUrl(HttpServletRequest request) {
        return alipayService.notifyUrl(request);
    }

}
