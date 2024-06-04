package com.ww.mall.pay.controller;

import com.ijpay.alipay.AliPayApiConfig;
import com.ijpay.alipay.AliPayApiConfigKit;
import com.ww.mall.pay.service.AlipayService;
import com.ww.mall.pay.vo.PayResult;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/aliPay")
public class AliPayController {

    @Resource
    private AlipayService alipayService;

    @RequestMapping("/test")
    public AliPayApiConfig test() {
        return AliPayApiConfigKit.getAliPayApiConfig();
    }

    @RequestMapping(value = "/appPay")
    public PayResult appPay() {
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
    public String tradePreCreatePay() {
        return alipayService.tradePreCreatePay();
    }

    @RequestMapping(value = "/tradeRefund")
    public String tradeRefund(@RequestParam(required = false, name = "outTradeNo") String outTradeNo, @RequestParam(required = false, name = "tradeNo") String tradeNo) {
        return alipayService.refund(outTradeNo, tradeNo);
    }

    @RequestMapping(value = "/tradeQuery")
    public String tradeQuery(@RequestParam(required = false, name = "outTradeNo") String outTradeNo, @RequestParam(required = false, name = "tradeNo") String tradeNo) {
        return alipayService.tradeQuery(outTradeNo, tradeNo);
    }

    @RequestMapping(value = "/toOauth")
    public void toOauth(HttpServletResponse response) {
        alipayService.toAuth(response);
    }

    @RequestMapping(value = "/redirect_uri")
    public String redirectUri(@RequestParam("app_id") String appId, @RequestParam("app_auth_code") String appAuthCode) {
        return alipayService.redirectUri(appId, appAuthCode);
    }

    @RequestMapping(value = "/openAuthTokenAppQuery")
    public String openAuthTokenAppQuery(@RequestParam("appAuthToken") String appAuthToken) {
        return alipayService.openAuthTokenAppQuery(appAuthToken);
    }

    @RequestMapping(value = "/return_url")
    public String returnUrl(HttpServletRequest request) {
        return alipayService.returnUrl(request);
    }

    @RequestMapping(value = "/cert_return_url")
    public String certReturnUrl(HttpServletRequest request) {
       return alipayService.certReturnUrl(request);
    }

    @RequestMapping(value = "/notify_url")
    public String notifyUrl(HttpServletRequest request) {
        return alipayService.notifyUrl(request);
    }

    @RequestMapping(value = "/cert_notify_url")
    public String certNotifyUrl(HttpServletRequest request) {
        return alipayService.certNotifyUrl(request);
    }

}
