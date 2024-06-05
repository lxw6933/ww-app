package com.ww.mall.pay.controller;

import com.ijpay.core.enums.SignType;
import com.ijpay.core.kit.WxPayKit;
import com.ijpay.wxpay.WxPayApi;
import com.ijpay.wxpay.WxPayApiConfig;
import com.ijpay.wxpay.WxPayApiConfigKit;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.pay.properties.WxPayProperties;
import com.ww.mall.pay.service.WxPayService;
import com.ww.mall.pay.vo.PayResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ww
 * @create 2024-06-05- 14:26
 * @description:
 */
@Slf4j
@RestController
@RequestMapping("/wxPay")
public class WxPayController {

    @Resource
    private WxPayProperties wxPayProperties;

    @Resource
    private WxPayService wxPayService;

    @RequestMapping(value = "/appPay")
    public PayResult appPay(HttpServletRequest request) {
        return wxPayService.appPay(request);
    }

    @RequestMapping(value = "/wapPay")
    public void wapPay(HttpServletRequest request, HttpServletResponse response) {
        wxPayService.wapPay(request, response);
    }

    @RequestMapping(value = "/webPay")
    public PayResult webPay(HttpServletRequest request, @RequestParam("total_fee") String totalFee) {
        return wxPayService.webPay(request, totalFee);
    }

    @RequestMapping(value = "/miniAppPay")
    public PayResult miniAppPay(HttpServletRequest request) {
        return wxPayService.miniAppPay(request);
    }

    /**
     * 刷卡支付
     */
    @RequestMapping(value = "/microPay")
    public PayResult microPay(HttpServletRequest request, HttpServletResponse response) {
        return wxPayService.microPay(request, response);
    }

    /**
     * 异步通知
     */
    @RequestMapping(value = "/payNotify")
    public String payNotify(HttpServletRequest request) {
        return wxPayService.payNotify(request);
    }

    /**
     * 扫码模式一
     */
    @RequestMapping(value = "/scanCode1")
    public PayResult scanCode1(HttpServletRequest request, HttpServletResponse response,
                               @RequestParam("productId") String productId) {
        return wxPayService.scanCode1(request, response, productId);
    }

    /**
     * 扫码支付模式二
     */
    @RequestMapping(value = "/scanCode2")
    public PayResult scanCode2(HttpServletRequest request, HttpServletResponse response,
                               @RequestParam("total_fee") String totalFee) {
        return wxPayService.scanCode2(request, response, totalFee);
    }

    /**
     * 扫码支付模式一回调
     */
    @RequestMapping(value = "/scanCodeNotify")
    public String scanCodeNotify(HttpServletRequest request, HttpServletResponse response) {
        return wxPayService.scanCodeNotify(request, response);
    }

    @RequestMapping(value = "/queryOrder")
    public String queryPayResult(@RequestParam(value = "transactionId", required = false) String transactionId,
                                 @RequestParam(value = "outTradeNo", required = false) String outTradeNo) {
        return wxPayService.queryPayResult(transactionId, outTradeNo);
    }

    /**
     * 微信退款
     */
    @RequestMapping(value = "/refund")
    public String refund(@RequestParam(value = "transactionId", required = false) String transactionId,
                         @RequestParam(value = "outTradeNo", required = false) String outTradeNo) {
        return wxPayService.refund(transactionId, outTradeNo);
    }

    /**
     * 退款通知
     */
    @RequestMapping(value = "/refundNotify")
    public String refundNotify(HttpServletRequest request) {
        return wxPayService.refundNotify(request);
    }

    /**
     * 微信退款查询
     */
    @RequestMapping(value = "/refundQuery")
    public String refundQuery(@RequestParam("transactionId") String transactionId,
                              @RequestParam("out_trade_no") String outTradeNo,
                              @RequestParam("out_refund_no") String outRefundNo,
                              @RequestParam("refund_id") String refundId) {
        return wxPayService.refundQuery(transactionId, outTradeNo, outRefundNo, refundId);
    }

    @RequestMapping(value = "/sendRedPack")
    public String sendRedPack(HttpServletRequest request, @RequestParam("openId") String openId) {
        return wxPayService.sendRedPack(request, openId);
    }

    @GetMapping("/getKey")
    public String getKey() {
        return WxPayApi.getSignKey(wxPayProperties.getMchId(), wxPayProperties.getPartnerKey(), SignType.MD5);
    }

    /**
     * 获取RSA加密公钥
     */
    @RequestMapping(value = "/getPublicKey")
    public String getPublicKey() {
        try {
            WxPayApiConfig wxPayApiConfig = WxPayApiConfigKit.getWxPayApiConfig();

            Map<String, String> params = new HashMap<>(4);
            params.put("mch_id", wxPayApiConfig.getMchId());
            params.put("nonce_str", String.valueOf(System.currentTimeMillis()));
            params.put("sign_type", "MD5");
            String createSign = WxPayKit.createSign(params, wxPayApiConfig.getPartnerKey(), SignType.MD5);
            params.put("sign", createSign);
            return WxPayApi.getPublicKey(params, wxPayApiConfig.getCertPath(), wxPayApiConfig.getMchId());
        } catch (Exception e) {
            throw new ApiException("获取加密公钥异常");
        }
    }

}
